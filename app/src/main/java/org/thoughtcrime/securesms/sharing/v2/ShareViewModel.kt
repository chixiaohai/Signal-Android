package org.thoughtcrime.securesms.sharing.v2

import android.content.Context
import android.net.Uri
import androidx.arch.core.util.Function
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.annimon.stream.Stream
import io.reactivex.rxjava3.core.Single
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.database.GroupTable
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.thoughtcrime.securesms.providers.BlobProvider
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.sharing.ShareContact
import org.thoughtcrime.securesms.sharing.ShareData
import org.thoughtcrime.securesms.sharing.ShareSelectionMappingModel
import org.thoughtcrime.securesms.util.DefaultValueLiveData
import org.thoughtcrime.securesms.util.adapter.mapping.MappingModelList
import java.util.Optional

class ShareViewModel private constructor() : ViewModel() {
  private val context: Context
  private val shareRepository: ShareRepository
  private val shareData: MutableLiveData<Optional<ShareData>?>
  private val selectedContacts: MutableLiveData<Set<ShareContact>>
  private val smsShareRestriction: LiveData<SmsShareRestriction>
  private var mediaUsed = false
  var isExternalShare = false
    private set

  fun onSingleMediaShared(uri: Uri, mimeType: String?) {
    isExternalShare = true
    shareRepository.getResolved(uri, mimeType, shareData::postValue)
  }

  fun onMultipleMediaShared(uris: List<Uri?>) {
    isExternalShare = true
    shareRepository.getResolved(uris, shareData::postValue)
  }

  val isMultiShare: Boolean
    get() = selectedContacts.value!!.size > 1

  fun onContactSelected(selectedContact: ShareContact): Single<ContactSelectResult> {
    return Single.fromCallable {
      if (selectedContact.recipientId.isPresent) {
        val recipient = Recipient.resolved(selectedContact.recipientId.get())
        if (recipient.isPushV2Group) {
          val record: Optional<GroupTable.GroupRecord> = SignalDatabase.groups.getGroup(recipient.requireGroupId())
          if (record.isPresent && record.get().isAnnouncementGroup && !record.get().isAdmin(Recipient.self())) {
            return@fromCallable ContactSelectResult.FALSE_AND_SHOW_PERMISSION_TOAST
          }
        }
      }
      val contacts: MutableSet<ShareContact> = LinkedHashSet(selectedContacts.value)
      if (contacts.add(selectedContact)) {
        selectedContacts.postValue(contacts)
        return@fromCallable ContactSelectResult.TRUE
      } else {
        return@fromCallable ContactSelectResult.FALSE
      }
    }
  }

  fun onContactDeselected(selectedContact: ShareContact) {
    val contacts: MutableSet<ShareContact> = LinkedHashSet(selectedContacts.value)
    if (contacts.remove(selectedContact)) {
      selectedContacts.value = contacts
    }
  }

  val shareContacts: Set<ShareContact>
    get() {
      val contacts = selectedContacts.value!!
      return contacts ?: emptySet()
    }
  val selectedContactModels: LiveData<Any>
    get() = Transformations.map(selectedContacts, Function { set: Set<ShareContact>? ->
      Stream.of(set)
        .mapIndexed { i: Int, c: ShareContact? -> ShareSelectionMappingModel(c!!, i == 0) }
        .collect(MappingModelList.toMappingModelList())
    })

  fun getSmsShareRestriction(): LiveData<SmsShareRestriction> {
    return Transformations.distinctUntilChanged(smsShareRestriction)
  }

  fun onNonExternalShare() {
    shareData.value = Optional.empty()
    isExternalShare = false
  }

  fun onSuccessfulShare() {
    mediaUsed = true
  }

  fun getShareData(): LiveData<Optional<ShareData>?> {
    return shareData
  }

  override fun onCleared() {
    val data: ShareData? = if (shareData.value != null) shareData.value!!.orElse(null) else null
    if (data != null && data.isExternal && data.isForIntent && !mediaUsed) {
      Log.i(TAG, "Clearing out unused data.")
      BlobProvider.getInstance().delete(context, data.uri)
    }
  }

  private fun updateShareRestriction(shareContacts: Set<ShareContact>): SmsShareRestriction {
    return if (shareContacts.isEmpty()) {
      SmsShareRestriction.NO_RESTRICTIONS
    } else if (shareContacts.size == 1) {
      val shareContact = shareContacts.iterator().next()
      if (shareContact.recipientId.isPresent) {
        val recipient = Recipient.live(shareContact.recipientId.get()).get()
        if (!recipient.isRegistered || recipient.isForceSmsSelection) {
          SmsShareRestriction.DISALLOW_MULTI_SHARE
        } else {
          SmsShareRestriction.DISALLOW_SMS_CONTACTS
        }
      } else {
        SmsShareRestriction.DISALLOW_MULTI_SHARE
      }
    } else {
      SmsShareRestriction.DISALLOW_SMS_CONTACTS
    }
  }

  enum class ContactSelectResult {
    TRUE, FALSE, FALSE_AND_SHOW_PERMISSION_TOAST
  }

  enum class SmsShareRestriction {
    NO_RESTRICTIONS, DISALLOW_SMS_CONTACTS, DISALLOW_MULTI_SHARE
  }

  class Factory : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(ShareViewModel())
    }
  }

  companion object {
    private val TAG = Log.tag(ShareViewModel::class.java)
  }

  init {
    context = ApplicationDependencies.getApplication()
    shareRepository = ShareRepository()
    shareData = MutableLiveData<Optional<ShareData>?>()
    selectedContacts = DefaultValueLiveData(emptySet())
    smsShareRestriction = Transformations.map(selectedContacts) { shareContacts: Set<ShareContact> -> updateShareRestriction(shareContacts) }
  }
}