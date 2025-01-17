package org.thoughtcrime.securesms.groups.ui.managegroup;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.signal.core.util.ThreadUtil;
import org.thoughtcrime.securesms.ContactSelectionListFragment;
import org.thoughtcrime.securesms.DialogWithListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.Mp02CustomDialog;
import org.thoughtcrime.securesms.contacts.ContactsCursorLoader;
import org.thoughtcrime.securesms.database.GroupTable;
import org.thoughtcrime.securesms.database.MediaTable;
import org.thoughtcrime.securesms.database.MentionUtil;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.database.loaders.MediaLoader;
import org.thoughtcrime.securesms.database.loaders.ThreadMediaLoader;
import org.thoughtcrime.securesms.groups.GroupAccessControl;
import org.thoughtcrime.securesms.groups.GroupId;
import org.thoughtcrime.securesms.groups.LiveGroup;
import org.thoughtcrime.securesms.groups.SelectionLimits;
import org.thoughtcrime.securesms.groups.ui.GroupChangeFailureReason;
import org.thoughtcrime.securesms.groups.ui.GroupErrors;
import org.thoughtcrime.securesms.groups.ui.GroupLimitDialog;
import org.thoughtcrime.securesms.groups.ui.GroupMemberEntry;
import org.thoughtcrime.securesms.groups.ui.addmembers.AddMembersActivity;
import org.thoughtcrime.securesms.groups.ui.managegroup.dialogs.GroupMentionSettingDialog;
import org.thoughtcrime.securesms.groups.v2.GroupLinkUrlAndStatus;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.recipients.RecipientUtil;
import org.thoughtcrime.securesms.util.AsynchronousCallback;
import org.thoughtcrime.securesms.util.DefaultValueLiveData;
import org.thoughtcrime.securesms.util.ExpirationUtil;
import org.thoughtcrime.securesms.util.FeatureFlags;
import org.thoughtcrime.securesms.util.livedata.LiveDataUtil;
import org.thoughtcrime.securesms.util.livedata.Store;
import org.thoughtcrime.securesms.util.views.SimpleProgressDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.thoughtcrime.securesms.conversation.ConversationActivity.RECIPIENT_EXTRA;
import static org.thoughtcrime.securesms.conversation.ConversationActivity.STRING_CURRENT_EXPIRATION;

public class ManageGroupViewModel extends ViewModel {

  private static final int MAX_UNCOLLAPSED_MEMBERS = 6;
  private static final int SHOW_COLLAPSED_MEMBERS  = 5;

  private final Context                                     context;
  private final ManageGroupRepository                       manageGroupRepository;
  private final LiveData<String>                            title;
  private final Store<Description>                          descriptionStore;
  private final LiveData<Description>                       description;
  private final LiveData<Boolean>                           isAdmin;
  private final LiveData<Boolean>                           canEditGroupAttributes;
  private final LiveData<Boolean>                           canAddMembers;
  private final LiveData<List<GroupMemberEntry.FullMember>> members;
  private final LiveData<Integer>                           pendingMemberCount;
  private final LiveData<Integer>                           pendingAndRequestingCount;
  private final LiveData<String>                            disappearingMessageTimer;
  private final LiveData<String>                            memberCountSummary;
  private final LiveData<String>                            fullMemberCountSummary;
  private final LiveData<GroupAccessControl>                editMembershipRights;
  private final LiveData<GroupAccessControl>                editGroupAttributesRights;
  private final LiveData<Recipient>                         groupRecipient;
  private final MutableLiveData<GroupViewState>             groupViewState          = new MutableLiveData<>(null);
  private final LiveData<MuteState>                         muteState;
  private final LiveData<Boolean>                           hasCustomNotifications;
  private final LiveData<Boolean>                           canCollapseMemberList;
  private final DefaultValueLiveData<CollapseState>         memberListCollapseState = new DefaultValueLiveData<>(CollapseState.COLLAPSED);
  private final LiveData<Boolean>                           canLeaveGroup;
  private final LiveData<Boolean>                           canBlockGroup;
  private final LiveData<Boolean>                           canUnblockGroup;
  private final LiveData<Boolean>                           showLegacyIndicator;
  private final LiveData<String>                            mentionSetting;
  private final LiveData<Boolean>                           groupLinkOn;
  private final LiveData<GroupInfoMessage>                  groupInfoMessage;

