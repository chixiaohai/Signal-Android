/*
 * Copyright (C) 2011 Whisper Systems
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
package org.thoughtcrime.securesms.conversation;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.signal.core.util.ThreadUtil;
import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.concurrent.SimpleTask;
import org.signal.core.util.logging.Log;
import org.signal.libsignal.protocol.InvalidMessageException;
import org.signal.libsignal.protocol.util.Pair;
import org.thoughtcrime.securesms.BlockUnblockDialog;
import org.thoughtcrime.securesms.GroupMembersDialog;
import org.thoughtcrime.securesms.MainActivity;
import org.thoughtcrime.securesms.MuteDialog;
import org.thoughtcrime.securesms.PassphraseRequiredActivity;
import org.thoughtcrime.securesms.PromptMmsActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.ShortcutLauncherActivity;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.TombstoneAttachment;
import org.thoughtcrime.securesms.audio.AudioRecorder;
import org.thoughtcrime.securesms.color.MaterialColor;
import org.thoughtcrime.securesms.components.AnimatingToggle;
import org.thoughtcrime.securesms.components.ComposeText;
import org.thoughtcrime.securesms.components.ConversationSearchBottomBar;
import org.thoughtcrime.securesms.components.HidingLinearLayout;
import org.thoughtcrime.securesms.components.InputAwareLayout;
import org.thoughtcrime.securesms.components.InputPanel;
import org.thoughtcrime.securesms.components.KeyboardAwareLinearLayout.OnKeyboardShownListener;
import org.thoughtcrime.securesms.components.Mp02CustomDialog;
import org.thoughtcrime.securesms.components.SendButton;
import org.thoughtcrime.securesms.components.TooltipPopup;
import org.thoughtcrime.securesms.components.TypingStatusSender;
import org.thoughtcrime.securesms.components.emoji.EmojiEventListener;
import org.thoughtcrime.securesms.components.emoji.EmojiStrings;
import org.thoughtcrime.securesms.components.emoji.MediaKeyboard;
import org.thoughtcrime.securesms.components.identity.UnverifiedBannerView;
import org.thoughtcrime.securesms.components.location.SignalPlace;
import org.thoughtcrime.securesms.components.mention.MentionAnnotation;
import org.thoughtcrime.securesms.components.reminder.ExpiredBuildReminder;
import org.thoughtcrime.securesms.components.reminder.Reminder;
import org.thoughtcrime.securesms.components.reminder.ServiceOutageReminder;
import org.thoughtcrime.securesms.components.reminder.UnauthorizedReminder;
import org.thoughtcrime.securesms.components.voice.VoiceNoteDraft;
import org.thoughtcrime.securesms.components.voice.VoiceNoteMediaController;
import org.thoughtcrime.securesms.components.voice.VoiceNotePlaybackState;
import org.thoughtcrime.securesms.contacts.ContactAccessor;
import org.thoughtcrime.securesms.contacts.ContactAccessor.ContactData;
import org.thoughtcrime.securesms.contacts.paged.ContactSearchKey;
import org.thoughtcrime.securesms.contacts.sync.ContactDiscovery;
import org.thoughtcrime.securesms.contactshare.Contact;
import org.thoughtcrime.securesms.contactshare.ContactShareEditActivity;
import org.thoughtcrime.securesms.contactshare.ContactUtil;
import org.thoughtcrime.securesms.contactshare.SimpleTextWatcher;
import org.thoughtcrime.securesms.conversation.ConversationGroupViewModel.GroupActiveState;
import org.thoughtcrime.securesms.conversation.ConversationMessage.ConversationMessageFactory;
import org.thoughtcrime.securesms.conversation.drafts.DraftRepository;
import org.thoughtcrime.securesms.conversation.drafts.DraftViewModel;
import org.thoughtcrime.securesms.conversation.ui.error.SafetyNumberChangeDialog;
import org.thoughtcrime.securesms.conversation.ui.groupcall.GroupCallViewModel;
import org.thoughtcrime.securesms.conversation.ui.inlinequery.InlineQueryResultsController;
import org.thoughtcrime.securesms.conversation.ui.inlinequery.InlineQueryViewModel;
import org.thoughtcrime.securesms.conversation.ui.mentions.MentionsPickerViewModel;
import org.thoughtcrime.securesms.crypto.SecurityEvent;
import org.thoughtcrime.securesms.database.DraftTable;
import org.thoughtcrime.securesms.database.GroupTable;
import org.thoughtcrime.securesms.database.MentionUtil;
import org.thoughtcrime.securesms.database.MentionUtil.UpdatedBodyAndMentions;
import org.thoughtcrime.securesms.database.MmsSmsColumns.Types;
import org.thoughtcrime.securesms.database.RecipientTable;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.database.ThreadTable;
import org.thoughtcrime.securesms.database.identity.IdentityRecordList;
import org.thoughtcrime.securesms.database.model.IdentityRecord;
import org.thoughtcrime.securesms.database.model.Mention;
import org.thoughtcrime.securesms.database.model.MessageId;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.MmsMessageRecord;
import org.thoughtcrime.securesms.database.model.ReactionRecord;
import org.thoughtcrime.securesms.database.model.StickerRecord;
import org.thoughtcrime.securesms.database.model.StoryType;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.events.GroupCallPeekEvent;
import org.thoughtcrime.securesms.events.ReminderUpdateEvent;
import org.thoughtcrime.securesms.giph.ui.GiphyActivity;
import org.thoughtcrime.securesms.groups.GroupId;
import org.thoughtcrime.securesms.groups.ui.GroupChangeFailureReason;
import org.thoughtcrime.securesms.groups.ui.GroupErrors;
import org.thoughtcrime.securesms.groups.ui.LeaveGroupDialog;
import org.thoughtcrime.securesms.groups.ui.managegroup.EditGroupActivity;
import org.thoughtcrime.securesms.groups.ui.managegroup.ManageGroupActivity;
import org.thoughtcrime.securesms.insights.InsightsLauncher;
import org.thoughtcrime.securesms.invites.InviteReminderModel;
import org.thoughtcrime.securesms.invites.InviteReminderRepository;
import org.thoughtcrime.securesms.jobs.GroupV1MigrationJob;
import org.thoughtcrime.securesms.jobs.GroupV2UpdateSelfProfileKeyJob;
import org.thoughtcrime.securesms.jobs.RequestGroupV2InfoJob;
import org.thoughtcrime.securesms.jobs.RetrieveProfileJob;
import org.thoughtcrime.securesms.jobs.ServiceOutageDetectionJob;
import org.thoughtcrime.securesms.keyboard.KeyboardPage;
import org.thoughtcrime.securesms.keyboard.KeyboardPagerViewModel;
import org.thoughtcrime.securesms.keyboard.emoji.EmojiKeyboardPageFragment;
import org.thoughtcrime.securesms.keyboard.emoji.search.EmojiSearchFragment;
import org.thoughtcrime.securesms.keyboard.gif.GifKeyboardPageFragment;
import org.thoughtcrime.securesms.keyvalue.PaymentsValues;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.linkpreview.LinkPreview;
import org.thoughtcrime.securesms.linkpreview.LinkPreviewRepository;
import org.thoughtcrime.securesms.linkpreview.LinkPreviewViewModel;
import org.thoughtcrime.securesms.maps.PlacePickerActivity;
import org.thoughtcrime.securesms.mediaoverview.MediaOverviewActivity;
import org.thoughtcrime.securesms.mediasend.Media;
import org.thoughtcrime.securesms.mediasend.MediaSendActivityResult;
import org.thoughtcrime.securesms.mediasend.v2.MediaSelectionActivity;
import org.thoughtcrime.securesms.messagedetails.MessageDetailsFragment;
import org.thoughtcrime.securesms.messagerequests.MessageRequestState;
import org.thoughtcrime.securesms.messagerequests.MessageRequestViewModel;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.mms.DecryptableStreamUriLoader;
import org.thoughtcrime.securesms.mms.GifSlide;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.ImageSlide;
import org.thoughtcrime.securesms.mms.LocationSlide;
import org.thoughtcrime.securesms.mms.MediaConstraints;
import org.thoughtcrime.securesms.mms.OutgoingMediaMessage;
import org.thoughtcrime.securesms.mms.OutgoingSecureMediaMessage;
import org.thoughtcrime.securesms.mms.QuoteId;
import org.thoughtcrime.securesms.mms.QuoteModel;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.mms.SlideFactory;
import org.thoughtcrime.securesms.mms.SlideFactory.MediaType;
import org.thoughtcrime.securesms.mms.StickerSlide;
import org.thoughtcrime.securesms.mms.VideoSlide;
import org.thoughtcrime.securesms.notifications.NotificationChannels;
import org.thoughtcrime.securesms.notifications.v2.ConversationId;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.permissions.RationaleDialog;
import org.thoughtcrime.securesms.profiles.spoofing.ReviewBannerView;
import org.thoughtcrime.securesms.profiles.spoofing.ReviewCardDialogFragment;
import org.thoughtcrime.securesms.providers.BlobProvider;
import org.thoughtcrime.securesms.ratelimit.RecaptchaProofBottomSheetFragment;
import org.thoughtcrime.securesms.reactions.ReactionsBottomSheetDialogFragment;
import org.thoughtcrime.securesms.reactions.any.ReactWithAnyEmojiBottomSheetDialogFragment;
import org.thoughtcrime.securesms.recipients.LiveRecipient;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientExporter;
import org.thoughtcrime.securesms.recipients.RecipientFormattingException;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.recipients.RecipientUtil;
import org.thoughtcrime.securesms.recipients.ui.disappearingmessages.RecipientDisappearingMessagesActivity;
import org.thoughtcrime.securesms.recipients.ui.managerecipient.ManageRecipientActivity;
import org.thoughtcrime.securesms.registration.RegistrationNavigationActivity;
import org.thoughtcrime.securesms.safety.SafetyNumberBottomSheet;
import org.thoughtcrime.securesms.search.MessageResult;
import org.thoughtcrime.securesms.service.KeyCachingService;
import org.thoughtcrime.securesms.sms.MessageSender;
import org.thoughtcrime.securesms.sms.OutgoingEncryptedMessage;
import org.thoughtcrime.securesms.sms.OutgoingEndSessionMessage;
import org.thoughtcrime.securesms.sms.OutgoingTextMessage;
import org.thoughtcrime.securesms.stickers.StickerEventListener;
import org.thoughtcrime.securesms.stickers.StickerLocator;
import org.thoughtcrime.securesms.stickers.StickerManagementActivity;
import org.thoughtcrime.securesms.stickers.StickerPackInstallEvent;
import org.thoughtcrime.securesms.stickers.StickerSearchRepository;
import org.thoughtcrime.securesms.util.AsynchronousCallback;
import org.thoughtcrime.securesms.util.Base64;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.BubbleUtil;
import org.thoughtcrime.securesms.util.CharacterCalculator.CharacterState;
import org.thoughtcrime.securesms.util.CommunicationActions;
import org.thoughtcrime.securesms.util.ContextUtil;
import org.thoughtcrime.securesms.util.ConversationUtil;
import org.thoughtcrime.securesms.util.DrawableUtil;
import org.thoughtcrime.securesms.util.DynamicDarkToolbarTheme;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.FullscreenHelper;
import org.thoughtcrime.securesms.util.IdentityUtil;
import org.thoughtcrime.securesms.util.LifecycleDisposable;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.MessageRecordUtil;
import org.thoughtcrime.securesms.util.MessageUtil;
import org.thoughtcrime.securesms.util.PlayStoreUtil;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.SignalLocalMetrics;
import org.thoughtcrime.securesms.util.SpanUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.TextSecurePreferences.MediaKeyboardMode;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.WindowUtil;
import org.thoughtcrime.securesms.util.concurrent.AssertedSuccessListener;
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture;
import org.thoughtcrime.securesms.util.concurrent.SettableFuture;
import org.thoughtcrime.securesms.util.views.Stub;
import org.thoughtcrime.securesms.wallpaper.ChatWallpaper;
import org.thoughtcrime.securesms.wallpaper.ChatWallpaperDimLevelUtil;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thoughtcrime.securesms.database.GroupTable.GroupRecord;

/**
 * Activity for displaying a message thread, as well as
 * composing/sending a new message into that thread.
 *
 * @author Moxie Marlinspike
 */
