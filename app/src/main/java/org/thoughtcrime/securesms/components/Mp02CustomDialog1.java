package org.thoughtcrime.securesms.components;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import org.thoughtcrime.securesms.R;

public class Mp02CustomDialog1 extends AlertDialog {

  private Context                                 mContext;

  private View     currentFocusView;
  private EditText view1;
  private TextView view2, view3;

  public Mp02CustomDialog1(Context context) {
    super(context, R.style.Mp02_Signal_CustomDialog);
    this.mContext = context;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.mp02_custom_dialog1);

    // init
    view1 = findViewById(R.id.et_length_limit);
    view2 = findViewById(R.id.tv_ok);
    view3 = findViewById(R.id.tv_cancel);
    view1.setFocusable(true);
    view2.setFocusable(true);
    view3.setFocusable(true);
    view1.requestFocus();
    Log.i("rsq","view1.requestFocus: "+view1.requestFocus());
    currentFocusView = view1;
    Log.i("rsq","onCreate() finish");
  }

  @Override
  public void show() {
    super.show();
    WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
    layoutParams.gravity = Gravity.BOTTOM;
    layoutParams.width   = LayoutParams.MATCH_PARENT;
    layoutParams.height  = LayoutParams.MATCH_PARENT;
    getWindow().getDecorView().setPadding(0, 0, 0, 0);
    getWindow().setAttributes(layoutParams);
    setupDialog();
  }

  private void setupDialog() {
    Log.i("rsq", "setupDialog() executed");

    view1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          view1.setTextSize(40);
          Log.i("rsq", "view 1 has focus");
        } else {
          view1.setTextSize(20);
          Log.i("rsq", "view 1 not has focus");
        }
      }
    });

    view2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          view2.setTextSize(40);
          Log.i("rsq", "view 2 has focus");
        } else {
          view2.setTextSize(20);
          Log.i("rsq", "view 2 not has focus");
        }
      }
    });

    view3.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          view3.setTextSize(40);
          Log.i("rsq", "view 3 has focus");
        } else {
          view3.setTextSize(20);
          Log.i("rsq", "view 3 not has focus");
        }
      }
    });
  }

  @Override
  public boolean onKeyDown(int KeyCode, KeyEvent ev) {
    Log.i("rsq", "onKeyDown executed");

    switch (KeyCode) {
      case KeyEvent.KEYCODE_DPAD_UP:
        Log.i("rsq", "UP");
        if (currentFocusView == view1) {
          Log.i("rsq", "currentFocusView == view1");
          break;
        } else if (currentFocusView == view2) {
          Log.i("rsq", "currentFocusView == view2");
          currentFocusView = view1;
          view1.requestFocus();
          break;
        } else if (currentFocusView == view3) {
          Log.i("rsq", "currentFocusView == view3");
          currentFocusView = view2;
          view2.requestFocus();
          break;
        }
      case KeyEvent.KEYCODE_DPAD_DOWN:
        Log.i("rsq","DOWN");
        if (currentFocusView == view1) {
          Log.i("rsq","currentFocusView == view1");
          currentFocusView = view2;
          view2.requestFocus();
          break;
        } else if (currentFocusView == view2) {
          Log.i("rsq","currentFocusView == view2");
          currentFocusView = view3;
          view3.requestFocus();
          break;
        } else if (currentFocusView == view3) {
          Log.i("rsq","currentFocusView == view3");
          break;
        }
      case KeyEvent.KEYCODE_BACK:
        Log.i("rsq","Back");
        this.dismiss();
        break;
    }
    return true;
  }
}