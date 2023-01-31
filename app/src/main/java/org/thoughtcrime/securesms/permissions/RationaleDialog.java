package org.thoughtcrime.securesms.permissions;


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.Mp02CustomDialog;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.ViewUtil;

public class RationaleDialog {

  private static final String TAG = "RationaleDialog";

  public static AlertDialog createNonMsgDialog(@NonNull Context context,
                                               String title,
                                               int positiveResId,
                                               int negativeResId,
                                               Mp02CustomDialog.Mp02DialogKeyListener positiveListener,
                                               Mp02CustomDialog.Mp02DialogKeyListener negativeListener,
                                               Mp02CustomDialog.Mp02OnBackKeyListener backKeyListenr) {
    Mp02CustomDialog dialog = new Mp02CustomDialog(context);
    if(title == null || title.equals("")){
      Log.e(TAG, "Dialog title is NULL!");
    }else{
      dialog.setMessage(title);
    }
    dialog.setPositiveListener(positiveResId, positiveListener);
    dialog.setNegativeListener(negativeResId, negativeListener);
    dialog.setBackKeyListener(backKeyListenr);
    return dialog;
  }

}