@SuppressLint("StaticFieldLeak")
public class ConversationActivity extends PassphraseRequiredActivity
    implements ConversationFragment.ConversationFragmentListener,
               AttachmentManager.AttachmentListener,
               OnKeyboardShownListener,
               InputPanel.Listener,
               InputPanel.MediaListener,
               ComposeText.CursorPositionChangedListener,
               ConversationSearchBottomBar.EventListener,
               StickerEventListener,
               AttachmentKeyboard.Callback,
               ConversationReactionOverlay.OnReactionSelectedListener,
               ReactWithAnyEmojiBottomSheetDialogFragment.Callback,
               SafetyNumberChangeDialog.Callback,
               ReactionsBottomSheetDialogFragment.Callback,
               MediaKeyboard.MediaKeyboardListener,
               EmojiEventListener,
               GifKeyboardPageFragment.Host,
               EmojiKeyboardPageFragment.Callback,
               EmojiSearchFragment.Callback
{

  private static final int SHORTCUT_ICON_SIZE = Build.VERSION.SDK_INT >= 26 ? ViewUtil.dpToPx(72) : ViewUtil.dpToPx(48 + 16 * 2);

  private static final String               TAG = Log.tag(ConversationActivity.class);
  public static        ConversationActivity instance;

  public static final  String RECIPIENT_EXTRA           = "recipient_id";
  public static final  String THREAD_ID_EXTRA           = "thread_id";
  private static final String STATE_REACT_WITH_ANY_PAGE = "STATE_REACT_WITH_ANY_PAGE";
  private static final String STATE_IS_SEARCH_REQUESTED = "STATE_IS_SEARCH_REQUESTED";
  public static final  String STRING_CURRENT_EXPIRATION = "current_expiration";
  public static final  String IS_CALL_TO_EXTRA          = "is_tel";

  private static final int REQUEST_CODE_SETTINGS = 1000;
  public static final  int STRING_OPEN_ACTIVITY  = 10000;
  private static final int PICK_GALLERY          = 1;
  private static final int PICK_DOCUMENT         = 2;
  private static final int PICK_AUDIO            = 3;
  private static final int PICK_CONTACT          = 4;
  private static final int GET_CONTACT_DETAILS   = 5;
  private static final int GROUP_EDIT            = 6;
  private static final int TAKE_PHOTO            = 7;
  private static final int ADD_CONTACT           = 8;
  private static final int PICK_LOCATION         = 9;
  public static final  int PICK_GIF              = 10;
  private static final int SMS_DEFAULT           = 11;
  private static final int MEDIA_SENDER          = 12;

  private   Toolbar                      toolbar;
  private   GlideRequests                glideRequests;
  protected ComposeText                  composeText;
  private   AnimatingToggle              buttonToggle;
  private   TextView                     sendText;
  private   SendButton                   sendButton;
  private   ImageButton                  attachButton;
  protected ConversationTitleView        titleView;
  private   TextView                     charactersLeft;
  private   ConversationFragment         fragment;
  private   Button                       unblockButton;
  private   Button                       makeDefaultSmsButton;
  private   Button                       registerButton;
  private   InputAwareLayout             container;
  //    protected Stub<ReminderView> reminderView;
  private   Stub<UnverifiedBannerView>   unverifiedBannerView;
  private   Stub<ReviewBannerView>       reviewBanner;
  private   TypingStatusTextWatcher      typingTextWatcher;
  private   ConversationSearchBottomBar  searchNav;
  private   MenuItem                     searchViewItem;
  //    private MessageRequestsBottomView messageRequestBottomView;
  private   ConversationReactionDelegate reactionDelegate;
  public    TextView                     my_record_time;
  private   TextView                     myRT;
  private   android.app.AlertDialog      builder;
//  SendButtonListener sendButtonListener01 = new SendButtonListener();

  private   AttachmentManager        attachmentManager;
  private   AudioRecorder            audioRecorder;
  private   BroadcastReceiver        securityUpdateReceiver;
  private   Stub<MediaKeyboard>      emojiDrawerStub;
  private   Stub<AttachmentKeyboard> attachmentKeyboardStub;
  protected HidingLinearLayout       quickAttachmentToggle;
  protected HidingLinearLayout       inlineAttachmentToggle;
  private   InputPanel               inputPanel;
  private   View                     panelParent;
  private   View                     noLongerMemberBanner;
  private   Stub<TextView>           cannotSendInAnnouncementGroupBanner;
  private   View                     requestingMemberBanner;
  private   View                     cancelJoinRequest;
  private   Stub<View>               mentionsSuggestions;
  private   MaterialButton           joinGroupCallButton;
  private   boolean                  callingTooltipShown;
  private   ImageView                wallpaper;
  private   View                     wallpaperDim;

  private InlineQueryViewModel         inlineQueryViewModel;
  private LinkPreviewViewModel         linkPreviewViewModel;
  private ConversationSearchViewModel  searchViewModel;
  private ConversationStickerViewModel stickerViewModel;
  private ConversationViewModel        viewModel;
  private InviteReminderModel          inviteReminderModel;
  private ConversationGroupViewModel   groupViewModel;
  private MentionsPickerViewModel      mentionsViewModel;
  private GroupCallViewModel           groupCallViewModel;
  private VoiceRecorderWakeLock        voiceRecorderWakeLock;
  private DraftViewModel               draftViewModel;
  private VoiceNoteMediaController     voiceNoteMediaController;
  private InlineQueryResultsController inlineQueryResultsController;

  private          LiveRecipient recipient;
  private          long          threadId;
  private          int           distributionType;
  private          boolean       isSecureText;
  private          boolean       isDefaultSms;
  private          int           reactWithAnyEmojiStartPage = -1;
  private          boolean       isMmsEnabled               = true;
  private          boolean       isSecurityInitialized      = false;
  private          boolean       isSearchRequested          = false;
  private volatile boolean       screenInitialized          = false;
  private          boolean       removeflag                 = false;
  private          boolean       isForward                  = false;

  private RecyclerView mRecyclerview;
  private ViewGroup    mVG;

  private Mp02CustomDialog mp02CustomDialog;

  private final LifecycleDisposable disposables     = new LifecycleDisposable();
  private       IdentityRecordList  identityRecords = new IdentityRecordList(Collections.emptyList());
  private final DynamicTheme        dynamicTheme    = new DynamicDarkToolbarTheme();
  private final DynamicLanguage     dynamicLanguage = new DynamicLanguage();
  private       List<String>        mList           = new ArrayList<>();
  private       List<String>        mListGp         = new ArrayList<>();
  private       TwoLineMenuAdapter  mTLAdapter;
  private       TwoLineMenuAdapter  mTLAdapterGp;
  RelativeLayout rlContainer;
  private int     record_flag   = 0;
  private String  sent_to_name;
  private boolean isKeyUpOrDown = true;
  private boolean isComposeTextFocus;

  private String[] AudioPermissions = {
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
  };
  private boolean  isAddContacts    = true;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  private void initMenuListGp() {
    mListGp.clear();
    boolean enabled = !(isPushGroupConversation() && !isActiveGroup());
    sent_to_name = titleView.getTitleName();
    String Send_text = getString(R.string.conversation_activity__send) + " " + sent_to_name;
//        mListGp.add(Send_text);
//        mListGp.add(getString(R.string.conversation__menu_voice_message));
//        mListGp.add(getString(R.string.conversation__menu_conversation_settings));
//        mListGp.add(getString(R.string.ExpirationDialog_disappearing_messages));
    if (enabled) {
      mListGp.add(Send_text);
      mListGp.add(getString(R.string.conversation__menu_voice_message));
      mListGp.add(getString(R.string.MessageRecord_group_call));
      mListGp.add(getString(R.string.conversation__menu_conversation_settings));
      mListGp.add(getString(R.string.conversation__menu_edit_group));
      mListGp.add(getString(R.string.conversation__menu_leave_group));
    } else {
      mListGp.add(getString(R.string.conversation__menu_conversation_settings));
      mListGp.add(getString(R.string.ExpirationDialog_disappearing_messages));
    }
  }

  private void initMenuList() {
    mList.clear();
    sent_to_name = titleView.getTitleName();
    String Send_text = getString(R.string.conversation_activity__send) + " " + sent_to_name;
    mList.add(Send_text);
    mList.add(getString(R.string.conversation__menu_voice_message));
    if (!sent_to_name.equals(getString(R.string.note_to_self))) {
      mList.add(getString(R.string.conversation_callable_insecure__menu_call));
    }
    mList.add(getString(R.string.conversation__menu_conversation_settings));
    mList.add(getString(R.string.conversation_secure_verified__menu_reset_secure_session));
    if (isAddContacts) {
      mList.add(getString(R.string.conversation_add_to_contacts__menu_add_to_contacts));
    }
  }

  public boolean isKeyUpOrDown() {
    return isKeyUpOrDown;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (isSingleConversation() && mRecyclerview.getLayoutManager().findViewByPosition(mList.size() - 1) != null &&
        keyCode == KeyEvent.KEYCODE_DPAD_DOWN && mRecyclerview.getLayoutManager().findViewByPosition(mList.size() - 1).hasFocus())
    {
      return true;
    }
    if (isGroupConversation() && mRecyclerview.getLayoutManager().findViewByPosition(mListGp.size() - 1) != null &&
        keyCode == KeyEvent.KEYCODE_DPAD_DOWN && mRecyclerview.getLayoutManager().findViewByPosition(mListGp.size() - 1).hasFocus())
    {
      return true;
    }
    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
      isKeyUpOrDown = false;
//            if (isComposeTextFocus && panelParent.getVisibility() == View.GONE){
//                panelParent.setVisibility(View.VISIBLE);
//                return true;
//            }
    }
    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
      isKeyUpOrDown = true;
    }
    if (keyCode == KeyEvent.KEYCODE_CALL && composeText.hasFocus()) {
      if (isBlocked()) {
        return true;
      }
      if (composeText.getText().length() > 0 || isGroupConversation() || attachmentManager.isAttachmentPresent()) {
        sendText.performClick();
        Log.d(TAG, "ConversationActivity  onKeyDown  sendText.performClick();");
        return true;
      } else if (isSingleConversation()) {
        handleDial(getRecipient(), true);
      }
      return true;
    }
    // if (mRecyclerview.getChildAt(0) == null) {
    //return true;
    if (mRecyclerview.getChildAt(5) != null) {
      View     v  = mRecyclerview.getLayoutManager().findViewByPosition(5);
      TextView tx = v.findViewById(R.id.menu_item);


      //TextView textView01 = mRecyclerview.getLayoutManager().findViewByPosition(1).findViewById(R.id.menu_item);
      if (mRecyclerview.getChildAt(5) != null && mRecyclerview.getChildAt(5).hasFocus()
          && keyCode == KeyEvent.KEYCODE_CALL && tx.getText().toString().startsWith("Call"))
      {
        handleDial(getRecipient(), isSecureText);
        return true;
      }
    }
    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && mRecyclerview.getChildAt(0) != null && mRecyclerview.getChildAt(0).hasFocus()) {
      mRecyclerview.setVisibility(View.GONE);
      if (isBlocked()) {
        unblockButton.requestFocus();
      } else if (registerButton.getVisibility() == View.VISIBLE) {
        registerButton.requestFocus();
      } else if (makeDefaultSmsButton.getVisibility() == View.VISIBLE) {
        makeDefaultSmsButton.requestFocus();
      } else {
        fragment.getList().scrollToPosition(-1);
        panelParent.setVisibility(View.VISIBLE);
        composeText.requestFocus();
      }
      mVG.setVisibility(View.VISIBLE);
      return true;
    }
    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && record_flag == 1) {
      inputPanel.onRecordReleased();
      inputPanel.mydisMiss();
      my_record_time.setVisibility(View.GONE);
      mRecyclerview.setVisibility(View.GONE);
      //composeText.requestFocus();
      // mTLAdapterGp.setScorllUp(false);
      //mTLAdapter.setScorllUp(false);
      mVG.setVisibility(View.VISIBLE);
      record_flag = 0;
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && composeText.hasFocus()) {
      if (!isBlocked())
        mRecyclerview.setVisibility(View.VISIBLE);
      return true;
    }
    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && sendButton.hasFocus()) {
      mRecyclerview.setVisibility(View.VISIBLE);
      return true;
    }
    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && fragment.isLongerMsgItem(keyCode)) {
      fragment.getList().smoothScrollBy(0, 40);
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && !composeText.isFocused() && fragment.isLongerMsgItem(keyCode)) {
      fragment.getList().smoothScrollBy(0, -40);
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && mRecyclerview.getVisibility() != View.VISIBLE && fragment.getCurrentPosition() == 0 && !isForward) {
      if (isBlocked()) {
        unblockButton.requestFocus();
      } else if (registerButton.getVisibility() == View.VISIBLE) {
        registerButton.requestFocus();
      } else if (makeDefaultSmsButton.getVisibility() == View.VISIBLE) {
        makeDefaultSmsButton.requestFocus();
      } else {
        isHideInputPanelView(false);
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && mRecyclerview.getVisibility() != View.VISIBLE && fragment.isAtBottom() && isForward) {
      isHideInputPanelView(false);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  private void isContact(String recipientNumber) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_CONTACTS }, 1);
    } else {
      Cursor cursor = null;
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null);
        }
        if (cursor != null) {
          while (cursor.moveToNext()) {
            @SuppressLint("Range") String contactNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
            if (contactNumber.equals(recipientNumber)) {
              isAddContacts = false;
              break;
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
    }
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    instance = this;
    if (ConversationIntents.isInvalid(getIntent().getExtras())) {
      Log.w(TAG, "[onCreate] Missing recipientId!");
      // TODO [greyson] Navigation
      startActivity(MainActivity.clearTop(this));
      finish();
      return;
    }

    isDefaultSms             = Util.isDefaultSmsProvider(this);
    voiceNoteMediaController = new VoiceNoteMediaController(this);
    voiceRecorderWakeLock    = new VoiceRecorderWakeLock(this);

    new FullscreenHelper(this).showSystemUI();

    ConversationIntents.Args args = ConversationIntents.Args.from(getIntent().getExtras());

    isSearchRequested = args.isWithSearchOpen();

    reportShortcutLaunch(args.getRecipientId());
    setContentView(R.layout.conversation_activity);

    fragment = initFragment(R.id.fragment_content, new ConversationFragment(), dynamicLanguage.getCurrentLocale());

    mVG            = (ViewGroup) findViewById(R.id.activity_framelayout);
    myRT           = findViewById(R.id.record_time);
    my_record_time = findViewById(R.id.my_record_time);
    rlContainer    = findViewById(R.id.rl_container);
    mRecyclerview  = (RecyclerView) findViewById(R.id.menu_message);
    LinearLayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    mRecyclerview.setLayoutManager(mLayoutManager);
    mRecyclerview.setClipToPadding(false);
    mRecyclerview.setClipChildren(false);
    mRecyclerview.setPadding(0, 76, 0, 200);

    initializeReceivers();
    initializeActionBar();
    initializeViews();
    //updateWallpaper(args.getWallpaper());
    initializeResources(args);
    initializeLinkPreviewObserver();
    initializeSearchObserver();
//        initializeStickerObserver();
    initializeViewModel(args);
    initializeGroupViewModel();
    initializeMentionsViewModel();
    initializeGroupCallViewModel();
    initializeDraftViewModel();
    initializeEnabledCheck();
    initializePendingRequestsBanner();
    initializeGroupV1MigrationsBanners();
    initializeSecurity(recipient.get().isRegistered(), isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        if (isFinishing()) {
          Log.w(TAG, "Activity is finishing. Not proceeding with initialization.");
          return;
        }
        initializeProfiles();
        initializeGv1Migration();
        initializeDraft(args).addListener(new AssertedSuccessListener<Boolean>() {
          @Override
          public void onSuccess(Boolean loadedDraft) {
            if (loadedDraft != null && loadedDraft) {
              Log.i(TAG, "Finished loading draft");
              ThreadUtil.runOnMain(() -> {
                if (fragment != null && fragment.isResumed()) {
                  fragment.moveToLastSeen();
                } else {
                  Log.w(TAG, "Wanted to move to the last seen position, but the fragment was in an invalid state");
                }
              });
            }

            if (TextSecurePreferences.isTypingIndicatorsEnabled(ConversationActivity.this)) {
              composeText.addTextChangedListener(typingTextWatcher);
            }
            composeText.setSelection(composeText.length(), composeText.length());

            screenInitialized = true;
          }
        });
      }
    });
    initializeInsightObserver();

    if (isSingleConversation() && recipient != null) {
      isContact(recipient.get().requireE164());
    }
    composeText.setOnKeyListener(new OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (makeDefaultSmsButton.getVisibility() == View.VISIBLE) {
          makeDefaultSmsButton.performClick();
          return false;
        }
        if (isBlocked()) {
          unblockButton.requestFocus();
          unblockButton.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, event);
          return false;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
          if (attachmentManager.isAttachmentPresent()) {
            attachmentManager.cleanup();
          }
          return false;
        }
        int index = composeText.getSelectionStart();
        if (KeyEvent.ACTION_DOWN == event.getAction()) {
          switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
              if (index == 0) {
                isHideInputPanelView(true);
                if (event.getRepeatCount() > 0) break;
                return true;
              } else if (index > 0) {
                index--;
                composeText.setSelection(index);
                break;
              }
              break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
              if (index == composeText.getText().toString().length()) {
                if (event.getRepeatCount() > 0) break;
                mRecyclerview.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> mRecyclerview.getLayoutManager().findViewByPosition(0).requestFocus(), 100);
              } else if (index < composeText.getText().toString().length()) {
                index++;
                composeText.setSelection(index);
                break;
              }
              break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
              mRecyclerview.setVisibility(View.VISIBLE);

              new Handler().postDelayed(() -> mRecyclerview.getLayoutManager().findViewByPosition(0).requestFocus(), 100);
              break;
            default:
              return false;

          }
        }
        return true;
      }
    });
    unblockButton.setOnKeyListener(new OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (KeyEvent.ACTION_DOWN == event.getAction()) {
          switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
              handleUnblock();
              return true;
          }
        }
        return false;
      }
    });

    if (getIntent().getBooleanExtra(IS_CALL_TO_EXTRA, false)) {
      handleDial(getRecipient(), isSecureText);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Log.i(TAG, "onNewIntent()");

    if (isFinishing()) {
      Log.w(TAG, "Activity is finishing...");
      return;
    }

    if (!screenInitialized) {
      Log.w(TAG, "Activity is in the middle of initialization. Restarting.");
      finish();
      startActivity(intent);
      return;
    }

    reactWithAnyEmojiStartPage = -1;
    if (!Util.isEmpty(composeText) || attachmentManager.isAttachmentPresent() || inputPanel.hasSaveableContent()) {
      saveDraft();
      attachmentManager.clear(glideRequests, false);
      inputPanel.clearQuote();
      silentlySetComposeText("");
    }

    if (ConversationIntents.isInvalid(intent.getExtras())) {
      Log.w(TAG, "[onNewIntent] Missing recipientId!");
      // TODO [greyson] Navigation
      startActivity(MainActivity.clearTop(this));
      finish();
      return;
    }

    setIntent(intent);

    ConversationIntents.Args args = ConversationIntents.Args.from(intent.getExtras());
    isSearchRequested = args.isWithSearchOpen();

    viewModel.setArgs(args);

    reportShortcutLaunch(viewModel.getArgs().getRecipientId());
    initializeResources(viewModel.getArgs());
    initializeSecurity(recipient.get().isRegistered(), isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        initializeDraft(viewModel.getArgs());
      }
    });

    if (fragment != null) {
      fragment.onNewIntent();
    }

    searchNav.setVisibility(View.GONE);
    if (args.isWithSearchOpen()) {
      if (searchViewItem != null && searchViewItem.expandActionView()) {
        searchViewModel.onSearchOpened();
      }
    }

    isForward = true;
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (recipient.get().isBlocked()) {
      mRecyclerview.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);

    EventBus.getDefault().register(this);
    initializeMmsEnabledCheck();
    initializeIdentityRecords();
    composeText.setMessageSendType(sendButton.getSelectedSendType());

    Recipient recipientSnapshot = recipient.get();

    titleView.setTitle(glideRequests, recipientSnapshot);
    sent_to_name = titleView.getTitleName();
    initMenuList();
    initMenuListGp();
    mTLAdapter   = new TwoLineMenuAdapter(rlContainer, 74, mList);
    mTLAdapterGp = new TwoLineMenuAdapter(rlContainer, 74, mListGp);
//        setActionBarColor(recipientSnapshot.getColor());
    setBlockedUserState(recipientSnapshot, isSecureText, isDefaultSms);
    calculateCharactersRemaining();

    if (recipientSnapshot.getGroupId().isPresent() && recipientSnapshot.getGroupId().get().isV2()) {
      GroupId.V2 groupId = recipientSnapshot.getGroupId().get().requireV2();

      ApplicationDependencies.getJobManager()
                             .startChain(new RequestGroupV2InfoJob(groupId))
                             .then(GroupV2UpdateSelfProfileKeyJob.withoutLimits(groupId))
                             .enqueue();

      if (viewModel.getArgs().isFirstTimeInSelfCreatedGroup()) {
        groupViewModel.inviteFriendsOneTimeIfJustSelfInGroup(getSupportFragmentManager(), groupId);
      }
    }

    if (groupCallViewModel != null) {
      groupCallViewModel.peekGroupCall();
    }

    setVisibleThread(threadId);
    ConversationUtil.refreshRecipientShortcuts();

    if (SignalStore.rateLimit().needsRecaptcha()) {
      RecaptchaProofBottomSheetFragment.show(getSupportFragmentManager());
    }
    composeText.postDelayed(() -> {
      if (getPanelLayout().getVisibility() == View.VISIBLE && composeText.getVisibility() == View.VISIBLE
          && mRecyclerview.getVisibility() != View.VISIBLE)
      {
        composeText.setFocusable(true);
        composeText.setFocusableInTouchMode(true);
        composeText.requestFocus();
      }
    }, 500);
