package org.thoughtcrime.securesms.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.annimon.stream.Stream;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.signal.core.util.concurrent.SignalExecutors;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.mp02anim.ItemAnimViewController;
import org.thoughtcrime.securesms.components.settings.BaseSettingsAdapter;
import org.thoughtcrime.securesms.components.settings.BaseSettingsFragment;
import org.thoughtcrime.securesms.components.settings.CustomizableSingleSelectSetting;
import org.thoughtcrime.securesms.components.settings.SingleSelectSetting;
import org.thoughtcrime.securesms.components.settings.app.wrapped.SettingsWrapperFragment;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.database.ThreadTable;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.keyvalue.KeepMessagesDuration;
import org.thoughtcrime.securesms.keyvalue.SettingsValues;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.permissions.RationaleDialog;
import org.thoughtcrime.securesms.permissions.RationaleDialog1;
import org.thoughtcrime.securesms.preferences.widgets.Mp02CommonPreference;
import org.signal.core.util.StringUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.adapter.mapping.MappingModelList;

import java.text.NumberFormat;

public class StoragePreferenceFragment extends ListSummaryPreferenceFragment implements Preference.OnPreferenceClickListener{

  private Mp02CommonPreference keepMessages;
  private Mp02CommonPreference trimLength;
  private Mp02CommonPreference mTrimLimitPref;

  private        PreferenceGroup        mPrefGroup;
  private        int                    mPrefCount;
  private        int                    mCurPoi = 1;
  private        ItemAnimViewController mParentViewController;
  private static Context                mContext;

  @Override
  public void onCreate(@Nullable Bundle paramBundle) {
    super.onCreate(paramBundle);

    findPreference("pref_storage_clear_message_history")
        .setOnPreferenceClickListener(new ClearMessageHistoryClickListener());

    trimLength = (Mp02CommonPreference)findPreference(SettingsValues.THREAD_TRIM_LENGTH);
    trimLength.setOnPreferenceClickListener(this);
    keepMessages = (Mp02CommonPreference)findPreference(SettingsValues.KEEP_MESSAGES_DURATION);
    keepMessages.setOnPreferenceClickListener(this);
    mTrimLimitPref = (Mp02CommonPreference) findPreference(TextSecurePreferences.THREAD_TRIM_LENGTH);
    mTrimLimitPref.setOnPreferenceClickListener(this);

    mPrefGroup = getPreferenceGroup();
    mPrefCount = mPrefGroup.getPreferenceCount();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
//    mParentViewController = getParentAnimViewController();
//    changeView(mCurPoi, false);
    mContext = getActivity();
    return view;
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    String key = preference.getKey();
    switch (key) {
      case SettingsValues.THREAD_TRIM_LENGTH:
        pushFragment(BaseSettingsFragment.create(new ConversationLengthLimitConfiguration()));
        break;
      case SettingsValues.KEEP_MESSAGES_DURATION:
        pushFragment(BaseSettingsFragment.create(new KeepMessagesConfiguration()));
        break;
    }
    return true;
  }

  @Override
  public boolean onKeyDown(KeyEvent event) {
    View v = getListView().getFocusedChild();
    if (v != null) {
      mCurPoi = (int) v.getTag();
    }
    switch (event.getKeyCode()) {
      case KeyEvent.KEYCODE_DPAD_DOWN:
        if (mCurPoi < mPrefCount - 1) {
          mCurPoi++;
          changeView(mCurPoi, true);
        }
        break;
      case KeyEvent.KEYCODE_DPAD_UP:
        if (mCurPoi > 1) {
          mCurPoi--;
          changeView(mCurPoi, false);
        }
        break;
    }
    return super.onKeyDown(event);
  }

  private void changeView(int currentPosition, boolean b) {
    Preference preference = mPrefGroup.getPreference(currentPosition);
    Preference preference1 = null;
    Preference preference2 = null;
    if (currentPosition > 0) {
      preference1 = mPrefGroup.getPreference(currentPosition - 1);
    }
    if (currentPosition < mPrefCount - 1) {
      preference2 = mPrefGroup.getPreference(currentPosition + 1);
    }

    String curTitle = "";
    String title1 = "";
    String title2 = "";
    curTitle = preference.getTitle().toString();
    if (preference1 != null) {
      title1 = preference1.getTitle().toString();
    }
    if (preference2 != null) {
      title2 = preference2.getTitle().toString();
    }
    if (b) {
      mParentViewController.actionUpIn(title1, curTitle);
    } else {
      mParentViewController.actionDownIn(title2, curTitle);
    }
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    addPreferencesFromResource(R.xml.preferences_storage);
  }

