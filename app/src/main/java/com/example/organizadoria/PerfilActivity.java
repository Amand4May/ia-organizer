package com.example.organizadoria;

import android.content.*;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.organizadoria.financeiro.domain.*;
import com.example.organizadoria.financeiro.data.*;
import com.google.firebase.auth.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class PerfilActivity extends AppCompatActivity {
    private EditText name,birth,email;private SharedPreferences prefs;
    private FinanceRepository repository;
    private final Runnable financialChanged=()->runOnUiThread(()->{if(!isFinishing())showFinancialFields();});
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_perfil);if(!Navigation.requireSession(this))return;
        Navigation.bind(this,R.id.nav_perfil);String uid=AppSession.uid(this);prefs=getSharedPreferences("DadosPerfil_"+uid,MODE_PRIVATE);
        name=findViewById(R.id.editNome);birth=findViewById(R.id.editDataNascimento);email=findViewById(R.id.editNovoEmail);
        name.setText(prefs.getString("nome",""));birth.setText(prefs.getString("nascimento",""));initials();dateMask();
        if(BuildConfig.LOCAL_ONLY){email.setText("Conta local");email.setEnabled(false);}
        else {FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();if(user!=null)email.setText(user.getEmail());}
        findViewById(R.id.iconEditNome).setOnClickListener(v->focus(name));findViewById(R.id.iconEditData).setOnClickListener(v->focus(birth));findViewById(R.id.iconEditEmail).setOnClickListener(v->focus(email));
        int[] fields={R.id.editRenda,R.id.editAssinaturas,R.id.editTotalInvestido};
        for(int id:fields){EditText value=findViewById(id);value.setFocusable(false);value.setCursorVisible(false);value.setOnClickListener(v->finance());}
        for(int id:new int[]{R.id.iconEditRenda,R.id.iconEditAssinaturas,R.id.iconEditInvest})findViewById(id).setOnClickListener(v->finance());
        findViewById(R.id.btnSalvarInfo).setOnClickListener(v->save());
        findViewById(R.id.btnSair).setOnClickListener(v->{AppSession.signOut(this);startActivity(new Intent(this,LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));});
        repository=AppServices.repository(this);
    }
    private void focus(EditText field){field.requestFocus();field.setSelection(field.getText().length());android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.showSoftInput(field,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}
    private void dateMask(){birth.addTextChangedListener(new android.text.TextWatcher(){
        private String current="";
        public void beforeTextChanged(CharSequence s,int start,int count,int after){}
        public void onTextChanged(CharSequence s,int start,int before,int count){
            if(s.toString().equals(current))return;
            String digits=s.toString().replaceAll("[^0-9]",""),previous=current.replaceAll("[^0-9]","");int length=digits.length(),cursor=length;
            for(int i=2;i<=length && i<6;i+=2)cursor++;if(digits.equals(previous))cursor--;
            digits=length<8?digits+"DDMMYYYY".substring(length):digits.substring(0,8);
            current=digits.substring(0,2)+"/"+digits.substring(2,4)+"/"+digits.substring(4,8);birth.setText(current);birth.setSelection(Math.min(Math.max(0,cursor),current.length()));
        }
        public void afterTextChanged(android.text.Editable s){}
    });}
    private void finance(){startActivity(new Intent(this,FinanceiroActivity.class).putExtra("financePage","settings"));}
    @Override protected void onStart(){super.onStart();if(repository!=null)repository.addListener(financialChanged);}
    @Override protected void onStop(){if(repository!=null)repository.removeListener(financialChanged);super.onStop();}
    @Override protected void onResume(){super.onResume();if(!Navigation.requireSession(this))return;showFinancialFields();}
    private void showFinancialFields(){
        if(repository==null)return;
        if(repository instanceof FirestoreFinanceRepository && ((FirestoreFinanceRepository)repository).statusMessage()!=null){for(int id:new int[]{R.id.editRenda,R.id.editTotalInvestido,R.id.editAssinaturas})((EditText)findViewById(id)).setText("Carregando…");return;}
        FinanceState state=repository.snapshot();FinanceEngine.Summary s=FinanceEngine.summarize(state,LocalDate.now().toString());
        ((EditText)findViewById(R.id.editRenda)).setText(Money.format(state.settings.salaryCents));
        ((EditText)findViewById(R.id.editTotalInvestido)).setText(Money.format(s.invested));
        long recurring=0;for(FinanceState.Rule rule:state.rules)if(rule.active)recurring+=rule.amountCents;
        ((EditText)findViewById(R.id.editAssinaturas)).setText(Money.format(recurring));
    }
    private void initials(){String n=name.getText().toString().trim();String[] parts=n.split("\\s+");((TextView)findViewById(R.id.textIniciais)).setText(n.isEmpty()?"?":(parts[0].substring(0,1)+(parts.length>1?parts[parts.length-1].substring(0,1):"")).toUpperCase(Locale.getDefault()));}
    private void save(){
        String n=name.getText().toString().trim(),b=birth.getText().toString().trim(),newEmail=email.getText().toString().trim();
        if(!b.isEmpty())try{LocalDate d=LocalDate.parse(b,DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));if(d.isAfter(LocalDate.now()))throw new IllegalArgumentException();}catch(Exception e){Toast.makeText(this,"Use uma data de nascimento válida: DD/MM/AAAA",Toast.LENGTH_LONG).show();return;}
        prefs.edit().putString("nome",n).putString("nascimento",b).apply();initials();
        if(!BuildConfig.LOCAL_ONLY){
            FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
            if(user!=null&&!newEmail.isEmpty()&&!newEmail.equals(user.getEmail())){
                user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task->Toast.makeText(this,task.isSuccessful()?"Perfil salvo. Confirme o novo e-mail pela mensagem enviada.":"Perfil salvo; não foi possível solicitar a troca de e-mail. Entre novamente e tente outra vez.",Toast.LENGTH_LONG).show());return;
            }
        }
        Toast.makeText(this,"Perfil salvo",Toast.LENGTH_SHORT).show();
    }
}
