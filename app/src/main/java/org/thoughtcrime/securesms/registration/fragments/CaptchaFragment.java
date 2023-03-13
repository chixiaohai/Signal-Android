package org.thoughtcrime.securesms.registration.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.InputDeviceCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.LoggingFragment;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.registration.viewmodel.BaseRegistrationViewModel;
import org.thoughtcrime.securesms.registration.viewmodel.RegistrationViewModel;

import java.io.Serializable;

/**
 * Fragment that displays a Captcha in a WebView.
 */
public final class CaptchaFragment extends LoggingFragment {
  private static final String       TAG                       = Log.tag(CaptchaFragment.class);
  private              WebView      mWebview;
  private              LinearLayout mMouse;
  public static final  String       EXTRA_VIEW_MODEL_PROVIDER = "view_model_provider";

  private BaseRegistrationViewModel viewModel;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_registration_captcha, container, false);
  }

  @Override
  @SuppressLint("SetJavaScriptEnabled")
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mWebview = view.findViewById(R.id.registration_captcha_web_view);
    mMouse   = view.findViewById(R.id.ll_mouse);

    mWebview.getSettings().setJavaScriptEnabled(true);
    mWebview.clearCache(true);

    mWebview.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url != null && url.startsWith(RegistrationConstants.SIGNAL_CAPTCHA_SCHEME)) {
          handleToken(url.substring(RegistrationConstants.SIGNAL_CAPTCHA_SCHEME.length()));
          return true;
        }
        return false;
      }
//      public void onPageFinished(WebView view, String url) {
//        view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
//                                                   SystemClock.uptimeMillis(),MotionEvent.ACTION_DOWN,160, 120, 0));
//        view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
//                                                   SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,160, 120, 0));
//      }

    });

    mWebview.setVerticalScrollBarEnabled(false);
    mWebview.loadUrl(RegistrationConstants.SIGNAL_CAPTCHA_URL);

    CaptchaViewModelProvider provider = null;
    if (getArguments() != null) {
      provider = (CaptchaViewModelProvider) requireArguments().getSerializable(EXTRA_VIEW_MODEL_PROVIDER);
    }

    if (provider == null) {
      viewModel = new ViewModelProvider(requireActivity()).get(RegistrationViewModel.class);
    } else {
      viewModel = provider.get(this);
    }

  }

  public void onKeyDown(int keyCode, int action) {
    if (mWebview == null) {
      return;
    }
    switch (keyCode) {
      case KeyEvent.KEYCODE_0:
        Log.d(TAG,"reload");
        mWebview.reload();
        break;
      case KeyEvent.KEYCODE_2:
        if (mMouse.getY() < 10) {
          if (mWebview.getY() < 10) {
            mWebview.setTranslationY(mWebview.getY() + 10);
          }
        } else {
          mMouse.setTranslationY(mMouse.getY() - 5);
          mWebview.scrollTo((int) mMouse.getX(), (int) mMouse.getY());
        }
        break;
      case KeyEvent.KEYCODE_4:
        if (mMouse.getX() > 0)
          mMouse.setTranslationX(mMouse.getX() - 5);
        break;
      case KeyEvent.KEYCODE_6:
        if (mMouse.getX() < 320)
          mMouse.setTranslationX(mMouse.getX() + 5);
        break;
      case KeyEvent.KEYCODE_8:
        if (mMouse.getY() == 240) {
          if (mWebview.getY() + mWebview.getHeight() > 240) {
            mWebview.setTranslationY(mWebview.getY() - 10);
          }
        } else {
          mMouse.setTranslationY(mMouse.getY() + 5);
          mWebview.scrollTo((int) mMouse.getX(), (int) mMouse.getY());
        }
        break;
      case KeyEvent.KEYCODE_5:
        if (action == KeyEvent.ACTION_DOWN) {
          mWebview.dispatchTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, mMouse.getX(), mMouse.getY()));
          mWebview.dispatchTouchEvent(createMotionEvent(MotionEvent.ACTION_UP, mMouse.getX(), mMouse.getY()));
        }
        break;
    }
  }

  private MotionEvent createMotionEvent(int action, Float x, Float y){
    long  currentTime = SystemClock.uptimeMillis();
    int   pointerCount = 1;

    MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[1];
    pointerProperties[0]=new MotionEvent.PointerProperties();
    pointerProperties[0].clear();
    pointerProperties[0].id = 0;
    pointerProperties[0].toolType = MotionEvent.TOOL_TYPE_FINGER;


    MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[1];
    pointerCoords[0]=new MotionEvent.PointerCoords();
    pointerCoords[0].clear();
    pointerCoords[0].x = x;
    pointerCoords[0].y = y;
    pointerCoords[0].pressure = 1.0f;
    pointerCoords[0].size = 1.0f;

    int metaState = 0;
    int buttonState = 0;
    float xPrecision = 1.0f;
    float yPrecision = 1.0f;
    int deviceId = 0;
    int edgeFlags = 0;
    int source = InputDeviceCompat.SOURCE_TOUCHSCREEN;
    int FLAG_TARGET_ACCESSIBILITY_FOCUS = 0x4000000;

    return MotionEvent.obtain(currentTime,
                              currentTime,
                              action,
                              pointerCount,
                              pointerProperties,
                              pointerCoords,
                              metaState,
                              buttonState,
                              xPrecision,
                              yPrecision,
                              deviceId,
                              edgeFlags, source, FLAG_TARGET_ACCESSIBILITY_FOCUS);
  }

  private void handleToken(@NonNull String token) {
    viewModel.setCaptchaResponse(token);
    NavHostFragment.findNavController(this).navigateUp();
  }
  public interface CaptchaViewModelProvider extends Serializable {
    @NonNull BaseRegistrationViewModel get(@NonNull CaptchaFragment fragment);
  }
}
