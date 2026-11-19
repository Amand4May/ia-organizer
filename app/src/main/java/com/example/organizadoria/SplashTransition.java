package com.example.organizadoria;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

/** Saída da tela de abertura: o "C" cresce na tela e o fundo azul some, revelando o app (Android 12+). */
final class SplashTransition {
    private SplashTransition() {}

    static void aplicar(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        activity.getSplashScreen().setOnExitAnimationListener(splash -> {
            View icone = splash.getIconView();
            if (icone != null) {
                icone.animate().scaleX(8f).scaleY(8f)
                        .setInterpolator(new AccelerateInterpolator())
                        .setDuration(500).start();
            }
            splash.animate().alpha(0f).setStartDelay(200).setDuration(350)
                    .withEndAction(splash::remove).start();
        });
    }
}