  @Override
  public void onResume() {
    super.onResume();
    keepMessages.setSummary(SignalStore.settings().getKeepMessagesDuration().getStringResource());

    trimLength.setSummary(SignalStore.settings().isTrimByLengthEnabled() ? getString(R.string.preferences_storage__s_messages, NumberFormat.getInstance().format(SignalStore.settings().getThreadTrimLength()))
                                                                         : getString(R.string.preferences_storage__none));
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  private void updateToolbarTitle(@StringRes int title) {
    if (getParentFragment() instanceof SettingsWrapperFragment) {
      ((SettingsWrapperFragment) getParentFragment()).setTitle(title);
    }
  }

  private void pushFragment(@NonNull Fragment fragment) {
    getParentFragmentManager().beginTransaction()
                              .replace(R.id.wrapped_fragment, fragment)
                              .addToBackStack(null)
                              .commit();
  }

  public void setOnFocusChangeListener(TextView view){
    view.setFocusable(true);
    view.setOnFocusChangeListener((v, hasFocus) -> {
      if(hasFocus){
        view.setTextColor(getResources().getColor(R.color.signal_button_secondary_text));
      }else {
        view.setTextColor(getResources().getColor(R.color.signal_button_secondary_text_disabled));
      }
    });
  }

  private class ClearMessageHistoryClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      AlertDialog dialog = new MaterialAlertDialogBuilder(requireActivity())
          .setTitle(R.string.preferences_storage__clear_message_history)
          .setMessage(R.string.preferences_storage__this_will_delete_all_message_history_and_media_from_your_device)
          .setPositiveButton(R.string.delete, (d, w) -> showAreYouReallySure())
          .setNegativeButton(android.R.string.cancel, null)
          .create();

      dialog.setOnShowListener(d -> {
        setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
        setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
      });

      dialog.show();

      return true;
    }