//    if (!TextSecurePreferences.isPushRegistered(this)) {
//      makeDefaultSmsButton.postDelayed(() -> {
//
//        if (builder == null) {
//          builder = RationaleDialog.createNonMsgDialog(this,
//                                                       getString(R.string.ConversationActivity_register_tip),
//                                                       R.string.yes,
//                                                       R.string.no,
//                                                       () -> {
//                                                         startActivity(RegistrationNavigationActivity.newIntentForReRegistration(this));
//                                                         finish();
//                                                       },
//                                                       () -> finish(),
//                                                       () -> finish()
//          );
//          builder.setCancelable(true);
//          builder.show();
//        } else if (!builder.isShowing()) {
//          builder.show();
//        }
//      }, 1000);
//    }

    if (isForward) {
      fragment.scrollToBottom();
      isHideInputPanelView(false);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (!isInBubble()) {
      ApplicationDependencies.getMessageNotifier().clearVisibleThread();
    }

    if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_end);
    inputPanel.onPause();

    fragment.setLastSeen(System.currentTimeMillis());
    markLastSeen();
    EventBus.getDefault().unregister(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    saveDraft();
    EventBus.getDefault().unregister(this);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    Log.i(TAG, "onConfigurationChanged(" + newConfig.orientation + ")");
    super.onConfigurationChanged(newConfig);
    composeText.setMessageSendType(sendButton.getSelectedSendType());

    if (emojiDrawerStub.resolved() && container.getCurrentInput() == emojiDrawerStub.get()) {
      container.hideAttachedInput(true);
    }

    if (reactionDelegate.isShowing()) {
      reactionDelegate.hide();
    }
  }

  @Override
  protected void onDestroy() {
    if (securityUpdateReceiver != null) unregisterReceiver(securityUpdateReceiver);
    super.onDestroy();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    return reactionDelegate.applyTouchEvent(ev) || super.dispatchTouchEvent(ev);
  }

  @Override
  public void onActivityResult(final int reqCode, int resultCode, Intent data) {
    Log.i(TAG, "onActivityResult called: " + reqCode + ", " + resultCode + " , " + data);
    super.onActivityResult(reqCode, resultCode, data);

    if (reqCode == STRING_OPEN_ACTIVITY) {
      mRecyclerview.setVisibility(View.GONE);
      composeText.requestFocus();
      mVG.setVisibility(View.VISIBLE);
    }

    if ((data == null && reqCode != TAKE_PHOTO && reqCode != SMS_DEFAULT) ||
        (resultCode != RESULT_OK && reqCode != SMS_DEFAULT))
    {
      updateLinkPreviewState();
      SignalStore.settings().setDefaultSms(Util.isDefaultSmsProvider(this));
      return;
    }

    switch (reqCode) {
      case PICK_DOCUMENT:
        setMedia(data.getData(), SlideFactory.MediaType.DOCUMENT);
        break;
      case PICK_AUDIO:
        setMedia(data.getData(), SlideFactory.MediaType.AUDIO);
        break;
      case PICK_CONTACT:
        if (isSecureText && !isSmsForced()) {
          openContactShareEditor(data.getData());
        } else {
          addAttachmentContactInfo(data.getData());
        }
        break;
      case GET_CONTACT_DETAILS:
        sendSharedContact(data.getParcelableArrayListExtra(ContactShareEditActivity.KEY_CONTACTS));
        break;
      case GROUP_EDIT:
        Recipient recipientSnapshot = recipient.get();

        onRecipientChanged(recipientSnapshot);
        titleView.setTitle(glideRequests, recipientSnapshot);
        NotificationChannels.getInstance().updateContactChannelName(recipientSnapshot);
        setBlockedUserState(recipientSnapshot, isSecureText, isDefaultSms);
        invalidateOptionsMenu();
        break;
      case TAKE_PHOTO:
        handleImageFromDeviceCameraApp();
        break;
      case ADD_CONTACT:
        SimpleTask.run(() -> {
          try {
            ContactDiscovery.refresh(this, recipient.get(), false);
          } catch (IOException e) {
            Log.w(TAG, "Failed to refresh user after adding to contacts.");
          }
          return null;
        }, nothing -> onRecipientChanged(recipient.get()));
        break;
      case PICK_LOCATION:
        SignalPlace place = new SignalPlace(PlacePickerActivity.addressFromData(data));
        attachmentManager.setLocation(place, getCurrentMediaConstraints());
        break;
      case PICK_GIF:
        onGifSelectSuccess(data.getData(),
                           data.getIntExtra(GiphyActivity.EXTRA_WIDTH, 0),
                           data.getIntExtra(GiphyActivity.EXTRA_HEIGHT, 0));
        break;
      case SMS_DEFAULT:
        initializeSecurity(isSecureText, isDefaultSms);
        break;
      case MEDIA_SENDER:
        MediaSendActivityResult result = MediaSendActivityResult.fromData(data);

        if (!Objects.equals(result.getRecipientId(), recipient.getId())) {
          Log.w(TAG, "Result's recipientId did not match ours! Result: " + result.getRecipientId() + ", Activity: " + recipient.getId());
          Toast.makeText(this, R.string.ConversationActivity_error_sending_media, Toast.LENGTH_SHORT).show();
          return;
        }

        sendButton.setSendType(result.getMessageSendType());

        if (result.isPushPreUpload()) {
          sendMediaMessage(result);
          return;
        }

        long expiresIn = TimeUnit.SECONDS.toMillis(recipient.get().getExpiresInSeconds());
        boolean initiating = threadId == -1;
        QuoteModel quote = result.isViewOnce() ? null : inputPanel.getQuote().orElse(null);
        SlideDeck slideDeck = new SlideDeck();
        List<Mention> mentions = new ArrayList<>(result.getMentions());

        for (Media mediaItem : result.getNonUploadedMedia()) {
          if (MediaUtil.isVideoType(mediaItem.getMimeType())) {
            slideDeck.addSlide(new VideoSlide(this, mediaItem.getUri(), mediaItem.getSize(), mediaItem.isVideoGif(), mediaItem.getWidth(), mediaItem.getHeight(), mediaItem.getCaption().orElse(null), mediaItem.getTransformProperties()
                                                                                                                                                                                                                .orElse(null)));
          } else if (MediaUtil.isGif(mediaItem.getMimeType())) {
            slideDeck.addSlide(new GifSlide(this, mediaItem.getUri(), mediaItem.getSize(), mediaItem.getWidth(), mediaItem.getHeight(), mediaItem.isBorderless(), mediaItem.getCaption().orElse(null)));
          } else if (MediaUtil.isImageType(mediaItem.getMimeType())) {
            slideDeck.addSlide(new ImageSlide(this, mediaItem.getUri(), mediaItem.getMimeType(), mediaItem.getSize(), mediaItem.getWidth(), mediaItem.getHeight(), mediaItem.isBorderless(), mediaItem.getCaption()
                                                                                                                                                                                                      .orElse(null), null, mediaItem
                                                  .getTransformProperties().orElse(null)));
          } else {
            Log.w(TAG, "Asked to send an unexpected mimeType: '" + mediaItem.getMimeType() + "'. Skipping.");
          }
        }

        final Context context = ConversationActivity.this.getApplicationContext();

        sendMediaMessage(result.getRecipientId(),
                         result.getMessageSendType(),
                         result.getBody(),
                         slideDeck,
                         quote,
                         Collections.emptyList(),
                         Collections.emptyList(),
                         mentions,
                         expiresIn,
                         result.isViewOnce(),
                         initiating,
                         true,
                         null).addListener(new AssertedSuccessListener<Void>() {
          @Override
          public void onSuccess(Void result) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
              Stream.of(slideDeck.getSlides())
                    .map(Slide::getUri)
                    .withoutNulls()
                    .filter(BlobProvider::isAuthority)
                    .forEach(uri -> BlobProvider.getInstance().delete(context, uri));
            });
          }
        });

        break;
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(STATE_REACT_WITH_ANY_PAGE, reactWithAnyEmojiStartPage);
    outState.putBoolean(STATE_IS_SEARCH_REQUESTED, isSearchRequested);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    reactWithAnyEmojiStartPage = savedInstanceState.getInt(STATE_REACT_WITH_ANY_PAGE, -1);
    isSearchRequested          = savedInstanceState.getBoolean(STATE_IS_SEARCH_REQUESTED, false);
  }

  private void setVisibleThread(long threadId) {
    if (!isInBubble()) {
      ApplicationDependencies.getMessageNotifier().setVisibleThread(ConversationId.forConversation(threadId));
    }
  }

  private void reportShortcutLaunch(@NonNull RecipientId recipientId) {
    ShortcutManagerCompat.reportShortcutUsed(this, ConversationUtil.getShortcutId(recipientId));
  }

  private void handleImageFromDeviceCameraApp() {
    if (attachmentManager.getCaptureUri() == null) {
      Log.w(TAG, "No image available.");
      return;
    }

    try {
      Uri mediaUri = BlobProvider.getInstance()
                                 .forData(getContentResolver().openInputStream(attachmentManager.getCaptureUri()), 0L)
                                 .withMimeType(MediaUtil.IMAGE_JPEG)
                                 .createForSingleSessionOnDisk(this);

      getContentResolver().delete(attachmentManager.getCaptureUri(), null, null);

      setMedia(mediaUri, MediaType.IMAGE);
    } catch (IOException ioe) {
      Log.w(TAG, "Could not handle public image", ioe);
    }
  }

  @Override
  public void startActivity(Intent intent) {
    if (intent.getStringExtra(Browser.EXTRA_APPLICATION_ID) != null) {
      intent.removeExtra(Browser.EXTRA_APPLICATION_ID);
    }

    try {
      super.startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Log.w(TAG, e);
      Toast.makeText(this, R.string.ConversationActivity_there_is_no_app_available_to_handle_this_link_on_your_device, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    GroupActiveState groupActiveState = groupViewModel.getGroupActiveState().getValue();
    boolean          isActiveGroup    = groupActiveState != null && groupActiveState.isActiveGroup();
    boolean          isActiveV2Group  = groupActiveState != null && groupActiveState.isActiveV2Group();
    boolean          isInActiveGroup  = groupActiveState != null && !groupActiveState.isActiveGroup();

    if (isBlocked()) {
      if (isActiveGroup) {
        inflater.inflate(R.menu.conversation_message_requests_group, menu);
      }

      inflater.inflate(R.menu.conversation_message_requests, menu);

      if (recipient != null && recipient.get().isMuted())
        inflater.inflate(R.menu.conversation_muted, menu);
      else inflater.inflate(R.menu.conversation_unmuted, menu);

      super.onCreateOptionsMenu(menu);
      return true;
    }

    if (isSecureText) {
      if (recipient.get().getExpiresInSeconds() > 0) {
        if (!isInActiveGroup) {
          inflater.inflate(R.menu.conversation_expiring_on, menu);
        }
        titleView.showExpiring(recipient);
      } else {
        if (!isInActiveGroup) {
          inflater.inflate(R.menu.conversation_expiring_off, menu);
        }
        titleView.clearExpiring();
      }
    }

    if (isSingleConversation()) {
      if (isSecureText) inflater.inflate(R.menu.conversation_callable_secure, menu);
      else inflater.inflate(R.menu.conversation_callable_insecure, menu);

      mRecyclerview.setAdapter(mTLAdapter);
      mTLAdapter.setOnItemClickListener(new TwoLineMenuAdapter.OnItemClickListener() {
        @Override
        public void onClick(int position) {
//          View v= mRecyclerview.getChildAt(position);
          View v = mRecyclerview.getLayoutManager().findViewByPosition(position);

          TextView tx       = v.findViewById(R.id.menu_item);
          String   textname = tx.getText().toString();
          if (getString(R.string.conversation_callable_insecure__menu_call).equals(textname)) {
            handleDial(getRecipient(), true);
          } else if (getString(R.string.conversation__menu_conversation_settings).equals(textname)) {
            handleConversationSettings();
          } else if (getString(R.string.ExpirationDialog_disappearing_messages).equals(textname)) {
            handleSelectMessageExpiration();
          } else if (getString(R.string.conversation_insecure__invite).equals(textname)) {
            handleInviteLink();
            mRecyclerview.setVisibility(View.GONE);
            composeText.requestFocus();
            mVG.setVisibility(View.VISIBLE);
          } else if (getString(R.string.conversation__menu_voice_message).equals(textname)) {
            boolean hasPermission = lacksPermission(AudioPermissions[0]);
            if (hasPermission) {
              Permissions.with(ConversationActivity.this)
                         .request(AudioPermissions[0])
                         .ifNecessary()
                         .onAllGranted(() -> {
                           sendVoiceMessage();
                         })
                         .execute();
            } else {
              sendVoiceMessage();
            }

            //onRecorderFinished();
            //onRecorderStarted();
          } else if (textname.startsWith(getString(R.string.conversation_activity__send))) {
            sendText.performClick();
            mRecyclerview.setVisibility(View.GONE);
            mVG.setVisibility(View.VISIBLE);
            fragment.getListLayoutManager().smoothScrollToPosition(ConversationActivity.this, 0, 100);
            panelParent.setVisibility(View.VISIBLE);
            composeText.requestFocus();
            //onRecorderFinished();
            //onRecorderStarted();
          } else if (getString(R.string.conversation_secure_verified__menu_reset_secure_session).equals(textname)) {
            handleResetSecureSession_MP02();
          } else if (getString(R.string.conversation_add_to_contacts__menu_add_to_contacts).equals(textname) && isAddContacts) {
            handleAddToContacts();
          } else if (getString(R.string.conversation_list_search_description).equals(textname)) {
            mRecyclerview.setVisibility(View.GONE);
            mVG.setVisibility(View.VISIBLE);
            handleSearch();
          }
        }
      });

    } else if (isGroupConversation()) {
      if (isActiveV2Group && Build.VERSION.SDK_INT > 19) {
        inflater.inflate(R.menu.conversation_callable_groupv2, menu);
        if (groupCallViewModel != null && Boolean.TRUE.equals(groupCallViewModel.hasActiveGroupCall().getValue())) {
          hideMenuItem(menu, R.id.menu_video_secure);
        }
        showGroupCallingTooltip();
      }

      inflater.inflate(R.menu.conversation_group_options, menu);

      mRecyclerview.setAdapter(mTLAdapterGp);
      mTLAdapterGp.setOnItemClickListener(new TwoLineMenuAdapter.OnItemClickListener() {
        @Override
        public void onClick(int position) {
          //View v= mRecyclerview.getChildAt(position);
          View v = mRecyclerview.getLayoutManager().findViewByPosition(position);
//          mRecyclerview.getLayoutManager().findViewByPosition(0).findViewById(R.id.menu_item).
//                  setOnClickListener(new SendButtonListener());
          TextView tx       = v.findViewById(R.id.menu_item);
          String   textname = tx.getText().toString();
          if (getString(R.string.conversation__menu_edit_group).equals(textname)) {
            handleEditPushGroup();
          } else if (getString(R.string.conversation__menu_conversation_settings).equals(textname)) {
            handleConversationSettings();
          } else if (getString(R.string.conversation__menu_leave_group).equals(textname)) {
            handleLeavePushGroup();
          } else if (getString(R.string.conversation__menu_voice_message).equals(textname)) {
            //onRecorderFinished();
            //onRecorderStarted();
            boolean hasPermission = lacksPermission(AudioPermissions[0]);
            if (hasPermission) {
              Permissions.with(ConversationActivity.this)
                         .request(AudioPermissions[0])
                         .ifNecessary()
                         .onAllGranted(() -> {
                           sendVoiceMessage();
                         })
                         .execute();
            } else {
              sendVoiceMessage();
            }
          } else if (getString(R.string.ExpirationDialog_disappearing_messages).equals(textname)) {
            handleSelectMessageExpiration();
          } else if (textname.startsWith(getString(R.string.conversation_activity__send))) {
            sendText.performClick();
            mRecyclerview.setVisibility(View.GONE);
            mVG.setVisibility(View.VISIBLE);
            fragment.getListLayoutManager().smoothScrollToPosition(ConversationActivity.this, 0, 100);
            panelParent.setVisibility(View.VISIBLE);
            composeText.requestFocus();
          } else if (getString(R.string.conversation_list_search_description).equals(textname)) {
            mRecyclerview.setVisibility(View.GONE);
            mVG.setVisibility(View.VISIBLE);
            handleSearch();
          } else if (getString(R.string.MessageRecord_group_call).equals(textname)) {
            handleVideo(getRecipient());
          }
        }
      });

      if (!isPushGroupConversation()) {
        inflater.inflate(R.menu.conversation_mms_group_options, menu);
        if (distributionType == ThreadTable.DistributionTypes.BROADCAST) {
          menu.findItem(R.id.menu_distribution_broadcast).setChecked(true);
        } else {
          menu.findItem(R.id.menu_distribution_conversation).setChecked(true);
        }
      }

      inflater.inflate(R.menu.conversation_active_group_options, menu);
    }

    inflater.inflate(R.menu.conversation, menu);

    if (isSingleConversation() && isSecureText) {
      inflater.inflate(R.menu.conversation_secure, menu);
    } else if (isSingleConversation()) {
      inflater.inflate(R.menu.conversation_insecure, menu);
    }

    if (recipient != null && recipient.get().isMuted())
      inflater.inflate(R.menu.conversation_muted, menu);
    else inflater.inflate(R.menu.conversation_unmuted, menu);

    if (isSingleConversation() && getRecipient().getContactUri() == null) {
      inflater.inflate(R.menu.conversation_add_to_contacts, menu);
    }

    if (recipient != null && recipient.get().isSelf()) {
      if (isSecureText) {
        hideMenuItem(menu, R.id.menu_call_secure);
        hideMenuItem(menu, R.id.menu_video_secure);
      } else {
        hideMenuItem(menu, R.id.menu_call_insecure);
      }

      hideMenuItem(menu, R.id.menu_mute_notifications);
    }

    if (recipient != null && recipient.get().isBlocked()) {
      if (isSecureText) {
        hideMenuItem(menu, R.id.menu_call_secure);
        hideMenuItem(menu, R.id.menu_video_secure);
        hideMenuItem(menu, R.id.menu_expiring_messages);
        hideMenuItem(menu, R.id.menu_expiring_messages_off);
      } else {

        hideMenuItem(menu, R.id.menu_call_insecure);
      }

      hideMenuItem(menu, R.id.menu_mute_notifications);
    }

    hideMenuItem(menu, R.id.menu_group_recipients);

    if (isActiveV2Group) {
      hideMenuItem(menu, R.id.menu_mute_notifications);
      hideMenuItem(menu, R.id.menu_conversation_settings);
    } else if (isGroupConversation()) {
      hideMenuItem(menu, R.id.menu_conversation_settings);
    }

    hideMenuItem(menu, R.id.menu_create_bubble);
    disposables.add(viewModel.canShowAsBubble().subscribe(canShowAsBubble -> {
      MenuItem item = menu.findItem(R.id.menu_create_bubble);

      if (item != null) {
        item.setVisible(canShowAsBubble && !isInBubble());
      }
    }));

    if (threadId == -1L) {
      hideMenuItem(menu, R.id.menu_view_media);
    }

    searchViewItem = menu.findItem(R.id.menu_search);

    SearchView searchView = (SearchView) searchViewItem.getActionView();
    SearchView.OnQueryTextListener queryListener = new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        searchViewModel.onQueryUpdated(query, threadId, true);
        searchNav.showLoading();
        fragment.onSearchQueryUpdated(query);
        return true;
      }

      @Override
      public boolean onQueryTextChange(String query) {
        searchViewModel.onQueryUpdated(query, threadId, false);
        searchNav.showLoading();
        fragment.onSearchQueryUpdated(query);
        return true;
      }
    };

    searchViewItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
      @Override
      public boolean onMenuItemActionExpand(MenuItem item) {
        searchView.setOnQueryTextListener(queryListener);
        searchViewModel.onSearchOpened();
        searchNav.setVisibility(View.VISIBLE);
        searchNav.setData(0, 0);
        inputPanel.setHideForSearch(true);

        for (int i = 0; i < menu.size(); i++) {
          if (!menu.getItem(i).equals(searchViewItem)) {
            menu.getItem(i).setVisible(false);
          }
        }
        return true;
      }

      @Override
      public boolean onMenuItemActionCollapse(MenuItem item) {
        searchView.setOnQueryTextListener(null);
        isSearchRequested = false;
        searchViewModel.onSearchClosed();
        searchNav.setVisibility(View.GONE);
        inputPanel.setHideForSearch(false);
        fragment.onSearchQueryUpdated(null);
        setBlockedUserState(recipient.get(), isSecureText, isDefaultSms);
        invalidateOptionsMenu();
        return true;
      }
    });

    if (isSearchRequested) {
      if (searchViewItem.expandActionView()) {
        searchViewModel.onSearchOpened();
      }
    }

    super.onCreateOptionsMenu(menu);
    return true;
  }

  private void sendVoiceMessage() {
    inputPanel.onRecordPressed();
    myRT.setVisibility(View.VISIBLE);
    record_flag = 1;
    my_record_time.setFocusableInTouchMode(true);
    my_record_time.requestFocus();
  }

  @Override
  public void invalidateOptionsMenu() {
    if (!isSearchRequested) {
      super.invalidateOptionsMenu();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case R.id.menu_call_secure:
        handleDial(getRecipient(), true);
        return true;
      case R.id.menu_video_secure:
        handleVideo(getRecipient());
        return true;
      case R.id.menu_call_insecure:
        handleDial(getRecipient(), false);
        return true;
      case R.id.menu_view_media:
        handleViewMedia();
        return true;
      case R.id.menu_add_shortcut:
        handleAddShortcut();
        return true;
      case R.id.menu_search:
        handleSearch();
        return true;
      case R.id.menu_add_to_contacts:
        handleAddToContacts();
        return true;
      case R.id.menu_reset_secure_session:
        handleResetSecureSession();
        return true;
      case R.id.menu_group_recipients:
        handleDisplayGroupRecipients();
        return true;
      case R.id.menu_distribution_broadcast:
        handleDistributionBroadcastEnabled(item);
        return true;
      case R.id.menu_distribution_conversation:
        handleDistributionConversationEnabled(item);
        return true;
      case R.id.menu_group_settings:
        handleManageGroup();
        return true;
      case R.id.menu_leave:
        handleLeavePushGroup();
        return true;
      case R.id.menu_invite:
        handleInviteLink();
        return true;
      case R.id.menu_mute_notifications:
        handleMuteNotifications();
        return true;
      case R.id.menu_unmute_notifications:
        handleUnmuteNotifications();
        return true;
      case R.id.menu_conversation_settings:
        handleConversationSettings();
        return true;
      case R.id.menu_expiring_messages_off:
      case R.id.menu_expiring_messages:
        handleSelectMessageExpiration();
        return true;
      case R.id.menu_create_bubble:
        handleCreateBubble();
        return true;
      case android.R.id.home:
        super.onBackPressed();
        return true;
    }

    return false;
  }

  @Override
  public boolean onMenuOpened(int featureId, Menu menu) {
    if (menu == null) {
      return super.onMenuOpened(featureId, null);
    }

    if (!SignalStore.uiHints().hasSeenGroupSettingsMenuToast()) {
      MenuItem settingsMenuItem = menu.findItem(R.id.menu_group_settings);

      if (settingsMenuItem != null && settingsMenuItem.isVisible()) {
        Toast toast = Toast.makeText(this, R.string.ConversationActivity__more_options_now_in_group_settings, Toast.LENGTH_SHORT);

        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        SignalStore.uiHints().markHasSeenGroupSettingsMenuToast();
      }
    }

    return super.onMenuOpened(featureId, menu);
  }

  @Override
  public void onBackPressed() {
    Log.d(TAG, "onBackPressed()");
    if (reactionDelegate.isShowing()) {
      reactionDelegate.hide();
    } else if (container.isInputOpen()) {
      container.hideCurrentInput(composeText);
    } else if (isSearchRequested) {
      if (searchViewItem != null) {
        searchViewItem.collapseActionView();
      }
    } else if (isInBubble()) {
      super.onBackPressed();
    } else {
      finish();
    }
  }

  @Override
  public void onKeyboardShown() {
    inputPanel.onKeyboardShown();
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onEvent(ReminderUpdateEvent event) {
    updateReminders();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  public void onAttachmentMediaClicked(@NonNull Media media) {
    linkPreviewViewModel.onUserCancel();
    startActivityForResult(MediaSelectionActivity.editor(this, sendButton.getSelectedSendType(), Collections.singletonList(media), recipient.getId(), composeText.getTextTrimmed()), MEDIA_SENDER);
    container.hideCurrentInput(composeText);
  }

  @Override
  public void onAttachmentSelectorClicked(@NonNull AttachmentKeyboardButton button) {
    switch (button) {
      case GALLERY:
        AttachmentManager.selectGallery(this.fragment, MEDIA_SENDER, recipient.get(), composeText.getTextTrimmed(), sendButton.getSelectedSendType(), inputPanel.getQuote().isPresent());
        break;
      case FILE:
        AttachmentManager.selectDocument(this.fragment, PICK_DOCUMENT);
        break;
      case CONTACT:
        AttachmentManager.selectContactInfo(this.fragment, PICK_CONTACT);
        break;
      case LOCATION:
        AttachmentManager.selectLocation(this.fragment, PICK_LOCATION, getSendButtonColor(sendButton.getSelectedSendType()));
        break;
      case PAYMENT:
        AttachmentManager.selectPayment(this.fragment, recipient.get());
        break;
    }

    container.hideCurrentInput(composeText);
  }

  @Override
  public void onAttachmentPermissionsRequested() {
    Permissions.with(this)
               .request(Manifest.permission.READ_EXTERNAL_STORAGE)
               .onAllGranted(() -> viewModel.onAttachmentKeyboardOpen())
               .withPermanentDenialDialog(getString(R.string.AttachmentManager_signal_requires_the_external_storage_permission_in_order_to_attach_photos_videos_or_audio))
               .execute();
  }

//////// Event Handlers

  private @ColorInt int getSendButtonColor(MessageSendType newTransport) {
    if (newTransport.usesSmsTransport()) {
      return getResources().getColor(newTransport.getBackgroundColorRes());
    } else if (recipient != null) {
      return getRecipient().getChatColors().asSingleColor();
    } else {
      return getResources().getColor(newTransport.getBackgroundColorRes());
    }
  }

  private void handleSelectMessageExpiration() {
    if (isPushGroupConversation() && !isActiveGroup()) {
      return;
    }

    startActivity(RecipientDisappearingMessagesActivity.forRecipient(this, recipient.getId()));
  }

  private void handleMuteNotifications() {
    MuteDialog.show(this, until -> {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          SignalDatabase.recipients()
                        .setMuted(recipient.getId(), until);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    });
  }

  private void handleConversationSettings() {
    if (isGroupConversation()) {
      handleManageGroup();
      return;
    }

    if (isBlocked()) return;

    Intent intent = ManageRecipientActivity.newIntentFromConversation(this, recipient.getId());
    startActivitySceneTransition(intent, titleView.findViewById(R.id.subtitle), "avatar");

//    Intent intent = ConversationSettingsActivity.forRecipient(this, recipient.getId());
//    Bundle bundle = ConversationSettingsActivity.createTransitionBundle(this, titleView.findViewById(R.id.contact_photo_image), toolbar);

//    ActivityCompat.startActivity(this, intent, bundle);
  }

  private void handleUnmuteNotifications() {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        SignalDatabase.recipients()
                      .setMuted(recipient.getId(), 0);

        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void handleUnblock() {
    final Context context = this.getApplicationContext();
    BlockUnblockDialog.showUnblockFor(this, getLifecycle(), recipient.get(), () -> {
      SignalExecutors.BOUNDED.execute(() -> {
        RecipientUtil.unblock(recipient.get());
      });
    });
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private void handleMakeDefaultSms() {
    if (TextSecurePreferences.isPushRegistered(this) && makeDefaultSmsButton.getVisibility() == View.VISIBLE) {
      new AlertDialog.Builder(this)
          .setMessage(R.string.ConversationActivity_other_party_disable_messages)
          .setNegativeButton(android.R.string.ok, null)
          .show();
      return;
    }
//        startActivityForResult(SmsUtil.getSmsRoleIntent(this), SMS_DEFAULT);
  }

  private void handleRegisterForSignal() {
    startActivity(RegistrationNavigationActivity.newIntentForReRegistration(this));
  }

  private void handleInviteLink() {
    String inviteText = getString(R.string.ConversationActivity_lets_switch_to_signal, getString(R.string.install_url));

    if (isDefaultSms) {
      composeText.appendInvite(inviteText);
    } else {
      Intent intent = new Intent(Intent.ACTION_SENDTO);
      intent.setData(Uri.parse("smsTo:" + recipient.get().requireSmsAddress()));
      intent.putExtra("sms_body", inviteText);
      intent.putExtra(Intent.EXTRA_TEXT, inviteText);
      startActivity(intent);
    }
  }

  private void handleResetSecureSession_MP02() {
    String rationaleDialogMessage = getString(R.string.ConversationActivity_reset_secure_session_question) + getString(R.string.ConversationActivity_this_may_help_if_youre_having_encryption_problems);

    android.app.AlertDialog dialog = RationaleDialog.createNonMsgDialog(this,
                                                                        rationaleDialogMessage,
                                                                        R.string.ConversationActivity_reset,
                                                                        android.R.string.cancel,
                                                                        () -> {
                                                                          if (isSingleConversation()) {
                                                                            final Context context = getApplicationContext();

                                                                            OutgoingEndSessionMessage endSessionMessage =
                                                                                new OutgoingEndSessionMessage(new OutgoingTextMessage(getRecipient(), "TERMINATE", 0, -1));

                                                                            new AsyncTask<OutgoingEndSessionMessage, Void, Long>() {
                                                                              @Override
                                                                              protected Long doInBackground(OutgoingEndSessionMessage... messages) {
                                                                                return MessageSender.send(context, messages[0], threadId, false, null, null);
                                                                              }

                                                                              @Override
                                                                              protected void onPostExecute(Long result) {
                                                                                sendComplete(result);
                                                                              }
                                                                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, endSessionMessage);
                                                                          }
                                                                          Log.e(TAG, "handleResetSecureSession_MP02");
//                    mRecyclerview.setVisibility(View.GONE);
                                                                          composeText.requestFocus();
                                                                          mRecyclerview.setVisibility(View.GONE);
                                                                        },
                                                                        null,
                                                                        null);
    dialog.show();
  }


  private void handleResetSecureSession() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.ConversationActivity_reset_secure_session_question);
    builder.setIcon(R.drawable.ic_warning);
    builder.setCancelable(true);
    builder.setMessage(R.string.ConversationActivity_this_may_help_if_youre_having_encryption_problems);
    builder.setPositiveButton(R.string.ConversationActivity_reset, (dialog, which) -> {
      if (isSingleConversation()) {
        final Context context = getApplicationContext();

        OutgoingEndSessionMessage endSessionMessage =
            new OutgoingEndSessionMessage(new OutgoingTextMessage(getRecipient(), "TERMINATE", 0, -1));

        new AsyncTask<OutgoingEndSessionMessage, Void, Long>() {
          @Override
          protected Long doInBackground(OutgoingEndSessionMessage... messages) {
            return MessageSender.send(context, messages[0], threadId, false, null, null);
          }

          @Override
          protected void onPostExecute(Long result) {
            sendComplete(result);
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, endSessionMessage);
      }
    });
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.show();
  }

  private void handleViewMedia() {
    startActivity(MediaOverviewActivity.forThread(this, threadId));
  }

  private void handleAddShortcut() {
    Log.i(TAG, "Creating home screen shortcut for recipient " + recipient.get().getId());

    final Context   context   = getApplicationContext();
    final Recipient recipient = this.recipient.get();

    GlideApp.with(this)
            .asBitmap()
            .load(recipient.getContactPhoto())
            .error(recipient.getFallbackContactPhoto().asDrawable(context, recipient.getAvatarColor(), false))
            .into(new CustomTarget<Bitmap>() {
              @Override
              public void onLoadFailed(@Nullable Drawable errorDrawable) {
                if (errorDrawable == null) {
                  throw new AssertionError();
                }

                Log.w(TAG, "Utilizing fallback photo for shortcut for recipient " + recipient.getId());

                SimpleTask.run(() -> DrawableUtil.toBitmap(errorDrawable, SHORTCUT_ICON_SIZE, SHORTCUT_ICON_SIZE),
                               bitmap -> addIconToHomeScreen(context, bitmap, recipient));
              }

              @Override
              public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                SimpleTask.run(() -> BitmapUtil.createScaledBitmap(resource, SHORTCUT_ICON_SIZE, SHORTCUT_ICON_SIZE),
                               bitmap -> addIconToHomeScreen(context, bitmap, recipient));
              }

              @Override
              public void onLoadCleared(@Nullable Drawable placeholder) {
              }
            });

  }

  private void handleCreateBubble() {
    ConversationIntents.Args args = viewModel.getArgs();

    BubbleUtil.displayAsBubble(this, args.getRecipientId(), args.getThreadId());
    finish();
  }

  private static void addIconToHomeScreen(@NonNull Context context,
                                          @NonNull Bitmap bitmap,
                                          @NonNull Recipient recipient)
  {
    IconCompat icon = IconCompat.createWithAdaptiveBitmap(bitmap);
    String name = recipient.isSelf() ? context.getString(R.string.note_to_self)
                                     : recipient.getDisplayName(context);

    ShortcutInfoCompat shortcutInfoCompat = new ShortcutInfoCompat.Builder(context, recipient.getId().serialize() + '-' + System.currentTimeMillis())
        .setShortLabel(name)
        .setIcon(icon)
        .setIntent(ShortcutLauncherActivity.createIntent(context, recipient.getId()))
        .build();

    if (ShortcutManagerCompat.requestPinShortcut(context, shortcutInfoCompat, null)) {
      Toast.makeText(context, context.getString(R.string.ConversationActivity_added_to_home_screen), Toast.LENGTH_LONG).show();
    }

    bitmap.recycle();
  }

  private void handleSearch() {
    searchViewModel.onSearchOpened();
  }

  private void handleLeavePushGroup() {
    if (getRecipient() == null) {
      Toast.makeText(this, getString(R.string.ConversationActivity_invalid_recipient),
                     Toast.LENGTH_LONG).show();
      return;
    }

    LeaveGroupDialog.handleLeavePushGroup(this, getRecipient().requireGroupId().requirePush(), this::finish);
  }

  private void handleEditPushGroup() {
    startActivityForResult(EditGroupActivity.newIntent(ConversationActivity.this, recipient.get().requireGroupId()),
                           GROUP_EDIT);
  }

  private void handleManageGroup() {
    startActivityForResult(ManageGroupActivity.newIntent(ConversationActivity.this, recipient.get().requireGroupId()),
                           GROUP_EDIT);
  }

  private void handleDistributionBroadcastEnabled(MenuItem item) {
    distributionType = ThreadTable.DistributionTypes.BROADCAST;
    item.setChecked(true);

    if (threadId != -1) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          SignalDatabase.threads()
                        .setDistributionType(threadId, ThreadTable.DistributionTypes.BROADCAST);
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private void handleDistributionConversationEnabled(MenuItem item) {
    distributionType = ThreadTable.DistributionTypes.CONVERSATION;
    item.setChecked(true);

    if (threadId != -1) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          SignalDatabase.threads()
                        .setDistributionType(threadId, ThreadTable.DistributionTypes.CONVERSATION);
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private void handleDial(final Recipient recipient, boolean isSecure) {
    if (recipient == null) return;

    if (isSecure) {
      CommunicationActions.startVoiceCall(this, recipient);
    } else {
      CommunicationActions.startInsecureCall(this, recipient);
    }
  }

  private void handleVideo(final Recipient recipient) {
    if (recipient == null) return;

    if (recipient.isPushV2Group() && groupCallViewModel.hasActiveGroupCall().getValue() == Boolean.FALSE && groupViewModel.isNonAdminInAnnouncementGroup()) {
      new MaterialAlertDialogBuilder(this).setTitle(R.string.ConversationActivity_cant_start_group_call)
                                          .setMessage(R.string.ConversationActivity_only_admins_of_this_group_can_start_a_call)
                                          .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                                          .show();
    } else {
      CommunicationActions.startVideoCall(this, recipient);
    }
  }

  private void handleDisplayGroupRecipients() {
    new GroupMembersDialog(this, getRecipient()).display();
  }

  private void handleAddToContacts() {
    if (recipient.get().isGroup()) return;

    try {
      startActivityForResult(RecipientExporter.export(recipient.get()).asAddContactIntent(), ADD_CONTACT);
    } catch (ActivityNotFoundException e) {
      Log.w(TAG, e);
    }
  }

  private boolean handleDisplayQuickContact() {
    if (isBlocked() || recipient.get().isGroup()) return false;

    if (recipient.get().getContactUri() != null) {
      ContactsContract.QuickContact.showQuickContact(ConversationActivity.this, titleView, recipient.get().getContactUri(), ContactsContract.QuickContact.MODE_LARGE, null);
    } else {
      handleAddToContacts();
    }

    return true;
  }

  private void handleAddAttachment() {
    if (this.isMmsEnabled || isSecureText) {
      viewModel.getRecentMedia().removeObservers(this);

      if (attachmentKeyboardStub.resolved() && container.isInputOpen() && container.getCurrentInput() == attachmentKeyboardStub.get()) {
        container.showSoftkey(composeText);
      } else {
        viewModel.getRecentMedia().observe(this, media -> attachmentKeyboardStub.get().onMediaChanged(media));
        attachmentKeyboardStub.get().setCallback(this);

        updatePaymentsAvailable();

        container.show(composeText, attachmentKeyboardStub.get());

        viewModel.onAttachmentKeyboardOpen();
      }
    } else {
      handleManualMmsRequired();
    }
  }

  private void updatePaymentsAvailable() {
    if (!attachmentKeyboardStub.resolved()) {
      return;
    }
    PaymentsValues paymentsValues = SignalStore.paymentsValues();
    if (paymentsValues.getPaymentsAvailability().isSendAllowed() &&
        !recipient.get().isSelf() &&
        !recipient.get().isGroup() &&
        recipient.get().isRegistered() &&
        !recipient.get().isForceSmsSelection())
    {
      attachmentKeyboardStub.get().filterAttachmentKeyboardButtons(null);
    } else {
      attachmentKeyboardStub.get().filterAttachmentKeyboardButtons(btn -> btn != AttachmentKeyboardButton.PAYMENT);
    }
  }

  private void handleManualMmsRequired() {
    Toast.makeText(this, R.string.MmsDownloader_error_reading_mms_settings, Toast.LENGTH_LONG).show();

    Bundle extras = getIntent().getExtras();
    Intent intent = new Intent(this, PromptMmsActivity.class);
    if (extras != null) intent.putExtras(extras);
    startActivity(intent);
  }

  private void handleRecentSafetyNumberChange() {
    List<IdentityRecord> records = identityRecords.getUnverifiedRecords();
    records.addAll(identityRecords.getUntrustedRecords());
    SafetyNumberBottomSheet
        .forIdentityRecordsAndDestination(
            records,
            new ContactSearchKey.RecipientSearchKey.KnownRecipient(recipient.getId())
        )
        .show(getSupportFragmentManager());
  }

  @Override
  public void onSendAnywayAfterSafetyNumberChange(@NonNull List<RecipientId> changedRecipients) {
    Log.d(TAG, "onSendAnywayAfterSafetyNumberChange");
    initializeIdentityRecords().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        sendMessage(null);
      }
    });
  }

  @Override
  public void onMessageResentAfterSafetyNumberChange() {
    Log.d(TAG, "onMessageResentAfterSafetyNumberChange");
    initializeIdentityRecords().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
      }
    });
  }

  @Override
  public void onCanceled() {
  }

  private void handleSecurityChange(boolean isSecureText, boolean isDefaultSms) {
    Log.i(TAG, "handleSecurityChange(" + isSecureText + ", " + isDefaultSms + ")");

    this.isSecureText          = isSecureText;
    this.isDefaultSms          = isDefaultSms;
    this.isSecurityInitialized = true;

    boolean isMediaMessage = recipient.get().isMmsGroup() || attachmentManager.isAttachmentPresent();

    sendButton.resetAvailableTransports(isMediaMessage);

    if (!isSecureText && !isPushGroupConversation() && !recipient.get().isServiceIdOnly()) {
      sendButton.disableTransportType(MessageSendType.TransportType.SIGNAL);
    }
    if (recipient.get().isPushGroup() || (!recipient.get().isMmsGroup() && !recipient.get().hasSmsAddress())) {
      sendButton.disableTransportType(MessageSendType.TransportType.SMS);
    }

    if (!recipient.get().isPushGroup() && recipient.get().isForceSmsSelection()) {
      sendButton.setDefaultTransport(MessageSendType.TransportType.SMS);
    } else {
      if (isSecureText || isPushGroupConversation() || recipient.get().isServiceIdOnly()) {
        sendButton.setDefaultTransport(MessageSendType.TransportType.SIGNAL);
      } else {
        sendButton.setDefaultTransport(MessageSendType.TransportType.SMS);
      }
    }

    calculateCharactersRemaining();
    invalidateOptionsMenu();
    setBlockedUserState(recipient.get(), isSecureText, isDefaultSms);
  }

  ///// Initializers

  private ListenableFuture<Boolean> initializeDraft(@NonNull ConversationIntents.Args args) {
    final SettableFuture<Boolean> result = new SettableFuture<>();

    final CharSequence   draftText        = args.getDraftText();
    final Uri            draftMedia       = getIntent().getData();
    final String         draftContentType = getIntent().getType();
    final MediaType      draftMediaType   = MediaType.from(draftContentType);
    final List<Media>    mediaList        = args.getMedia();
    final StickerLocator stickerLocator   = args.getStickerLocator();
    final boolean        borderless       = args.isBorderless();

    if (stickerLocator != null && draftMedia != null) {
      Log.d(TAG, "Handling shared sticker.");
      sendSticker(stickerLocator, Objects.requireNonNull(draftContentType), draftMedia, 0, true);
      return new SettableFuture<>(false);
    }

    if (draftMedia != null && draftContentType != null && borderless) {
      SimpleTask.run(getLifecycle(),
                     () -> getKeyboardImageDetails(draftMedia),
                     details -> sendKeyboardImage(draftMedia, draftContentType, details));
      return new SettableFuture<>(false);
    }

    if (!Util.isEmpty(mediaList)) {
      Log.d(TAG, "Handling shared Media.");
      Intent sendIntent = MediaSelectionActivity.editor(this, sendButton.getSelectedSendType(), mediaList, recipient.getId(), draftText);
      startActivityForResult(sendIntent, MEDIA_SENDER);
      return new SettableFuture<>(false);
    }

    if (draftText != null) {
      composeText.setText("");
      composeText.append(draftText);
      result.set(true);
    }

    if (draftMedia != null && draftMediaType != null) {
      Log.d(TAG, "Handling shared Data.");
      return setMedia(draftMedia, draftMediaType);
    }

    if (draftText == null && draftMedia == null && draftMediaType == null) {
      return initializeDraftFromDatabase();
    } else {
      updateToggleButtonState();
      result.set(false);
    }

    return result;
  }

  @NonNull @Override public VoiceNoteMediaController getVoiceNoteMediaController() {
    return null;
  }

  @Override public void openGifSearch() {

  }

  public static class TwoLineMenuAdapter extends RecyclerView.Adapter<TwoLineMenuAdapter.ViewHolder> {

    private List<String> mMenuList;
    public  int          mFocusHeight;
    public  int          mNormalHeight;
    public  int          mNormalPaddingX;
    public  int          mFocusPaddingX;
    public  int          mFocusTextSize;
    public  int          mNormalTextSize;
    private Animation    animDownAndGone, animDownAndVisible, animUpAndGone, animUpAndVisible;
    RelativeLayout rl;
    int            mfts;
    int            mfh;
    int            mt;

    public interface OnItemClickListener {
      void onClick(int position);
    }

    public OnItemClickListener mListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
      this.mListener = listener;
    }

    public TwoLineMenuAdapter(List<String> menulist) {
      mMenuList = menulist;
    }

    public TwoLineMenuAdapter(RelativeLayout relativeLayout, int marginTop, List<String> menulist) {
      mMenuList = menulist;
      mt        = marginTop;
      rl        = relativeLayout;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View      view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_menu_item, parent, false);
      Resources res  = parent.getContext().getResources();
      //Resources res = getResources();
      mFocusHeight  = res.getDimensionPixelSize(R.dimen.focus_item_height);
      mNormalHeight = res.getDimensionPixelSize(R.dimen.item_height);

      mFocusTextSize  = res.getDimensionPixelSize(R.dimen.focus_item_textsize);
      mNormalTextSize = res.getDimensionPixelSize(R.dimen.item_textsize);

      mFocusPaddingX  = res.getDimensionPixelSize(R.dimen.focus_item_padding_x);
      mNormalPaddingX = res.getDimensionPixelSize(R.dimen.item_padding_x);
      view.setOnFocusChangeListener(new OnFocusChangeListener() {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
          startFocusAnimationSingleLine(v, hasFocus);
          if (hasFocus) {
            ((TextView) view).setTextColor(Color.WHITE);
          } else {
            ((TextView) view).setTextColor(Color.GRAY);
          }

        }
      });
      ViewHolder holder_menu = new ViewHolder(view);
      return holder_menu;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
      String item = mMenuList.get(position);
      holder.itemView.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          mListener.onClick(position);
        }
      });
      holder.menutext.setText(item);
      holder.menutext.setTag(position);
    }

    private void startFocusAnimationSingleLine(View v, boolean focused) {
      ValueAnimator va;
      TextView      item = (TextView) v;

      if (focused) {
        va = ValueAnimator.ofFloat(0, 1);
      } else {
        va = ValueAnimator.ofFloat(1, 0);
      }

      va.addUpdateListener(valueAnimator -> {
        float scale    = (float) valueAnimator.getAnimatedValue();
        float height   = ((float) (mFocusHeight - mNormalHeight)) * (scale) + (float) mNormalHeight;
        float textSize = ((float) (mFocusTextSize - mNormalTextSize)) * (scale) + (float) mNormalTextSize;
        float padding  = (float) mNormalPaddingX - ((float) (mNormalPaddingX - mFocusPaddingX)) * (scale);
        int   alpha    = (int) ((float) 0x81 + (float) ((0xff - 0x81)) * (scale));
        int   color    = alpha * 0x1000000 + 0xffffff;
        item.setTextSize((int) textSize);
        item.setTextColor(color);
        item.setPadding(
            (int) padding, item.getPaddingTop(),
            item.getPaddingRight(), item.getPaddingBottom());
        item.getLayoutParams().height = (int) height;
      });
      FastOutLinearInInterpolator FastOutLinearInInterpolator = new FastOutLinearInInterpolator();
      va.setInterpolator(FastOutLinearInInterpolator);
      if (focused) {
        va.setDuration(270);
        va.start();
      } else {
        va.setDuration(270);
        va.start();
      }
      item.setEllipsize(TextUtils.TruncateAt.MARQUEE);
    }

    public void removeData(int position) {
      mMenuList.remove(position);
      notifyItemRemoved(position);
      notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
      return mMenuList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
      TextView menutext;

      public ViewHolder(@NonNull View itemView) {
        super(itemView);
        menutext = (TextView) itemView.findViewById(R.id.menu_item);
      }
    }
  }

  private void initializeEnabledCheck() {
    groupViewModel.getSelfMemberLevel().observe(this, selfMembership -> {
      boolean canSendMessages;
      boolean leftGroup;
      boolean canCancelRequest;

      if (selfMembership == null) {
        leftGroup        = false;
        canSendMessages  = true;
        canCancelRequest = false;
        if (cannotSendInAnnouncementGroupBanner.resolved()) {
          cannotSendInAnnouncementGroupBanner.get().setVisibility(View.GONE);
        }
      } else {
        switch (selfMembership.getMemberLevel()) {
          case NOT_A_MEMBER:
            leftGroup = true;
            canSendMessages = false;
            canCancelRequest = false;
            break;
          case PENDING_MEMBER:
            leftGroup = false;
            canSendMessages = false;
            canCancelRequest = false;
            break;
          case REQUESTING_MEMBER:
            leftGroup = false;
            canSendMessages = false;
            canCancelRequest = true;
            break;
          case FULL_MEMBER:
          case ADMINISTRATOR:
            leftGroup = false;
            canSendMessages = true;
            canCancelRequest = false;
            break;
          default:
            throw new AssertionError();
        }
        if (!leftGroup && !canCancelRequest && selfMembership.isAnnouncementGroup() && selfMembership.getMemberLevel() != GroupTable.MemberLevel.ADMINISTRATOR) {
          canSendMessages = false;
          cannotSendInAnnouncementGroupBanner.get().setVisibility(View.VISIBLE);
          cannotSendInAnnouncementGroupBanner.get().setMovementMethod(LinkMovementMethod.getInstance());
          cannotSendInAnnouncementGroupBanner.get().setText(SpanUtil.clickSubstring(this, R.string.ConversationActivity_only_s_can_send_messages, R.string.ConversationActivity_admins, v -> {
            ShowAdminsBottomSheetDialog.show(getSupportFragmentManager(), getRecipient().requireGroupId().requireV2());
          }));
        } else if (cannotSendInAnnouncementGroupBanner.resolved()) {
          cannotSendInAnnouncementGroupBanner.get().setVisibility(View.GONE);
        }
      }

      noLongerMemberBanner.setVisibility(leftGroup ? View.VISIBLE : View.GONE);
      requestingMemberBanner.setVisibility(canCancelRequest ? View.VISIBLE : View.GONE);
      if (canCancelRequest) {
        cancelJoinRequest.setOnClickListener(v -> ConversationGroupViewModel.onCancelJoinRequest(getRecipient(), new AsynchronousCallback.MainThread<Void, GroupChangeFailureReason>() {
          @Override
          public void onComplete(@Nullable Void result) {
            Log.d(TAG, "Cancel request complete");
          }

          @Override
          public void onError(@Nullable GroupChangeFailureReason error) {
            Log.d(TAG, "Cancel join request failed " + error);
            Toast.makeText(ConversationActivity.this, GroupErrors.getUserDisplayMessage(error), Toast.LENGTH_SHORT).show();
          }
        }.toWorkerCallback()));
      }

      inputPanel.setHideForGroupState(!canSendMessages);
      inputPanel.setEnabled(canSendMessages);
      sendButton.setEnabled(canSendMessages);
//      attachButton.setEnabled(canSendMessages);
    });
  }

  private void initializePendingRequestsBanner() {
    groupViewModel.getActionableRequestingMembers()
                  .observe(this, actionablePendingGroupRequests -> updateReminders());
  }

  private void initializeGroupV1MigrationsBanners() {
    groupViewModel.getGroupV1MigrationSuggestions()
                  .observe(this, s -> updateReminders());
  }


  private ListenableFuture<Boolean> initializeDraftFromDatabase() {
    SettableFuture<Boolean> future = new SettableFuture<>();

    new AsyncTask<Void, Void, Pair<DraftTable.Drafts, CharSequence>>() {
      @Override
      protected Pair<DraftTable.Drafts, CharSequence> doInBackground(Void... params) {
        Context           context       = ConversationActivity.this;
        DraftTable        draftDatabase = SignalDatabase.drafts();
        DraftTable.Drafts results       = draftDatabase.getDrafts(threadId);
        DraftTable.Draft  mentionsDraft = results.getDraftOfType(DraftTable.Draft.MENTION);
        Spannable         updatedText   = null;

        if (mentionsDraft != null) {
          String                 text     = results.getDraftOfType(DraftTable.Draft.TEXT).getValue();
          List<Mention>          mentions = MentionUtil.bodyRangeListToMentions(context, Base64.decodeOrThrow(mentionsDraft.getValue()));
          UpdatedBodyAndMentions updated  = MentionUtil.updateBodyAndMentionsWithDisplayNames(context, text, mentions);

          updatedText = new SpannableString(updated.getBody());
          MentionAnnotation.setMentionAnnotations(updatedText, updated.getMentions());
        }

        draftDatabase.clearDrafts(threadId);

        return new Pair<>(results, updatedText);
      }

      @Override
      protected void onPostExecute(Pair<DraftTable.Drafts, CharSequence> draftsWithUpdatedMentions) {
        DraftTable.Drafts drafts      = Objects.requireNonNull(draftsWithUpdatedMentions.first());
        CharSequence      updatedText = draftsWithUpdatedMentions.second();

        if (drafts.isEmpty()) {
          future.set(false);
          updateToggleButtonState();
          return;
        }

        AtomicInteger draftsRemaining = new AtomicInteger(drafts.size());
        AtomicBoolean success         = new AtomicBoolean(false);
        ListenableFuture.Listener<Boolean> listener = new AssertedSuccessListener<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {
            success.compareAndSet(false, result);

            if (draftsRemaining.decrementAndGet() <= 0) {
              future.set(success.get());
            }
          }
        };

        for (DraftTable.Draft draft : drafts) {
          try {
            switch (draft.getType()) {
              case DraftTable.Draft.TEXT:
                composeText.setText(updatedText == null ? draft.getValue() : updatedText);
                listener.onSuccess(true);
                break;
              case DraftTable.Draft.LOCATION:
                attachmentManager.setLocation(SignalPlace.deserialize(draft.getValue()), getCurrentMediaConstraints()).addListener(listener);
                break;
              case DraftTable.Draft.IMAGE:
                setMedia(Uri.parse(draft.getValue()), MediaType.IMAGE).addListener(listener);
                break;
              case DraftTable.Draft.AUDIO:
                setMedia(Uri.parse(draft.getValue()), MediaType.AUDIO).addListener(listener);
                break;
              case DraftTable.Draft.VIDEO:
                setMedia(Uri.parse(draft.getValue()), MediaType.VIDEO).addListener(listener);
                break;
              case DraftTable.Draft.QUOTE:
                SettableFuture<Boolean> quoteResult = new SettableFuture<>();
                new QuoteRestorationTask(draft.getValue(), quoteResult).execute();
                quoteResult.addListener(listener);
                break;
            }
          } catch (IOException e) {
            Log.w(TAG, e);
          }
        }

        updateToggleButtonState();
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    return future;
  }

  private ListenableFuture<Boolean> initializeSecurity(final boolean currentSecureText,
                                                       final boolean currentIsDefaultSms)
  {
    final SettableFuture<Boolean> future = new SettableFuture<>();

    handleSecurityChange(currentSecureText || isPushGroupConversation(), currentIsDefaultSms);

    new AsyncTask<Recipient, Void, boolean[]>() {
      @Override
      protected boolean[] doInBackground(Recipient... params) {
        Context   context   = ConversationActivity.this;
        Recipient recipient = params[0].resolve();
        Log.i(TAG, "Resolving registered state...");
        RecipientTable.RegisteredState registeredState;

        if (recipient.isPushGroup()) {
          Log.i(TAG, "Push group recipient...");
          registeredState = RecipientTable.RegisteredState.REGISTERED;
        } else {
          Log.i(TAG, "Checking through resolved recipient");
          registeredState = recipient.resolve().getRegistered();
        }

        Log.i(TAG, "Resolved registered state: " + registeredState);
        boolean signalEnabled = Recipient.self().isRegistered();

        if (registeredState == RecipientTable.RegisteredState.UNKNOWN) {
          try {
            Log.i(TAG, "Refreshing directory for user: " + recipient.getId().serialize());
            registeredState = ContactDiscovery.refresh(context, recipient, false);
          } catch (IOException e) {
            Log.w(TAG, e);
          }
        }

        Log.i(TAG, "Returning registered state...");
        return new boolean[] { registeredState == RecipientTable.RegisteredState.REGISTERED && signalEnabled,
                               Util.isDefaultSmsProvider(context) };
      }

      @Override
      protected void onPostExecute(boolean[] result) {
        if (result[0] != currentSecureText || result[1] != currentIsDefaultSms) {
          Log.i(TAG, "onPostExecute() handleSecurityChange: " + result[0] + " , " + result[1]);
          handleSecurityChange(result[0], result[1]);
        }
        future.set(true);
        onSecurityUpdated();
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipient.get());

    return future;
  }

  private void onSecurityUpdated() {
    Log.i(TAG, "onSecurityUpdated()");
    updateReminders();
    updateDefaultSubscriptionId(recipient.get().getDefaultSubscriptionId());
    updatePaymentsAvailable();
  }

  private void initializeInsightObserver() {
    inviteReminderModel = new InviteReminderModel(this, new InviteReminderRepository(this));
    inviteReminderModel.loadReminder(recipient, this::updateReminders);
  }

  protected void updateReminders() {
    Optional<Reminder> inviteReminder              = inviteReminderModel.getReminder();
    Integer            actionableRequestingMembers = groupViewModel.getActionableRequestingMembers().getValue();
    List<RecipientId>  gv1MigrationSuggestions     = groupViewModel.getGroupV1MigrationSuggestions().getValue();

    if (UnauthorizedReminder.isEligible(this)) {
      if (fragment != null && fragment.getList() != null) {
        fragment.getList().setFocusable(false);
      }
      if (mp02CustomDialog == null) {
        mp02CustomDialog = new Mp02CustomDialog(this);
        mp02CustomDialog.setMessage(getString(R.string.UnauthorizedReminder_device_no_longer_registered) + "\n"
                                    + getString(R.string.UnauthorizedReminder_this_is_likely_because_you_registered_your_phone_number_with_Signal_on_a_different_device));
        mp02CustomDialog.setPositiveListener(android.R.string.ok, () -> {
          startActivity(RegistrationNavigationActivity.newIntentForReRegistration(this));
        });
        mp02CustomDialog.setNegativeListener(android.R.string.cancel, () -> {
          finish();
        });
        mp02CustomDialog.setBackKeyListener(() -> {
          finish();
        });
        mp02CustomDialog.show();
      }
      //reminderView.get().showReminder(new UnauthorizedReminder(this));
    } else if (ExpiredBuildReminder.isEligible()) {
//            reminderView.get().showReminder(new ExpiredBuildReminder(this));
//            reminderView.get().setOnActionClickListener(this::handleReminderAction);
    } else if (ServiceOutageReminder.isEligible(this)) {
      ApplicationDependencies.getJobManager().add(new ServiceOutageDetectionJob());
//            reminderView.get().showReminder(new ServiceOutageReminder(this));
    } else if (TextSecurePreferences.isPushRegistered(this) &&
               TextSecurePreferences.isShowInviteReminders(this) &&
               !isSecureText &&
               inviteReminder.isPresent() &&
               !recipient.get().isGroup())
    {
//            reminderView.get().setOnActionClickListener(this::handleReminderAction);
//            reminderView.get().setOnDismissListener(() -> inviteReminderModel.dismissReminder());
//            reminderView.get().showReminder(inviteReminder.get());
    } else if (actionableRequestingMembers != null && actionableRequestingMembers > 0) {
//            reminderView.get().showReminder(PendingGroupJoinRequestsReminder.create(this, actionableRequestingMembers));
//            reminderView.get().setOnActionClickListener(id -> {
//                if (id == R.id.reminder_action_review_join_requests) {
//                    startActivity(ManagePendingAndRequestingMembersActivity.newIntent(this, getRecipient().getGroupId().get().requireV2()));
//                }
//            });
    } else if (gv1MigrationSuggestions != null && gv1MigrationSuggestions.size() > 0 && recipient.get().isPushV2Group()) {
//            reminderView.get().showReminder(new GroupsV1MigrationSuggestionsReminder(this, gv1MigrationSuggestions));
//            reminderView.get().setOnActionClickListener(actionId -> {
//                if (actionId == R.id.reminder_action_gv1_suggestion_add_members) {
//                    GroupsV1MigrationSuggestionsDialog.show(this, recipient.get().requireGroupId().requireV2(), gv1MigrationSuggestions);
//                } else if (actionId == R.id.reminder_action_gv1_suggestion_no_thanks) {
//                    groupViewModel.onSuggestedMembersBannerDismissed(recipient.get().requireGroupId(), gv1MigrationSuggestions);
//                }
//            });
//            reminderView.get().setOnDismissListener(() -> {
//            });
//        } else if (reminderView.resolved()) {
//            reminderView.get().hide();
    }
  }

  private void handleReminderAction(@IdRes int reminderActionId) {
    if (reminderActionId == R.id.reminder_action_invite) {
      handleInviteLink();
//            reminderView.get().requestDismiss();
    } else if (reminderActionId == R.id.reminder_action_view_insights) {
      InsightsLauncher.showInsightsDashboard(getSupportFragmentManager());
    } else if (reminderActionId == R.id.reminder_action_update_now) {
      PlayStoreUtil.openPlayStoreOrOurApkDownloadPage(this);
    } else {
      throw new IllegalArgumentException("Unknown ID: " + reminderActionId);
    }
  }

  private void updateDefaultSubscriptionId(Optional<Integer> defaultSubscriptionId) {
    Log.i(TAG, "updateDefaultSubscriptionId(" + defaultSubscriptionId.orElse(null) + ")");
    sendButton.setDefaultSubscriptionId(defaultSubscriptionId.orElse(null));
  }

  private void initializeMmsEnabledCheck() {
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        return Util.isMmsCapable(ConversationActivity.this);
      }

      @Override
      protected void onPostExecute(Boolean isMmsEnabled) {
        ConversationActivity.this.isMmsEnabled = isMmsEnabled;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private ListenableFuture<Boolean> initializeIdentityRecords() {
    final SettableFuture<Boolean> future = new SettableFuture<>();

    new AsyncTask<Recipient, Void, Pair<IdentityRecordList, String>>() {
      @Override
      protected @NonNull
      Pair<IdentityRecordList, String> doInBackground(Recipient... params) {
        List<Recipient> recipients;

        if (params[0].isGroup()) {
          recipients = SignalDatabase.groups()
                                     .getGroupMembers(params[0].requireGroupId(), GroupTable.MemberSet.FULL_MEMBERS_EXCLUDING_SELF);
        } else {
          recipients = Collections.singletonList(params[0]);
        }

        long               startTime          = System.currentTimeMillis();
        IdentityRecordList identityRecordList = ApplicationDependencies.getProtocolStore().aci().identities().getIdentityRecords(recipients);

        Log.i(TAG, String.format(Locale.US, "Loaded %d identities in %d ms", recipients.size(), System.currentTimeMillis() - startTime));

        String message = null;

        if (identityRecordList.isUnverified()) {
          message = IdentityUtil.getUnverifiedBannerDescription(ConversationActivity.this, identityRecordList.getUnverifiedRecipients());
        }

        return new Pair<>(identityRecordList, message);
      }

      @Override
      protected void onPostExecute(@NonNull Pair<IdentityRecordList, String> result) {
        Log.i(TAG, "Got identity records: " + result.first().isUnverified());
        identityRecords = result.first();

        if (result.second() != null) {
          Log.d(TAG, "Replacing banner...");
                    /*unverifiedBannerView.get().display(result.second(), result.first().getUnverifiedRecords(),
                            new UnverifiedClickedListener(),
                            new UnverifiedDismissedListener());*/
        } else if (unverifiedBannerView.resolved()) {
          Log.d(TAG, "Clearing banner...");
          /*unverifiedBannerView.get().hide();*/
        }

        titleView.setVerified(isSecureText && identityRecords.isVerified());

        future.set(true);
      }

    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipient.get());

    return future;
  }

  private void initializeViews() {
    ViewGroup vg = findViewById(R.id.llt);
    vg.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        composeText.requestFocus();
        Log.e(TAG, "initializeViews");
      }
    });

    toolbar   = findViewById(R.id.toolbar);
    titleView = findViewById(R.id.conversation_title_view);
    titleView.setVisibility(View.GONE);
    buttonToggle = findViewById(R.id.button_toggle);
    sendButton   = findViewById(R.id.send_button);
    sendText     = findViewById(R.id.send_text);
    sendText.setOnFocusChangeListener(new OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          mRecyclerview.setVisibility(View.VISIBLE);

          if (!isActiveGroup() && (removeflag == false)) {
//                        mTLAdapterGp.removeData(0);
//                        mTLAdapterGp.removeData(0);
//                        mTLAdapterGp.notifyItemRemoved(0);
//                        mTLAdapterGp.notifyItemRemoved(0);
//                        mTLAdapter.notifyDataSetChanged();
            removeflag = true;
          }

          mVG.setVisibility(View.GONE);
        } else {
          sendText.setHeight(40);
          sendText.setTextSize(25);
          sendText.setTextColor(Color.GRAY);
        }
      }
    });
