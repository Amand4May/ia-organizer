package com.example.organizadoria.financeiro.ui;

import android.widget.*;
import com.example.organizadoria.financeiro.domain.*;
import static com.example.organizadoria.financeiro.domain.FinanceState.*;
import static com.example.organizadoria.financeiro.domain.FinanceEngine.*;
import static com.example.organizadoria.financeiro.ui.FinanceUi.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class FinanceScreens {
    private FinanceScreens(){}
    static String displayDate(String d){try{return LocalDate.parse(d).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}catch(Exception e){return d==null?"—":d;}}
    static String kindLabel(Kind k){switch(k){case INCOME:return "Receita";case EXPENSE:return "Despesa";case CONTRIBUTION:return "Aporte";case TRANSFER:return "Transferência";default:return "Atualização de saldo";}}
    static int color(Kind k){return k==Kind.INCOME?GREEN:k==Kind.EXPENSE?ORANGE:BLUE;}
    static String statusLabel(FinanceState s,Forecast f){switch(status(s,f)){case PARTIAL:return "Parcial";case SETTLED:return "Realizado";case CANCELLED:return "Cancelado";default:return "Planejado";}}
    static void render(FinanceFragment f,LinearLayout p,FinanceState s,String page){
        if(page.equals("setup")){FinanceOnboarding.render(f,p,s);return;}
        if(!s.settings.configured){f.title("Financeiro");eyebrow(p,"MAIS CLAREZA, MENOS PLANILHAS");LinearLayout c=hero(p);add(c,text(p.getContext(),"Seu dinheiro,\ncom destino.",30,WHITE,true),14);note(c,"Veja o que pode gastar hoje e construa o que quer realizar amanhã.");pill(c,"Saldo → Compromissos → Disponível",CYAN);button(c,"Configurar meu financeiro",()->f.open("setup"),true);heading(p,"Um começo simples");note(p,"1. Seu saldo atual\n2. Seu dia de recebimento\n3. Quanto quer investir");note(p,"Depois, conte ao Cogni o que aconteceu. Você confere e confirma antes de salvar.");return;}
        switch(page){
            case "assistant":f.title("Conte ao Cogni");f.assistant=new FinanceAssistant(f,p);break;
            case "cycle":cycle(f,p,s);break;case "investments":investments(f,p,s);break;case "goals":goals(f,p,s);break;
            case "statement":statement(f,p,s);break;case "planning":planning(f,p,s);break;case "settings":settings(f,p,s);break;
            case "accounts":accounts(f,p,s);break;case "legacy":legacy(f,p,s);break;default:overview(f,p,s);
        }
    }
    static void overview(FinanceFragment f,LinearLayout p,FinanceState s){
        f.title("Financeiro");Summary x=summarize(s,f.today());
        if(!s.settings.balancesConfirmed){note(p,"Falta confirmar os saldos das suas contas para calcular o disponível com precisão.");button(p,"Confirmar meus saldos",()->f.open("settings"),true);}
        eyebrow(p,"SEU DINHEIRO, COM CLAREZA");LinearLayout hero=hero(p);add(hero,text(p.getContext(),"DISPONÍVEL ATÉ O PRÓXIMO SALÁRIO",11,CYAN,true),12);
        add(hero,text(p.getContext(),Money.format(x.available),34,x.available<0?RED:WHITE,true),8);
        long days=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(f.today()),LocalDate.parse(x.nextSalary));note(hero,x.available<0?"Os compromissos superam seu saldo. Revise o planejamento.":"Livre para usar nos próximos "+days+" dias.");add(hero,new FinanceDistribution(p.getContext(),x.available,x.committed,x.pendingContributions,x.cashReservations),12);
        note(hero,Money.format(x.committed)+" em contas · "+Money.format(x.pendingContributions)+" em aportes pendentes"+(x.cashReservations>0?"\n"+Money.format(x.cashReservations)+" em reservas":""));
        add(hero,text(p.getContext(),"Próximo recebimento previsto: "+displayDate(x.nextSalary),12,MUTED,false),0);
        gridMetrics(p,new String[]{"Saldo nas contas","Comprometido","Investido","Patrimônio"},
                new String[]{Money.format(x.cash),Money.format(x.committed),Money.format(x.invested),Money.format(x.assets)},new int[]{WHITE,ORANGE,BLUE,GREEN});
        LinearLayout composer=card(p);add(composer,text(p.getContext(),"O que aconteceu com seu dinheiro?",17,WHITE,true),6);note(composer,"“Gastei R$40 no corte” ou “Tenho R$8.000 a 100% do CDI”.");button(composer,"Conte ao Cogni",()->f.open("assistant"),true);
        heading(p,"Seu espaço financeiro");navigation(p,new String[]{"Ciclo","Investimentos","Metas","Extrato","Planejamento","Contas"},new String[]{"Até o próximo salário","CDI e projeções","Um passo de cada vez","Planejado e realizado","O que vem pela frente","Seu patrimônio"},new String[]{"↻","↗","◎","≡","◷","▣"},new int[]{CYAN,BLUE,GREEN,WHITE,ORANGE,CYAN},new Runnable[]{()->f.open("cycle"),()->f.open("investments"),()->f.open("goals"),()->f.open("statement"),()->f.open("planning"),()->f.open("accounts")});
        section(p,"O que vem agora","Ver tudo",()->f.open("planning"));int count=0;
        for(Forecast a:pending(s))if(a.date.compareTo(x.nextSalary)<0){planRow(f,p,s,a);if(++count==3)break;}
        if(count==0)note(p,"Nenhum compromisso financeiro previsto para este período.");
        button(p,"Registrar movimentação",()->FinanceForms.movement(f,null),true);
        if(s.legacy.stream().anyMatch(a->!a.reviewed))button(p,"Revisar registros antigos",()->f.open("legacy"),false);
    }
    static void cycle(FinanceFragment f,LinearLayout p,FinanceState s){
        f.title("Ciclo financeiro");Cycle c=activeCycle(s);Summary x=summarize(s,f.today());
        if(c!=null){
            note(p,displayDate(c.startDate)+" até o próximo recebimento previsto, em "+displayDate(x.nextSalary));
            if(c.salaryEntryId==null)note(p,"O recebimento principal ainda não foi confirmado neste ciclo.");
            metric(p,"Disponível no ciclo",Money.format(x.available),x.available<0?RED:WHITE);
            gridMetrics(p,new String[]{"Receitas realizadas","Gastos realizados","Aportes realizados","Ainda separado"},new String[]{Money.format(x.income),Money.format(x.expenses),Money.format(x.contributions),Money.format(x.committed+x.pendingContributions+x.cashReservations)},new int[]{GREEN,ORANGE,BLUE,CYAN});add(p,new FinanceDistribution(p.getContext(),x.available,x.committed,x.pendingContributions,x.cashReservations),12);note(p,"Azul claro: disponível · Laranja: contas · Azul: aportes pendentes · Verde: metas");
        }else note(p,"Ciclo fechado. O dinheiro e os compromissos permanecem registrados. O próximo recebimento inicia um novo ciclo.");
        button(p,"Confirmar recebimento do salário",()->FinanceForms.salary(f),true);
        button(p,"Ajustar salário e plano de aporte",()->f.open("settings"),false);
        if(c!=null)button(p,"Fechar ciclo e destinar a sobra",()->FinanceForms.close(f),false);
        heading(p,"Histórico de ciclos");
        List<Cycle> history=new ArrayList<>(s.cycles);Collections.reverse(history);int shown=0;
        for(Cycle old:history)if(old.closed){
            row(p,displayDate(old.startDate)+" — "+displayDate(old.closedDate),old.remainderDestination,
                    Money.format(old.freeRemainder),old.freeRemainder<0?RED:GREEN,()->FinanceForms.cycleDetails(f,old));shown++;
        }
        if(shown==0)note(p,"O primeiro fechamento aparecerá aqui.");
    }
    static void investments(FinanceFragment f,LinearLayout p,FinanceState s){FinanceInvestments.render(f,p,s);}
    static void goals(FinanceFragment f,LinearLayout p,FinanceState s){
        f.title("Metas");note(p,"Destine dinheiro que já existe nas suas contas ou aplicações. Reservas não aumentam o patrimônio.");
        button(p,"Criar meta",()->FinanceForms.goal(f),true);
        if(s.goals.isEmpty())note(p,"Qual é o próximo objetivo que você quer realizar?");
        for(Goal g:s.goals){
            long progress=allocated(s,g.id);LinearLayout box=card(p);add(box,text(p.getContext(),g.title,18,WHITE,true),6);
            note(box,Money.format(progress)+" de "+Money.format(g.targetCents)+" · até "+displayDate(g.deadline));
            ProgressBar bar=new ProgressBar(p.getContext(),null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress((int)Math.min(100,progress*100.0/g.targetCents));add(box,bar,16);bar.getLayoutParams().height=dp(p.getContext(),8);bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(BORDER));
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));button(box,"Destinar dinheiro",()->FinanceForms.allocate(f,g.id),true);button(box,"Gerenciar meta",()->FinanceForms.alert(f,g.title).setItems(new String[]{"Simular o que falta","Editar meta","Liberar reservas"},(d,i)->{if(i==0)FinanceForms.goalProjection(f,g.id);else if(i==1)FinanceForms.editGoal(f,g.id);else FinanceForms.release(f,g.id);}).show(),false);
        }
    }
    static void planRow(FinanceFragment f,LinearLayout p,FinanceState s,Forecast a){
        String detail=displayDate(a.date)+" · "+statusLabel(s,a);
        if(remaining(s,a)>0 && a.date.compareTo(f.today())<0)detail+=" · atrasado";
        row(p,a.title,detail,Money.format(a.cancelled?a.amountCents:remaining(s,a)),a.cancelled?MUTED:color(a.kind),()->FinanceForms.forecastDetails(f,a.id));
    }
    static void entryRow(FinanceFragment f,LinearLayout p,Entry e){
        row(p,e.title,displayDate(e.date)+" · "+kindLabel(e.kind)+(e.voided?" · estornado":" · realizado"),Money.format(e.amountCents),e.voided?MUTED:color(e.kind),()->FinanceForms.entryDetails(f,e.id));
    }
    static boolean matches(FinanceFragment f,String title,String date){
        return CommandRouter.normalize(title).contains(CommandRouter.normalize(f.search)) && (f.fromDate.isEmpty() || date.compareTo(f.fromDate)>=0) && (f.toDate.isEmpty() || date.compareTo(f.toDate)<=0);
    }
    static void statement(FinanceFragment f,LinearLayout p,FinanceState s){
        f.title("Extrato");note(p,"Filtro: "+f.filter+(f.search.isEmpty()?"":" · "+f.search)+(f.fromDate.isEmpty()?"":" · desde "+displayDate(f.fromDate))+(f.toDate.isEmpty()?"":" · até "+displayDate(f.toDate)));
        button(p,"Buscar e filtrar",()->FinanceForms.filters(f),false);button(p,"Nova movimentação",()->FinanceForms.movement(f,null),true);
        String[] shortcuts={"Tudo","Planejados","Realizados","Aportes"};segments(p,shortcuts,Arrays.asList(shortcuts).indexOf(f.filter),i->{f.filter=shortcuts[i];f.render();});
        List<Object> rows=new ArrayList<>();
        for(Forecast a:s.forecasts)if(matches(f,a.title,a.date) && ((f.filter.equals("Cancelados") && a.cancelled) || ((f.filter.equals("Planejados") || f.filter.equals("Tudo")) && remaining(s,a)>0)))rows.add(a);
        for(Entry a:s.entries)if(matches(f,a.title,a.date)){
            boolean include=f.filter.equals("Estornados")?a.voided:!a.voided && (f.filter.equals("Tudo") || f.filter.equals("Realizados") ||
                    f.filter.equals("Receitas")&&a.kind==Kind.INCOME || f.filter.equals("Despesas")&&a.kind==Kind.EXPENSE || f.filter.equals("Aportes")&&a.kind==Kind.CONTRIBUTION);
            if(include)rows.add(a);
        }
        rows.sort((a,b)->{String da=a instanceof Entry?((Entry)a).date:((Forecast)a).date,db=b instanceof Entry?((Entry)b).date:((Forecast)b).date;return db.compareTo(da);});
        note(p,rows.size()+" registros encontrados");for(Object a:rows){if(a instanceof Entry)entryRow(f,p,(Entry)a);else planRow(f,p,s,(Forecast)a);}
        if(rows.isEmpty())note(p,"Nenhum registro neste filtro.");
    }
    static void planning(FinanceFragment f,LinearLayout p,FinanceState s){
        f.title("Planejamento");eyebrow(p,"UM OLHAR ADIANTE");button(p,"Planejar com uma frase",()->f.open("assistant"),true);button(p,"Planejar despesa ou aporte",()->FinanceForms.plan(f,null),false);
        button(p,"Cadastrar conta recorrente",()->FinanceForms.rule(f),false);
        heading(p,"Próximos 90 dias");
        note(p,"Projeção de caixa: considera recebimentos esperados, despesas pendentes e aportes previstos. Contas atrasadas entram hoje. O resultado não é dinheiro já disponível.");
        SortedMap<String,Long> forecast=cashForecast(s,f.today(),90);double[] points=new double[91];long last=summarize(s,f.today()).cash;double minimum=Double.POSITIVE_INFINITY;for(int day=0;day<=90;day++){String date=LocalDate.parse(f.today()).plusDays(day).toString();if(forecast.containsKey(date))last=forecast.get(date);points[day]=last/100.0;minimum=Math.min(minimum,points[day]);}
        LinearLayout chart=card(p);TextView selected=text(p.getContext(),"Em 90 dias: "+Money.format(Math.round(points[90]*100)),18,CYAN,true);add(chart,selected,6);add(chart,new FinanceChart(p.getContext(),points,null,null,"dias",day->selected.setText(displayDate(LocalDate.parse(f.today()).plusDays(day).toString())+" · "+Money.format(Math.round(points[day]*100)))),12);if(minimum<0)pill(chart,"Atenção: caixa projetado negativo no período",ORANGE);button(chart,"Ver datas e valores",()->{Form detail=new Form(f.requireContext());for(Map.Entry<String,Long> point:forecast.entrySet())row(detail.body,displayDate(point.getKey()),"Caixa após os eventos do dia",Money.format(point.getValue()),point.getValue()<0?RED:WHITE,null);detail.dialog("Próximos 90 dias","Fechar",d->d.dismiss());},false);
        heading(p,"Contas recorrentes");int active=0;
        for(Rule rule:s.rules)if(rule.active){row(p,rule.title,"Todo dia "+rule.day,Money.format(rule.amountCents),ORANGE,()->FinanceForms.ruleDetails(f,rule.id));active++;}
        if(active==0)note(p,"Nenhuma conta recorrente cadastrada.");
        heading(p,"Previsões pendentes");String end=LocalDate.parse(f.today()).plusDays(90).toString();
        for(Forecast a:pending(s))if(a.date.compareTo(end)<=0)planRow(f,p,s,a);
    }
    static void settings(FinanceFragment f,LinearLayout p,FinanceState s){
        f.title("Ajustes do Financeiro");eyebrow(p,"UMA COISA DE CADA VEZ");note(p,"Ajuste apenas o que precisar.");
        if(!s.settings.balancesConfirmed)button(p,"Confirmar saldos iniciais",()->f.open("setup"),true);
        row(p,"Salário e ciclo","Recebimento previsto no dia "+s.settings.payDay,Money.format(s.settings.salaryCents),GREEN,()->FinanceOnboarding.salary(f));
        row(p,"Plano de aporte",s.settings.percentContribution?Money.input(s.settings.contributionBasisPoints)+"% do salário":"Valor mensal separado ao receber",s.settings.percentContribution?"›":Money.format(s.settings.contributionCents),BLUE,()->FinanceForms.contribution(f));
        row(p,"Contas e saldos","Atualize o que você já tem","›",CYAN,()->f.open("accounts"));row(p,"Investimentos","Indexador, taxas e projeções","›",BLUE,()->f.open("investments"));row(p,"Histórico anterior","Conferir antes de importar","›",MUTED,()->f.open("legacy"));
    }
    static void accounts(FinanceFragment f,LinearLayout p,FinanceState s){
        f.title("Contas e patrimônio");note(p,"Transferências entre suas contas preservam o patrimônio. Atualizar saldo registra um ajuste, sem inventar receita ou despesa.");
        for(Account a:s.accounts)row(p,a.name,a.kind==AccountKind.CASH?"Conta de movimentação":"Aplicação financeira",Money.format(balance(s,a.id,f.today())),a.kind==AccountKind.CASH?WHITE:BLUE,()->FinanceForms.accountBalance(f,a.id));
        button(p,"Adicionar conta ou aplicação",()->FinanceForms.account(f),true);
        button(p,"Transferir entre contas",()->FinanceForms.movement(f,Kind.TRANSFER),false);
    }
    static void legacy(FinanceFragment f,LinearLayout p,FinanceState s){
        f.title("Revisar histórico");note(p,"Os registros antigos não distinguem planejamento de pagamento. Revise cada item. Valores já incluídos no saldo inicial devem ficar somente como histórico, para evitar dupla contagem.");
        String profile=FinanceForms.previousProfile(f);if(!profile.isEmpty())note(p,profile);
        button(p,"Buscar registros da versão anterior",()->FinanceForms.importLegacy(f),false);
        int count=0;for(LegacyItem old:s.legacy)if(!old.reviewed){row(p,old.title,displayDate(old.date)+" · revisão pendente",Money.format(old.amountCents),MUTED,()->FinanceForms.reviewLegacy(f,old.id));count++;}
        if(count==0)note(p,"Nenhum registro aguardando revisão.");
        if(s.legacy.stream().anyMatch(a->a.reviewed)){heading(p,"Histórico revisado");for(LegacyItem old:s.legacy)if(old.reviewed)row(p,old.title,displayDate(old.date)+" · registro anterior preservado",Money.format(old.amountCents),MUTED,null);}
    }
}
