package com.example.organizadoria;

import android.app.Activity;
import android.content.Intent;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class Navigation {
    private Navigation(){}
    public static boolean requireSession(Activity activity) {
        if(AppSession.uid(activity)!=null)return true;
        activity.startActivity(new Intent(activity,LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));activity.finish();return false;
    }
    public static void bind(Activity activity,int selected) {
        BottomNavigationView bar=activity.findViewById(R.id.bottomNav);bar.setSelectedItemId(selected);
        bar.setOnItemSelectedListener(item->{
            if(item.getItemId()==selected)return true;
            Class<?> target=item.getItemId()==R.id.nav_inicio?MainActivity.class:item.getItemId()==R.id.nav_agenda?AgendaActivity.class:item.getItemId()==R.id.nav_financas?FinanceiroActivity.class:PerfilActivity.class;
            Intent intent=new Intent(activity,target);
            if(target==MainActivity.class)intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);activity.overridePendingTransition(0,0);
            if(!(activity instanceof MainActivity))activity.finish();
            return false;
        });
    }
}