//    attachButton             = findViewById(R.id.attach_button);
    composeText            = findViewById(R.id.embedded_text_editor);
    charactersLeft         = findViewById(R.id.space_left);
    emojiDrawerStub        = ViewUtil.findStubById(this, R.id.emoji_drawer_stub);
    attachmentKeyboardStub = ViewUtil.findStubById(this, R.id.attachment_keyboard_stub);
    unblockButton          = findViewById(R.id.unblock_button);
    makeDefaultSmsButton   = findViewById(R.id.make_default_sms_button);
    registerButton         = findViewById(R.id.register_button);
    container              = findViewById(R.id.layout_container);
//        reminderView = ViewUtil.findStubById(this, R.id.reminder_stub);
    unverifiedBannerView   = ViewUtil.findStubById(this, R.id.unverified_banner_stub);
    reviewBanner           = ViewUtil.findStubById(this, R.id.review_banner_stub);
    quickAttachmentToggle  = findViewById(R.id.quick_attachment_toggle);
    inlineAttachmentToggle = findViewById(R.id.inline_attachment_container);
    inputPanel             = findViewById(R.id.bottom_panel);
    panelParent            = findViewById(R.id.conversation_activity_panel_parent);
    searchNav              = findViewById(R.id.conversation_search_nav);
//        messageRequestBottomView = findViewById(R.id.conversation_activity_message_request_bottom_bar);
    mentionsSuggestions = ViewUtil.findStubById(this, R.id.conversation_mention_suggestions_stub);
    //wallpaper                = findViewById(R.id.conversation_wallpaper);
    //wallpaperDim             = findViewById(R.id.conversation_wallpaper_dim);

