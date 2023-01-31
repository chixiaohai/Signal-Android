package org.thoughtcrime.securesms;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.registration.fragments.WelcomeFragment;
import org.thoughtcrime.securesms.registration.viewmodel.RegistrationViewModel;

public class DisclaimerFragment extends Fragment {
    private static final String TAG = DisclaimerFragment.class.getSimpleName();
    private TextView mDisclaimerTV;
    //    private TextView mContinueTv;
//    private TextView mContinuePreTv;
//    private LinearLayout mContinueLl;
//    private boolean showTvContinue;
    private RegistrationViewModel model;

    private static final float WELCOME_OPTIOON_SCALE_FOCUS = 1.5f;
    private static final float WELCOME_OPTIOON_SCALE_NON_FOCUS = 1.0f;
    private static final float WELCOME_OPTIOON_TRANSLATION_X_FOCUS = 12.0f;
    private static final float WELCOME_OPTIOON_TRANSLATION_X_NON_FOCUS = 1.0f;

    private void MP02_Animate(View view, boolean b) {
        float scale = b ? WELCOME_OPTIOON_SCALE_FOCUS : WELCOME_OPTIOON_SCALE_NON_FOCUS;
        float transx = b ? WELCOME_OPTIOON_TRANSLATION_X_FOCUS : WELCOME_OPTIOON_TRANSLATION_X_NON_FOCUS;
        ViewCompat.animate(view)
                  .scaleX(scale)
                  .scaleY(scale)
                  .translationX(transx)
                  .start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registration_disclaimer, container, false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.model = getRegistrationViewModel(requireActivity());
        mDisclaimerTV = view.findViewById(R.id.disclaimer_tv);

//        mContinueTv = view.findViewById(R.id.disclaimer_continue);
//        mContinuePreTv = view.findViewById(R.id.disclaimer_continue_pre);
//        mContinueLl = view.findViewById(R.id.disclaimer_continue_container);


        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.disclaimer_1));
        builder.append(getString(R.string.disclaimer_2));
        builder.append(getString(R.string.disclaimer_3));
        builder.append(getString(R.string.disclaimer_4));
        builder.append(getString(R.string.disclaimer_5));
        builder.append(getString(R.string.disclaimer_6));
        builder.append(getString(R.string.disclaimer_7));
        builder.append(getString(R.string.disclaimer_8));
        builder.append(getString(R.string.disclaimer_9));
        mDisclaimerTV.setText(builder);

//        mContinueLl.setOnFocusChangeListener((v, hasFocus) -> {
//            MP02_Animate(mContinueTv, hasFocus);
//            Log.d(TAG, "onViewCreated");
//        });
//
//        mContinueLl.setOnClickListener(v ->  continueClicked(mContinueTv));

    }

    /*private void continueClicked(@NonNull View view) {
        Permissions.with(this)
                .request(Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE)
                .ifNecessary()
                .withRationaleDialog(getString(R.string.RegistrationActivity_signal_needs_access_to_your_contacts_and_media_in_order_to_connect_with_friends),
                        R.drawable.ic_contacts_white_48dp, R.drawable.ic_folder_white_48dp)
                .onAnyResult(() -> {
                    gatherInformationAndContinue(mContinueTv);
                })
                .execute();
    }*/

    protected static RegistrationViewModel getRegistrationViewModel(@NonNull FragmentActivity activity) {
        return new ViewModelProvider(activity).get(RegistrationViewModel.class);
    }

    protected @NonNull RegistrationViewModel getModel() {
        return model;
    }

    protected boolean isCover(View view) {
        boolean cover = false;
        Rect rect = new Rect();
        cover = view.getGlobalVisibleRect(rect);
        if (cover) {
            if (rect.width() >= view.getMeasuredWidth() && rect.height() >= view.getMeasuredHeight()) {
                return !cover;
            }
        }
        return true;
    }

    public void onKeyDown() {
        /*if (!showTvContinue) {
            Log.d(TAG, "onKeyDown showTvContinue=false");
            showTvContinue = !isCover(mContinuePreTv);
            if (!isCover(mContinuePreTv)) {
                mContinueLl.setVisibility(View.VISIBLE);
                mContinuePreTv.setVisibility(View.GONE);
                mContinueLl.requestFocus();
            } else {
                mContinueLl.setVisibility(View.GONE);
                mContinuePreTv.setVisibility(View.VISIBLE);
            }
        } else Log.d(TAG, "onKeyDown showTvContinue=true");*/
    }

    public void onKeyUp() {
        /*if (mContinueLl.getVisibility() == View.VISIBLE) {
            mContinueLl.setVisibility(View.GONE);
            mContinuePreTv.setVisibility(View.VISIBLE);
            showTvContinue = false;
        }*/
    }

    public boolean onKeyDown(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            WelcomeFragment.isBackFromDisclaimerFragment = true;
            WelcomeFragment.isBackFromTermsFragment = false;
            Navigation.findNavController(getView())
                      .navigate(DisclaimerFragmentDirections.actionDisclaimerFragmentToWelcomeFragment3());
            return true;
        }
        return false;
    }
}
