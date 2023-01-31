package org.thoughtcrime.securesms;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.components.ContactFilterView;
import org.thoughtcrime.securesms.components.ContactFilterView.OnFilterChangedListener;
import org.thoughtcrime.securesms.contacts.ContactsCursorLoader.DisplayMode;
import org.thoughtcrime.securesms.contacts.SelectedContact;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.groups.SelectionLimits;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.sms.MessageSender;
import org.thoughtcrime.securesms.sms.OutgoingTextMessage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarInviteTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture.Listener;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class InviteActivity extends PassphraseRequiredActivity implements ContactSelectionListFragment2.OnContactSelectedListener,
                                                                          ContactSelectionListFragment2.SearchCallBack, ContactSelectionListFragment2.SendSmsToCallback
{

  private static final String                        TAG          = InviteActivity.class.getSimpleName();
  public static final  boolean                       DEBUG        = Build.TYPE.equals("userdebug");
  private              ContactSelectionListFragment2 contactsFragment;
  private              ViewGroup                     smsSendFrame;
  private              Button                        smsSendButton;
  private              RecyclerView                  rcyInvite;
  private              Animation                     slideInAnimation;
  private              Animation                     slideOutAnimation;
  private              DynamicTheme                  dynamicTheme = new DynamicNoActionBarInviteTheme();

  public int mFocusHeight;
  public int mNormalHeight;
  public int mNormalPaddingX;
  public int mFocusPaddingX;
  public int mFocusTextSize;
  public int mNormalTextSize;

  private String textStr = "";

  @Override
  protected void onPreCreate() {
    super.onPreCreate();
    dynamicTheme.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    getIntent().putExtra(ContactSelectionListFragment.DISPLAY_MODE, DisplayMode.FLAG_SMS);
    getIntent().putExtra(ContactSelectionListFragment.SELECTION_LIMITS, SelectionLimits.NO_LIMITS);
    getIntent().putExtra(ContactSelectionListFragment.HIDE_COUNT, true);
    getIntent().putExtra(ContactSelectionListFragment.REFRESHABLE, false);

    setContentView(R.layout.invite_activity);

    initializeResources();
    initView();
  }

  private void initView() {

    InviteFriendContactsAdapter inviteFriendContactsAdapter = new InviteFriendContactsAdapter();

    rcyInvite.setLayoutManager(new LinearLayoutManager(this));
    rcyInvite.setAdapter(inviteFriendContactsAdapter);
    rcyInvite.setClipToPadding(false);
    rcyInvite.setClipChildren(false);
    rcyInvite.setPadding(0, 76, 0, 200);
//    rcyInvite.addItemDecoration(new StickyHeaderDecoration(concatenateAdapter, true, true));
    rcyInvite.requestFocus();

  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (smsSendFrame.getVisibility() == View.VISIBLE) {
      return super.onKeyDown(keyCode, event);
    }

    return super.onKeyDown(keyCode, event);
  }

  private void initializeResources() {
    slideInAnimation  = loadAnimation(R.anim.slide_from_bottom);
    slideOutAnimation = loadAnimation(R.anim.slide_to_bottom);

    Button            smsCancelButton = findViewById(R.id.cancel_sms_button);
    Toolbar           smsToolbar      = findViewById(R.id.sms_send_frame_toolbar);
    ContactFilterView contactFilter   = findViewById(R.id.contact_filter_edit_text);

    smsSendFrame     = findViewById(R.id.sms_send_frame);
    smsSendButton    = findViewById(R.id.send_sms_button);
    rcyInvite        = ViewUtil.findById(this, R.id.rcv_first);
    contactsFragment = (ContactSelectionListFragment2) getSupportFragmentManager().findFragmentById(R.id.contact_selection_list_fragment);

    updateSmsButtonText(contactsFragment.getSelectedContacts().size());

    smsCancelButton.setOnClickListener(new SmsCancelClickListener());
    smsSendButton.setOnClickListener(new SmsSendClickListener());
    contactFilter.setOnFilterChangedListener(new ContactFilterChangedListener());
    smsToolbar.setNavigationIcon(R.drawable.ic_search_conversation_24);

    Resources res = getResources();
    mFocusHeight  = res.getDimensionPixelSize(R.dimen.focus_item_height);
    mNormalHeight = res.getDimensionPixelSize(R.dimen.item_height);

    mFocusTextSize  = res.getDimensionPixelSize(R.dimen.focus_item_textsize);
    mNormalTextSize = res.getDimensionPixelSize(R.dimen.item_textsize);

    mFocusPaddingX  = res.getDimensionPixelSize(R.dimen.focus_item_padding_x);
    mNormalPaddingX = res.getDimensionPixelSize(R.dimen.item_padding_x);
  }

  private Animation loadAnimation(@AnimRes int animResId) {
    final Animation animation = AnimationUtils.loadAnimation(this, animResId);
    animation.setInterpolator(new FastOutSlowInInterpolator());
    return animation;
  }

  @Override
  public void onContactSelected(Optional<RecipientId> recipientId, String number) {

  }

  @Override
  public void onContactDeselected(Optional<RecipientId> recipientId, String number) {
    updateSmsButtonText(contactsFragment.getSelectedContacts().size());
  }

  public void onSelectionChanged() {

  }

  private void sendSmsInvites() {
    new SendSmsInvitesAsyncTask(this, textStr)
        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                           contactsFragment.getSelectedContacts()
                                           .toArray(new SelectedContact[0]));
  }

  private void updateSmsButtonText(int count) {
    smsSendButton.setText(getResources().getString(R.string.InviteActivity_send_sms, count));
    smsSendButton.setEnabled(count > 0);
  }

  @Override public void onBackPressed() {
    if (smsSendFrame.getVisibility() == View.VISIBLE) {
      rcyInvite.setVisibility(View.VISIBLE);
      cancelSmsSelection();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onSupportNavigateUp() {
    if (smsSendFrame.getVisibility() == View.VISIBLE) {
      cancelSmsSelection();
      return false;
    } else {
      return super.onSupportNavigateUp();
    }
  }

  private void cancelSmsSelection() {
    contactsFragment.reset();
    updateSmsButtonText(contactsFragment.getSelectedContacts().size());
    ViewUtil.animateOut(smsSendFrame, slideOutAnimation, View.GONE);
  }

  @Override
  public void onSearch(View view) {

  }

  @Override
  public void onSend() {
    if (contactsFragment.getSelectedContacts().size() == 0) {
      Toast.makeText(this, R.string.InviteActivity_send_nullmsg_toast, Toast.LENGTH_SHORT).show();
      return;
    }
    new AlertDialog.Builder(InviteActivity.this)
        .setTitle(getResources().getQuantityString(R.plurals.InviteActivity_send_sms_invites,
                                                   contactsFragment.getSelectedContacts().size(),
                                                   contactsFragment.getSelectedContacts().size()))
        .setMessage(textStr)
        .setPositiveButton(R.string.yes, (dialog, which) -> sendSms(this, textStr))
        .show();

  }

  public void sendSms(Context context, String text) {
    List<String> numbers = new ArrayList<>();
    for (SelectedContact con : contactsFragment.getSelectedContacts()) {
      if (DEBUG) Log.d(TAG, "select number:" + con.getNumber());
      numbers.add(con.getNumber());
    }
    String numbersStr = "";
    String symbol     = ",";
    if (numbers != null && !numbers.isEmpty()) {
      numbersStr = TextUtils.join(symbol, numbers);
    }
    Uri    uri    = Uri.parse("smsto:" + numbersStr);
    Intent intent = new Intent();
    intent.setData(uri);
    intent.putExtra("sms_body", text);
    intent.setAction(Intent.ACTION_SENDTO);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(context);
      if (defaultSmsPackageName != null) {
        intent.setPackage(defaultSmsPackageName);
      }
    }
    if (!(context instanceof Activity)) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    try {
      context.startActivity(intent);
    } catch (Exception e) {
    }
  }

  public class SmsClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      ViewUtil.animateIn(smsSendFrame, slideInAnimation);
      rcyInvite.setVisibility(View.GONE);

    }
  }

  private class SmsCancelClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      cancelSmsSelection();
    }
  }

  private class SmsSendClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      new AlertDialog.Builder(InviteActivity.this)
          .setTitle(getResources().getQuantityString(R.plurals.InviteActivity_send_sms_invites,
                                                     contactsFragment.getSelectedContacts().size(),
                                                     contactsFragment.getSelectedContacts().size()))
          .setMessage(textStr)
          .setPositiveButton(R.string.yes, (dialog, which) -> sendSmsInvites())
          .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
          .show();
    }
  }

  private class ContactFilterChangedListener implements OnFilterChangedListener {
    @Override
    public void onFilterChanged(String filter) {
      contactsFragment.setQueryFilter(filter);
    }
  }

  @SuppressLint("StaticFieldLeak")
  private class SendSmsInvitesAsyncTask extends ProgressDialogAsyncTask<SelectedContact, Void, Void> {
    private final String message;

    SendSmsInvitesAsyncTask(Context context, String message) {
      super(context, R.string.InviteActivity_sending, R.string.InviteActivity_sending);
      this.message = message;
    }

    @Override
    protected Void doInBackground(SelectedContact... contacts) {
      final Context context = getContext();
      if (context == null) return null;

      for (SelectedContact contact : contacts) {
        RecipientId recipientId    = contact.getOrCreateRecipientId(context);
        Recipient   recipient      = Recipient.resolved(recipientId);
        int         subscriptionId = recipient.getDefaultSubscriptionId().orElse(-1);

        MessageSender.send(context, new OutgoingTextMessage(recipient, message, subscriptionId), -1L, true, null, null);

        if (recipient.getContactUri() != null) {
          SignalDatabase.recipients().setHasSentInvite(recipient.getId());
        }
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      final Context context = getContext();
      if (context == null) return;
      rcyInvite.setVisibility(View.VISIBLE);
      ViewUtil.animateOut(smsSendFrame, slideOutAnimation, View.GONE).addListener(new Listener<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
          contactsFragment.reset();
        }

        @Override
        public void onFailure(ExecutionException e) {}
      });
      Toast.makeText(context, R.string.InviteActivity_invitations_sent, Toast.LENGTH_LONG).show();
    }
  }

  /**
   * new add ---- for modifying layout
   */
  class InviteFriendContactsAdapter extends RecyclerView.Adapter<InviteFriendContactsAdapter.ViewHolder> {

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      if (viewType == 0) {
        return new ViewHolder(LayoutInflater.from(InviteActivity.this).inflate(R.layout.contact_selection_invite_text_item, parent, false));
      } else if (viewType == 1) {
        return new ViewHolder(LayoutInflater.from(InviteActivity.this).inflate(R.layout.contact_selection_invite_text_item, parent, false));
      }
      return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      if (position == 0) {
        TextView editText = holder.itemView.findViewById(R.id.tv_name);
        editText.setText(getResources().getString(R.string.InviteActivity_lets_switch_to_signal, getString(R.string.install_url)));
        editText.setOnFocusChangeListener(focusChangeListener);
        textStr = editText.getText().toString();

      } else if (position == 1) {
        TextView shared = holder.itemView.findViewById(R.id.tv_name);
        shared.setText(getResources().getString(R.string.InviteActivity_share_with_contacts));
        shared.setOnFocusChangeListener(focusChangeListener);
        holder.itemView.setOnClickListener(new SmsClickListener());
      }
    }

    @Override
    public int getItemCount() {
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      return position;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

      public ViewHolder(@NonNull View itemView) {
        super(itemView);
      }
    }
  }

  private View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      startAnimationView(v, hasFocus);
    }
  };

  private void startAnimationView(View v, boolean hasFocus) {

    ValueAnimator va;
    if (hasFocus) {
      va = ValueAnimator.ofFloat(0, 1);
    } else {
      va = ValueAnimator.ofFloat(1, 0);
    }

    TextView textView = (TextView) v;
    va.addUpdateListener(li -> {
      float scale    = (float) li.getAnimatedValue();
      int   height   = (int) (mNormalHeight + (mFocusHeight - mNormalHeight) * scale);
      int   textSize = (int) (mNormalTextSize + (mFocusTextSize - mNormalTextSize) * scale);
      int   paddingX = (int) (mNormalPaddingX + (mFocusPaddingX - mNormalPaddingX) * scale);
      int   alpha    = (int) ((float) 0x81 + ((float) 0xff - (float) 0x81) * scale);
      int   color    = alpha * 0x1000000 + 0xffffff;

      textView.setTextSize(textSize);
      textView.setTextColor(color);
      textView.setHeight(height);
      textView.setPadding(paddingX, textView.getPaddingTop(), textView.getPaddingRight(), textView.getPaddingBottom());
      textView.getLayoutParams().height = height;

    });

    FastOutSlowInInterpolator fastOutSlowInInterpolator = new FastOutSlowInInterpolator();
    va.setInterpolator(fastOutSlowInInterpolator);
    va.setDuration(300);
    va.start();
  }

}
