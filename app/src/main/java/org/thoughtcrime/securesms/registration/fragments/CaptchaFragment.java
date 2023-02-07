package org.thoughtcrime.securesms.registration.fragments;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.StrictMode;
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
  private              LinearLayout linearLayout;
  private              Rect         mouseRect;
  private              Rect         webRect;
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

    mWebview     = view.findViewById(R.id.registration_captcha_web_view);
    linearLayout = view.findViewById(R.id.ll_mouse);

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

//    mWebview.setOnTouchListener(new View.OnTouchListener() {
//      @Override public boolean onTouch(View v, MotionEvent event) {
//        Log.d(TAG, "lyh  setOnTouchListener, view has focus " + v.getId() + "    " + event.getAction());
//        Log.d(TAG, "lyh  setOnTouchListener, view has focus " + v.findFocus().getId() + "    " + event.getAction());
//        Log.d(TAG, "lyh  setOnTouchListener, view has focus " + v.findFocus() + "    " + event.getAction());
//        return false;
//      }
//    });
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
        if (linearLayout.getY() < 10) {
          if (mWebview.getY() < 10) {
            mWebview.setTranslationY(mWebview.getY() + 10);
          }
        } else {
          linearLayout.setTranslationY(linearLayout.getY() - 5);
          mWebview.scrollTo((int) linearLayout.getX(),(int) linearLayout.getY());
        }
//        mWebview.dispatchKeyEvent(new KeyEvent(action, KeyEvent.KEYCODE_DPAD_UP));
        break;
      case KeyEvent.KEYCODE_4:
        if (linearLayout.getX() > 0)
          linearLayout.setTranslationX(linearLayout.getX() - 5);
//        mWebview.dispatchKeyEvent(new KeyEvent(action, KeyEvent.KEYCODE_DPAD_LEFT));
        break;
      case KeyEvent.KEYCODE_6:
        if (linearLayout.getX() < 320)
          linearLayout.setTranslationX(linearLayout.getX() + 5);
//        mWebview.dispatchKeyEvent(new KeyEvent(action, KeyEvent.KEYCODE_DPAD_RIGHT));
        break;
      case KeyEvent.KEYCODE_8:
        if (linearLayout.getY() == 240) {
          if (mWebview.getY() + mWebview.getHeight() > 240) {
            mWebview.setTranslationY(mWebview.getY() - 10);
          }
        } else {
          linearLayout.setTranslationY(linearLayout.getY() + 5);
          mWebview.scrollTo((int) linearLayout.getX(),(int) linearLayout.getY());
        }
//        mWebview.dispatchKeyEvent(new KeyEvent(action, KeyEvent.KEYCODE_DPAD_DOWN));
        break;
      case KeyEvent.KEYCODE_5:
          if (action == KeyEvent.ACTION_DOWN) {
            mWebview.dispatchKeyEvent(new KeyEvent(action, KeyEvent.KEYCODE_SPACE));
          }
//        mWebview.dispatchKeyEvent(new KeyEvent(action, KeyEvent.KEYCODE_ENTER));
        mWebview.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                                                       SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, linearLayout.getX(), linearLayout.getY(), 0));
        Log.d(TAG, "lyh  鼠标坐标   x = " + linearLayout.getX() + ", y = " + linearLayout.getY());
//        mWebview.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
//                                                       SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, linearLayout.getX(), linearLayout.getY(), 0));
        break;
      case KeyEvent.KEYCODE_1:
        if (action == KeyEvent.ACTION_DOWN) {
          mWebview.dispatchKeyEvent(new KeyEvent(action, KeyEvent.KEYCODE_SPACE));
        }
        mWebview.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                                                       SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, linearLayout.getX(), linearLayout.getY(), 0));
        break;
      case KeyEvent.KEYCODE_3:
        if (action == KeyEvent.ACTION_DOWN) {
          mWebview.dispatchKeyEvent(new KeyEvent(action, KeyEvent.KEYCODE_SPACE));
        }
        mWebview.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                                                       SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, linearLayout.getX(), linearLayout.getY(), 0));
        mWebview.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                                                       SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, linearLayout.getX(), linearLayout.getY(), 0));
        break;
    }
  }

  private void handleToken(@NonNull String token) {
    viewModel.setCaptchaResponse(token);
    NavHostFragment.findNavController(this).navigateUp();
  }
  public interface CaptchaViewModelProvider extends Serializable {
    @NonNull BaseRegistrationViewModel get(@NonNull CaptchaFragment fragment);
  }
}
