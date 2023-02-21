package org.thoughtcrime.securesms.recipients.ui.managerecipient;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;

import com.annimon.stream.Stream;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.database.GroupTable;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.database.ThreadTable;
import org.thoughtcrime.securesms.database.model.IdentityRecord;
import org.thoughtcrime.securesms.notifications.NotificationChannels;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientId;

import java.util.ArrayList;
import java.util.List;

final class ManageRecipientRepository {

  private static final String TAG = Log.tag(ManageRecipientRepository.class);

  private final Context     context;
  private final RecipientId recipientId;

  ManageRecipientRepository(@NonNull Context context, @NonNull RecipientId recipientId) {
    this.context     = context;
    this.recipientId = recipientId;
  }

  public RecipientId getRecipientId() {
    return recipientId;
  }

  void getThreadId(@NonNull Consumer<Long> onGetThreadId) {
    SignalExecutors.BOUNDED.execute(() -> onGetThreadId.accept(getThreadId()));
  }

  @WorkerThread
  private long getThreadId() {
    ThreadTable threadDatabase = SignalDatabase.threads();
    Recipient   groupRecipient = Recipient.resolved(recipientId);

    return threadDatabase.getOrCreateThreadIdFor(groupRecipient);
  }

//  void getIdentity(@NonNull Consumer<IdentityRecord> callback) {
//    SignalExecutors.BOUNDED.execute(() -> callback.accept(SignalDatabase.identities()
//                                                                        .getIdentity(recipientId)
//                                                                        .orElse(null)));
//  }

  void getGroupMembership(@NonNull Consumer<List<RecipientId>> onComplete) {
    SignalExecutors.BOUNDED.execute(() -> {
      GroupTable                   groupDatabase   = SignalDatabase.groups();
      List<GroupTable.GroupRecord> groupRecords    = groupDatabase.getPushGroupsContainingMember(recipientId);
      ArrayList<RecipientId>       groupRecipients = new ArrayList<>(groupRecords.size());

      for (GroupTable.GroupRecord groupRecord : groupRecords) {
        groupRecipients.add(groupRecord.getRecipientId());
      }

      onComplete.accept(groupRecipients);
    });
  }

  public void getRecipient(@NonNull Consumer<Recipient> recipientCallback) {
    SignalExecutors.BOUNDED.execute(() -> recipientCallback.accept(Recipient.resolved(recipientId)));
  }

  void setMuteUntil(long until) {
    SignalExecutors.BOUNDED.execute(() -> SignalDatabase.recipients().setMuted(recipientId, until));
  }

  void refreshRecipient() {
    SignalExecutors.UNBOUNDED.execute(() -> {
//      try {
//        DirectoryHelper.refreshDirectoryFor(context, Recipient.resolved(recipientId), false);
//      } catch (IOException e) {
        Log.w(TAG, "Failed to refresh user after adding to contacts.");
//      }
    });
  }

  @WorkerThread
  @NonNull List<Recipient> getSharedGroups(@NonNull RecipientId recipientId) {
    return Stream.of(SignalDatabase.groups()
                                   .getPushGroupsContainingMember(recipientId))
                 .filter(g -> g.getMembers().contains(Recipient.self().getId()))
                 .map(GroupTable.GroupRecord::getRecipientId)
                 .map(Recipient::resolved)
                 .sortBy(gr -> gr.getDisplayName(context))
                 .toList();
  }

  void getActiveGroupCount(@NonNull Consumer<Integer> onComplete) {
    SignalExecutors.BOUNDED.execute(() -> onComplete.accept(SignalDatabase.groups().getActiveGroupCount()));
  }

  @WorkerThread
  boolean hasCustomNotifications(Recipient recipient) {
    if (recipient.getNotificationChannel() != null || !NotificationChannels.supported()) {
      return true;
    }

    return NotificationChannels.updateWithShortcutBasedChannel(context, recipient);
  }
}
