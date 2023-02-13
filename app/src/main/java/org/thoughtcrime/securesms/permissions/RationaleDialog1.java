package org.thoughtcrime.securesms.permissions;


import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.components.Mp02CustomDialog1;

public class RationaleDialog1 {

  public static AlertDialog createNonMsgDialog(@NonNull Context context)
  {
    Mp02CustomDialog1 dialog = new Mp02CustomDialog1(context);
    return dialog;
  }
}