  private ManageGroupViewModel(@NonNull Context context, @NonNull GroupId groupId, @NonNull ManageGroupRepository manageGroupRepository) {
    this.context               = context;
    this.manageGroupRepository = manageGroupRepository;

    manageGroupRepository.getGroupState(groupId, this::groupStateLoaded);

    LiveGroup liveGroup = new LiveGroup(groupId);

    this.title                     = Transformations.map(liveGroup.getTitle(),
                                                         title -> TextUtils.isEmpty(title) ? context.getString(R.string.Recipient_unknown)
                                                                                           : title);
    this.groupRecipient            = liveGroup.getGroupRecipient();
    this.isAdmin                   = liveGroup.isSelfAdmin();
    this.canCollapseMemberList     = LiveDataUtil.combineLatest(memberListCollapseState,
                                                                Transformations.map(liveGroup.getFullMembers(), m -> m.size() > MAX_UNCOLLAPSED_MEMBERS),
                                                                (state, hasEnoughMembers) -> state != CollapseState.OPEN && hasEnoughMembers);
    this.members                   = LiveDataUtil.combineLatest(liveGroup.getFullMembers(),
                                                                memberListCollapseState,
                                                                ManageGroupViewModel::filterMemberList);
    this.pendingMemberCount        = liveGroup.getPendingMemberCount();
    this.pendingAndRequestingCount = liveGroup.getPendingAndRequestingMemberCount();
    this.showLegacyIndicator       = Transformations.map(groupRecipient, recipient -> recipient.requireGroupId().isV1());
    this.memberCountSummary        = LiveDataUtil.combineLatest(liveGroup.getMembershipCountDescription(context.getResources()),
                                                                this.showLegacyIndicator,
                                                                (description, legacy) -> legacy ? String.format("%s · %s", description, context.getString(R.string.ManageGroupActivity_legacy_group))
                                                                                                : description);
    this.fullMemberCountSummary    = liveGroup.getFullMembershipCountDescription(context.getResources());
    this.editMembershipRights      = liveGroup.getMembershipAdditionAccessControl();
    this.editGroupAttributesRights = liveGroup.getAttributesAccessControl();
    this.disappearingMessageTimer  = Transformations.map(liveGroup.getExpireMessages(), expiration -> ExpirationUtil.getExpirationDisplayValue(context, expiration));
    this.canEditGroupAttributes    = liveGroup.selfCanEditGroupAttributes();
    this.canAddMembers             = liveGroup.selfCanAddMembers();
    this.muteState                 = Transformations.map(this.groupRecipient,
                                                         recipient -> new MuteState(recipient.getMuteUntil(), recipient.isMuted()));
    this.hasCustomNotifications    = LiveDataUtil.mapAsync(this.groupRecipient, manageGroupRepository::hasCustomNotifications);
    this.canLeaveGroup             = liveGroup.isActive();
    this.canBlockGroup             = Transformations.map(this.groupRecipient, recipient -> RecipientUtil.isBlockable(recipient) && !recipient.isBlocked());
    this.canUnblockGroup           = Transformations.map(this.groupRecipient, Recipient::isBlocked);
    this.mentionSetting            = Transformations.distinctUntilChanged(Transformations.map(this.groupRecipient,
                                                                                              recipient -> MentionUtil.getMentionSettingDisplayValue(context, recipient.getMentionSetting())));
    this.groupLinkOn               = Transformations.map(liveGroup.getGroupLink(), GroupLinkUrlAndStatus::isEnabled);
    this.groupInfoMessage          = Transformations.map(this.groupRecipient,
                                                         recipient -> {
                                                           boolean showLegacyInfo = recipient.requireGroupId().isV1();

                                                           if (showLegacyInfo && recipient.getParticipantIds().size() > FeatureFlags.groupLimits().getHardLimit()) {
                                                             return GroupInfoMessage.LEGACY_GROUP_TOO_LARGE;
                                                           } else if (showLegacyInfo) {
                                                             return GroupInfoMessage.LEGACY_GROUP_UPGRADE;
                                                           } else if (groupId.isMms()) {
                                                             return GroupInfoMessage.MMS_WARNING;
                                                           } else {
                                                             return GroupInfoMessage.NONE;
                                                           }
                                                         });

    this.descriptionStore = new Store<>(Description.NONE);
    this.description      = groupId.isV2() ? this.descriptionStore.getStateLiveData() : LiveDataUtil.empty();

    if (groupId.isV2()) {
      this.descriptionStore.update(liveGroup.getDescription(), (description, state) -> new Description(description, state.shouldLinkifyWebLinks, state.canEditDescription));
      this.descriptionStore.update(LiveDataUtil.mapAsync(groupRecipient, r -> RecipientUtil.isMessageRequestAccepted(context, r)), (linkify, state) -> new Description(state.description, linkify, state.canEditDescription));
      this.descriptionStore.update(this.canEditGroupAttributes, (canEdit, state) -> new Description(state.description, state.shouldLinkifyWebLinks, canEdit));
    }
  }