//    ImageButton                       quickCameraToggle      = findViewById(R.id.quick_camera_toggle);
    ImageButton                       inlineAttachmentButton = findViewById(R.id.inline_attachment_button);
    Stub<ConversationReactionOverlay> reactionOverlayStub    = ViewUtil.findStubById(this, R.id.conversation_reaction_scrubber_stub);
    reactionDelegate = new ConversationReactionDelegate(reactionOverlayStub);

    noLongerMemberBanner                = findViewById(R.id.conversation_no_longer_member_banner);
    cannotSendInAnnouncementGroupBanner = ViewUtil.findStubById(this, R.id.conversation_cannot_send_announcement_stub);
    requestingMemberBanner              = findViewById(R.id.conversation_requesting_banner);
    cancelJoinRequest                   = findViewById(R.id.conversation_cancel_request);
    joinGroupCallButton                 = findViewById(R.id.conversation_group_cal_join);

    container.setIsBubble(isInBubble());
    container.addOnKeyboardShownListener(this);
    inputPanel.setListener(this);
    inputPanel.setThis(my_record_time);
    inputPanel.setMediaListener(this);
    my_record_time.setOnKeyListener(new OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
          inputPanel.onRecordReleased();
          inputPanel.mydisMiss();
          my_record_time.setVisibility(View.GONE);
          mRecyclerview.setVisibility(View.GONE);
          mVG.setVisibility(View.VISIBLE);
          panelParent.setVisibility(View.VISIBLE);
          composeText.requestFocus();
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
          inputPanel.onRecordCanceled();
        }
        inputPanel.mydisMiss();
        my_record_time.setVisibility(View.GONE);
        mRecyclerview.setVisibility(View.GONE);
        mVG.setVisibility(View.VISIBLE);
        panelParent.setVisibility(View.VISIBLE);
        composeText.requestFocus();
        return true;
      }
    });

    attachmentManager = new AttachmentManager(ConversationActivity.this, this.getPanelLayout(), this);
    audioRecorder     = new AudioRecorder(this);
    typingTextWatcher = new TypingStatusTextWatcher();

    SendButtonListener        sendButtonListener        = new SendButtonListener();
    ComposeKeyPressedListener composeKeyPressedListener = new ComposeKeyPressedListener();

    composeText.setOnEditorActionListener(sendButtonListener);
    composeText.setCursorPositionChangedListener(this);
//    attachButton.setOnClickListener(new AttachButtonListener());
//    attachButton.setOnLongClickListener(new AttachButtonLongClickListener());
    sendButton.setOnClickListener(sendButtonListener);
    sendText.setOnClickListener(sendButtonListener);
    sendButton.setEnabled(true);
    sendButton.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          sendButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.blue_500)));
        }
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          sendButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.white)));
        }
      }
    });
    sendButton.addOnSelectionChangedListener((newMessageSendType, manuallySelected) -> {
      if (this == null) {
        Log.w(TAG, "onSelectionChanged called in detached state. Ignoring.");
        return;
      }

      calculateCharactersRemaining();
      updateLinkPreviewState();
      linkPreviewViewModel.onTransportChanged(newMessageSendType.usesSmsTransport());
      composeText.setMessageSendType(newMessageSendType);

      updateSendButtonColor(newMessageSendType);

      if (manuallySelected) recordTransportPreference(newMessageSendType);
    });

    titleView.setOnClickListener(v -> handleConversationSettings());
    titleView.setOnLongClickListener(v -> handleDisplayQuickContact());
    unblockButton.setOnClickListener(v -> handleUnblock());
    makeDefaultSmsButton.setOnClickListener(v -> handleMakeDefaultSms());
    registerButton.setOnClickListener(v -> handleRegisterForSignal());

    composeText.setOnKeyListener(composeKeyPressedListener);
    composeText.addTextChangedListener(composeKeyPressedListener);
    composeText.setOnEditorActionListener(sendButtonListener);
    composeText.setOnClickListener(composeKeyPressedListener);
    composeText.setOnFocusChangeListener(composeKeyPressedListener);