    private void showAreYouReallySure() {
      AlertDialog dialog = new MaterialAlertDialogBuilder(requireActivity())
          .setTitle(R.string.preferences_storage__are_you_sure_you_want_to_delete_all_message_history)
          .setMessage(R.string.preferences_storage__all_message_history_will_be_permanently_removed_this_action_cannot_be_undone)
          .setPositiveButton(R.string.preferences_storage__delete_all_now, (d, w) -> SignalExecutors.BOUNDED.execute(() -> SignalDatabase.threads().deleteAllConversations()))
          .setNegativeButton(android.R.string.cancel, null)
          .create();

      dialog.setOnShowListener(d -> {
        setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
        setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
      });

      dialog.show();
    }
  }

  public class KeepMessagesConfiguration extends BaseSettingsFragment.Configuration implements SingleSelectSetting.SingleSelectSelectionChangedListener {

    @Override
    public void configureAdapter(@NonNull BaseSettingsAdapter adapter) {
      adapter.configureSingleSelect(this);
    }

    @Override
    public @NonNull MappingModelList getSettings() {
      KeepMessagesDuration currentDuration = SignalStore.settings().getKeepMessagesDuration();
      return Stream.of(KeepMessagesDuration.values())
                   .map(duration -> new SingleSelectSetting.Item(duration, activity.getString(duration.getStringResource()), duration.equals(currentDuration)))
                   .collect(MappingModelList.toMappingModelList());
    }

    @Override
    public void onSelectionChanged(@NonNull Object selection) {
      KeepMessagesDuration currentDuration = SignalStore.settings().getKeepMessagesDuration();
      KeepMessagesDuration newDuration     = (KeepMessagesDuration) selection;

      if (newDuration.ordinal() > currentDuration.ordinal()) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.preferences_storage__delete_older_messages)
            .setMessage(activity.getString(R.string.preferences_storage__this_will_permanently_delete_all_message_history_and_media, activity.getString(newDuration.getStringResource())))
            .setPositiveButton(R.string.delete, (d, w) -> updateTrimByTime(newDuration))
            .setNegativeButton(android.R.string.cancel, null)
            .create();

        dialog.setOnShowListener(d -> {
          setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
          setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
        });

        dialog.show();
      } else {
        updateTrimByTime(newDuration);
      }
    }

    private void updateTrimByTime(@NonNull KeepMessagesDuration newDuration) {
      SignalStore.settings().setKeepMessagesForDuration(newDuration);
      updateSettingsList();
      ApplicationDependencies.getTrimThreadsByDateManager().scheduleIfNecessary();
    }
  }

  public class ConversationLengthLimitConfiguration extends BaseSettingsFragment.Configuration implements CustomizableSingleSelectSetting.CustomizableSingleSelectionListener {

    private static final int CUSTOM_LENGTH = -1;

    @Override
    public void configureAdapter(@NonNull BaseSettingsAdapter adapter) {
      adapter.configureSingleSelect(this);
      adapter.configureCustomizableSingleSelect(this);
    }

    @Override
    public @NonNull MappingModelList getSettings() {
      int              trimLength   = SignalStore.settings().isTrimByLengthEnabled() ? SignalStore.settings().getThreadTrimLength() : 0;
      int[]            options      = activity.getResources().getIntArray(R.array.conversation_length_limit);
      boolean          hasSelection = false;
      MappingModelList settings     = new MappingModelList();

      for (int option : options) {
        boolean isSelected = option == trimLength;
        String  text       = option == 0 ? activity.getString(R.string.preferences_storage__none)
                                         : activity.getString(R.string.preferences_storage__s_messages, NumberFormat.getInstance().format(option));

        settings.add(new SingleSelectSetting.Item(option, text, isSelected));

        hasSelection = hasSelection || isSelected;
      }

      int currentValue = SignalStore.settings().getThreadTrimLength();
      settings.add(new CustomizableSingleSelectSetting.Item(CUSTOM_LENGTH,
                                                            activity.getString(R.string.preferences_storage__custom),
                                                            !hasSelection,
                                                            currentValue,
                                                            activity.getString(R.string.preferences_storage__s_messages, NumberFormat.getInstance().format(currentValue))));
      return settings;
    }

    @SuppressLint("InflateParams")
    @Override
    public void onCustomizeClicked(@Nullable CustomizableSingleSelectSetting.Item item) {
      boolean trimLengthEnabled = SignalStore.settings().isTrimByLengthEnabled();
      int     trimLength        = trimLengthEnabled ? SignalStore.settings().getThreadTrimLength() : 0;

      View     view     = LayoutInflater.from(activity).inflate(R.layout.customizable_setting_edit_text, null, false);
      EditText editText = view.findViewById(R.id.customizable_setting_edit_text);
      if (trimLength > 0) {
        editText.setText(String.valueOf(trimLength));
      }

      AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
          .setTitle(R.string.preferences__conversation_length_limit)
          .setView(view)
          .setPositiveButton(android.R.string.ok, (d, w) -> onSelectionChanged(Integer.parseInt(editText.getText().toString())))
          .setNegativeButton(android.R.string.cancel, (d, w) -> updateSettingsList())
          .create();

      dialog.setOnShowListener(d -> {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(editText.getText()));
        setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
        setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
        editText.requestFocus();
        editText.addTextChangedListener(new TextWatcher() {
          @Override
          public void afterTextChanged(@NonNull Editable sequence) {
            CharSequence trimmed = StringUtil.trimSequence(sequence);
            if (TextUtils.isEmpty(trimmed)) {
              sequence.replace(0, sequence.length(), "");
            } else {
              try {
                Integer.parseInt(trimmed.toString());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                return;
              } catch (NumberFormatException e) {
                String onlyDigits = trimmed.toString().replaceAll("[^\\d]", "");
                if (!onlyDigits.equals(trimmed.toString())) {
                  sequence.replace(0, sequence.length(), onlyDigits);
                }
              }
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
          }

          @Override
          public void beforeTextChanged(@NonNull CharSequence sequence, int start, int count, int after) {}

          @Override
          public void onTextChanged(@NonNull CharSequence sequence, int start, int before, int count) {}
        });
      });

      dialog.show();
    }

    @Override
    public void onSelectionChanged(@NonNull Object selection) {
      boolean trimLengthEnabled = SignalStore.settings().isTrimByLengthEnabled();
      int     trimLength        = trimLengthEnabled ? SignalStore.settings().getThreadTrimLength() : 0;
      int     newTrimLength     = (Integer) selection;

      if (newTrimLength > 0 && (!trimLengthEnabled || newTrimLength < trimLength)) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.preferences_storage__delete_older_messages)
            .setMessage(activity.getString(R.string.preferences_storage__this_will_permanently_trim_all_conversations_to_the_d_most_recent_messages, NumberFormat.getInstance().format(newTrimLength)))
            .setPositiveButton(R.string.delete, (d, w) -> updateTrimByLength(newTrimLength))
            .setNegativeButton(android.R.string.cancel, null)
            .create();

        dialog.setOnShowListener(d -> {
          setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
          setOnFocusChangeListener(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
        });

        dialog.show();
      } else if (newTrimLength == CUSTOM_LENGTH) {
        onCustomizeClicked(null);
      } else {
        updateTrimByLength(newTrimLength);
      }
    }

    private void updateTrimByLength(int length) {
      boolean restrictingChange = !SignalStore.settings().isTrimByLengthEnabled() || length < SignalStore.settings().getThreadTrimLength();

      SignalStore.settings().setThreadTrimByLengthEnabled(length > 0);
      SignalStore.settings().setThreadTrimLength(length);
      updateSettingsList();

      if (SignalStore.settings().isTrimByLengthEnabled() && restrictingChange) {
        KeepMessagesDuration keepMessagesDuration = SignalStore.settings().getKeepMessagesDuration();

        long trimBeforeDate = keepMessagesDuration != KeepMessagesDuration.FOREVER ? System.currentTimeMillis() - keepMessagesDuration.getDuration()
                                                                                   : ThreadTable.NO_TRIM_BEFORE_DATE_SET;

        SignalExecutors.BOUNDED.execute(() -> SignalDatabase.threads().trimAllThreads(length, trimBeforeDate));
      }
    }
  }
}
