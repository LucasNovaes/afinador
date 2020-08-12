package com.app.afinador_rockstar;

import android.content.Context;
import android.content.Intent;


class NavUtils {
    public static void showSettingsActivity(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }
}
