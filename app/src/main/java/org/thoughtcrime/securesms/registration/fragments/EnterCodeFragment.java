package org.thoughtcrime.securesms.registration.fragments;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import org.signal.core.util.concurrent.SimpleTask;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.registration.viewmodel.RegistrationViewModel;
import org.thoughtcrime.securesms.util.FeatureFlags;

import java.io.IOException;

public final class EnterCodeFragment extends BaseEnterCodeFragment<RegistrationViewModel> implements SignalStrengthPhoneStateListener.Callback {

  private static final String TAG = Log.tag(EnterCodeFragment.class);


  public EnterCodeFragment() {
    super(R.layout.fragment_registration_enter_code);
  }

  @Override
  protected RegistrationViewModel getViewModel() {
    return new ViewModelProvider(requireActivity()).get(RegistrationViewModel.class);
  }

  @Override
  protected void handleSuccessfulVerify() {
    SimpleTask.run(() -> {
      long startTime = System.currentTimeMillis();
      try {
        FeatureFlags.refreshSync();
        Log.i(TAG, "Took " + (System.currentTimeMillis() - startTime) + " ms to get feature flags.");
      } catch (IOException e) {
        Log.w(TAG, "Failed to refresh flags after " + (System.currentTimeMillis() - startTime) + " ms.", e);
      }
      return null;
    }, none -> displaySuccess(() -> Navigation.findNavController(requireView()).navigate(EnterCodeFragmentDirections.actionSuccessfulRegistration())));

  }

  @Override
  protected void navigateToCaptcha() {
    NavHostFragment.findNavController(this).navigate(EnterCodeFragmentDirections.actionRequestCaptcha());
  }

  @Override
  protected void navigateToRegistrationLock(long timeRemaining) {
    Navigation.findNavController(requireView())
            .navigate(EnterCodeFragmentDirections.actionRequireKbsLockPin(timeRemaining, false));
  }


  @Override
  protected void navigateToKbsAccountLocked() {
    Navigation.findNavController(requireView()).navigate(RegistrationLockFragmentDirections.actionAccountLocked());
  }

}
