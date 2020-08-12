package com.app.afinador_rockstar;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;


class DialogUtils {
    public static void showPermissionDialog(Context context, String message, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.permission)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, listener)
                .show();
    }
}