  @WorkerThread
  private void groupStateLoaded(@NonNull ManageGroupRepository.GroupStateResult groupStateResult) {
    groupViewState.postValue(new GroupViewState(groupStateResult.getThreadId(),
                                                groupStateResult.getRecipient(),
                                                () -> new ThreadMediaLoader(context, groupStateResult.getThreadId(), MediaLoader.MediaType.GALLERY, MediaTable.Sorting.Newest).getCursor()));
  }

  LiveData<List<GroupMemberEntry.FullMember>> getMembers() {
    return members;
  }

  LiveData<Integer> getPendingMemberCount() {
    return pendingMemberCount;
  }

  LiveData<Integer> getPendingAndRequestingCount() {
    return pendingAndRequestingCount;
  }

  LiveData<String> getMemberCountSummary() {
    return memberCountSummary;
  }

  LiveData<String> getFullMemberCountSummary() {
    return fullMemberCountSummary;
  }

  LiveData<Recipient> getGroupRecipient() {
    return groupRecipient;
  }

  LiveData<GroupViewState> getGroupViewState() {
    return groupViewState;
  }

  LiveData<String> getTitle() {
    return title;
  }

  LiveData<Description> getDescription() {
    return description;
  }

  LiveData<MuteState> getMuteState() {
    return muteState;
  }

  LiveData<GroupAccessControl> getMembershipRights() {
    return editMembershipRights;
  }

  LiveData<GroupAccessControl> getEditGroupAttributesRights() {
    return editGroupAttributesRights;
  }

  LiveData<Boolean> getIsAdmin() {
    return isAdmin;
  }

  LiveData<Boolean> getCanEditGroupAttributes() {
    return canEditGroupAttributes;
  }

  LiveData<Boolean> getCanAddMembers() {
    return canAddMembers;
  }

  LiveData<String> getDisappearingMessageTimer() {
    return disappearingMessageTimer;
  }

  LiveData<Boolean> hasCustomNotifications() {
    return hasCustomNotifications;
  }

  LiveData<Boolean> getCanCollapseMemberList() {
    return canCollapseMemberList;
  }

  LiveData<Boolean> getCanBlockGroup() {
    return canBlockGroup;
  }

  LiveData<Boolean> getCanUnblockGroup() {
    return canUnblockGroup;
  }

  LiveData<Boolean> getCanLeaveGroup() {
    return canLeaveGroup;
  }

  LiveData<String> getMentionSetting() {
    return mentionSetting;
  }

  LiveData<Boolean> getGroupLinkOn() {
    return groupLinkOn;
  }

  LiveData<GroupInfoMessage> getGroupInfoMessage() {
    return groupInfoMessage;
  }

  void handleExpirationSelection(Context context) {
    boolean activeGroup = isActiveGroup();

    if (isPushGroupConversation() && !activeGroup) {
      return;
    }

    //final long thread = this.threadId;
    Intent intent = new Intent(context, DialogWithListActivity.class);
    intent.putExtra(STRING_CURRENT_EXPIRATION, groupRecipient.getValue().getExpiresInSeconds());
    intent.putExtra(RECIPIENT_EXTRA, groupRecipient.getValue().getId());
    //intent.putExtra(THREAD_ID_EXTRA, threadId);
    context.startActivity(intent);
  }

  private boolean isActiveGroup() {
    if (!isGroupConversation()) return false;

    Optional<GroupTable.GroupRecord> record = SignalDatabase.groups().getGroup(Recipient.self().getId());
    return record.isPresent() && record.get().isActive();
  }

  private boolean isGroupConversation() {
    return Recipient.self() != null && Recipient.self().isGroup();
  }

  private boolean isPushGroupConversation() {
    return Recipient.self() != null && Recipient.self().isPushGroup();
  }

  void applyMembershipRightsChange(@NonNull GroupAccessControl newRights) {
    manageGroupRepository.applyMembershipRightsChange(getGroupId(), newRights, this::showErrorToast);
  }

  void applyAttributesRightsChange(@NonNull GroupAccessControl newRights) {
    manageGroupRepository.applyAttributesRightsChange(getGroupId(), newRights, this::showErrorToast);
  }

