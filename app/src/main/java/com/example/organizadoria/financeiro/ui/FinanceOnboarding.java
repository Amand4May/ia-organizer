package com.example.organizadoria.financeiro.ui;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.example.organizadoria.financeiro.domain.FinanceState;
import com.example.organizadoria.financeiro.domain.Money;
import static com.example.organizadoria.financeiro.domain.FinanceEngine.*;
import static com.example.organizadoria.financeiro.ui.FinanceUi.*;
final class FinanceOnboarding {
 static void watch(TextInputEditText edit,java.util.function.Consumer<String> callback){edit.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){callback.accept(s.toString());}public void afterTextChanged(Editable e){}});}
 static void render(FinanceFragment f,LinearLayout p,FinanceState s){
  f.title("Seu financeiro");if(!f.setup.containsKey("cash")){f.setup.putString("cash",Money.input(s.settings.configured?balance(s,CASH,f.today()):0));f.setup.putString("invested",Money.input(s.settings.configured?balance(s,INVEST,f.today()):0));f.setup.putString("salary",Money.input(s.settings.salaryCents));f.setup.putString("day",String.valueOf(s.settings.payDay));f.setup.putString("contribution",Money.input(s.settings.percentContribution?s.settings.contributionBasisPoints:s.settings.contributionCents));f.setup.putBoolean("percent",s.settings.percentContribution);}
  eyebrow(p,"DO SEU JEITO · "+(f.setupStep+1)+" DE 3");String[] titles={"Comece pelo que\nvocê já tem.","Quando seu\ndinheiro chega?","Separe um pouco\npara o futuro."};add(p,text(p.getContext(),titles[f.setupStep],28,WHITE,true),12);
  String[] notes={"Seu saldo de hoje é o ponto de partida. Organize o restante aos poucos.","O ciclo acompanha seu recebimento. Pode deixar o salário em zero e ajustar depois.","Seu aporte será separado quando o salário for confirmado."};note(p,notes[f.setupStep]);Form form=new Form(p.getContext());
  if(f.setupStep==0){field(f,form,"cash","Saldo na conta principal (R$)");field(f,form,"invested","Quanto já tenho investido (R$)");note(form.body,"Inclua o salário no saldo se ele já entrou. Não vamos registrá-lo de novo.");}
  else if(f.setupStep==1){field(f,form,"salary","Salário líquido previsto (R$)");form.numberField("day","Dia do recebimento",f.setup.getString("day"));watch(form.inputs.get("day"),v->f.setup.putString("day",v));note(form.body,"Dias inexistentes usam o último dia do mês.");}
  else{CheckBox percent=form.check("Prefiro um percentual do salário",f.setup.getBoolean("percent"));percent.setOnCheckedChangeListener((v,b)->f.setup.putBoolean("percent",b));field(f,form,"contribution","Aporte mensal (R$ ou %)");note(form.body,"Pode começar com zero. O plano não realiza transferências bancárias.");}
  form.scroll.removeView(form.body);add(p,form.body,12);button(p,f.setupStep<2?"Continuar":"Revisar e começar",()->{configureDraft(f,s.copy(),f.today());if(f.setupStep<2){f.setupStep++;f.render();}else review(f);},true);if(f.setupStep>0)button(p,"Voltar à etapa anterior",()->{f.setupStep--;f.render();},false);
 }
 private static void field(FinanceFragment f,Form form,String key,String label){form.moneyField(key,label,f.setup.getString(key,"0,00"));watch(form.inputs.get(key),value->f.setup.putString(key,value));}
 private static long amount(FinanceFragment f,String key){return Money.parse(f.setup.getString(key,"0,00"));}
 private static int day(FinanceFragment f){try{int value=Integer.parseInt(f.setup.getString("day","5"));if(value<1||value>31)throw new IllegalArgumentException();return value;}catch(Exception e){throw new IllegalArgumentException("Escolha um dia entre 1 e 31.");}}
 private static void configureDraft(FinanceFragment f,FinanceState s,String today){boolean percent=f.setup.getBoolean("percent");long contribution=amount(f,"contribution");if(percent&&(contribution<0||contribution>10000))throw new IllegalArgumentException("Use de 0 a 100%.");configure(s,amount(f,"cash"),amount(f,"invested"),amount(f,"salary"),day(f),percent,percent?0:contribution,percent?(int)contribution:0,s.settings.expectedAnnualReturn,today);}
 private static void review(FinanceFragment f){Form form=new Form(f.requireContext());note(form.body,"Saldo atual: "+Money.format(amount(f,"cash"))+"\nJá investido: "+Money.format(amount(f,"invested"))+"\nSalário previsto: "+Money.format(amount(f,"salary"))+" · dia "+day(f)+"\nAporte: "+(f.setup.getBoolean("percent")?Money.input(amount(f,"contribution"))+"%":Money.format(amount(f,"contribution"))));note(form.body,"São valores de partida. Nenhum salário será lançado novamente.");String today=f.today();form.dialog("Tudo certo para começar?","Começar",d->f.mutate(state->configureDraft(f,state,today),d,f::home));}
 static void salary(FinanceFragment f){FinanceState s=f.state();Form form=new Form(f.requireContext());form.moneyField("salary","Salário líquido previsto (R$)",Money.input(s.settings.salaryCents));form.numberField("day","Dia do recebimento",String.valueOf(s.settings.payDay));note(form.body,"Ajusta a previsão, sem registrar recebimentos.");form.dialog("Salário e ciclo","Salvar",d->{long amount=FinanceForms.value(form,"salary");int day=FinanceForms.integer(form,"day");f.mutate(state->configure(state,balance(state,CASH,f.today()),balance(state,INVEST,f.today()),amount,day,state.settings.percentContribution,state.settings.contributionCents,state.settings.contributionBasisPoints,state.settings.expectedAnnualReturn,f.today()),d);});}
}
