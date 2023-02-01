/*
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.conversationlist;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.WorkerThread;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.SimpleColorFilter;
import com.annimon.stream.Stream;
import com.google.android.material.animation.ArgbEvaluatorCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.signal.core.util.DimensionUnit;
import org.signal.core.util.Stopwatch;
import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.concurrent.SimpleTask;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.MainFragment;
import org.thoughtcrime.securesms.MainNavigator;
import org.thoughtcrime.securesms.MuteDialog;
import org.thoughtcrime.securesms.NewConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.RatingManager;
import org.thoughtcrime.securesms.components.UnreadPaymentsView;
import org.thoughtcrime.securesms.components.menu.SignalBottomActionBar;
import org.thoughtcrime.securesms.components.menu.SignalContextMenu;
import org.thoughtcrime.securesms.components.registration.PulsingFloatingActionButton;
import org.thoughtcrime.securesms.components.reminder.CdsPermanentErrorReminder;
import org.thoughtcrime.securesms.components.reminder.CdsTemporyErrorReminder;
import org.thoughtcrime.securesms.components.reminder.DozeReminder;
import org.thoughtcrime.securesms.components.reminder.ExpiredBuildReminder;
import org.thoughtcrime.securesms.components.reminder.OutdatedBuildReminder;
import org.thoughtcrime.securesms.components.reminder.PushRegistrationReminder;
import org.thoughtcrime.securesms.components.reminder.Reminder;
import org.thoughtcrime.securesms.components.reminder.ReminderView;
import org.thoughtcrime.securesms.components.reminder.ServiceOutageReminder;
import org.thoughtcrime.securesms.components.reminder.UnauthorizedReminder;
//import org.thoughtcrime.securesms.components.settings.app.notifications.manual.NotificationProfileSelectionFragment;
import org.thoughtcrime.securesms.components.voice.VoiceNoteMediaControllerOwner;
import org.thoughtcrime.securesms.components.voice.VoiceNotePlayerView;
import org.thoughtcrime.securesms.contacts.sync.CdsPermanentErrorBottomSheet;
import org.thoughtcrime.securesms.contacts.sync.CdsTemporaryErrorBottomSheet;
import org.thoughtcrime.securesms.conversation.ConversationFragment;
import org.thoughtcrime.securesms.conversationlist.model.Conversation;
import org.thoughtcrime.securesms.database.MessageTable.MarkedMessageInfo;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.database.ThreadTable;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.events.ReminderUpdateEvent;
import org.thoughtcrime.securesms.insights.InsightsLauncher;
import org.thoughtcrime.securesms.jobs.ServiceOutageDetectionJob;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
//import org.thoughtcrime.securesms.main.Material3OnScrollHelperBinder;
//import org.thoughtcrime.securesms.main.SearchBinder;
import org.thoughtcrime.securesms.megaphone.Megaphones;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.notifications.MarkReadReceiver;
import org.thoughtcrime.securesms.notifications.profiles.NotificationProfile;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.ratelimit.RecaptchaProofBottomSheetFragment;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.service.KeyCachingService;
import org.thoughtcrime.securesms.sms.MessageSender;
import org.thoughtcrime.securesms.storage.StorageSyncHelper;
//import org.thoughtcrime.securesms.stories.tabs.ConversationListTabsViewModel;
import org.thoughtcrime.securesms.util.AppForegroundObserver;
import org.thoughtcrime.securesms.util.AppStartup;
import org.thoughtcrime.securesms.util.CommunicationActions;
import org.thoughtcrime.securesms.util.ConversationUtil;
import org.thoughtcrime.securesms.util.FeatureFlags;
import org.thoughtcrime.securesms.util.LifecycleDisposable;
import org.thoughtcrime.securesms.util.PlayStoreUtil;
import org.thoughtcrime.securesms.util.SignalLocalMetrics;
import org.thoughtcrime.securesms.util.SnapToTopDataObserver;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.task.SnackbarAsyncTask;
import org.thoughtcrime.securesms.util.views.SimpleProgressDialog;
import org.thoughtcrime.securesms.util.views.Stub;
import org.whispersystems.signalservice.api.websocket.WebSocketConnectionState;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static android.app.Activity.RESULT_OK;


public class ConversationListFragment extends MainFragment implements ConversationListAdapter.OnConversationClickListener,
                                                                      ConversationListAdapter.OnClearFilterClickListener,
                                                                      MainNavigator.BackHandler, View.OnKeyListener
{
  public static final short MESSAGE_REQUESTS_REQUEST_CODE_CREATE_NAME = 32562;
  public static final short SMS_ROLE_REQUEST_CODE                     = 32563;

  private static final int LIST_SMOOTH_SCROLL_TO_TOP_THRESHOLD = 25;

  private static final String TAG = Log.tag(ConversationListFragment.class);

  private static final int MAXIMUM_PINNED_CONVERSATIONS = 4;
  public static int longClickItemPosition = -1;

  private RecyclerView                   list;
  private ConversationListViewModel      viewModel;
  private RecyclerView.Adapter           activeAdapter;
  private ConversationListAdapter        defaultAdapter;
  private SnapToTopDataObserver          snapToTopDataObserver;
  private Drawable                       archiveDrawable;

  protected ConversationListArchiveItemDecoration archiveDecoration;
  protected ConversationListItemAnimator          itemAnimator;
  private   Stopwatch                             startupStopwatch;

  private boolean isFromLauncher = false;
  private boolean isFirstEnter = false;

  public static ConversationListFragment newInstance() {
    return new ConversationListFragment();
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    Intent intent = getActivity().getIntent();
    isFromLauncher = intent.getBooleanExtra("fromLauncher",false);
    startupStopwatch = new Stopwatch("startup");
    isFirstEnter = true;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    return inflater.inflate(R.layout.conversation_list_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    list                    = view.findViewById(R.id.list);
//    constraintLayout        = view.findViewById(R.id.constraint_layout);
    archiveDecoration = new ConversationListArchiveItemDecoration(new ColorDrawable(getResources().getColor(R.color.conversation_list_archive_background_end)));

    list.setLayoutManager(new LinearLayoutManager(requireActivity()));
    list.setItemAnimator(new ConversationListItemAnimator());
    list.setClipToPadding(false);
    list.setClipChildren(false);
    list.setPadding(0, 76, 0, 200);
    list.addItemDecoration(archiveDecoration);

    snapToTopDataObserver = new SnapToTopDataObserver(list);

    new ItemTouchHelper(new ArchiveListenerCallback(getResources().getColor(R.color.conversation_list_archive_background_start),
                                                    getResources().getColor(R.color.conversation_list_archive_background_end))).attachToRecyclerView(list);

    initializeViewModel();
    initializeListAdapters();
    initializeTypingObserver();

    if (isFromLauncher){
      list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver
          .OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          list.post(new Runnable() {
            @Override
            public void run() {
              handleCreateConversation(getDefaultAdapter().getConversation().getThreadRecord().getThreadId(),getDefaultAdapter().getConversation().getThreadRecord().getRecipient(),getDefaultAdapter().getConversation().getThreadRecord().getDistributionType());
            }
          });
          defaultAdapter.setFromLauncher(true);
          isFromLauncher = false;
          list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
      });
    }

    RatingManager.showRatingDialogIfNecessary(requireContext());
  }

  public ConversationListAdapter getDefaultAdapter(){
    return defaultAdapter;
  }

  @Override
  public void onResume() {
    super.onResume();
    //Add for MP02 Notification
    Intent intent = new Intent();
    intent.setAction("clear.notification.from.signal");
    requireActivity().sendBroadcast(intent);

    EventBus.getDefault().register(this);

    if (Util.isDefaultSmsProvider(requireContext())) {
      InsightsLauncher.showInsightsModal(requireContext(), requireFragmentManager());
    }

    if (list.getAdapter() != defaultAdapter) {
      setAdapter(defaultAdapter);
    }

//    if (activeAdapter != null) {
//      activeAdapter.notifyDataSetChanged();
//    }

    if (SignalStore.rateLimit().needsRecaptcha()) {
      Log.i(TAG, "Recaptcha required.");
      RecaptchaProofBottomSheetFragment.show(getChildFragmentManager());
    }
    if(getNavigator().getFromSearch()) {
      getNavigator().setFromSearch(false);
      list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver
          .OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          View view = list.getLayoutManager().findViewByPosition(4); //Search
          if (view != null) {
            view.requestFocus();
          }
          list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
      });
    }

    if(getNavigator().getFromArchive()) {
      getNavigator().setFromArchive(false);
      list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver
          .OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          View view = list.getLayoutManager().findViewByPosition(list.getLayoutManager().getItemCount() - 1);//Archive
          if (view != null) {
            view.requestFocus();
          }
          list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
      });
    }

    if(getNavigator().getFromOptions()) {
      getNavigator().setFromOptions(false);
      list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver
          .OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          View view = list.getLayoutManager().findViewByPosition(longClickItemPosition);
          if (view != null) {
            view.requestFocus();
            longClickItemPosition = -1;
          }else{
            View view1 = list.getLayoutManager().findViewByPosition(list.getLayoutManager().getItemCount() - 1);//Archive
            if (view1 != null) {
              view1.requestFocus();
            }
          }
          list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
      });
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    ConversationFragment.prepare(requireContext());
  }

  @Override
  public void onPause() {
    super.onPause();

    EventBus.getDefault().unregister(this);
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    menu.clear();
    inflater.inflate(R.menu.text_secure_normal, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.menu_insights).setVisible(Util.isDefaultSmsProvider(requireContext()));
    menu.findItem(R.id.menu_clear_passphrase).setVisible(!TextSecurePreferences.isPasswordDisabled(requireContext()));
    menu.findItem(R.id.menu_filter_unread_chats).setVisible(FeatureFlags.chatFilters());
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case R.id.menu_new_group:
        handleCreateGroup(); return true;
      case R.id.menu_settings:
        handleDisplaySettings(); return true;
      case R.id.menu_clear_passphrase:
        handleClearPassphrase(); return true;
      case R.id.menu_mark_all_read:
        handleMarkAllRead(); return true;
      case R.id.menu_invite:
        handleInvite(); return true;
      case R.id.menu_insights:
        handleInsights(); return true;
//      case R.id.menu_notification_profile:
//        handleNotificationProfile(); return true;
      case R.id.menu_filter_unread_chats:
        handleFilterUnreadChats(); return true;
    }

    return false;
  }

  private boolean closeSearchIfOpen() {
    return false;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (resultCode != RESULT_OK) {
      return;
    }
  }

  @Override
  public void onShowArchiveClick() {
    getNavigator().goToArchiveList();
  }



  private void initializeListAdapters() {
    defaultAdapter          = new ConversationListAdapter(getViewLifecycleOwner(), GlideApp.with(this), this,this);

    defaultAdapter.setData(getDatas());
    defaultAdapter.setHasDivider(true, getDatas().size() - 1);
    setAdapter(defaultAdapter);

    defaultAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) {
        startupStopwatch.split("data-set");
        SignalLocalMetrics.ColdStart.onConversationListDataLoaded();
        defaultAdapter.unregisterAdapterDataObserver(this);
        list.post(() -> {
          AppStartup.getInstance().onCriticalRenderEventEnd();
          startupStopwatch.split("first-render");
          startupStopwatch.stop(TAG);
        });
      }
    });
  }
  private List<String> getDatas() {
    List<String> datas = Arrays.asList(getResources().getStringArray(R.array.conversation_list_item_menu));
    return datas;
  }

  @SuppressWarnings("rawtypes")
  private void setAdapter(@NonNull RecyclerView.Adapter adapter) {
    RecyclerView.Adapter oldAdapter = activeAdapter;

    activeAdapter = adapter;

    if (oldAdapter == activeAdapter) {
      return;
    }

    if (adapter instanceof ConversationListAdapter) {
      viewModel.getPagingController()
               .observe(getViewLifecycleOwner(),
                        controller -> ((ConversationListAdapter) adapter).setPagingController(controller));
    }

    list.setAdapter(adapter);

    if (adapter == defaultAdapter) {
      defaultAdapter.registerAdapterDataObserver(snapToTopDataObserver);
    } else {
      defaultAdapter.unregisterAdapterDataObserver(snapToTopDataObserver);
    }
  }

  private void initializeTypingObserver() {
    ApplicationDependencies.getTypingStatusRepository().getTypingThreads().observe(getViewLifecycleOwner(), threadIds -> {
      if (threadIds == null) {
        threadIds = Collections.emptySet();
      }

      defaultAdapter.setTypingThreads(threadIds);
    });
  }

  protected boolean isArchived() {
    return false;
  }

  private void initializeViewModel() {
    ConversationListViewModel.Factory viewModelFactory = new ConversationListViewModel.Factory(isArchived(),
                                                                                               getString(R.string.note_to_self));

    viewModel = new ViewModelProvider(this, (ViewModelProvider.Factory) viewModelFactory).get(ConversationListViewModel.class);

    viewModel.getConversationList().observe(getViewLifecycleOwner(), this::onSubmitList);
  }

  private void handleCreateGroup() {
    getNavigator().goToGroupCreation();
  }

  private void handleDisplaySettings() {
    getNavigator().goToAppSettings();
  }

  private void handleClearPassphrase() {
    Intent intent = new Intent(requireActivity(), KeyCachingService.class);
    intent.setAction(KeyCachingService.CLEAR_KEY_ACTION);
    requireActivity().startService(intent);
  }

  private void handleMarkAllRead() {
    Context context = requireContext();

    SignalExecutors.BOUNDED.execute(() -> {
      List<MarkedMessageInfo> messageIds = SignalDatabase.threads().setAllThreadsRead();

      ApplicationDependencies.getMessageNotifier().updateNotification(context);
      MarkReadReceiver.process(context, messageIds);
    });
  }

  private void handleInvite() {
    getNavigator().goToInvite();
  }

  private void handleInsights() {
    getNavigator().goToInsights();
  }

//  private void handleNotificationProfile() {
//    NotificationProfileSelectionFragment.show(getParentFragmentManager());
//  }

  private void handleFilterUnreadChats() {
    viewModel.toggleUnreadChatsFilter();
  }

  private void handleCreateConversation(long threadId, Recipient recipient, int distributionType) {
    getNavigator().goToConversation(recipient.getId(), threadId, distributionType, -1);
  }


  private void onSubmitList(@NonNull List<Conversation> conversationList) {
    List<Conversation> myList = new ArrayList<>(conversationList);
    if (defaultAdapter.getDataSize() > 0) {
      for (int position = 0; position < defaultAdapter.getDataSize(); position++) {
        myList.add(position, new Conversation());
      }
    }
    defaultAdapter.submitList(myList);
    if (isFirstEnter) {
      defaultAdapter.notifyDataSetChanged();
      isFirstEnter = false;
    }
    onPostSubmitList(conversationList.size());
  }

  protected void onPostSubmitList(int conversationCount) {
    if (conversationCount >= 6 && (SignalStore.onboarding().shouldShowInviteFriends() || SignalStore.onboarding().shouldShowNewGroup())) {
      SignalStore.onboarding().clearAll();
      ApplicationDependencies.getMegaphoneRepository().markFinished(Megaphones.Event.ONBOARDING);
    }
  }

  @Override
  public void onConversationClick(ConversationListItem item, Conversation conversation) {
    int viewType = item.getViewType();
    if (viewType == ConversationListAdapter.MENU_OPTIONS_TYPE) {
      String menuName = item.getFromText();
      List<String> data = getDatas();
      if (menuName.equals(data.get(0))) { //New Conversaton
        System.out.println("lyh   click  New Conversaton");
        startActivity(new Intent(getActivity(), NewConversationActivity.class));
      } else if (menuName.equals(data.get(1))) { //New Group
        getNavigator().goToGroupCreation();
      } else if (menuName.equals(data.get(2))) {// Mark all read
        handleMarkAllRead();
      } else if (menuName.equals(data.get(3))) {// Setting
        getNavigator().goToAppSettings();
      } else if (menuName.equals(data.get(4))) {//Search
        getNavigator().goToSearch();
      }
    } else {
      handleCreateConversation(conversation.getThreadRecord().getThreadId(), conversation.getThreadRecord().getRecipient(), conversation.getThreadRecord().getDistributionType());
    }
  }

  @Override
  public boolean onConversationLongClick(ConversationListItem item) {
    Set<Long> batchSet = Collections.synchronizedSet(new HashSet<Long>());
    batchSet.add(item.getThreadId());
    getNavigator().setCurrentConversation(item.getThreadId(), batchSet);
    getNavigator().setCurrentConversation(item.getThreadId(), batchSet);
    getNavigator().goToOptionsList();
    return true;
  }


  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onEvent(ReminderUpdateEvent event) {

  }

  @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
  public void onEvent(MessageSender.MessageSentEvent event) {
    EventBus.getDefault().removeStickyEvent(event);
    closeSearchIfOpen();
  }

//  protected Callback requireCallback() {
//    return ((Callback) getParentFragment());
//  }

//  protected Toolbar getToolbar(@NonNull View rootView) {
//    return requireCallback().getToolbar();
//  }

  protected @PluralsRes int getArchivedSnackbarTitleRes() {
    return R.plurals.ConversationListFragment_conversations_archived;
  }

  protected @DrawableRes int getArchiveIconRes() {
    return R.drawable.ic_archive_24;
  }

  @WorkerThread
  protected void archiveThreads(Set<Long> threadIds) {
    SignalDatabase.threads().setArchived(threadIds, true);
  }

  @WorkerThread
  protected void reverseArchiveThreads(Set<Long> threadIds) {
    SignalDatabase.threads().setArchived(threadIds, false);
  }

  @SuppressLint("StaticFieldLeak")
  protected void onItemSwiped(long threadId, int unreadCount, int unreadSelfMentionsCount) {
    archiveDecoration.onArchiveStarted();

    new SnackbarAsyncTask<Long>(getViewLifecycleOwner().getLifecycle(),
                                requireView(),
                                getResources().getQuantityString(R.plurals.ConversationListFragment_conversations_archived, 1, 1),
                                getString(R.string.ConversationListFragment_undo),
                                getResources().getColor(R.color.amber_500),
                                Snackbar.LENGTH_LONG,
                                false)
    {
      private final ThreadTable threadTable = SignalDatabase.threads();

      private List<Long> pinnedThreadIds;
      @Override
      protected void executeAction(@Nullable Long parameter) {
        Context context = requireActivity();

        pinnedThreadIds = threadTable.getPinnedThreadIds();
        threadTable.archiveConversation(threadId);

        if (unreadCount > 0) {
          List<MarkedMessageInfo> messageIds = threadTable.setRead(threadId, false);
          ApplicationDependencies.getMessageNotifier().updateNotification(context);
          MarkReadReceiver.process(context, messageIds);
        }
      }

      @Override
      protected void reverseAction(@Nullable Long parameter) {
        Context context = requireActivity();

        threadTable.unarchiveConversation(threadId);
        threadTable.restorePins(pinnedThreadIds);

        if (unreadCount > 0) {
          threadTable.incrementUnread(threadId, unreadCount, unreadSelfMentionsCount);
          ApplicationDependencies.getMessageNotifier().updateNotification(context);
        }
      }
    }.executeOnExecutor(SignalExecutors.BOUNDED, threadId);
  }

  @Override public boolean onKey(View view, int i, KeyEvent keyEvent) {
    if (i == KeyEvent.KEYCODE_CALL) {
      List<String> data     = getDatas();
      String       fromText = defaultAdapter.getFromText().getFromText();
      if (fromText.equals(data.get(0))) { //New Conversaton
      } else if (fromText.equals(data.get(1))) { //New Group
      } else if (fromText.equals(data.get(2))) {// Mark all read
      } else if (fromText.equals(data.get(3))) {// Setting
      } else if (fromText.equals(data.get(4))) {//Search
      } else if (TextUtils.isEmpty(fromText)) {
      } else {
        handleDial(defaultAdapter.getFromText().getRecipient());
      }
      return true;
    }
    return false;
  }

  private void handleDial(final Recipient recipient) {
    if (recipient == null) return;
    Log.d(TAG, "MENGNAN handleDial");
    CommunicationActions.startVoiceCall(getActivity(), recipient);
  }

  @Override public boolean onBackPressed() {
    return closeSearchIfOpen();
  }

  @Override public void onClearFilterClick() {
    viewModel.toggleUnreadChatsFilter();
  }

  private class ArchiveListenerCallback extends ItemTouchHelper.SimpleCallback {

    private static final long SWIPE_ANIMATION_DURATION = 175;

    private static final float MIN_ICON_SCALE = 0.85f;
    private static final float MAX_ICON_SCALE = 1f;

    private final int archiveColorStart;
    private final int archiveColorEnd;

    private final float ESCAPE_VELOCITY    = ViewUtil.dpToPx(1000);
    private final float VELOCITY_THRESHOLD = ViewUtil.dpToPx(1000);

    private WeakReference<RecyclerView.ViewHolder> lastTouched;

    ArchiveListenerCallback(@ColorInt int archiveColorStart, @ColorInt int archiveColorEnd) {
      super(0, ItemTouchHelper.RIGHT);
      this.archiveColorStart = archiveColorStart;
      this.archiveColorEnd   = archiveColorEnd;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target)
    {
      return false;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
      return Math.min(ESCAPE_VELOCITY, VELOCITY_THRESHOLD);
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
      return VELOCITY_THRESHOLD;
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
      if (viewHolder.itemView instanceof ConversationListItemAction      ||
          viewHolder instanceof ConversationListAdapter.HeaderViewHolder ||
          viewHolder.itemView.isSelected())
      {
        return 0;
      }

      return super.getSwipeDirs(recyclerView, viewHolder);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
      if (lastTouched != null) {
        Log.w(TAG, "Falling back to slower onSwiped() event.");
        onTrueSwipe(viewHolder);
        lastTouched = null;
      }
    }

    @Override
    public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
      if (animationType == ItemTouchHelper.ANIMATION_TYPE_SWIPE_SUCCESS && lastTouched != null && lastTouched.get() != null) {
        onTrueSwipe(lastTouched.get());
        lastTouched = null;
      } else if (animationType == ItemTouchHelper.ANIMATION_TYPE_SWIPE_CANCEL) {
        lastTouched = null;
      }

      return SWIPE_ANIMATION_DURATION;
    }

    private void onTrueSwipe(RecyclerView.ViewHolder viewHolder) {
      ThreadRecord thread = ((ConversationListItem) viewHolder.itemView).getThread();

      onItemSwiped(thread.getThreadId(), thread.getUnreadCount(), thread.getUnreadSelfMentionsCount());
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState,
                            boolean isCurrentlyActive)
    {
      float absoluteDx = Math.abs(dX);

      if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
        Resources resources       = getResources();
        View      itemView        = viewHolder.itemView;
        float     percentDx       = absoluteDx / viewHolder.itemView.getWidth();
        int       color           = ArgbEvaluatorCompat.getInstance().evaluate(Math.min(1f, percentDx * (1 / 0.25f)), archiveColorStart, archiveColorEnd);
        float     scaleStartPoint = DimensionUnit.DP.toPixels(48f);
        float     scaleEndPoint   = DimensionUnit.DP.toPixels(96f);

        float scale;
        if (absoluteDx < scaleStartPoint) {
          scale = MIN_ICON_SCALE;
        } else if (absoluteDx > scaleEndPoint) {
          scale = MAX_ICON_SCALE;
        } else {
          scale = Math.min(MAX_ICON_SCALE, MIN_ICON_SCALE + ((absoluteDx - scaleStartPoint) / (scaleEndPoint - scaleStartPoint)) * (MAX_ICON_SCALE - MIN_ICON_SCALE));
        }

        if (absoluteDx > 0) {
          if (archiveDrawable == null) {
            archiveDrawable = Objects.requireNonNull(AppCompatResources.getDrawable(requireContext(), getArchiveIconRes()));
            archiveDrawable.setColorFilter(new SimpleColorFilter(ContextCompat.getColor(requireContext(), R.color.signal_colorOnPrimary)));
            archiveDrawable.setBounds(0, 0, archiveDrawable.getIntrinsicWidth(), archiveDrawable.getIntrinsicHeight());
          }

          canvas.save();
          canvas.clipRect(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());

          canvas.drawColor(color);

          float gutter = resources.getDimension(R.dimen.dsl_settings_gutter);
          float extra  = resources.getDimension(R.dimen.conversation_list_fragment_archive_padding);

          if (ViewUtil.isLtr(requireContext())) {
            canvas.translate(itemView.getLeft() + gutter + extra,
                             itemView.getTop() + (itemView.getBottom() - itemView.getTop() - archiveDrawable.getIntrinsicHeight()) / 2f);
          } else {
            canvas.translate(itemView.getRight() - gutter - extra,
                             itemView.getTop() + (itemView.getBottom() - itemView.getTop() - archiveDrawable.getIntrinsicHeight()) / 2f);
          }

          canvas.scale(scale, scale, archiveDrawable.getIntrinsicWidth() / 2f, archiveDrawable.getIntrinsicHeight() / 2f);

          archiveDrawable.draw(canvas);
          canvas.restore();

          ViewCompat.setElevation(viewHolder.itemView, DimensionUnit.DP.toPixels(4f));
        } else if (absoluteDx == 0) {
          ViewCompat.setElevation(viewHolder.itemView, DimensionUnit.DP.toPixels(0f));
        }

        viewHolder.itemView.setTranslationX(dX);
      } else {
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
      }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
      super.clearView(recyclerView, viewHolder);

      if (itemAnimator == null) {
        return;
      }

      ViewCompat.setElevation(viewHolder.itemView, 0);
      lastTouched = null;

      View view = getView();
      if (view != null) {
        itemAnimator.postDisable(view.getHandler());
      } else {
        itemAnimator.disable();
      }
    }
  }

//  public interface Callback extends Material3OnScrollHelperBinder, SearchBinder {
//    @NonNull Toolbar getToolbar();
//
//    @NonNull View getUnreadPaymentsDot();
//
//    @NonNull Stub<Toolbar> getBasicToolbar();
//
//    void updateNotificationProfileStatus(@NonNull List<NotificationProfile> notificationProfiles);
//
//    void updateProxyStatus(@NonNull WebSocketConnectionState state);
//
//    void onMultiSelectStarted();
//
//    void onMultiSelectFinished();
//  }
}