  void blockAndLeave(@NonNull FragmentActivity activity) {
    Mp02CustomDialog dialog = new Mp02CustomDialog(activity);
    if (groupRecipient.getValue().isGroup()) {
      if (SignalDatabase.groups().isActive(groupRecipient.getValue().requireGroupId())) {
        dialog.setMessage(activity.getString(R.string.BlockUnblockDialog_you_will_no_longer_receive_messages_or_updates));
        dialog.setPositiveListener(R.string.BlockUnblockDialog_block_and_leave, new Mp02CustomDialog.Mp02DialogKeyListener() {
          @Override
          public void onDialogKeyClicked() {
            onBlockAndLeaveConfirmed();
            dialog.dismiss();
          }
        });
        dialog.setNegativeListener(android.R.string.cancel, null);
      } else {
        dialog.setMessage(activity.getString(R.string.BlockUnblockDialog_group_members_wont_be_able_to_add_you));
        dialog.setPositiveListener(R.string.RecipientPreferenceActivity_block, new Mp02CustomDialog.Mp02DialogKeyListener() {
          @Override
          public void onDialogKeyClicked() {
            onBlockAndLeaveConfirmed();
            dialog.dismiss();
          }
        });
        dialog.setNegativeListener(android.R.string.cancel, null);
      }
    } else {
      dialog.setMessage(activity.getString(R.string.BlockUnblockDialog_blocked_people_wont_be_able_to_call_you_or_send_you_messages));
      dialog.setPositiveListener(R.string.BlockUnblockDialog_block, new Mp02CustomDialog.Mp02DialogKeyListener() {
        @Override
        public void onDialogKeyClicked() {
          onBlockAndLeaveConfirmed();
          dialog.dismiss();
        }
      });
      dialog.setNegativeListener(android.R.string.cancel, null);
    }
    dialog.show();
  }

  void unblock(@NonNull FragmentActivity activity) {
    Mp02CustomDialog dialog = new Mp02CustomDialog(activity);
    if (groupRecipient.getValue().isGroup()) {
      dialog.setMessage(activity.getString(R.string.BlockUnblockDialog_group_members_will_be_able_to_add_you));
      dialog.setPositiveListener(R.string.RecipientPreferenceActivity_unblock, new Mp02CustomDialog.Mp02DialogKeyListener() {
        @Override
        public void onDialogKeyClicked() {
          RecipientUtil.unblock(groupRecipient.getValue());
          dialog.dismiss();
        }
      });
      dialog.setNegativeListener(android.R.string.cancel, null);
    } else {
      dialog.setMessage(activity.getString(R.string.BlockUnblockDialog_you_will_be_able_to_call_and_message_each_other));
      dialog.setPositiveListener(R.string.RecipientPreferenceActivity_unblock, new Mp02CustomDialog.Mp02DialogKeyListener() {
        @Override
        public void onDialogKeyClicked() {
          RecipientUtil.unblock(groupRecipient.getValue());
          dialog.dismiss();
        }
      });
      dialog.setNegativeListener(android.R.string.cancel, null);
    }
    dialog.show();
  }

  void onAddMembers(@NonNull List<RecipientId> selected,
                    @NonNull AsynchronousCallback.MainThread<AddMembersResult, GroupChangeFailureReason> callback)
  {
    manageGroupRepository.addMembers(getGroupId(), selected, callback.toWorkerCallback());
  }

  void setMuteUntil(long muteUntil) {
    manageGroupRepository.setMuteUntil(getGroupId(), muteUntil);
  }

  void clearMuteUntil() {
    manageGroupRepository.setMuteUntil(getGroupId(), 0);
  }

  void revealCollapsedMembers() {
    memberListCollapseState.setValue(CollapseState.OPEN);
  }

  void handleMentionNotificationSelection() {
    manageGroupRepository.getRecipient(getGroupId(), r -> GroupMentionSettingDialog.show(context, r.getMentionSetting(), setting -> manageGroupRepository.setMentionSetting(getGroupId(), setting)));
  }

  private void onBlockAndLeaveConfirmed() {
    SimpleProgressDialog.DismissibleDialog dismissibleDialog = SimpleProgressDialog.showDelayed(context);

    manageGroupRepository.blockAndLeaveGroup(getGroupId(),
                                             e -> {
                                               dismissibleDialog.dismiss();
                                               showErrorToast(e);
                                             },
                                             dismissibleDialog::dismiss);
  }

  private @NonNull GroupId getGroupId() {
    return groupRecipient.getValue().requireGroupId();
  }

  private static @NonNull List<GroupMemberEntry.FullMember> filterMemberList(@NonNull List<GroupMemberEntry.FullMember> members,
                                                                             @NonNull CollapseState collapseState)
  {
    if (collapseState == CollapseState.COLLAPSED && members.size() > MAX_UNCOLLAPSED_MEMBERS) {
      return members.subList(0, SHOW_COLLAPSED_MEMBERS);
    } else {
      return members;
    }
  }

