package com.example.organizadoria;

import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.example.organizadoria.financeiro.ui.FinanceFragment;

public class FinanceiroActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);setContentView(R.layout.activity_financeiro);
        if(!Navigation.requireSession(this))return;
        Navigation.bind(this,R.id.nav_financas);
        findViewById(R.id.financeBack).setOnClickListener(v->getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.financeSettings).setOnClickListener(v->open("settings"));
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){
                if(getSupportFragmentManager().getBackStackEntryCount()>0)getSupportFragmentManager().popBackStack();
                else finish();
            }
        });
        if(state==null) {
            String page=getIntent().getStringExtra("financePage");
            getSupportFragmentManager().beginTransaction().replace(R.id.financeContainer,FinanceFragment.create("overview")).commit();
            if(page!=null && !page.equals("overview"))open(page);
        }
    }
    public void open(String page) {
        getSupportFragmentManager().beginTransaction().replace(R.id.financeContainer,FinanceFragment.create(page)).addToBackStack(page).commit();
    }
    public void title(String value){((TextView)findViewById(R.id.tituloFinanceiro)).setText(value);}
    public void home(){getSupportFragmentManager().popBackStackImmediate(null,androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);getSupportFragmentManager().beginTransaction().replace(R.id.financeContainer,FinanceFragment.create("overview")).commit();}
}
