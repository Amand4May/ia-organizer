package com.example.organizadoria.financeiro.domain;

import java.text.Normalizer;
import java.util.*;
import static com.example.organizadoria.financeiro.domain.FinanceState.*;
import static com.example.organizadoria.financeiro.domain.FinanceEngine.*;
import static com.example.organizadoria.financeiro.domain.OrganizerCommand.Action.*;

/** Ponte Agenda/Financeiro: IDs estáveis, conciliação de previsões e operações atômicas no repositório. */
public final class CommandRouter {
    public static final class AmbiguousMatch extends IllegalArgumentException {
        public final List<String> forecastIds;
        public final int commandIndex;
        AmbiguousMatch(List<String> ids,int index) { super("Qual previsão você quer confirmar?"); forecastIds=ids;commandIndex=index; }
    }
    private CommandRouter() {}
    public static String normalize(String text) {
        return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
    }
    private static String category(String title) {
        String n=normalize(title);
        if(n.matches(".*(cabelo|corte|barbeiro|barbearia).*")) return "cabelo";
        if(n.matches(".*(dentista|odontolog).*")) return "dentista";
        return n.replaceAll("\\b(gastei|paguei|com|de|do|da|no|na|o|a|um|uma|reais|r)\\b", " ").replaceAll("[^a-z ]", "").trim().replaceAll(" +"," ");
    }
    private static Forecast match(FinanceState s,OrganizerCommand c,Kind kind,int index) {
        if(c.targetId!=null) return forecast(s,c.targetId);
        List<Forecast> candidates=new ArrayList<>();
        for(Forecast f:pending(s)) if(f.kind==kind && f.date.compareTo(date(c.date).plusDays(14).toString())<=0) {
            if(kind==Kind.CONTRIBUTION || (c.action==SALARY && "salary".equals(f.ruleId)) ||
                    (!category(c.title).isEmpty() && category(c.title).equals(category(f.title)))) candidates.add(f);
        }
        if(candidates.size()>1) {
            List<String> ids=new ArrayList<>();for(Forecast f:candidates) ids.add(f.id);throw new AmbiguousMatch(ids,index);
        }
        return candidates.isEmpty()?null:candidates.get(0);
    }
    public static List<String> apply(FinanceState s,List<OrganizerCommand> commands,String operationId,String today) {
        if(commands==null || commands.isEmpty() || commands.size()>20) throw new IllegalArgumentException("Comando vazio ou com itens demais.");
        if(s.appliedCommands.contains(operationId)) return Collections.singletonList("Esse comando já foi registrado.");
        List<String> messages=new ArrayList<>();
        boolean needsMoney=commands.stream().anyMatch(c->c.action!=TASK || c.amount>0);
        if(needsMoney && !s.settings.configured) {
            configure(s,0,0,0,5,false,0,0,8,today);s.settings.balancesConfirmed=false;
        }
        for(int i=0;i<commands.size();i++) {
            OrganizerCommand c=commands.get(i);String key=operationId+":"+i;
            if(c.action==null) throw new IllegalArgumentException("Ação não reconhecida.");
            if(c.date==null) c.date=today; date(c.date);
            if(c.amount<0) throw new IllegalArgumentException("Use valores positivos.");
            switch(c.action) {
                case TASK: {
                    AgendaItem task=addAgenda(s,key,c.title,c.date,c.time);
                    if(c.amount>0) {
                        Forecast f=plan(s,key+":expense",Kind.EXPENSE,c.title,c.amount,c.date,CASH,null,task.id);task.forecastId=f.id;
                    }
                    messages.add("Compromisso agendado"+(c.amount>0?" e "+Money.format(c.amount)+" planejados.":"."));break;
                }
                case PLAN_EXPENSE:case PLAN_CONTRIBUTION: {
                    boolean invest=c.action==PLAN_CONTRIBUTION;
                    plan(s,key,invest?Kind.CONTRIBUTION:Kind.EXPENSE,c.title,c.amount,c.date,CASH,invest?INVEST:null,null);
                    messages.add("Previsão de "+Money.format(c.amount)+" registrada.");break;
                }
                case EXPENSE:case INCOME:case SALARY:case CONTRIBUTION: {
                    Kind kind=c.action==EXPENSE?Kind.EXPENSE:c.action==CONTRIBUTION?Kind.CONTRIBUTION:Kind.INCOME;
                    Forecast f=match(s,c,kind,i);
                    if(c.action==SALARY && f==null) {
                        f=plan(s,"salary:"+c.date,Kind.INCOME,"Salário",c.amount,c.date,CASH,null,null);f.ruleId="salary";
                    }
                    boolean complete=!c.partial && kind!=Kind.CONTRIBUTION;
                    if(f!=null) record(s,key,kind,c.title.isEmpty()?f.title:c.title,c.amount,c.date,
                            kind==Kind.INCOME?null:f.accountId,kind==Kind.INCOME?f.accountId:f.targetAccountId,f.id,complete,today);
                    else record(s,key,kind,c.title,c.amount,c.date,kind==Kind.INCOME?null:CASH,
                            kind==Kind.INCOME?CASH:kind==Kind.CONTRIBUTION?INVEST:null,null,complete,today);
                    messages.add(f!=null?"Previsão conciliada: "+Money.format(c.amount)+" realizados.":Money.format(c.amount)+" registrados.");break;
                }
                case WITHDRAWAL:
                    record(s,key,Kind.TRANSFER,"Resgate",c.amount,c.date,INVEST,CASH,null,true,today);
                    messages.add("Resgate registrado, mantendo o patrimônio.");break;
                case CONFIG_SALARY: {
                    boolean confirmed=s.settings.balancesConfirmed;
                    configure(s,balance(s,CASH,today),balance(s,INVEST,today),c.amount,c.day,s.settings.percentContribution,s.settings.contributionCents,
                            s.settings.contributionBasisPoints,s.settings.expectedAnnualReturn,today);
                    s.settings.balancesConfirmed=confirmed;
                    messages.add("Salário previsto para o dia "+c.day+". Confirme quando receber.");break;
                }
                case CONFIG_CONTRIBUTION:
                    setContribution(s,c.percent,c.amount,c.basisPoints,today);
                    messages.add("Plano de aporte atualizado para o ciclo atual e os próximos recebimentos.");break;
                case RECURRING_BILL:
                    addRule(s,c.title,c.amount,c.day,c.date,CASH,today);messages.add("Conta recorrente cadastrada.");break;
                case SET_INVESTED:
                    reconcileBalance(s,INVEST,c.amount,today);messages.add("Posição investida atualizada.");break;
                case CONFIG_RETURN:
                    Projection.calculate(0,0,c.annualRate,1);s.settings.expectedAnnualReturn=c.annualRate;InvestmentProjection.profile(account(s,INVEST),InvestmentProjection.FIXED,c.annualRate);
                    messages.add("Rentabilidade esperada salva apenas para simulações.");break;
                case GOAL:
                    addGoal(s,c.title,c.amount,c.date,today);messages.add("Meta cadastrada.");break;
                case SIMULATE:case REQUIRED_CONTRIBUTION: {
                    
                    if(c.action==REQUIRED_CONTRIBUTION) messages.add("Aporte estimado: "+Money.format(InvestmentProjection.requiredPortfolioMonthly(s,c.amount,c.annualRate,c.months,null,today))+"/mês em "+c.months+" meses.");
                    else {
                        long monthly=c.amountSpecified || c.amount>0?c.amount:plannedContribution(s,s.settings.salaryCents);
                        double[] values=InvestmentProjection.portfolio(s,monthly,c.annualRate,c.months,null,today);
                        messages.add("Em "+c.months+" meses: "+Money.format(Math.round(values[c.months]*100))+" com "+Money.format(monthly)+"/mês.");
                    }
                    messages.add((Double.isNaN(c.annualRate)?"Estimativa com as taxas cadastradas por aplicação":"Estimativa a "+c.annualRate+"% a.a.")+", com aportes na aplicação principal ao fim do mês, sem impostos, taxas ou inflação. Retorno não garantido.");break;
                }
            }
        }
        s.appliedCommands.add(operationId);return messages;
    }
}