  @WorkerThread
  private void showErrorToast(@NonNull GroupChangeFailureReason e) {
    ThreadUtil.runOnMain(() -> Toast.makeText(context, GroupErrors.getUserDisplayMessage(e), Toast.LENGTH_LONG).show());
  }

  public void onAddMembersClick(@NonNull Fragment fragment, int resultCode) {
    manageGroupRepository.getGroupCapacity(getGroupId(), capacity -> {
      int remainingCapacity = capacity.getRemainingCapacity();
      if (remainingCapacity <= 0) {
        GroupLimitDialog.showHardLimitMessage(fragment.requireContext());
      } else {
        Intent intent = new Intent(fragment.requireActivity(), AddMembersActivity.class);
        intent.putExtra(AddMembersActivity.GROUP_ID, getGroupId().toString());
        intent.putExtra(ContactSelectionListFragment.DISPLAY_MODE, ContactsCursorLoader.DisplayMode.FLAG_PUSH);
        intent.putExtra(ContactSelectionListFragment.SELECTION_LIMITS, new SelectionLimits(capacity.getSelectionWarning(), capacity.getSelectionLimit()));
        intent.putParcelableArrayListExtra(ContactSelectionListFragment.CURRENT_SELECTION, new ArrayList<>(capacity.getMembersWithoutSelf()));
        fragment.startActivityForResult(intent, resultCode);
      }
    });
  }

  public void onShowAllMembersClick(@NonNull Fragment fragment,View view){
    revealCollapsedMembers();
    view.setVisibility(View.GONE);
  }

  static final class AddMembersResult {
    private final int             numberOfMembersAdded;
    private final List<Recipient> newInvitedMembers;

    AddMembersResult(int numberOfMembersAdded, @NonNull List<Recipient> newInvitedMembers) {
      this.numberOfMembersAdded = numberOfMembersAdded;
      this.newInvitedMembers    = newInvitedMembers;
    }

    int getNumberOfMembersAdded() {
      return numberOfMembersAdded;
    }

    List<Recipient> getNewInvitedMembers() {
      return newInvitedMembers;
    }
  }

  static final class GroupViewState {
             private final long          threadId;
    @NonNull private final Recipient     groupRecipient;
    @NonNull private final CursorFactory mediaCursorFactory;

    private GroupViewState(long threadId,
                           @NonNull Recipient groupRecipient,
                           @NonNull CursorFactory mediaCursorFactory)
    {
      this.threadId           = threadId;
      this.groupRecipient     = groupRecipient;
      this.mediaCursorFactory = mediaCursorFactory;
    }

    long getThreadId() {
      return threadId;
    }

    @NonNull Recipient getGroupRecipient() {
      return groupRecipient;
    }

    @NonNull CursorFactory getMediaCursorFactory() {
      return mediaCursorFactory;
    }
  }

  static final class MuteState {
    private final long    mutedUntil;
    private final boolean isMuted;

    MuteState(long mutedUntil, boolean isMuted) {
      this.mutedUntil = mutedUntil;
      this.isMuted    = isMuted;
    }

    public long getMutedUntil() {
      return mutedUntil;
    }

    public boolean isMuted() {
      return isMuted;
    }
  }

  enum GroupInfoMessage {
    NONE,
    LEGACY_GROUP_LEARN_MORE,
    LEGACY_GROUP_UPGRADE,
    LEGACY_GROUP_TOO_LARGE,
    MMS_WARNING
  }

  private enum CollapseState {
    OPEN,
    COLLAPSED
  }

  interface CursorFactory {
    Cursor create();
  }

  public static class Description {
    private static final Description NONE = new Description("", false, false);

    private final String  description;
    private final boolean shouldLinkifyWebLinks;
    private final boolean canEditDescription;

    public Description(String description, boolean shouldLinkifyWebLinks, boolean canEditDescription) {
      this.description           = description;
      this.shouldLinkifyWebLinks = shouldLinkifyWebLinks;
      this.canEditDescription    = canEditDescription;
    }

    public @NonNull String getDescription() {
      return description;
    }

    public boolean shouldLinkifyWebLinks() {
      return shouldLinkifyWebLinks;
    }

    public boolean canEditDescription() {
      return canEditDescription;
    }
  }

  public static class Factory implements ViewModelProvider.Factory {
    private final Context context;
    private final GroupId groupId;

    public Factory(@NonNull Context context, @NonNull GroupId groupId) {
      this.context = context;
      this.groupId = groupId;
    }

    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      //noinspection unchecked
      return (T) new ManageGroupViewModel(context, groupId, new ManageGroupRepository(context.getApplicationContext()));
    }
  }
}