//    if (Camera.getNumberOfCameras() > 0) {
//      quickCameraToggle.setVisibility(View.VISIBLE);
//      quickCameraToggle.setOnClickListener(new QuickCameraToggleListener());
//    } else {
//      quickCameraToggle.setVisibility(View.GONE);
//    }

    searchNav.setEventListener(this);

    inlineAttachmentButton.setOnClickListener(v -> handleAddAttachment());

    reactionDelegate.setOnReactionSelectedListener(this);

    joinGroupCallButton.setOnClickListener(v -> handleVideo(getRecipient()));
    voiceNoteMediaController.getVoiceNotePlaybackState().observe(ConversationActivity.this, inputPanel.getPlaybackStateObserver());
  }

  private void updateSendButtonColor(MessageSendType newMessageSendType) {
    buttonToggle.getBackground().setColorFilter(getSendButtonColor(newMessageSendType), PorterDuff.Mode.MULTIPLY);
    buttonToggle.getBackground().invalidateSelf();
  }

  private void updateWallpaper(@Nullable ChatWallpaper chatWallpaper) {
    if (chatWallpaper != null) {
      chatWallpaper.loadInto(wallpaper);
      ChatWallpaperDimLevelUtil.applyDimLevelForNightMode(wallpaperDim, chatWallpaper);
    } else {
      wallpaper.setImageDrawable(null);
      wallpaperDim.setVisibility(View.GONE);
    }
  }

  protected void initializeActionBar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    toolbar.setVisibility(View.GONE);

    ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar == null) throw new AssertionError();

    supportActionBar.setDisplayHomeAsUpEnabled(false);
    supportActionBar.setDisplayShowTitleEnabled(true);

    if (isInBubble()) {
      supportActionBar.setHomeAsUpIndicator(DrawableUtil.tint(ContextUtil.requireDrawable(this, R.drawable.ic_notification),
                                                              ContextCompat.getColor(this, R.color.signal_accent_primary)));
      toolbar.setNavigationOnClickListener(unused -> startActivity(MainActivity.clearTop(this)));
    }
  }

  protected boolean isInBubble() {
    return false;
  }

  private void initializeResources(@NonNull ConversationIntents.Args args) {
    if (recipient != null) {
      recipient.removeObservers(this);
    }

    recipient        = Recipient.live(args.getRecipientId());
    threadId         = args.getThreadId();
    distributionType = args.getDistributionType();
    glideRequests    = GlideApp.with(this);

    Log.i(TAG, "[initializeResources] Recipient: " + recipient.getId() + ", Thread: " + threadId);

    recipient.observe(this, this::onRecipientChanged);
  }

  private void initializeLinkPreviewObserver() {
    linkPreviewViewModel = new ViewModelProvider(this, new LinkPreviewViewModel.Factory(new LinkPreviewRepository())).get(LinkPreviewViewModel.class);

    linkPreviewViewModel.getLinkPreviewState().observe(this, previewState -> {
      if (previewState == null) return;

      if (previewState.isLoading()) {
        inputPanel.setLinkPreviewLoading();
      } else if (previewState.hasLinks() && !previewState.getLinkPreview().isPresent()) {
        inputPanel.setLinkPreviewNoPreview(previewState.getError());
      } else {
        inputPanel.setLinkPreview(glideRequests, previewState.getLinkPreview());
      }

      updateToggleButtonState();
    });
  }

  private void initializeSearchObserver() {
    ConversationSearchViewModel.Factory viewModelFactory = new ConversationSearchViewModel.Factory(getString(R.string.note_to_self));

    searchViewModel = new ViewModelProvider(this, (ViewModelProvider.Factory) viewModelFactory).get(ConversationSearchViewModel.class);

    searchViewModel.getSearchResults().observe(this, result -> {
      if (result == null) return;

      if (!result.getResults().isEmpty()) {
        MessageResult messageResult = result.getResults().get(result.getPosition());
        fragment.jumpToMessage(messageResult.getMessageRecipient().getId(), messageResult.getReceivedTimestampMs(), searchViewModel::onMissingResult);
      }

      searchNav.setData(result.getPosition(), result.getResults().size());
    });
  }

  private void initializeStickerObserver() {
    StickerSearchRepository repository = new StickerSearchRepository(this);

    stickerViewModel = new ViewModelProvider(this, new ConversationStickerViewModel.Factory(getApplication(), repository))
        .get(ConversationStickerViewModel.class);

    stickerViewModel.getStickerResults().observe(this, stickers -> {
      if (stickers == null) return;

      inputPanel.setStickerSuggestions(stickers);
    });

    stickerViewModel.getStickersAvailability().observe(this, stickersAvailable -> {
      if (stickersAvailable == null) return;

      boolean           isSystemEmojiPreferred = TextSecurePreferences.isSystemEmojiPreferred(this);
      MediaKeyboardMode keyboardMode           = TextSecurePreferences.getMediaKeyboardMode(this);
      boolean           stickerIntro           = !TextSecurePreferences.hasSeenStickerIntroTooltip(this);

      if (stickersAvailable) {
        inputPanel.showMediaKeyboardToggle(true);
        switch (keyboardMode) {
          case EMOJI:
            inputPanel.setMediaKeyboardToggleMode(isSystemEmojiPreferred ? KeyboardPage.STICKER : KeyboardPage.EMOJI);
            break;
          case STICKER:
            inputPanel.setMediaKeyboardToggleMode(KeyboardPage.STICKER);
            break;
          case GIF:
            inputPanel.setMediaKeyboardToggleMode(KeyboardPage.GIF);
            break;
        }
        if (stickerIntro) showStickerIntroductionTooltip();
      }

      if (emojiDrawerStub.resolved()) {
        initializeMediaKeyboardProviders();
      }
    });
  }

  private void initializeViewModel(@NonNull ConversationIntents.Args args) {
    this.viewModel = new ViewModelProvider(this, new ConversationViewModel.Factory()).get(ConversationViewModel.class);

    this.viewModel.setArgs(args);
    this.viewModel.getEvents().observe(this, this::onViewModelEvent);
  }

  private void initializeGroupViewModel() {
    groupViewModel = new ViewModelProvider(this, new ConversationGroupViewModel.Factory()).get(ConversationGroupViewModel.class);
    recipient.observe(this, groupViewModel::onRecipientChange);
    groupViewModel.getGroupActiveState().observe(this, unused -> invalidateOptionsMenu());
    groupViewModel.getReviewState().observe(this, this::presentGroupReviewBanner);
  }

  private void initializeMentionsViewModel() {
    mentionsViewModel = new ViewModelProvider(this, new MentionsPickerViewModel.Factory()).get(MentionsPickerViewModel.class);

    recipient.observe(this, r -> {
      if (r.isPushV2Group() && !mentionsSuggestions.resolved()) {
        mentionsSuggestions.get();
      }
      mentionsViewModel.onRecipientChange(r);
    });

    composeText.setMentionValidator(annotations -> {
      if (!getRecipient().isPushV2Group() || !getRecipient().isActiveGroup()) {
        return annotations;
      }

      Set<String> validRecipientIds = Stream.of(getRecipient().getParticipantIds())
                                            .map(id -> MentionAnnotation.idToMentionAnnotationValue(id))
                                            .collect(Collectors.toSet());

      return Stream.of(annotations)
                   .filterNot(a -> validRecipientIds.contains(a.getValue()))
                   .toList();
    });

    mentionsViewModel.getSelectedRecipient().observe(this, recipient -> {
      composeText.replaceTextWithMention(recipient.getDisplayName(this), recipient.getId());
    });
  }

  public void initializeGroupCallViewModel() {
    groupCallViewModel = new ViewModelProvider(this, new GroupCallViewModel.Factory()).get(GroupCallViewModel.class);

    recipient.observe(this, r -> {
      groupCallViewModel.onRecipientChange(r);
    });

    groupCallViewModel.hasActiveGroupCall().observe(this, hasActiveCall -> {
      invalidateOptionsMenu();
      joinGroupCallButton.setVisibility(hasActiveCall ? View.VISIBLE : View.GONE);
    });

    groupCallViewModel.groupCallHasCapacity().observe(this, hasCapacity -> joinGroupCallButton.setText(hasCapacity ? R.string.ConversationActivity_join : R.string.ConversationActivity_full));
  }

  public void initializeDraftViewModel() {
    draftViewModel = new ViewModelProvider(this, new DraftViewModel.Factory(new DraftRepository())).get(DraftViewModel.class);
    recipient.observe(this, r -> {
      draftViewModel.onRecipientChanged(r);
    });
    disposables.add(
        draftViewModel
            .getState()
            .distinctUntilChanged(state -> state.getVoiceNoteDraft())
            .subscribe(state -> {
              inputPanel.setVoiceNoteDraft(state.getVoiceNoteDraft());
              updateToggleButtonState();
            })
    );
  }

  private void showGroupCallingTooltip() {
    if (Build.VERSION.SDK_INT == 19 || !SignalStore.tooltips().shouldShowGroupCallingTooltip() || callingTooltipShown) {
      return;
    }

    View anchor = findViewById(R.id.menu_video_secure);
    if (anchor == null) {
      Log.w(TAG, "Video Call tooltip anchor is null. Skipping tooltip...");
      return;
    }

    callingTooltipShown = true;

    SignalStore.tooltips().markGroupCallSpeakerViewSeen();
    TooltipPopup.forTarget(anchor)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.signal_accent_green))
                .setTextColor(getResources().getColor(R.color.core_white))
                .setText(R.string.ConversationActivity__tap_here_to_start_a_group_call)
                .setOnDismissListener(() -> SignalStore.tooltips().markGroupCallingTooltipSeen())
                .show(TooltipPopup.POSITION_BELOW);
  }

  private void showStickerIntroductionTooltip() {
    TextSecurePreferences.setMediaKeyboardMode(this, MediaKeyboardMode.STICKER);
    inputPanel.setMediaKeyboardToggleMode(KeyboardPage.STICKER);

    TooltipPopup.forTarget(inputPanel.getMediaKeyboardToggleAnchorView())
                .setBackgroundTint(getResources().getColor(R.color.core_ultramarine))
                .setTextColor(getResources().getColor(R.color.core_white))
                .setText(R.string.ConversationActivity_new_say_it_with_stickers)
                .setOnDismissListener(() -> {
                  TextSecurePreferences.setHasSeenStickerIntroTooltip(this, true);
                  EventBus.getDefault().removeStickyEvent(StickerPackInstallEvent.class);
                })
                .show(TooltipPopup.POSITION_ABOVE);
  }

  @Override
  public void onReactionSelected(MessageRecord messageRecord, String emoji) {
    final Context context = getApplicationContext();

    reactionDelegate.hide();

    SignalExecutors.BOUNDED.execute(() -> {
      ReactionRecord oldRecord = Stream.of(messageRecord.getReactions())
                                       .filter(record -> record.getAuthor().equals(Recipient.self().getId()))
                                       .findFirst()
                                       .orElse(null);

      if (oldRecord != null && oldRecord.getEmoji().equals(emoji)) {
        MessageSender.sendReactionRemoval(context, new MessageId(messageRecord.getId(), messageRecord.isMms()), oldRecord);
      } else {
        MessageSender.sendNewReaction(context, new MessageId(messageRecord.getId(), messageRecord.isMms()), emoji);
      }
    });
  }

  @Override
  public void onCustomReactionSelected(@NonNull MessageRecord messageRecord, boolean hasAddedCustomEmoji) {
    ReactionRecord oldRecord = Stream.of(messageRecord.getReactions())
                                     .filter(record -> record.getAuthor().equals(Recipient.self().getId()))
                                     .findFirst()
                                     .orElse(null);

    if (oldRecord != null && hasAddedCustomEmoji) {
      final Context context = getApplicationContext();

      reactionDelegate.hide();

      SignalExecutors.BOUNDED.execute(() -> MessageSender.sendReactionRemoval(context,
                                                                              new MessageId(messageRecord.getId(), messageRecord.isMms()),
                                                                              oldRecord));
    } else {
      reactionDelegate.hideForReactWithAny();

      ReactWithAnyEmojiBottomSheetDialogFragment.createForMessageRecord(messageRecord, reactWithAnyEmojiStartPage)
                                                .show(getSupportFragmentManager(), "BOTTOM");
    }
  }

  @Override
  public void onReactWithAnyEmojiDialogDismissed() {
    reactionDelegate.hide();
  }

  @Override
  public void onReactWithAnyEmojiSelected(@NonNull String emoji) {
  }

  @Override
  public void onSearchMoveUpPressed() {
    searchViewModel.onMoveUp();
  }

  @Override
  public void onSearchMoveDownPressed() {
    searchViewModel.onMoveDown();
  }

  private void initializeProfiles() {
    if (!isSecureText) {
      Log.i(TAG, "SMS contact, no profile fetch needed.");
      return;
    }

    RetrieveProfileJob.enqueueAsync(recipient.getId());
  }

  private void initializeGv1Migration() {
    GroupV1MigrationJob.enqueuePossibleAutoMigrate(recipient.getId());
  }

  private void onRecipientChanged(@NonNull Recipient recipient) {
    Log.i(TAG, "onModified(" + recipient.getId() + ") " + recipient.getRegistered());
    titleView.setTitle(glideRequests, recipient);
    titleView.setVerified(identityRecords.isVerified());
    setBlockedUserState(recipient, isSecureText, isDefaultSms);
//        setActionBarColor(recipient.getColor());
    updateReminders();
    updateDefaultSubscriptionId(recipient.getDefaultSubscriptionId());
    initializeSecurity(isSecureText, isDefaultSms);
    //updateWallpaper(recipient.getWallpaper());

    if (searchViewItem == null || !searchViewItem.isActionViewExpanded()) {
      invalidateOptionsMenu();
    }

    if (groupViewModel != null) {
      groupViewModel.onRecipientChange(recipient);
    }

    if (mentionsViewModel != null) {
      mentionsViewModel.onRecipientChange(recipient);
    }

    if (groupCallViewModel != null) {
      groupCallViewModel.onRecipientChange(recipient);
    }
    if (draftViewModel != null) {
      draftViewModel.onRecipientChanged(recipient);
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onIdentityRecordUpdate(final IdentityRecord event) {
    initializeIdentityRecords();
  }

  @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
  public void onStickerPackInstalled(final StickerPackInstallEvent event) {
    if (!TextSecurePreferences.hasSeenStickerIntroTooltip(this)) return;

    EventBus.getDefault().removeStickyEvent(event);

    if (!inputPanel.isStickerMode()) {
      TooltipPopup.forTarget(inputPanel.getMediaKeyboardToggleAnchorView())
                  .setText(R.string.ConversationActivity_sticker_pack_installed)
                  .setIconGlideModel(event.getIconGlideModel())
                  .show(TooltipPopup.POSITION_ABOVE);
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
  public void onGroupCallPeekEvent(@NonNull GroupCallPeekEvent event) {
    if (groupCallViewModel != null) {
      groupCallViewModel.onGroupCallPeekEvent(event);
    }
  }

  private void initializeReceivers() {
    securityUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        initializeSecurity(isSecureText, isDefaultSms);
        calculateCharactersRemaining();
      }
    };

    registerReceiver(securityUpdateReceiver,
                     new IntentFilter(SecurityEvent.SECURITY_UPDATE_EVENT),
                     KeyCachingService.KEY_PERMISSION, null);
  }

  //////// Helper Methods

  private ListenableFuture<Boolean> setMedia(@Nullable Uri uri, @NonNull MediaType mediaType) {
    return setMedia(uri, mediaType, 0, 0, false, false);
  }

  private ListenableFuture<Boolean> setMedia(@Nullable Uri uri, @NonNull MediaType mediaType, int width, int height, boolean borderless, boolean videoGif) {
    if (uri == null) {
      return new SettableFuture<>(false);
    }

    if (MediaType.VCARD.equals(mediaType) && isSecureText) {
      openContactShareEditor(uri);
      return new SettableFuture<>(false);
    } else if (MediaType.IMAGE.equals(mediaType) || MediaType.GIF.equals(mediaType) || MediaType.VIDEO.equals(mediaType)) {
      String mimeType = MediaUtil.getMimeType(this, uri);
      if (mimeType == null) {
        mimeType = mediaType.toFallbackMimeType();
      }

      Media media = new Media(uri, mimeType, 0, width, height, 0, 0, borderless, videoGif, Optional.empty(), Optional.empty(), Optional.empty());
      startActivityForResult(MediaSelectionActivity.editor(ConversationActivity.this, sendButton.getSelectedSendType(), Collections.singletonList(media), recipient.getId(), composeText.getTextTrimmed()), MEDIA_SENDER);
      return new SettableFuture<>(false);
    } else {
      return attachmentManager.setMedia(glideRequests, uri, mediaType, getCurrentMediaConstraints(), width, height);
    }
  }

  private void openContactShareEditor(Uri contactUri) {
    Intent intent = ContactShareEditActivity.getIntent(this, Collections.singletonList(contactUri), getSendButtonColor(sendButton.getSelectedSendType()));
    startActivityForResult(intent, GET_CONTACT_DETAILS);
  }

  private void addAttachmentContactInfo(Uri contactUri) {
    ContactAccessor contactDataList = ContactAccessor.getInstance();
    ContactData     contactData     = contactDataList.getContactData(this, contactUri);

    if (contactData.numbers.size() == 1) composeText.append(contactData.numbers.get(0).number);
    else if (contactData.numbers.size() > 1) selectContactInfo(contactData);
  }

  private void sendSharedContact(List<Contact> contacts) {
    long    expiresIn  = TimeUnit.SECONDS.toMillis(recipient.get().getExpiresInSeconds());
    boolean initiating = threadId == -1;

    sendMediaMessage(recipient.getId(), sendButton.getSelectedSendType(), "", attachmentManager.buildSlideDeck(), null, contacts, Collections.emptyList(), Collections.emptyList(), expiresIn, false, initiating, false, null);
  }

  private void selectContactInfo(ContactData contactData) {
    final CharSequence[] numbers     = new CharSequence[contactData.numbers.size()];
    final CharSequence[] numberItems = new CharSequence[contactData.numbers.size()];

    for (int i = 0; i < contactData.numbers.size(); i++) {
      numbers[i]     = contactData.numbers.get(i).number;
      numberItems[i] = contactData.numbers.get(i).type + ": " + contactData.numbers.get(i).number;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setIcon(R.drawable.ic_account_box);
    builder.setTitle(R.string.ConversationActivity_select_contact_info);

    builder.setItems(numberItems, (dialog, which) -> composeText.append(numbers[which]));
    builder.show();
  }

  private DraftTable.Drafts getDraftsForCurrentState() {
    DraftTable.Drafts drafts = new DraftTable.Drafts();

    if (recipient.get().isGroup() && !recipient.get().isActiveGroup()) {
      return drafts;
    }

    if (!Util.isEmpty(composeText)) {
      drafts.add(new DraftTable.Draft(DraftTable.Draft.TEXT, composeText.getTextTrimmed().toString()));
      List<Mention> draftMentions = composeText.getMentions();
      if (!draftMentions.isEmpty()) {
        drafts.add(new DraftTable.Draft(DraftTable.Draft.MENTION, Base64.encodeBytes(MentionUtil.mentionsToBodyRangeList(draftMentions).toByteArray())));
      }
    }

    for (Slide slide : attachmentManager.buildSlideDeck().getSlides()) {
      if (slide.hasAudio() && slide.getUri() != null)
        drafts.add(new DraftTable.Draft(DraftTable.Draft.AUDIO, slide.getUri().toString()));
      else if (slide.hasVideo() && slide.getUri() != null)
        drafts.add(new DraftTable.Draft(DraftTable.Draft.VIDEO, slide.getUri().toString()));
      else if (slide.hasLocation())
        drafts.add(new DraftTable.Draft(DraftTable.Draft.LOCATION, ((LocationSlide) slide).getPlace().serialize()));
      else if (slide.hasImage() && slide.getUri() != null)
        drafts.add(new DraftTable.Draft(DraftTable.Draft.IMAGE, slide.getUri().toString()));
    }

    Optional<QuoteModel> quote = inputPanel.getQuote();

    if (quote.isPresent()) {
      drafts.add(new DraftTable.Draft(DraftTable.Draft.QUOTE, new QuoteId(quote.get().getId(), quote.get().getAuthor()).serialize()));
    }
    DraftTable.Draft voiceNoteDraft = draftViewModel.getVoiceNoteDraft();
    if (voiceNoteDraft != null) {
      drafts.add(voiceNoteDraft);
    }

    return drafts;
  }

  protected ListenableFuture<Long> saveDraft() {
    final SettableFuture<Long> future = new SettableFuture<>();

    if (this.recipient == null) {
      future.set(threadId);
      return future;
    }

    final DraftTable.Drafts drafts               = getDraftsForCurrentState();
    final long              thisThreadId         = this.threadId;
    final RecipientId       recipientId          = this.recipient.getId();
    final int               thisDistributionType = this.distributionType;

    new AsyncTask<Long, Void, Long>() {
      @Override
      protected Long doInBackground(Long... params) {
        ThreadTable ThreadTable   = SignalDatabase.threads();
        DraftTable  draftDatabase = SignalDatabase.drafts();
        long        threadId      = params[0];

        if (drafts.size() > 0) {
          if (threadId == -1)
            threadId = ThreadTable.getOrCreateThreadIdFor(getRecipient(), thisDistributionType);

          draftDatabase.replaceDrafts(threadId, drafts);
          ThreadTable.updateSnippet(threadId, drafts.getSnippet(ConversationActivity.this),
                                    drafts.getUriSnippet(),
                                    System.currentTimeMillis(), Types.BASE_DRAFT_TYPE, true);
        } else if (threadId > 0) {
          ThreadTable.update(threadId, false);
        }

        if (drafts.isEmpty()) {
          draftDatabase.clearDrafts(threadId);
        }

        return threadId;
      }

      @Override
      protected void onPostExecute(Long result) {
        future.set(result);
      }

    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, thisThreadId);

    return future;
  }

  private void setActionBarColor(MaterialColor color) {
    ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar == null) throw new AssertionError();
    int actionBarColor = color.toActionBarColor(this);
    supportActionBar.setBackgroundDrawable(new ColorDrawable(actionBarColor));
    WindowUtil.setStatusBarColor(getWindow(), color.toStatusBarColor(this));

    joinGroupCallButton.setTextColor(actionBarColor);
    joinGroupCallButton.setIconTint(ColorStateList.valueOf(actionBarColor));
    joinGroupCallButton.setRippleColor(ColorStateList.valueOf(actionBarColor));
  }

  private void setBlockedUserState(Recipient recipient, boolean isSecureText, boolean isDefaultSms) {
    this.isSecureText = isSecureText;
    if (recipient.isBlocked()) {
      unblockButton.setVisibility(View.VISIBLE);
      unblockButton.requestFocus();
      unblockButton.requestFocusFromTouch();
      inputPanel.setHideForBlockedState(true);
      makeDefaultSmsButton.setVisibility(View.GONE);
      registerButton.setVisibility(View.GONE);
      mRecyclerview.setVisibility(View.GONE);
    } else if (!isSecureText && isPushGroupConversation()) {
      unblockButton.setVisibility(View.GONE);
      inputPanel.setHideForBlockedState(true);
      makeDefaultSmsButton.setVisibility(View.GONE);
      registerButton.setVisibility(View.VISIBLE);
    } else if (!isSecureText && !isDefaultSms && recipient.hasSmsAddress()) {
      if (composeText.hasFocus()) {
        composeText.clearFocus();
      }
      unblockButton.setVisibility(View.GONE);
      inputPanel.setVisibility(View.GONE);
      makeDefaultSmsButton.setVisibility(View.VISIBLE);
      makeDefaultSmsButton.setFocusable(true);
      makeDefaultSmsButton.requestFocus();
      registerButton.setVisibility(View.GONE);
    } else {
      boolean inactivePushGroup = isPushGroupConversation() && !recipient.isActiveGroup();
      inputPanel.setHideForBlockedState(inactivePushGroup);
      unblockButton.setVisibility(View.GONE);
      makeDefaultSmsButton.setVisibility(View.GONE);
      registerButton.setVisibility(View.GONE);
    }
  }

  private void calculateCharactersRemaining() {
    String          messageBody    = composeText.getTextTrimmed().toString();
    MessageSendType sendType       = sendButton.getSelectedSendType();
    CharacterState  characterState = sendType.calculateCharacters(messageBody);

    if (characterState.charactersRemaining <= 15 || characterState.messagesSpent > 1) {
      charactersLeft.setText(String.format(Locale.getDefault(),
                                           "%d/%d (%d)",
                                           characterState.charactersRemaining,
                                           characterState.maxTotalMessageSize,
                                           characterState.messagesSpent));
      charactersLeft.setVisibility(View.VISIBLE);
    } else {
      charactersLeft.setVisibility(View.GONE);
    }
  }

  private void initializeMediaKeyboardProviders() {
    KeyboardPagerViewModel keyboardPagerViewModel = new ViewModelProvider(this).get(KeyboardPagerViewModel.class);

    switch (TextSecurePreferences.getMediaKeyboardMode(this)) {
      case EMOJI:
        keyboardPagerViewModel.switchToPage(KeyboardPage.EMOJI);
        break;
      case STICKER:
        keyboardPagerViewModel.switchToPage(KeyboardPage.STICKER);
        break;
      case GIF:
        keyboardPagerViewModel.switchToPage(KeyboardPage.GIF);
        break;
    }
  }

  private boolean isBlocked() {
    return unblockButton.getVisibility() == View.VISIBLE;
  }

//    private boolean isInMessageRequest() {
//        return messageRequestBottomView.getVisibility() == View.VISIBLE;
//    }

  private boolean isSingleConversation() {
    return getRecipient() != null && !getRecipient().isGroup();
  }

  private boolean isActiveGroup() {
    if (!isGroupConversation()) return false;

    Optional<GroupRecord> record = SignalDatabase.groups().getGroup(getRecipient().getId());
    return record.isPresent() && record.get().isActive();
  }

  @SuppressWarnings("SimplifiableIfStatement")
  private boolean isSelfConversation() {
    if (!TextSecurePreferences.isPushRegistered(this)) return false;
    if (recipient.get().isGroup()) return false;

    return recipient.get().isSelf();
  }

  private boolean isGroupConversation() {
    return getRecipient() != null && getRecipient().isGroup();
  }

  private boolean isPushGroupConversation() {
    return getRecipient() != null && getRecipient().isPushGroup();
  }

  private boolean isPushGroupV1Conversation() {
    return getRecipient() != null && getRecipient().isPushV1Group();
  }

  private boolean isSmsForced() {
    return sendButton.isManualSelection() && sendButton.getSelectedSendType().usesSmsTransport();
  }

  protected Recipient getRecipient() {
    return this.recipient.get();
  }

  protected long getThreadId() {
    return this.threadId;
  }

  private String getMessage() throws InvalidMessageException {
    String rawText = composeText.getTextTrimmed().toString();

    if (rawText.length() < 1 && !attachmentManager.isAttachmentPresent())
      throw new InvalidMessageException(getString(R.string.ConversationActivity_message_is_empty_exclamation));

    return rawText;
  }

  private MediaConstraints getCurrentMediaConstraints() {
    return sendButton.getSelectedSendType().usesSignalTransport()
           ? MediaConstraints.getPushMediaConstraints()
           : MediaConstraints.getMmsMediaConstraints(sendButton.getSelectedSendType().getSimSubscriptionIdOr(-1));
  }

  private void markLastSeen() {
    new AsyncTask<Long, Void, Void>() {
      @Override
      protected Void doInBackground(Long... params) {
        SignalDatabase.threads().setLastSeen(params[0]);
        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
  }

  protected void sendComplete(long threadId) {
    boolean refreshFragment = (threadId != this.threadId);
    this.threadId = threadId;

    if (fragment == null || !fragment.isVisible() || isFinishing()) {
      return;
    }

    fragment.setLastSeen(0);

    if (refreshFragment) {
      fragment.reload(recipient.get(), threadId);
      setVisibleThread(threadId);
    }

    fragment.scrollToBottom();
    attachmentManager.cleanup();
    composeText.setFocusable(true);
    composeText.setFocusableInTouchMode(true);
    composeText.requestFocus();

    updateLinkPreviewState();
  }

  private void sendMessage(@Nullable String metricId) {
    if (inputPanel.isRecordingInLockedMode()) {
      inputPanel.releaseRecordingLock();
      return;
    }

    DraftTable.Draft voiceNote = draftViewModel.getVoiceNoteDraft();
    if (voiceNote != null) {
      AudioSlide audioSlide = AudioSlide.createFromVoiceNoteDraft(this, voiceNote);

      sendVoiceNote(Objects.requireNonNull(audioSlide.getUri()), audioSlide.getFileSize());
      return;
    }

    try {
      Recipient recipient = getRecipient();

      if (recipient == null) {
        throw new RecipientFormattingException("Badly formatted");
      }

      String          message    = getMessage();
      MessageSendType sendType   = sendButton.getSelectedSendType();
      long            expiresIn  = TimeUnit.SECONDS.toMillis(recipient.getExpiresInSeconds());
      boolean         initiating = threadId == -1;
      boolean         needsSplit = !sendType.usesSmsTransport() && message.length() > sendType.calculateCharacters(message).maxPrimaryMessageSize;
      boolean isMediaMessage = attachmentManager.isAttachmentPresent() ||
                               recipient.isGroup() ||
                               recipient.getEmail().isPresent() ||
                               inputPanel.getQuote().isPresent() ||
                               composeText.hasMentions() ||
                               linkPreviewViewModel.hasLinkPreview() ||
                               needsSplit;

      Log.i(TAG, "[sendMessage] recipient: " + recipient.getId() + ", threadId: " + threadId + ",  sendType: " + (sendType.usesSignalTransport() ? "signal" : "sms") + ", isManual: " + sendButton.isManualSelection());

      if ((recipient.isMmsGroup() || recipient.getEmail().isPresent()) && !viewModel.getConversationStateSnapshot().isMmsEnabled()) {
        handleManualMmsRequired();
      } else if (sendType.usesSignalTransport() && (identityRecords.isUnverified(true) || identityRecords.isUntrusted(true))) {
        handleRecentSafetyNumberChange();
      } else if (isMediaMessage) {
        sendMediaMessage(sendType, expiresIn, false, initiating, metricId);
      } else {
        sendTextMessage(sendType, expiresIn, initiating, metricId);
      }
    } catch (RecipientFormattingException ex) {
      Toast.makeText(this,
                     R.string.ConversationActivity_recipient_is_not_a_valid_sms_or_email_address_exclamation,
                     Toast.LENGTH_LONG).show();
      Log.w(TAG, ex);
    } catch (InvalidMessageException ex) {
      Toast.makeText(this, R.string.ConversationActivity_message_is_empty_exclamation,
                     Toast.LENGTH_SHORT).show();
      Log.w(TAG, ex);
    }
  }

  private void sendMediaMessage(@NonNull MediaSendActivityResult result) {
    long                 thread        = this.threadId;
    long                 expiresIn     = TimeUnit.SECONDS.toMillis(recipient.get().getExpiresInSeconds());
    QuoteModel           quote         = result.isViewOnce() ? null : inputPanel.getQuote().orElse(null);
    List<Mention>        mentions      = new ArrayList<>(result.getMentions());
    OutgoingMediaMessage message       = new OutgoingMediaMessage(recipient.get(), new SlideDeck(), result.getBody(), System.currentTimeMillis(), -1, expiresIn, result.isViewOnce(), distributionType, result.getStoryType(), null, false, quote, Collections.emptyList(), Collections.emptyList(), mentions, null);
    OutgoingMediaMessage secureMessage = new OutgoingSecureMediaMessage(message);

    final Context context = this.getApplicationContext();

    ApplicationDependencies.getTypingStatusSender().onTypingStopped(thread);

    inputPanel.clearQuote();
    attachmentManager.clear(glideRequests, false);
    silentlySetComposeText("");

    long id = fragment.stageOutgoingMessage(secureMessage);

    SimpleTask.run(() -> {
      long resultId = MessageSender.sendPushWithPreUploadedMedia(context, secureMessage, result.getPreUploadResults(), thread, null);

      int deleted = SignalDatabase.attachments().deleteAbandonedPreuploadedAttachments();
      Log.i(TAG, "Deleted " + deleted + " abandoned attachments.");

      return resultId;
    }, this::sendComplete);
  }

  private void sendMediaMessage(@NonNull MessageSendType sendType, final long expiresIn, final boolean viewOnce, final boolean initiating, @Nullable String metricId)
      throws InvalidMessageException
  {
    Log.i(TAG, "Sending media message...");
    List<LinkPreview> linkPreviews = linkPreviewViewModel.onSend();
    sendMediaMessage(recipient.getId(),
                     sendType,
                     getMessage(),
                     attachmentManager.buildSlideDeck(),
                     inputPanel.getQuote().orElse(null),
                     Collections.emptyList(),
                     linkPreviews,
                     composeText.getMentions(),
                     expiresIn,
                     viewOnce,
                     initiating,
                     true,
                     metricId);
  }

  private ListenableFuture<Void> sendMediaMessage(@NonNull RecipientId recipientId,
                                                  @NonNull MessageSendType sendType,
                                                  @NonNull String body,
                                                  SlideDeck slideDeck,
                                                  QuoteModel quote,
                                                  List<Contact> contacts,
                                                  List<LinkPreview> previews,
                                                  List<Mention> mentions,
                                                  final long expiresIn,
                                                  final boolean viewOnce,
                                                  final boolean initiating,
                                                  final boolean clearComposeBox,
                                                  final @Nullable String metricId)
  {
    if (!viewModel.isDefaultSmsApplication() && sendType.usesSmsTransport() && recipient.get().hasSmsAddress()) {
      showDefaultSmsPrompt();
      return new SettableFuture<>(null);
    }

    final boolean sendPush = sendType.usesSignalTransport();
    final long    thread   = this.threadId;

    if (sendPush) {
      MessageUtil.SplitResult splitMessage = MessageUtil.getSplitMessage(this, body, sendButton.getSelectedSendType().calculateCharacters(body).maxPrimaryMessageSize);
      body = splitMessage.getBody();

      if (splitMessage.getTextSlide().isPresent()) {
        slideDeck.addSlide(splitMessage.getTextSlide().get());
      }
    }

    OutgoingMediaMessage outgoingMessageCandidate = new OutgoingMediaMessage(Recipient.resolved(recipientId), slideDeck, body, System.currentTimeMillis(), sendType.getSimSubscriptionIdOr(-1), expiresIn, viewOnce, distributionType, StoryType.NONE, null, false, quote, contacts, previews, mentions, null);

    final SettableFuture<Void> future  = new SettableFuture<>();
    final Context              context = this.getApplicationContext();

    final OutgoingMediaMessage outgoingMessage;

    if (sendPush) {
      outgoingMessage = new OutgoingSecureMediaMessage(outgoingMessageCandidate);
      ApplicationDependencies.getTypingStatusSender().onTypingStopped(thread);
    } else {
      outgoingMessage = outgoingMessageCandidate.withExpiry(0);
    }

    Permissions.with(this)
               .request(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS)
               .ifNecessary(!sendPush)
               .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_sms_permission_in_order_to_send_an_sms))
               .onAllGranted(() -> {
                 if (clearComposeBox) {
                   inputPanel.clearQuote();
                   attachmentManager.clear(glideRequests, false);
                   silentlySetComposeText("");
                 }

                 final long id = fragment.stageOutgoingMessage(outgoingMessage);

                 SimpleTask.run(() -> {
                   return MessageSender.send(context, outgoingMessage, thread, sendType.usesSmsTransport(), metricId, null);
                 }, result -> {
                   sendComplete(result);
                   future.set(null);
                 });
               })
               .onAnyDenied(() -> future.set(null))
               .execute();

    return future;
  }

  private void sendTextMessage(@NonNull MessageSendType sendType, final long expiresIn, final boolean initiating, final @Nullable String metricId)
      throws InvalidMessageException
  {
    if (!viewModel.isDefaultSmsApplication() && sendType.usesSmsTransport() && recipient.get().hasSmsAddress()) {
      showDefaultSmsPrompt();
      return;
    }

    final long    thread      = this.threadId;
    final Context context     = this.getApplicationContext();
    final String  messageBody = getMessage();
    final boolean sendPush    = sendType.usesSignalTransport();

    OutgoingTextMessage message;

    if (sendPush) {
      message = new OutgoingEncryptedMessage(recipient.get(), messageBody, expiresIn);
      ApplicationDependencies.getTypingStatusSender().onTypingStopped(thread);
    } else {
      message = new OutgoingTextMessage(recipient.get(), messageBody, 0, sendType.getSimSubscriptionIdOr(-1));
    }

    Permissions.with(this)
               .request(Manifest.permission.SEND_SMS)
               .ifNecessary(!sendPush)
               .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_sms_permission_in_order_to_send_an_sms))
               .onAllGranted(() -> {
                 final long id = new SecureRandom().nextLong();
                 SimpleTask.run(() -> {
                   return MessageSender.send(context, message, thread, sendType.usesSmsTransport(), metricId, null);
                 }, this::sendComplete);

                 silentlySetComposeText("");
                 fragment.stageOutgoingMessage(message, id);
               })
               .execute();
  }


  private void showDefaultSmsPrompt() {
    new AlertDialog.Builder(this)
        .setMessage(R.string.ConversationActivity_signal_cannot_sent_sms_mms_messages_because_it_is_not_your_default_sms_app)
        .setNegativeButton(R.string.ConversationActivity_no, (dialog, which) -> dialog.dismiss())
        .setPositiveButton(R.string.ConversationActivity_yes, (dialog, which) -> handleMakeDefaultSms())
        .show();
  }

  private void updateToggleButtonState() {
    if (inputPanel.isRecordingInLockedMode()) {
      buttonToggle.display(sendButton);
      quickAttachmentToggle.show();

      inlineAttachmentToggle.hide();
      return;
    }

    if (draftViewModel.getVoiceNoteDraft() != null) {
      buttonToggle.display(sendButton);
      quickAttachmentToggle.hide();
      inlineAttachmentToggle.hide();
      return;
    }

    if (composeText.getText().length() == 0 && !attachmentManager.isAttachmentPresent()) {
      buttonToggle.display(attachButton);
      quickAttachmentToggle.show();
      inlineAttachmentToggle.hide();
    } else {
      buttonToggle.display(sendButton);
      quickAttachmentToggle.hide();

      if (!attachmentManager.isAttachmentPresent() && !linkPreviewViewModel.hasLinkPreviewUi()) {
        inlineAttachmentToggle.show();
      } else {
        inlineAttachmentToggle.hide();
      }
    }
  }

  private void onViewModelEvent(@NonNull ConversationViewModel.Event event) {
    if (event == ConversationViewModel.Event.SHOW_RECAPTCHA) {
      RecaptchaProofBottomSheetFragment.show(getSupportFragmentManager());
    } else {
      throw new AssertionError("Unexpected event!");
    }
  }

  private void updateLinkPreviewState() {
    if (SignalStore.settings().isLinkPreviewsEnabled() && viewModel.isPushAvailable() && !sendButton.getSelectedSendType().usesSmsTransport() && !attachmentManager.isAttachmentPresent()) {
      linkPreviewViewModel.onEnabled();
      linkPreviewViewModel.onTextChanged(this, composeText.getTextTrimmed().toString(), composeText.getSelectionStart(), composeText.getSelectionEnd());
    } else {
      linkPreviewViewModel.onUserCancel();
    }
  }

  private void recordTransportPreference(MessageSendType sendType) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        RecipientTable recipientTable = SignalDatabase.recipients();

        recipientTable.setDefaultSubscriptionId(recipient.getId(), sendType.getSimSubscriptionIdOr(-1));

        if (!recipient.resolve().isPushGroup()) {
          recipientTable.setForceSmsSelection(recipient.getId(), recipient.get().getRegistered() == RecipientTable.RegisteredState.REGISTERED && sendType.usesSmsTransport());
        }

        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Override
  public void onRecorderPermissionRequired() {
    Permissions.with(this)
               .request(Manifest.permission.RECORD_AUDIO)
               .ifNecessary()
               .withRationaleDialog(getString(R.string.ConversationActivity_to_send_audio_messages_allow_signal_access_to_your_microphone), R.drawable.ic_mic_solid_24)
               .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_requires_the_microphone_permission_in_order_to_send_audio_messages))
               .execute();
  }

  @Override
  public void onRecorderStarted() {
    Vibrator vibrator = ServiceUtil.getVibrator(this);
    vibrator.vibrate(20);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

    audioRecorder.startRecording();
  }

  @Override
  public void onRecorderLocked() {
    voiceRecorderWakeLock.acquire();
    updateToggleButtonState();
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
  }

  @Override
  public void onRecorderFinished() {
    voiceRecorderWakeLock.release();
    updateToggleButtonState();
    Vibrator vibrator = ServiceUtil.getVibrator(this);
    vibrator.vibrate(20);

    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

    ListenableFuture<VoiceNoteDraft> future = audioRecorder.stopRecording();
    future.addListener(new ListenableFuture.Listener<VoiceNoteDraft>() {
      @Override
      public void onSuccess(final @NonNull VoiceNoteDraft result) {
        sendVoiceNote(result.getUri(), result.getSize());
      }

      @Override
      public void onFailure(ExecutionException e) {
        Toast.makeText(ConversationActivity.this, R.string.ConversationActivity_unable_to_record_audio, Toast.LENGTH_LONG).show();
      }
    });
  }

  @Override
  public void onRecorderCanceled() {
    voiceRecorderWakeLock.release();
    updateToggleButtonState();
    Vibrator vibrator = ServiceUtil.getVibrator(this);
    vibrator.vibrate(50);

    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

    ListenableFuture<VoiceNoteDraft> future = audioRecorder.stopRecording();
    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
      future.addListener(new DeleteCanceledVoiceNoteListener());
    } else {
      draftViewModel.saveEphemeralVoiceNoteDraft(future);
    }
  }

  @Override
  public void onEmojiToggle() {
    if (!emojiDrawerStub.resolved()) {
//            Boolean stickersAvailable = stickerViewModel.getStickersAvailability().getValue();

//            initializeMediaKeyboardProviders();
      initializeMediaKeyboardProviders();

      inputPanel.setMediaKeyboard(emojiDrawerStub.get());
    }

    if (container.getCurrentInput() == emojiDrawerStub.get()) {
      container.showSoftkey(composeText);
    } else {
      container.show(composeText, emojiDrawerStub.get());
    }
  }

  @Override
  public void onLinkPreviewCanceled() {
    linkPreviewViewModel.onUserCancel();
  }

  @Override
  public void onStickerSuggestionSelected(@NonNull StickerRecord sticker) {
    sendSticker(sticker, true);
  }

  @Override public void onQuoteChanged(long id, @NonNull RecipientId author) {

  }

  @Override public void onQuoteCleared() {

  }

  @Override
  public void onMediaSelected(@NonNull Uri uri, String contentType) {
    if (MediaUtil.isGif(contentType) || MediaUtil.isImageType(contentType)) {
      SimpleTask.run(getLifecycle(),
                     () -> getKeyboardImageDetails(uri),
                     details -> sendKeyboardImage(uri, contentType, details));
    } else if (MediaUtil.isVideoType(contentType)) {
      setMedia(uri, MediaType.VIDEO);
    } else if (MediaUtil.isAudioType(contentType)) {
      setMedia(uri, MediaType.AUDIO);
    }
  }

  @Override
  public void onCursorPositionChanged(int start, int end) {
    linkPreviewViewModel.onTextChanged(this, composeText.getTextTrimmed().toString(), start, end);
  }

  @Override
  public void onStickerSelected(@NonNull StickerRecord stickerRecord) {
    sendSticker(stickerRecord, false);
  }

  @Override
  public void onStickerManagementClicked() {
    startActivity(StickerManagementActivity.getIntent(this));
    container.hideAttachedInput(true);
  }

  private void sendVoiceNote(@NonNull Uri uri, long size) {
    boolean    initiating = threadId == -1;
    long       expiresIn  = TimeUnit.SECONDS.toMillis(recipient.get().getExpiresInSeconds());
    AudioSlide audioSlide = new AudioSlide(this, uri, size, MediaUtil.AUDIO_AAC, true);
    SlideDeck  slideDeck  = new SlideDeck();

    slideDeck.addSlide(audioSlide);

    ListenableFuture<Void> sendResult = sendMediaMessage(recipient.getId(),
                                                         sendButton.getSelectedSendType(),
                                                         "",
                                                         slideDeck,
                                                         inputPanel.getQuote().orElse(null),
                                                         Collections.emptyList(),
                                                         Collections.emptyList(),
                                                         composeText.getMentions(),
                                                         expiresIn,
                                                         false,
                                                         initiating,
                                                         true,
                                                         null);
    sendResult.addListener(new AssertedSuccessListener<Void>() {
      @Override
      public void onSuccess(Void nothing) {
        draftViewModel.deleteBlob(uri);
      }
    });
  }


  private void sendSticker(@NonNull StickerRecord stickerRecord, boolean clearCompose) {
    sendSticker(new StickerLocator(stickerRecord.getPackId(), stickerRecord.getPackKey(), stickerRecord.getStickerId(), stickerRecord.getEmoji()), stickerRecord.getContentType(), stickerRecord.getUri(), stickerRecord.getSize(), clearCompose);

    SignalExecutors.BOUNDED.execute(() -> SignalDatabase.stickers()
                                                        .updateStickerLastUsedTime(stickerRecord.getRowId(), System.currentTimeMillis())
    );
  }

  private void sendSticker(@NonNull StickerLocator stickerLocator, @NonNull String contentType, @NonNull Uri uri, long size, boolean clearCompose) {
    if (sendButton.getSelectedSendType().usesSmsTransport()) {
      Media  media  = new Media(uri, contentType, System.currentTimeMillis(), StickerSlide.WIDTH, StickerSlide.HEIGHT, size, 0, false, false, Optional.empty(), Optional.empty(), Optional.empty());
      Intent intent = MediaSelectionActivity.editor(this, sendButton.getSelectedSendType(), Collections.singletonList(media), recipient.getId(), composeText.getTextTrimmed());
      startActivityForResult(intent, MEDIA_SENDER);
      return;
    }

    long            expiresIn    = TimeUnit.SECONDS.toMillis(recipient.get().getExpiresInSeconds());
    boolean         initiating   = threadId == -1;
    MessageSendType sendType     = sendButton.getSelectedSendType();
    SlideDeck       slideDeck    = new SlideDeck();
    Slide           stickerSlide = new StickerSlide(this, uri, size, stickerLocator, contentType);

    slideDeck.addSlide(stickerSlide);

    sendMediaMessage(recipient.getId(), sendType, "", slideDeck, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), expiresIn, false, initiating, clearCompose, null);
  }

  private void silentlySetComposeText(String text) {
    typingTextWatcher.setEnabled(false);
    composeText.setText(text);
    typingTextWatcher.setEnabled(true);
  }

  @Override
  public void onReactionsDialogDismissed() {
    fragment.clearFocusedItem();
  }

  @Override
  public void onShown() {
    if (inputPanel != null) {
      inputPanel.getMediaKeyboardListener().onShown();
    }
  }

  @Override
  public void onHidden() {
    if (inputPanel != null) {
      inputPanel.getMediaKeyboardListener().onHidden();
    }
  }

  @Override
  public void onKeyboardChanged(@NonNull KeyboardPage page) {
    if (inputPanel != null) {
      inputPanel.getMediaKeyboardListener().onKeyboardChanged(page);
    }
  }

  @Override
  public void onEmojiSelected(String emoji) {
    if (inputPanel != null) {
      inputPanel.onEmojiSelected(emoji);
    }
  }

  @Override
  public void onKeyEvent(KeyEvent keyEvent) {
    if (keyEvent != null) {
      inputPanel.onKeyEvent(keyEvent);
    }
  }

  @Override
  public void onGifSelectSuccess(@NonNull Uri blobUri, int width, int height) {
    setMedia(blobUri,
             Objects.requireNonNull(MediaType.from(BlobProvider.getMimeType(blobUri))),
             width,
             height,
             false,
             true);
  }

  @Override
  public boolean isMms() {
    return !isSecureText;
  }

  @Override
  public void openEmojiSearch() {
    if (emojiDrawerStub.resolved()) {
      emojiDrawerStub.get().onOpenEmojiSearch();
    }
  }

  @Override public void closeEmojiSearch() {
    if (emojiDrawerStub.resolved()) {
      emojiDrawerStub.get().onCloseEmojiSearch();
    }
  }

  @Override
  public void onVoiceNoteDraftPlay(@NonNull Uri audioUri, double progress) {
    voiceNoteMediaController.startSinglePlaybackForDraft(audioUri, threadId, progress);
  }

  @Override
  public void onVoiceNoteDraftPause(@NonNull Uri audioUri) {
    voiceNoteMediaController.pausePlayback(audioUri);
  }

  @Override
  public void onVoiceNoteDraftSeekTo(@NonNull Uri audioUri, double progress) {
    voiceNoteMediaController.seekToPosition(audioUri, progress);
  }

  @Override
  public void onVoiceNoteDraftDelete(@NonNull Uri audioUri) {
    voiceNoteMediaController.stopPlaybackAndReset(audioUri);
    draftViewModel.deleteVoiceNoteDraft();
  }

  // Listeners

  private final class DeleteCanceledVoiceNoteListener implements ListenableFuture.Listener<VoiceNoteDraft> {
    @Override
    public void onSuccess(final VoiceNoteDraft result) {
      draftViewModel.cancelEphemeralVoiceNoteDraft(result.asDraft());
    }

    @Override
    public void onFailure(ExecutionException e) {}
  }

  private class QuickCameraToggleListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      Permissions.with(ConversationActivity.this)
                 .request(Manifest.permission.CAMERA)
                 .ifNecessary()
                 .withRationaleDialog(getString(R.string.ConversationActivity_to_capture_photos_and_video_allow_signal_access_to_the_camera), R.drawable.ic_camera_24)
                 .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_the_camera_permission_to_take_photos_or_video))
                 .onAllGranted(() -> {
                   composeText.clearFocus();
                   startActivityForResult(MediaSelectionActivity.camera(ConversationActivity.this, sendButton.getSelectedSendType(), recipient.getId(), inputPanel.getQuote().isPresent()), MEDIA_SENDER);
                   overridePendingTransition(R.anim.camera_slide_from_bottom, R.anim.stationary);
                 })
                 .onAnyDenied(() -> Toast.makeText(ConversationActivity.this, R.string.ConversationActivity_signal_needs_camera_permissions_to_take_photos_or_video, Toast.LENGTH_LONG).show())
                 .execute();
    }
  }

  private class SendButtonListener implements OnClickListener, TextView.OnEditorActionListener {
    @Override
    public void onClick(View v) {
      String metricId = recipient.get().isGroup() ? SignalLocalMetrics.GroupMessageSend.start()
                                                  : SignalLocalMetrics.IndividualMessageSend.start();
      sendMessage(metricId);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_SEND) {
        sendButton.performClick();
        return true;
      }
      return false;
    }
  }

  private class ComposeKeyPressedListener implements OnKeyListener, OnClickListener, TextWatcher, OnFocusChangeListener {

    int beforeLength;

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_BACK)
        return false;
      if (event.getAction() == KeyEvent.ACTION_DOWN) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
          if (SignalStore.settings().isEnterKeySends()) {
            sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
            return true;
          }
        }
      } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        mRecyclerview.setVisibility(View.VISIBLE);
        return false;
      }
      return false;
    }

    @Override
    public void onClick(View v) {
      container.showSoftkey(composeText);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      beforeLength = composeText.getTextTrimmed().length();
    }

    @Override
    public void afterTextChanged(Editable s) {
      calculateCharactersRemaining();

      if (composeText.getTextTrimmed().length() == 0 || beforeLength == 0) {
        composeText.postDelayed(ConversationActivity.this::updateToggleButtonState, 50);
      }

//            stickerViewModel.onInputTextUpdated(s.toString());
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      isComposeTextFocus = hasFocus;
      if (hasFocus && container.getCurrentInput() == emojiDrawerStub.get()) {
        container.showSoftkey(composeText);
      }
    }
  }

  private class TypingStatusTextWatcher extends SimpleTextWatcher {

    private boolean enabled = true;

    private String previousText = "";

    @Override
    public void onTextChanged(String text) {
      if (enabled && threadId > 0 && isSecureText && !isSmsForced() && !recipient.get().isBlocked() && !recipient.get().isSelf()) {
        TypingStatusSender typingStatusSender = ApplicationDependencies.getTypingStatusSender();

        if (text.length() == 0) {
          typingStatusSender.onTypingStoppedWithNotify(threadId);
        } else if (text.length() < previousText.length() && previousText.contains(text)) {
          typingStatusSender.onTypingStopped(threadId);
        } else {
          typingStatusSender.onTypingStarted(threadId);
        }

        previousText = text;
      }
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  @Override
  public void onMessageRequest(@NonNull MessageRequestViewModel viewModel) {
    viewModel.onAccept();
//        messageRequestBottomView.setAcceptOnClickListener(v -> viewModel.onAccept());
//        messageRequestBottomView.setDeleteOnClickListener(v -> onMessageRequestDeleteClicked(viewModel));
//        messageRequestBottomView.setBlockOnClickListener(v -> onMessageRequestBlockClicked(viewModel));
//        messageRequestBottomView.setUnblockOnClickListener(v -> onMessageRequestUnblockClicked(viewModel));
//        messageRequestBottomView.setGroupV1MigrationContinueListener(v -> GroupsV1MigrationInitiationBottomSheetDialogFragment.showForInitiation(getSupportFragmentManager(), recipient.getId()));

    viewModel.getRequestReviewDisplayState().observe(this, this::presentRequestReviewBanner);
    viewModel.getMessageData().observe(this, this::presentMessageRequestState);
    viewModel.getFailures().observe(this, this::showGroupChangeErrorToast);
    viewModel.getMessageRequestStatus().observe(this, status -> {
      switch (status) {
        case IDLE:
//                    hideMessageRequestBusy();
          break;
        case ACCEPTING:
        case BLOCKING:
        case DELETING:
//                    showMessageRequestBusy();
          break;
        case ACCEPTED:
//                    hideMessageRequestBusy();
          break;
        case BLOCKED_AND_REPORTED:
//                    hideMessageRequestBusy();
          Toast.makeText(this, R.string.ConversationActivity__reported_as_spam_and_blocked, Toast.LENGTH_SHORT).show();
          break;
        case DELETED:
        case BLOCKED:
//                    hideMessageRequestBusy();
          finish();
      }
    });
  }

  private void presentRequestReviewBanner(@NonNull MessageRequestViewModel.RequestReviewDisplayState state) {
    switch (state) {
      case SHOWN:
        reviewBanner.get().setVisibility(View.VISIBLE);

        CharSequence message = new SpannableStringBuilder().append(SpanUtil.bold(getString(R.string.ConversationFragment__review_requests_carefully)))
                                                           .append(" ")
                                                           .append(getString(R.string.ConversationFragment__signal_found_another_contact_with_the_same_name));

        reviewBanner.get().setBannerMessage(message);

        Drawable drawable = ContextUtil.requireDrawable(this, R.drawable.ic_info_white_24).mutate();
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.signal_icon_tint_primary));

        reviewBanner.get().setBannerIcon(drawable);
        reviewBanner.get().setOnClickListener(unused -> handleReviewRequest(recipient.getId()));
        break;
      case HIDDEN:
        reviewBanner.get().setVisibility(View.GONE);
        break;
      default:
        break;
    }
  }

  private void presentGroupReviewBanner(@NonNull ConversationGroupViewModel.ReviewState groupReviewState) {
    if (groupReviewState.getCount() > 0) {
      reviewBanner.get().setVisibility(View.VISIBLE);
      reviewBanner.get().setBannerMessage(getString(R.string.ConversationFragment__d_group_members_have_the_same_name, groupReviewState.getCount()));
      reviewBanner.get().setBannerRecipient(groupReviewState.getRecipient());
      reviewBanner.get().setOnClickListener(unused -> handleReviewGroupMembers(groupReviewState.getGroupId()));
    } else if (reviewBanner.resolved()) {
      reviewBanner.get().setVisibility(View.GONE);
    }
  }

