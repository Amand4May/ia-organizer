package com.example.organizadoria;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;

/** Toda decisão de backend passa pela variante. Local nunca consulta FirebaseAuth. */
public final class AppSession {
    private AppSession() {}
    public static String uid(Context context) {
        if(BuildConfig.LOCAL_ONLY) return context.getSharedPreferences("local_session",Context.MODE_PRIVATE)
                .getBoolean("active",false)?"local-user":null;
        return FirebaseAuth.getInstance().getUid();
    }
    public static void enterLocal(Context context) {
        if(!BuildConfig.LOCAL_ONLY) throw new IllegalStateException("Disponível apenas na cópia local.");
        context.getSharedPreferences("local_session",Context.MODE_PRIVATE).edit().putBoolean("active",true).apply();
    }
    public static void signOut(Context context) {
        if(BuildConfig.LOCAL_ONLY) context.getSharedPreferences("local_session",Context.MODE_PRIVATE).edit().putBoolean("active",false).apply();
        else FirebaseAuth.getInstance().signOut();
        AppServices.release();
    }
}
