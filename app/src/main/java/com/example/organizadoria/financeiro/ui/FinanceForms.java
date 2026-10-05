package com.example.organizadoria.financeiro.ui;

import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.example.organizadoria.*;
import com.example.organizadoria.financeiro.domain.*;
import static com.example.organizadoria.financeiro.domain.FinanceState.*;
import static com.example.organizadoria.financeiro.domain.FinanceEngine.*;
import static com.example.organizadoria.financeiro.ui.FinanceUi.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

final class FinanceForms {
    private FinanceForms(){}
    static AlertDialog.Builder alert(FinanceFragment f,String title){return new AlertDialog.Builder(f.requireContext(),R.style.Theme_Finance_Dialog).setTitle(title);}
    static void message(FinanceFragment f,String title,String text){alert(f,title).setMessage(text).setPositiveButton("Fechar",null).show();}
    static List<String> accountNames(FinanceState s){List<String> names=new ArrayList<>();for(Account a:s.accounts)names.add(a.name);return names;}
    static int accountIndex(FinanceState s,String id){for(int i=0;i<s.accounts.size();i++)if(s.accounts.get(i).id.equals(id))return i;return 0;}
    static long value(Form form,String key){return Money.parse(form.value(key));}
    static double rate(Form form,String key){try{return Double.parseDouble(form.value(key).replace(',','.'));}catch(Exception e){throw new IllegalArgumentException("Informe uma taxa válida.");}}
    static int integer(Form form,String key){try{return Integer.parseInt(form.value(key));}catch(Exception e){throw new IllegalArgumentException("Informe um número inteiro válido.");}}
    static String previousProfile(FinanceFragment f){
        String uid=AppSession.uid(f.requireContext());if(uid==null)return "";
        android.content.SharedPreferences prefs=f.requireContext().getSharedPreferences("DadosPerfil_"+uid,android.content.Context.MODE_PRIVATE);
        StringBuilder text=new StringBuilder();
        for(String[] field:new String[][]{{"renda","Renda"},{"investimentos","Investimentos"},{"assinaturas","Assinaturas"}}){String value=prefs.getString(field[0],"");if(value!=null&&!value.trim().isEmpty())text.append(field[1]).append(": ").append(value).append("\n");}
        return text.length()==0?"":"Valores do perfil anterior, para conferir:\n"+text+"Confirme os saldos atuais e cadastre as contas recorrentes com seus vencimentos. Esses valores não foram somados automaticamente.";
    }
    static void contribution(FinanceFragment f){
        FinanceState s=f.state();Form form=new Form(f.requireContext());CheckBox pct=form.check("Usar percentual do salário",s.settings.percentContribution);
        form.moneyField("value","Valor em R$ ou percentual",s.settings.percentContribution?Money.input(s.settings.contributionBasisPoints):Money.input(s.settings.contributionCents));
        note(form.body,"O plano atualiza o valor ainda pendente do ciclo e a regra dos próximos recebimentos. Aportes já realizados são preservados.");
        form.dialog("Plano de aporte","Salvar",d->{long amount=value(form,"value");boolean percent=pct.isChecked();if(percent && (amount<0 || amount>10000))throw new IllegalArgumentException("Percentual deve estar entre 0 e 100%.");
            OrganizerCommand c=new OrganizerCommand(OrganizerCommand.Action.CONFIG_CONTRIBUTION);c.percent=percent;c.basisPoints=percent?(int)amount:0;c.amount=percent?0:amount;
            String command=id();f.mutate(state->CommandRouter.apply(state,Collections.singletonList(c),command,f.today()),d);
        });
    }
    static void salary(FinanceFragment f){
        FinanceState s=f.state();List<Forecast> salaries=new ArrayList<>();List<String> labels=new ArrayList<>();
        labels.add("Recebimento sem previsão anterior");
        for(Forecast a:pending(s))if("salary".equals(a.ruleId)){salaries.add(a);labels.add(FinanceScreens.displayDate(a.date)+" · "+Money.format(remaining(s,a)));}
        Form form=new Form(f.requireContext());Spinner forecast=form.select("Qual recebimento?",labels);if(!salaries.isEmpty())forecast.setSelection(1);
        form.moneyField("value","Valor líquido recebido (R$)",Money.input(s.settings.salaryCents));form.dateField("date","Data efetiva (AAAA-MM-DD)",f.today());
        CheckBox partial=form.check("Este é um recebimento parcial",false);
        note(form.body,"Confirme somente dinheiro que já entrou. O recebimento principal inicia o ciclo e separa o aporte planejado.");
        String command=id();form.dialog("Recebi meu salário","Confirmar",d->{
            OrganizerCommand c=new OrganizerCommand(OrganizerCommand.Action.SALARY);c.title="Salário";c.amount=value(form,"value");c.date=form.value("date");c.partial=partial.isChecked();
            int index=forecast.getSelectedItemPosition();if(index>0)c.targetId=salaries.get(index-1).id;
            f.mutate(state->CommandRouter.apply(state,Collections.singletonList(c),command,f.today()),d);
        });
    }
    static void movement(FinanceFragment f,Kind forced){
        FinanceState s=f.state();Form form=new Form(f.requireContext());
        Kind[] kinds=forced==null?new Kind[]{Kind.EXPENSE,Kind.INCOME,Kind.CONTRIBUTION,Kind.TRANSFER}:new Kind[]{forced};
        List<String> labels=new ArrayList<>();for(Kind k:kinds)labels.add(FinanceScreens.kindLabel(k));Spinner type=form.select("Tipo",labels);
        form.textField("title","Descrição",forced==Kind.CONTRIBUTION?"Aporte":"");form.moneyField("amount","Valor realizado (R$)","");form.dateField("date","Data efetiva (AAAA-MM-DD)",f.today());
        Spinner from=form.select("Conta de saída (despesa/transferência)",accountNames(s));from.setSelection(accountIndex(s,CASH));
        Spinner to=form.select("Conta de entrada (receita/transferência)",accountNames(s));to.setSelection(accountIndex(s,forced==Kind.CONTRIBUTION?INVEST:CASH));
        List<Forecast> plans=pending(s);List<String> choices=new ArrayList<>();choices.add("Sem previsão vinculada");for(Forecast a:plans)choices.add(FinanceScreens.kindLabel(a.kind)+": "+a.title+" · "+Money.format(remaining(s,a)));
        Spinner plan=form.select("Conciliar com uma previsão",choices);
        if(forced==Kind.CONTRIBUTION){int count=0,index=0;for(int i=0;i<plans.size();i++)if(plans.get(i).kind==Kind.CONTRIBUTION){count++;index=i+1;}if(count==1)plan.setSelection(index);}
        CheckBox partial=form.check("Manter o restante da previsão pendente",forced==Kind.CONTRIBUTION);
        note(form.body,"Se vincular uma previsão, serão utilizadas as contas dela. Transferir de conta para aplicação será registrado como aporte.");
        String entryId=id();form.dialog("Registrar movimentação","Registrar",d->{
            Kind kind=kinds[type.getSelectedItemPosition()];String title=form.value("title"),date=form.value("date");long amount=value(form,"amount");
            String source=s.accounts.get(from.getSelectedItemPosition()).id,target=s.accounts.get(to.getSelectedItemPosition()).id;
            int link=plan.getSelectedItemPosition();String forecastId=link>0?plans.get(link-1).id:null;
            if(forecastId!=null){Forecast a=plans.get(link-1);source=a.accountId;target=a.targetAccountId;if(kind==Kind.INCOME)target=a.accountId;}
            if(kind==Kind.INCOME)source=null;if(kind==Kind.EXPENSE)target=null;
            if(kind==Kind.TRANSFER && FinanceEngine.account(s,source).kind==AccountKind.CASH && FinanceEngine.account(s,target).kind==AccountKind.INVESTMENT)kind=Kind.CONTRIBUTION;
            Kind finalKind=kind;String src=source,dst=target;boolean complete=!partial.isChecked();
            f.mutate(state->record(state,entryId,finalKind,title,amount,date,src,dst,forecastId,complete,f.today()),d);
        });
    }
    static void withdrawal(FinanceFragment f){
        FinanceState s=f.state();Form form=new Form(f.requireContext());List<Account> investments=new ArrayList<>(),cash=new ArrayList<>();List<String> invNames=new ArrayList<>(),cashNames=new ArrayList<>();
        for(Account a:s.accounts)if(a.kind==AccountKind.INVESTMENT){investments.add(a);invNames.add(a.name);}else{cash.add(a);cashNames.add(a.name);}
        Spinner source=form.select("Aplicação de origem",invNames),target=form.select("Conta de destino",cashNames);form.moneyField("value","Valor resgatado (R$)","");form.dateField("date","Data (AAAA-MM-DD)",f.today());
        String entry=id();form.dialog("Registrar resgate","Registrar",d->{long amount=value(form,"value");String date=form.value("date"),from=investments.get(source.getSelectedItemPosition()).id,to=cash.get(target.getSelectedItemPosition()).id;
            f.mutate(state->record(state,entry,Kind.TRANSFER,"Resgate",amount,date,from,to,null,true,f.today()),d);});
    }
    static void plan(FinanceFragment f,String editingId){
        FinanceState s=f.state();Forecast old=editingId==null?null:forecast(s,editingId);Form form=new Form(f.requireContext());
        Kind[] kinds=old==null?new Kind[]{Kind.EXPENSE,Kind.CONTRIBUTION,Kind.INCOME}:new Kind[]{old.kind};List<String> labels=new ArrayList<>();for(Kind k:kinds)labels.add(FinanceScreens.kindLabel(k));Spinner type=form.select("Tipo",labels);
        form.textField("title","Descrição",old==null?"":old.title);form.moneyField("value","Valor total previsto (R$)",old==null?"":Money.input(old.amountCents));form.dateField("date","Vencimento (AAAA-MM-DD)",old==null?f.today():old.date);
        String key=id();form.dialog(old==null?"Nova previsão":"Editar previsão","Salvar",d->{Kind kind=kinds[type.getSelectedItemPosition()];String title=form.value("title"),date=form.value("date");long amount=value(form,"value");
            f.mutate(state->{if(editingId!=null)editForecast(state,editingId,title,amount,date);else FinanceEngine.plan(state,key,kind,title,amount,date,CASH,kind==Kind.CONTRIBUTION?INVEST:null,null);},d);});
    }
    static void forecastDetails(FinanceFragment f,String id){
        FinanceState s=f.state();Forecast a=forecast(s,id);
        String text=FinanceScreens.kindLabel(a.kind)+" · "+FinanceScreens.displayDate(a.date)+"\nPlanejado: "+Money.format(a.amountCents)+"\nRealizado: "+Money.format(paid(s,a))+"\nRestante: "+Money.format(remaining(s,a));
        if(a.agendaId!=null)text+="\nVinculado a um compromisso da Agenda.";
        if(a.completed || a.cancelled){message(f,a.title,text+"\n"+FinanceScreens.statusLabel(s,a));return;}
        alert(f,a.title).setMessage(text).setPositiveButton("Confirmar valor",(d,w)->settlement(f,id)).setNeutralButton("Editar",(d,w)->plan(f,id)).setNegativeButton("Cancelar previsão",(d,w)->
            alert(f,"Cancelar previsão?").setMessage("Os valores já realizados serão preservados. O restante deixará de ficar comprometido.").setPositiveButton("Cancelar previsão",(x,y)->f.mutate(state->cancelForecast(state,id,f.today()),null)).setNegativeButton("Voltar",null).show()).show();
    }
    static void settlement(FinanceFragment f,String id){
        FinanceState s=f.state();Forecast a=forecast(s,id);Form form=new Form(f.requireContext());form.moneyField("value","Valor efetivamente realizado (R$)",Money.input(remaining(s,a)));
        CheckBox partial=form.check("Manter diferença como pendente",a.kind==Kind.CONTRIBUTION);note(form.body,"Ao encerrar, uma diferença menor libera o valor que sobrou da previsão.");
        String key=id();form.dialog(a.title,"Confirmar",d->{long amount=value(form,"value");boolean complete=!partial.isChecked();f.mutate(state->settle(state,key,id,amount,complete,f.today()),d);});
    }
    static void entryDetails(FinanceFragment f,String id){
        Entry entry=null;for(Entry e:f.state().entries)if(e.id.equals(id))entry=e;if(entry==null)return;Entry e=entry;
        String text=FinanceScreens.kindLabel(e.kind)+" · "+FinanceScreens.displayDate(e.date)+"\n"+Money.format(e.amountCents)+(e.voided?"\nEstornado: "+e.voidReason:"");
        if(e.forecastId!=null){Forecast p=forecast(f.state(),e.forecastId);text+="\nPrevisão original: "+Money.format(p.amountCents)+"\nSituação: "+FinanceScreens.statusLabel(f.state(),p);}
        AlertDialog.Builder b=alert(f,e.title).setMessage(text).setNegativeButton("Fechar",null);
        if(!e.voided && e.kind!=Kind.VALUATION){b.setPositiveButton("Corrigir",(d,w)->correctEntry(f,e));b.setNeutralButton("Estornar",(d,w)->{
            Form form=new Form(f.requireContext());form.textField("reason","Motivo do estorno","");form.dialog("Estornar movimentação","Estornar",dialog->{String reason=form.value("reason");f.mutate(state->voidEntry(state,id,reason,f.today()),dialog);});});}
        b.show();
    }
    static void correctEntry(FinanceFragment f,Entry e){
        Form form=new Form(f.requireContext());form.textField("title","Descrição",e.title);form.moneyField("value","Valor correto (R$)",Money.input(e.amountCents));form.dateField("date","Data correta (AAAA-MM-DD)",e.date);
        note(form.body,"A correção estorna o registro anterior e cria outro na mesma operação, preservando o histórico. Ciclos fechados não são reescritos.");String key=id();
        form.dialog("Corrigir movimentação","Salvar correção",d->{long amount=value(form,"value");String title=form.value("title"),date=form.value("date");
            f.mutate(state->{boolean complete=e.forecastId!=null && forecast(state,e.forecastId).completed;voidEntry(state,e.id,"Substituído por correção",f.today());record(state,key,e.kind,title,amount,date,e.fromAccountId,e.toAccountId,e.forecastId,complete,f.today());},d);});
    }
    static void rule(FinanceFragment f){
        Form form=new Form(f.requireContext());form.textField("title","Descrição","");form.moneyField("value","Valor previsto (R$)","");form.numberField("day","Dia do vencimento (1–31)","10");form.dateField("start","Primeira data válida (AAAA-MM-DD)",f.today());
        form.dialog("Conta recorrente","Cadastrar",d->{String title=form.value("title"),start=form.value("start");long amount=value(form,"value");int day=integer(form,"day");f.mutate(state->addRule(state,title,amount,day,start,CASH,f.today()),d);});
    }
    static void ruleDetails(FinanceFragment f,String id){
        Rule selected=null;for(Rule r:f.state().rules)if(r.id.equals(id))selected=r;if(selected==null)return;Rule r=selected;
        alert(f,r.title).setMessage(Money.format(r.amountCents)+" todo dia "+r.day+".\nEncerrar a recorrência cancela somente previsões futuras sem pagamento. Contas atrasadas e pagamentos parciais permanecem.")
                .setPositiveButton("Encerrar recorrência",(d,w)->f.mutate(state->stopRule(state,id,f.today()),null)).setNeutralButton("Alterar próximos valores",(d,w)->editRule(f,r)).setNegativeButton("Voltar",null).show();
    }
    static void editRule(FinanceFragment f,Rule old){
        Form form=new Form(f.requireContext());form.moneyField("value","Novo valor previsto (R$)",Money.input(old.amountCents));form.numberField("day","Novo dia de vencimento",String.valueOf(old.day));
        form.dialog("Alterar recorrência","Salvar",d->{long amount=value(form,"value");int day=integer(form,"day");f.mutate(state->{stopRule(state,old.id,f.today());addRule(state,old.title,amount,day,f.today(),old.accountId,f.today());},d);});
    }
    static void account(FinanceFragment f){
        Form form=new Form(f.requireContext());form.textField("name","Nome da conta","");Spinner type=form.select("Tipo",Arrays.asList("Conta de movimentação","Aplicação / investimento"));form.moneyField("value","Saldo existente hoje (R$)","0,00");
        form.dialog("Adicionar conta","Salvar",d->{String name=form.value("name");long opening=value(form,"value");AccountKind kind=type.getSelectedItemPosition()==0?AccountKind.CASH:AccountKind.INVESTMENT;f.mutate(state->addAccount(state,name,kind,opening,f.today()),d);});
    }
    static void accountBalance(FinanceFragment f,String id){
        FinanceState s=f.state();Account a=FinanceEngine.account(s,id);Form form=new Form(f.requireContext());form.moneyField("value","Saldo atual confirmado (R$)",Money.input(balance(s,id,f.today())));
        note(form.body,"Use o valor total atual dessa conta. Uma atualização de valor é registrada separadamente de receitas, gastos e aportes.");
        form.dialog(a.name,"Atualizar saldo",d->{long amount=value(form,"value");f.mutate(state->reconcileBalance(state,id,amount,f.today()),d);});
    }
    static void goal(FinanceFragment f){
        Form form=new Form(f.requireContext());form.textField("title","Nome do objetivo","");form.moneyField("value","Valor da meta (R$)","");form.dateField("date","Prazo (AAAA-MM-DD)",LocalDate.parse(f.today()).plusYears(1).toString());
        form.dialog("Nova meta","Criar",d->{String title=form.value("title"),date=form.value("date");long amount=value(form,"value");f.mutate(state->addGoal(state,title,amount,date,f.today()),d);});
    }
    static void editGoal(FinanceFragment f,String id){
        Goal goal=FinanceEngine.goal(f.state(),id);Form form=new Form(f.requireContext());
        AlertDialog[] editor=new AlertDialog[1];
        form.textField("title","Nome do objetivo",goal.title);form.moneyField("value","Valor da meta (R$)",Money.input(goal.targetCents));form.dateField("date","Prazo (AAAA-MM-DD)",goal.deadline);
        button(form.body,"Excluir meta e liberar reservas",()->alert(f,"Excluir meta?").setMessage("O dinheiro permanece nas contas. As reservas desta meta serão liberadas.")
                .setPositiveButton("Excluir",(a,b)->f.mutate(state->FinanceEngine.removeGoal(state,id),editor[0])).setNegativeButton("Voltar",null).show(),false);
        editor[0]=form.dialog("Editar meta","Salvar",d->{String title=form.value("title"),deadline=form.value("date");long target=value(form,"value");f.mutate(state->FinanceEngine.editGoal(state,id,title,target,deadline),d);});
    }
    static void allocate(FinanceFragment f,String goalId){
        FinanceState s=f.state();Form form=new Form(f.requireContext());Spinner source=form.select("Onde o dinheiro já está?",accountNames(s));form.moneyField("value","Valor a destinar (R$)","");
        note(form.body,"A destinação não transfere dinheiro nem soma patrimônio. Valores na conta deixam de ficar disponíveis para outros gastos.");
        form.dialog("Destinar à meta","Reservar",d->{String account=s.accounts.get(source.getSelectedItemPosition()).id;long amount=value(form,"value");f.mutate(state->FinanceEngine.allocate(state,goalId,account,amount,f.today()),d);});
    }
    static void release(FinanceFragment f,String goalId){
        alert(f,"Liberar reservas?").setMessage("O dinheiro continua nas mesmas contas e aplicações. Somente a destinação a esta meta será removida.").setPositiveButton("Liberar",(d,w)->f.mutate(state->releaseGoal(state,goalId),null)).setNegativeButton("Voltar",null).show();
    }
    static void goalProjection(FinanceFragment f,String goalId){
        FinanceState s=f.state();Goal g=FinanceEngine.goal(s,goalId);int months=(int)Math.max(1,Math.ceil(ChronoUnit.DAYS.between(LocalDate.parse(f.today()),LocalDate.parse(g.deadline))/30.436875));
        long needed=Projection.requiredMonthly(allocated(s,g.id),g.targetCents,s.settings.expectedAnnualReturn,months);
        message(f,g.title,"Considerando "+Money.format(allocated(s,g.id))+" já destinados e "+months+" meses:\n\nAporte estimado de "+Money.format(needed)+" por mês, a "+s.settings.expectedAnnualReturn+"% a.a.\n\nA simulação pressupõe que o valor destinado esteja aplicado. Sem impostos, taxas ou inflação. Retorno não garantido.");
    }
    static void simulation(FinanceFragment f,boolean inverse){
        FinanceState s=f.state();Form form=new Form(f.requireContext());form.moneyField("initial","Valor inicial investido (R$)",Money.input(Math.max(0,summarize(s,f.today()).invested)));
        form.moneyField("rate","Rentabilidade anual esperada (%)",String.valueOf(s.settings.expectedAnnualReturn));form.numberField("months","Prazo personalizado (meses)","60");
        if(inverse)form.moneyField("target","Meta de patrimônio (R$)","100000,00");
        else {form.moneyField("monthly","Aporte mensal — cenário A (R$)",Money.input(plannedContribution(s,s.settings.salaryCents)));form.moneyField("alternative","Aporte mensal — cenário B (R$)","700,00");form.moneyField("rateB","Rentabilidade anual — cenário B (%)",String.valueOf(s.settings.expectedAnnualReturn));}
        note(form.body,"Aportes no fim do mês. Taxa anual efetiva convertida para mensal. Estimativas nominais, sem impostos, taxas ou inflação. Retorno não garantido.");
        form.dialog(inverse?"Aporte necessário":"Comparar cenários","Calcular",d->{
            long initial=value(form,"initial");double annual=rate(form,"rate");int months=integer(form,"months");String result;
            if(inverse){long target=value(form,"target"),needed=Projection.requiredMonthly(initial,target,annual,months);result="Para chegar a "+Money.format(target)+" em "+months+" meses:\n\n"+Money.format(needed)+" por mês.";}
            else {
                long a=value(form,"monthly"),b=value(form,"alternative");double rateB=rate(form,"rateB");Projection pa=Projection.calculate(initial,a,annual,months),pb=Projection.calculate(initial,b,rateB,months);
                result="Em "+months+" meses\n\nCenário A · "+Money.format(a)+"/mês · "+annual+"% a.a.\nTotal: "+Money.format(Math.round(pa.finalValue*100))+"\nCapital aportado: "+Money.format(Math.round(pa.contributed*100))+"\nRendimento estimado: "+Money.format(Math.round(pa.estimatedReturn*100))+"\n\nCenário B · "+Money.format(b)+"/mês · "+rateB+"% a.a.\nTotal: "+Money.format(Math.round(pb.finalValue*100))+"\nCapital aportado: "+Money.format(Math.round(pb.contributed*100))+"\nRendimento estimado: "+Money.format(Math.round(pb.estimatedReturn*100));
            }
            message(f,"Simulação",result+"\n\nEstimativa, não retorno garantido. Nenhum saldo foi alterado.");
        });
    }
    static void close(FinanceFragment f){
        FinanceState s=f.state();Summary x=summarize(s,f.today());Form form=new Form(f.requireContext());
        note(form.body,"Receitas: "+Money.format(x.income)+"\nDespesas: "+Money.format(x.expenses)+"\nAportes realizados: "+Money.format(x.contributions)+"\nSaldo nas contas: "+Money.format(x.cash)+"\nContas pendentes: "+Money.format(x.committed)+"\nAportes pendentes: "+Money.format(x.pendingContributions)+"\nReservas: "+Money.format(x.cashReservations)+"\n\nSobra livre: "+Money.format(x.available));
        Spinner destination=form.select("Destino da sobra",Arrays.asList("Carregar para o próximo ciclo","Reservar para uma meta","Planejar novo aporte","Registrar aporte realizado agora"));
        form.moneyField("value","Valor a destinar (R$)",Money.input(Math.max(0,x.available)));
        List<Account> cashAccounts=new ArrayList<>();List<String> cashNames=new ArrayList<>();for(Account a:s.accounts)if(a.kind==AccountKind.CASH){cashAccounts.add(a);cashNames.add(a.name);}
        Spinner source=form.select("Conta de origem da destinação",cashNames);
        List<String> names=new ArrayList<>();for(Goal g:s.goals)names.add(g.title);if(names.isEmpty())names.add("Cadastre uma meta antes de reservar");Spinner goals=form.select("Meta (se escolher reservar)",names);
        note(form.body,"A parte não destinada é carregada. Contas e aportes pendentes continuam comprometidos. Fechar não cria nova receita.");
        String operation=id();form.dialog("Fechar ciclo","Confirmar fechamento",d->{int choice=destination.getSelectedItemPosition();long amount=value(form,"value");String goalId=s.goals.isEmpty()?null:s.goals.get(goals.getSelectedItemPosition()).id;
            if(choice==1 && goalId==null)throw new IllegalArgumentException("Crie uma meta primeiro.");
            String sourceId=cashAccounts.get(source.getSelectedItemPosition()).id;
            f.mutate(state->finishCycle(state,operation,f.today(),RemainderDestination.values()[choice],amount,goalId,sourceId),d);
        });
    }
    static void cycleDetails(FinanceFragment f,Cycle c){
        message(f,"Ciclo fechado",FinanceScreens.displayDate(c.startDate)+" — "+FinanceScreens.displayDate(c.closedDate)+"\n\nSaldo inicial: "+Money.format(c.openingCash)+"\nReceitas: "+Money.format(c.income)+"\nDespesas: "+Money.format(c.expenses)+"\nAportes: "+Money.format(c.contributions)+"\nResgates: "+Money.format(c.withdrawals)+"\nAjustes de saldo: "+Money.format(c.adjustments)+"\nSaldo final: "+Money.format(c.closingCash)+"\nContas pendentes: "+Money.format(c.committed)+"\nAportes pendentes: "+Money.format(c.pendingContributions)+"\nReservas: "+Money.format(c.cashReservations)+"\nSobra antes da destinação: "+Money.format(c.freeRemainder)+"\nDestinado no fechamento: "+Money.format(c.remainderAllocated)+"\n\n"+c.remainderDestination);
    }
    static void filters(FinanceFragment f){
        Form form=new Form(f.requireContext());List<String> options=Arrays.asList("Tudo","Planejados","Realizados","Receitas","Despesas","Aportes","Estornados","Cancelados");Spinner type=form.select("Mostrar",options);type.setSelection(Math.max(0,options.indexOf(f.filter)));
        form.textField("search","Buscar descrição",f.search);form.dateField("from","A partir de (AAAA-MM-DD, opcional)",f.fromDate);form.dateField("to","Até (AAAA-MM-DD, opcional)",f.toDate);
        form.dialog("Filtrar extrato","Aplicar",d->{String start=form.value("from"),end=form.value("to");if(!start.isEmpty())date(start);if(!end.isEmpty())date(end);if(!start.isEmpty()&&!end.isEmpty()&&start.compareTo(end)>0)throw new IllegalArgumentException("A data inicial deve vir antes da final.");
            f.filter=options.get(type.getSelectedItemPosition());f.search=form.value("search");f.fromDate=start;f.toDate=end;d.dismiss();f.render();});
    }
    static void importLegacy(FinanceFragment f){
        if(BuildConfig.LOCAL_ONLY){message(f,"Cópia local","Esta instalação usa dados separados e não consulta o Firebase original. Para testar a migração, use os dados sintéticos da suíte de testes.");return;}
        String uid=AppSession.uid(f.requireContext());if(uid==null)return;
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("tarefas").get().addOnSuccessListener(snapshot->{
            List<LegacyItem> items=new ArrayList<>();
            try{for(DocumentSnapshot doc:snapshot.getDocuments()){
                String type=doc.getString("tipo");if("tarefa".equalsIgnoreCase(type))continue;
                LegacyItem a=new LegacyItem();a.id="firestore:"+doc.getId();a.title=doc.getString("descricao");a.type=type;a.date=doc.getString("data");
                Object raw=doc.get("valor");a.amountCents=raw==null?0:Money.parse(String.valueOf(raw));items.add(a);
            }}catch(Exception e){if(f.isAdded())f.error(e);return;}
            if(f.isAdded())f.mutate(state->{for(LegacyItem item:items)stageLegacy(state,item);},null);
        }).addOnFailureListener(e->{if(f.isAdded())f.error(e);});
    }
    static void reviewLegacy(FinanceFragment f,String id){
        LegacyItem old=null;for(LegacyItem a:f.state().legacy)if(a.id.equals(id))old=a;if(old==null)return;LegacyItem item=old;
        Form form=new Form(f.requireContext());note(form.body,item.title+" · "+Money.format(item.amountCents)+"\nSe este dinheiro já está refletido no saldo cadastrado, mantenha somente o histórico.");
        Spinner choice=form.select("Como tratar?",Arrays.asList("Somente histórico: já incluído no saldo","Despesa ainda prevista","Receita ainda prevista","Aporte ainda planejado"));
        form.dateField("date","Data prevista (AAAA-MM-DD)",item.date==null?f.today():item.date);
        form.dialog("Revisar registro antigo","Confirmar",d->{int option=choice.getSelectedItemPosition();String due=form.value("date");
            f.mutate(state->{LegacyItem record=null;for(LegacyItem a:state.legacy)if(a.id.equals(id))record=a;if(record==null||record.reviewed)return;
                if(option>0){Kind kind=option==1?Kind.EXPENSE:option==2?Kind.INCOME:Kind.CONTRIBUTION;FinanceEngine.plan(state,"legacy:"+id,kind,item.title,item.amountCents,due,CASH,kind==Kind.CONTRIBUTION?INVEST:null,null);}record.reviewed=true;},d);});
    }
}