//    private void showMessageRequestBusy() {
//        messageRequestBottomView.showBusy();
//    }

//    private void hideMessageRequestBusy() {
//        messageRequestBottomView.hideBusy();
//    }

  private void handleReviewGroupMembers(@Nullable GroupId.V2 groupId) {
    if (groupId == null) {
      return;
    }

    ReviewCardDialogFragment.createForReviewMembers(groupId)
                            .show(getSupportFragmentManager(), null);
  }

  private void handleReviewRequest(@NonNull RecipientId recipientId) {
    if (recipientId == Recipient.UNKNOWN.getId()) {
      return;
    }

    ReviewCardDialogFragment.createForReviewRequest(recipientId)
                            .show(getSupportFragmentManager(), null);
  }

  private void showGroupChangeErrorToast(@NonNull GroupChangeFailureReason e) {
    Toast.makeText(this, GroupErrors.getUserDisplayMessage(e), Toast.LENGTH_LONG).show();
  }

  @Override
  public void handleReaction(@NonNull ConversationMessage conversationMessage,
                             @NonNull ConversationReactionOverlay.OnActionSelectedListener onActionSelectedListener,
                             @NonNull SelectedConversationModel selectedConversationModel,
                             @NonNull ConversationReactionOverlay.OnHideListener onHideListener)
  {
    reactionDelegate.setOnActionSelectedListener(onActionSelectedListener);
    reactionDelegate.setOnHideListener(onHideListener);
    reactionDelegate.show(this, recipient.get(), conversationMessage, groupViewModel.isNonAdminInAnnouncementGroup(), selectedConversationModel);
    composeText.clearFocus();
    if (attachmentKeyboardStub.resolved()) {
      attachmentKeyboardStub.get().hide(true);
    }
  }

  public View getPanelLayout() {
    return panelParent;
  }

  public int getComposePanelVisibility() {
    return inputPanel.getVisibility();
  }

  public void isHideInputPanelView(boolean isHide) {
    if (isHide) {
      panelParent.setVisibility(View.GONE);
    } else {
      panelParent.setVisibility(View.VISIBLE);
//            composeText.requestFocus();
      composeText.postDelayed(() -> {
        if (composeText.getVisibility() == View.VISIBLE) {
          composeText.setFocusable(true);
          composeText.setFocusableInTouchMode(true);
          composeText.requestFocus();
        }
      }, 300);
    }
  }


  @Override
  public void onMessageWithErrorClicked(@NonNull MessageRecord messageRecord) {
    if (messageRecord.isIdentityMismatchFailure()) {
      SafetyNumberBottomSheet
          .forMessageRecord(this, messageRecord)
          .show(getSupportFragmentManager());
    } else if (messageRecord.hasFailedWithNetworkFailures()) {
      new MaterialAlertDialogBuilder(this)
          .setMessage(R.string.conversation_activity__message_could_not_be_sent)
          .setNegativeButton(android.R.string.cancel, null)
          .setPositiveButton(R.string.conversation_activity__send, (dialog, which) -> {
            SignalExecutors.BOUNDED.execute(() -> {
              MessageSender.resend(this, messageRecord);
            });
          })
          .show();
    } else {
      MessageDetailsFragment.create(messageRecord, recipient.getId()).show(getSupportFragmentManager(), null);
    }
  }

  @Override
  public void onVoiceNotePause(@NonNull Uri uri) {
    voiceNoteMediaController.pausePlayback(uri);
  }

  @Override
  public void onVoiceNotePlay(@NonNull Uri uri, long messageId, double progress) {
    voiceNoteMediaController.startConsecutivePlayback(uri, messageId, progress);
  }

  @Override public void onVoiceNoteResume(@NonNull Uri uri, long messageId) {

  }

  @Override
  public void onVoiceNoteSeekTo(@NonNull Uri uri, double progress) {
    voiceNoteMediaController.seekToPosition(uri, progress);
  }

  @Override public void onVoiceNotePlaybackSpeedChanged(@NonNull Uri uri, float speed) {

  }

