package com.app.afinador_rockstar;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;


class Utils {
    private static final double LOG2 = Math.log(2);

    public static float dpToPixels(Context context) {
        Resources r = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, r.getDisplayMetrics());
    }

    public static double log2(double v) {
        return Math.log(v) / LOG2;
    }

    public static void restoreDefaultColors(Context context, View view, final NeedleView needleView) {

        final GradientDrawable drawable = (GradientDrawable) view.getBackground();
        boolean darkTheme = Preferences.getBoolean(context, context.getString(R.string.pref_dark_theme_key));

        drawable.setAlpha(0);
        // If Theme Dark: color is 'White' else if Theme Light: color is 'Gray 600'
        needleView.setmNeedleColor(darkTheme ? Color.WHITE : Color.argb(255,117,117,117));

    }

    public static void tuneful(View view, NeedleView needleView) {

        view.setBackgroundResource(R.drawable.border);
        final GradientDrawable drawable = (GradientDrawable) view.getBackground();

        ValueAnimator animator = ValueAnimator.ofInt(55, 255);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                drawable.setAlpha((int) animation.getAnimatedValue());
            }
        });

        needleView.setmNeedleColor(GREEN);
        drawable.setStroke(20, GREEN);

        animator.setTarget(drawable);
        animator.setDuration(800);
        animator.start();
    }

    public static void tuneless(final View view, final NeedleView needleView) {

        view.setBackgroundResource(R.drawable.border);
        final GradientDrawable drawable = (GradientDrawable) view.getBackground();

        ValueAnimator animator = ValueAnimator.ofInt(55, 255);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                drawable.setAlpha((int) animation.getAnimatedValue());
            }
        });

        needleView.setmNeedleColor(RED);
        drawable.setStroke(20, RED);

        animator.setTarget(drawable);
        animator.setDuration(800);
        animator.start();
    }

    public static void setupActivityTheme(Activity activity) {
        boolean dark = Preferences.getBoolean(activity, activity.getString(R.string.pref_dark_theme_key));
        if (dark) {
            activity.setTheme(R.style.AppThemeDark);
        } else {
            activity.setTheme(R.style.AppThemeLight);
        }
    }

    public static boolean checkPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
    }
}