//    @Override
//    public void onVoiceNotePlaybackSpeedChanged(@NonNull Uri uri, float speed) {
//        voiceNoteMediaController.setPlaybackSpeed(uri, speed);
//    }

  @Override
  public void onRegisterVoiceNoteCallbacks(@NonNull Observer<VoiceNotePlaybackState> onPlaybackStartObserver) {
    voiceNoteMediaController.getVoiceNotePlaybackState().observe(this, onPlaybackStartObserver);
  }

  @Override
  public void onUnregisterVoiceNoteCallbacks(@NonNull Observer<VoiceNotePlaybackState> onPlaybackStartObserver) {
    voiceNoteMediaController.getVoiceNotePlaybackState().removeObserver(onPlaybackStartObserver);
  }

  @Override public void onInviteToSignal() {

  }

  @Override
  public void onCursorChanged() {
    if (!reactionDelegate.isShowing()) {
      return;
    }

    SimpleTask.run(() -> {
      //noinspection CodeBlock2Expr
      return SignalDatabase.mmsSms()
                           .checkMessageExists(reactionDelegate.getMessageRecord());
    }, messageExists -> {
      if (!messageExists) {
        reactionDelegate.hide();
      }
    });
  }

  @Override public int getSendButtonTint() {
    return 0;
  }

  @Override public boolean isKeyboardOpen() {
    return false;
  }

  @Override public boolean isAttachmentKeyboardOpen() {
    return false;
  }

  @Override public void openAttachmentKeyboard() {

  }

  @Override
  public void setThreadId(long threadId) {
    this.threadId = threadId;
  }

  @Override
  public void handleReplyMessage(ConversationMessage conversationMessage) {
    MessageRecord messageRecord = conversationMessage.getMessageRecord();

    Recipient author;
    fragment.getList().scrollBy(0, -300);
    fragment.getList().scrollToPosition(0);
    fragment.getList().smoothScrollBy(0, 40);
    if (messageRecord.isOutgoing()) {
      author = Recipient.self();
    } else {
      author = messageRecord.getIndividualRecipient();
    }

    fragment.getListLayoutManager().smoothScrollToPosition(ConversationActivity.this, 0, 100);
    panelParent.setVisibility(View.VISIBLE);
    composeText.requestFocus();

    if (messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getSharedContacts().isEmpty()) {
      Contact   contact     = ((MmsMessageRecord) messageRecord).getSharedContacts().get(0);
      String    displayName = ContactUtil.getDisplayName(contact);
      String    body        = getString(R.string.ConversationActivity_quoted_contact_message, EmojiStrings.BUST_IN_SILHOUETTE, displayName);
      SlideDeck slideDeck   = new SlideDeck();

      if (contact.getAvatarAttachment() != null) {
        slideDeck.addSlide(MediaUtil.getSlideForAttachment(this, contact.getAvatarAttachment()));
      }

      inputPanel.setQuote(GlideApp.with(this),
                          messageRecord.getDateSent(),
                          author,
                          body,
                          slideDeck,
                          MessageRecordUtil.getRecordQuoteType(messageRecord));

    } else if (messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getLinkPreviews().isEmpty()) {
      LinkPreview linkPreview = ((MmsMessageRecord) messageRecord).getLinkPreviews().get(0);
      SlideDeck   slideDeck   = new SlideDeck();

      if (linkPreview.getThumbnail().isPresent()) {
        slideDeck.addSlide(MediaUtil.getSlideForAttachment(this, linkPreview.getThumbnail().get()));
      }

      inputPanel.setQuote(GlideApp.with(this),
                          messageRecord.getDateSent(),
                          author,
                          conversationMessage.getDisplayBody(this),
                          slideDeck,
                          MessageRecordUtil.getRecordQuoteType(messageRecord));
    } else {
      SlideDeck slideDeck = messageRecord.isMms() ? ((MmsMessageRecord) messageRecord).getSlideDeck() : new SlideDeck();

      if (messageRecord.isMms() && ((MmsMessageRecord) messageRecord).isViewOnce()) {
        Attachment attachment = new TombstoneAttachment(MediaUtil.VIEW_ONCE, true);
        slideDeck = new SlideDeck();
        slideDeck.addSlide(MediaUtil.getSlideForAttachment(this, attachment));
      }

      inputPanel.setQuote(GlideApp.with(this),
                          messageRecord.getDateSent(),
                          author,
                          conversationMessage.getDisplayBody(this),
                          slideDeck,
                          MessageRecordUtil.getRecordQuoteType(messageRecord));
    }

    inputPanel.clickOnComposeInput();
  }

  @Override
  public void onMessageActionToolbarOpened() {
    searchViewItem.collapseActionView();
  }

  @Override public void onMessageActionToolbarClosed() {

  }

  @Override public void onBottomActionBarVisibilityChanged(int visibility) {

  }

  @Override
  public void onForwardClicked() {
    inputPanel.clearQuote();
  }

  @Override
  public void onAttachmentChanged() {
    handleSecurityChange(isSecureText, isDefaultSms);
    updateToggleButtonState();
    updateLinkPreviewState();
  }

  @Override public void onLocationRemoved() {

  }

  private int inputAreaHeight() {
    int height = panelParent.getMeasuredHeight();

    if (attachmentKeyboardStub.resolved()) {
      View keyboard = attachmentKeyboardStub.get();
      if (keyboard.getVisibility() == View.VISIBLE) {
        return height + keyboard.getMeasuredHeight();
      }
    }

    return height;
  }

  private void onMessageRequestDeleteClicked(@NonNull MessageRequestViewModel requestModel) {
    Recipient recipient = requestModel.getRecipient().getValue();
    if (recipient == null) {
      Log.w(TAG, "[onMessageRequestDeleteClicked] No recipient!");
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this)
        .setNeutralButton(R.string.ConversationActivity_cancel, (d, w) -> d.dismiss());

    if (recipient.isGroup() && recipient.isBlocked()) {
      builder.setTitle(R.string.ConversationActivity_delete_conversation);
      builder.setMessage(R.string.ConversationActivity_this_conversation_will_be_deleted_from_all_of_your_devices);
      builder.setPositiveButton(R.string.ConversationActivity_delete, (d, w) -> requestModel.onDelete());
    } else if (recipient.isGroup()) {
      builder.setTitle(R.string.ConversationActivity_delete_and_leave_group);
      builder.setMessage(R.string.ConversationActivity_you_will_leave_this_group_and_it_will_be_deleted_from_all_of_your_devices);
      builder.setNegativeButton(R.string.ConversationActivity_delete_and_leave, (d, w) -> requestModel.onDelete());
    } else {
      builder.setTitle(R.string.ConversationActivity_delete_conversation);
      builder.setMessage(R.string.ConversationActivity_this_conversation_will_be_deleted_from_all_of_your_devices);
      builder.setNegativeButton(R.string.ConversationActivity_delete, (d, w) -> requestModel.onDelete());
    }

    builder.show();
  }

  private void onMessageRequestBlockClicked(@NonNull MessageRequestViewModel requestModel) {
    Recipient recipient = requestModel.getRecipient().getValue();
    if (recipient == null) {
      Log.w(TAG, "[onMessageRequestBlockClicked] No recipient!");
      return;
    }

    BlockUnblockDialog.showBlockAndReportSpamFor(this, getLifecycle(), recipient, requestModel::onBlock, requestModel::onBlockAndReportSpam);
  }

  private void onMessageRequestUnblockClicked(@NonNull MessageRequestViewModel requestModel) {
    Recipient recipient = requestModel.getRecipient().getValue();
    if (recipient == null) {
      Log.w(TAG, "[onMessageRequestUnblockClicked] No recipient!");
      return;
    }

    BlockUnblockDialog.showUnblockFor(this, getLifecycle(), recipient, requestModel::onUnblock);
  }

  private static void hideMenuItem(@NonNull Menu menu, @IdRes int menuItem) {
    if (menu.findItem(menuItem) != null) {
      menu.findItem(menuItem).setVisible(false);
    }
  }

  @WorkerThread
  private @Nullable
  KeyboardImageDetails getKeyboardImageDetails(@NonNull Uri uri) {
    try {
      Bitmap bitmap = glideRequests.asBitmap()
                                   .load(new DecryptableStreamUriLoader.DecryptableUri(uri))
                                   .skipMemoryCache(true)
                                   .diskCacheStrategy(DiskCacheStrategy.NONE)
                                   .submit()
                                   .get(1000, TimeUnit.MILLISECONDS);
      int topLeft = bitmap.getPixel(0, 0);
      return new KeyboardImageDetails(bitmap.getWidth(), bitmap.getHeight(), Color.alpha(topLeft) < 255);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      return null;
    }
  }

  private void sendKeyboardImage(@NonNull Uri uri, @NonNull String contentType, @Nullable KeyboardImageDetails details) {
    if (details == null || !details.hasTransparency) {
      setMedia(uri, Objects.requireNonNull(MediaType.from(contentType)));
      return;
    }

    long      expiresIn  = TimeUnit.SECONDS.toMillis(recipient.get().getExpiresInSeconds());
    boolean   initiating = threadId == -1;
    SlideDeck slideDeck  = new SlideDeck();

    if (MediaUtil.isGif(contentType)) {
      slideDeck.addSlide(new GifSlide(this, uri, 0, details.width, details.height, details.hasTransparency, null));
    } else if (MediaUtil.isImageType(contentType)) {
      slideDeck.addSlide(new ImageSlide(this, uri, contentType, 0, details.width, details.height, details.hasTransparency, null, null));
    } else {
      throw new AssertionError("Only images are supported!");
    }

    sendMediaMessage(recipient.getId(),
                     sendButton.getSelectedSendType(),
                     "",
                     slideDeck,
                     null,
                     Collections.emptyList(),
                     Collections.emptyList(),
                     composeText.getMentions(),
                     expiresIn,
                     false,
                     initiating,
                     false,
                     null);
  }

  private class QuoteRestorationTask extends AsyncTask<Void, Void, ConversationMessage> {

    private final String                  serialized;
    private final SettableFuture<Boolean> future;

    QuoteRestorationTask(@NonNull String serialized, @NonNull SettableFuture<Boolean> future) {
      this.serialized = serialized;
      this.future     = future;
    }

    @Override
    protected ConversationMessage doInBackground(Void... voids) {
      QuoteId quoteId = QuoteId.deserialize(ConversationActivity.this, serialized);

      if (quoteId == null) {
        return null;
      }

      Context context = getApplicationContext();

      MessageRecord messageRecord = SignalDatabase.mmsSms().getMessageFor(quoteId.getId(), quoteId.getAuthor());
      if (messageRecord == null) {
        return null;
      }

      return ConversationMessageFactory.createWithUnresolvedData(context, messageRecord);
    }

    @Override
    protected void onPostExecute(ConversationMessage conversationMessage) {
      if (conversationMessage != null) {
        handleReplyMessage(conversationMessage);
        future.set(true);
      } else {
        Log.e(TAG, "Failed to restore a quote from a draft. No matching message record.");
        future.set(false);
      }
    }
  }

  private void presentMessageRequestState(@Nullable MessageRequestViewModel.MessageData messageData) {
    if (!Util.isEmpty(viewModel.getArgs().getDraftText()) ||
        viewModel.getArgs().getMedia() != null ||
        viewModel.getArgs().getStickerLocator() != null)
    {
      Log.d(TAG, "[presentMessageRequestState] Have extra, so ignoring provided state.");
      unblockButton.setVisibility(View.GONE);
//            messageRequestBottomView.setVisibility(View.GONE);
    } else if (isPushGroupV1Conversation() && !isActiveGroup()) {
      Log.d(TAG, "[presentMessageRequestState] Inactive push group V1, so ignoring provided state.");
      unblockButton.setVisibility(View.GONE);
//            messageRequestBottomView.setVisibility(View.GONE);
    } else if (messageData == null) {
      Log.d(TAG, "[presentMessageRequestState] Null messageData. Ignoring.");
    } else if (messageData.getMessageState() == MessageRequestState.NONE) {
      Log.d(TAG, "[presentMessageRequestState] No message request necessary.");
      unblockButton.setVisibility(View.GONE);
//            messageRequestBottomView.setVisibility(View.GONE);
    } else {
      Log.d(TAG, "[presentMessageRequestState] " + messageData.getMessageState());
      unblockButton.setVisibility(View.VISIBLE);
//            messageRequestBottomView.setMessageData(messageData);
//            messageRequestBottomView.setVisibility(View.VISIBLE);
    }

    invalidateOptionsMenu();
  }

  /**
   * Determine if the permission is missing
   */
  private boolean lacksPermission(String permission) {
    return ContextCompat.checkSelfPermission(ConversationActivity.this, permission) ==
           PackageManager.PERMISSION_DENIED;
  }

  private static class KeyboardImageDetails {
    private final int     width;
    private final int     height;
    private final boolean hasTransparency;

    private KeyboardImageDetails(int width, int height, boolean hasTransparency) {
      this.width           = width;
      this.height          = height;
      this.hasTransparency = hasTransparency;
    }
  }
}
