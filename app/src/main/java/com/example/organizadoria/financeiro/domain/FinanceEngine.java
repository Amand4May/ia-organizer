package com.example.organizadoria.financeiro.domain;

import java.time.*;
import java.util.*;
import static com.example.organizadoria.financeiro.domain.FinanceState.*;

/** Regras financeiras sem dependência de Android, Firebase, rede ou relógio global. */
public final class FinanceEngine {
    public static final String CASH = "conta-principal", INVEST = "investimentos";
    private FinanceEngine() {}
    public static String id() { return UUID.randomUUID().toString(); }
    private static void require(boolean ok,String message) { if(!ok) throw new IllegalArgumentException(message); }
    public static LocalDate date(String text) {
        try { return LocalDate.parse(text); } catch(Exception e) { throw new IllegalArgumentException("Informe uma data válida (AAAA-MM-DD)."); }
    }
    public static String payday(YearMonth month,int day) {
        require(day>=1 && day<=31,"Dia de recebimento deve estar entre 1 e 31.");
        return month.atDay(Math.min(day,month.lengthOfMonth())).toString();
    }
    public static String nextPayday(String after,int day) {
        LocalDate d=date(after); String candidate=payday(YearMonth.from(d),day);
        return candidate.compareTo(after)>0 ? candidate : payday(YearMonth.from(d).plusMonths(1),day);
    }
    public static Account account(FinanceState s,String id) {
        for(Account a:s.accounts) if(a.id.equals(id)) return a;
        throw new IllegalArgumentException("Conta não encontrada.");
    }
    public static Forecast forecast(FinanceState s,String id) {
        for(Forecast f:s.forecasts) if(f.id.equals(id)) return f;
        throw new IllegalArgumentException("Previsão não encontrada.");
    }
    public static Goal goal(FinanceState s,String id) {
        for(Goal g:s.goals) if(g.id.equals(id)) return g;
        throw new IllegalArgumentException("Meta não encontrada.");
    }
    public static Cycle activeCycle(FinanceState s) {
        for(Cycle c:s.cycles) if(c.id.equals(s.activeCycleId) && !c.closed) return c;
        return null;
    }
    public static long balance(FinanceState s,String accountId,String onDate) {
        Account a=account(s,accountId);
        long value=a.openingDate.compareTo(onDate)<=0 ? a.openingCents : 0;
        for(Entry e:s.entries) if(!e.voided && e.date.compareTo(onDate)<=0) {
            if(accountId.equals(e.toAccountId)) value=Math.addExact(value,e.amountCents);
            if(accountId.equals(e.fromAccountId)) value=Math.subtractExact(value,e.amountCents);
        }
        return value;
    }
    public static long paid(FinanceState s,Forecast f) {
        long total=0;
        for(Entry e:s.entries) if(!e.voided && f.id.equals(e.forecastId)) total=Math.addExact(total,e.amountCents);
        return total;
    }
    public static long remaining(FinanceState s,Forecast f) {
        return f.cancelled || f.completed ? 0 : Math.max(0,f.amountCents-paid(s,f));
    }
    public static PlanStatus status(FinanceState s,Forecast f) {
        if(f.cancelled) return PlanStatus.CANCELLED;
        if(f.completed || paid(s,f)>=f.amountCents) return PlanStatus.SETTLED;
        return paid(s,f)>0 ? PlanStatus.PARTIAL : PlanStatus.PLANNED;
    }
    public static final class Summary {
        public long cash, invested, committed, pendingContributions, cashReservations, available, assets;
        public long income, expenses, contributions, withdrawals, adjustments;
        public String nextSalary;
    }
    public static Summary summarize(FinanceState s,String today) {
        date(today); Summary x=new Summary(); Cycle cycle=activeCycle(s);
        x.nextSalary=nextPayday(today,s.settings.payDay);
        for(Account a:s.accounts) {
            long b=balance(s,a.id,today);
            if(a.kind==AccountKind.CASH) x.cash=Math.addExact(x.cash,b); else x.invested=Math.addExact(x.invested,b);
            if(cycle!=null && cycle.id.equals(a.openingCycleId) && a.kind==AccountKind.CASH && a.openingDate.compareTo(today)<=0) x.adjustments=Math.addExact(x.adjustments,a.openingCents);
        }
        for(Forecast f:s.forecasts) if(f.date.compareTo(x.nextSalary)<0) {
            long rem=remaining(s,f);
            if(f.kind==Kind.EXPENSE) x.committed=Math.addExact(x.committed,rem);
            if(f.kind==Kind.CONTRIBUTION) x.pendingContributions=Math.addExact(x.pendingContributions,rem);
        }
        for(Allocation a:s.allocations) if(a.date.compareTo(today)<=0 && account(s,a.accountId).kind==AccountKind.CASH)
            x.cashReservations=Math.addExact(x.cashReservations,a.amountCents);
        x.available=Math.subtractExact(Math.subtractExact(Math.subtractExact(x.cash,x.committed),x.pendingContributions),x.cashReservations);
        x.assets=Math.addExact(x.cash,x.invested);
        for(Entry e:s.entries) if(!e.voided && e.date.compareTo(today)<=0 && cycle!=null && cycle.id.equals(e.cycleId)) {
            if(e.kind==Kind.INCOME) x.income=Math.addExact(x.income,e.amountCents);
            if(e.kind==Kind.EXPENSE) x.expenses=Math.addExact(x.expenses,e.amountCents);
            if(e.kind==Kind.CONTRIBUTION) x.contributions=Math.addExact(x.contributions,e.amountCents);
            if(e.kind==Kind.VALUATION && account(s,e.toAccountId).kind==AccountKind.CASH) x.adjustments=Math.addExact(x.adjustments,e.amountCents);
            if(e.kind==Kind.TRANSFER && e.fromAccountId!=null && account(s,e.fromAccountId).kind==AccountKind.INVESTMENT
                    && account(s,e.toAccountId).kind==AccountKind.CASH) x.withdrawals=Math.addExact(x.withdrawals,e.amountCents);
        }
        return x;
    }
    public static void configure(FinanceState s,long cash,long invested,long salary,int day,
                                 boolean percent,long contribution,int bps,double annual,String today) {
        date(today); payday(YearMonth.from(date(today)),day);
        require(invested>=0 && salary>=0 && contribution>=0,"Valores de renda, investimento e aporte devem ser positivos.");
        Money.percent(salary,bps); Projection.calculate(0,0,annual,1);
        boolean first=!s.settings.configured;
        boolean contributionChanged=s.settings.percentContribution!=percent || s.settings.contributionCents!=contribution || s.settings.contributionBasisPoints!=bps;
        boolean salaryChanged=first || s.settings.salaryCents!=salary || s.settings.payDay!=day;
        if(first) {
            s.accounts.add(new Account(CASH,"Conta principal",AccountKind.CASH,cash,today));
            s.accounts.add(new Account(INVEST,"Investimentos",AccountKind.INVESTMENT,invested,today));
            s.settings.validFrom=today; s.settings.cashAccountId=CASH; s.settings.investmentAccountId=INVEST;
        }
        if(!first && !s.settings.balancesConfirmed) {
            reconcileBalance(s,CASH,cash,today);
            reconcileBalance(s,INVEST,invested,today);
        }
        s.settings.configured=true; s.settings.balancesConfirmed=true; s.settings.salaryCents=salary; s.settings.payDay=day;
        s.settings.percentContribution=percent; s.settings.contributionCents=contribution;
        s.settings.contributionBasisPoints=bps; s.settings.expectedAnnualReturn=annual;
        // Cancelar só ocorrências futuras ainda não confirmadas; preservar o histórico recebido.
        for(Forecast f:s.forecasts) if(salaryChanged && "salary".equals(f.ruleId) && f.date.compareTo(today)>=0 && paid(s,f)==0) {
            boolean remainsOnSchedule=f.date.equals(payday(YearMonth.from(date(f.date)),day)) && salary>0;
            f.cancelled=!remainsOnSchedule;f.amountCents=salary;
        }
        if(first) openCycle(s,today,null);
        ensureSchedule(s,today);
        if(contributionChanged) setContribution(s,percent,contribution,bps,today);
    }
    private static void ready(FinanceState s) { require(s.settings.configured,"Configure suas contas no Financeiro primeiro."); }
    public static Account addAccount(FinanceState s,String name,AccountKind kind,long opening,String today) {
        ready(s); require(name!=null && !name.trim().isEmpty(),"Informe o nome da conta.");
        require(kind!=AccountKind.INVESTMENT || opening>=0,"Investimento não pode ser negativo."); date(today);
        Account a=new Account(id(),name.trim(),kind,opening,today); Cycle c=activeCycle(s);a.openingCycleId=c==null?null:c.id;s.accounts.add(a); return a;
    }
    public static void reconcileBalance(FinanceState s,String accountId,long desired,String today) {
        ready(s);date(today);Account a=account(s,accountId);
        require(a.kind!=AccountKind.INVESTMENT || desired>=0,"Investimento não pode ser negativo.");
        long reserved=0;for(Allocation allocation:s.allocations)if(allocation.accountId.equals(accountId))reserved=Math.addExact(reserved,allocation.amountCents);
        require(desired>=reserved || reserved==0,"Reavalie as reservas das metas antes de reduzir o saldo abaixo do valor destinado.");
        long delta=Math.subtractExact(desired,balance(s,accountId,today));
        if(delta==0) return;
        Entry e=new Entry(); e.id=id(); e.kind=Kind.VALUATION; e.title="Atualização de saldo · "+a.name;
        e.date=today; e.toAccountId=accountId; e.amountCents=delta;
        Cycle c=activeCycle(s); e.cycleId=c==null?null:c.id; s.entries.add(e);
    }
    public static long plannedContribution(FinanceState s,long salary) {
        return s.settings.percentContribution ? Money.percent(salary,s.settings.contributionBasisPoints) : s.settings.contributionCents;
    }
    public static void setContribution(FinanceState s,boolean percent,long amount,int basisPoints,String today) {
        ready(s);date(today);require(amount>=0,"Aporte não pode ser negativo.");Money.percent(0,basisPoints);
        s.settings.percentContribution=percent;s.settings.contributionCents=amount;s.settings.contributionBasisPoints=basisPoints;
        Cycle current=activeCycle(s);if(current==null)return;
        for(Forecast salary:new ArrayList<>(s.forecasts))if("salary".equals(salary.ruleId)&&paid(s,salary)>0) {
            boolean inCycle=false;
            for(Entry e:s.entries)if(!e.voided&&salary.id.equals(e.forecastId)&&current.id.equals(e.cycleId))inCycle=true;
            if(!inCycle)continue;
            long target=plannedContribution(s,paid(s,salary));String key="salary-contribution:"+salary.id;
            Forecast existing=null;for(Forecast f:s.forecasts)if(f.id.equals(key))existing=f;
            if(existing==null && target>0)existing=plan(s,key,Kind.CONTRIBUTION,"Aporte do ciclo",target,today,CASH,INVEST,null);
            if(existing!=null) {
                existing.amountCents=Math.max(target,paid(s,existing));existing.completed=paid(s,existing)>=existing.amountCents;
                existing.cancelled=false;existing.cancelledBySalaryReversal=false;existing.settledBelowPlan=false;
            }
        }
    }
    public static Forecast plan(FinanceState s,String stableId,Kind kind,String title,long cents,String due,
                                String accountId,String targetId,String agendaId) {
        ready(s); date(due); require(cents>0,"Informe um valor maior que zero.");
        require(kind==Kind.INCOME || kind==Kind.EXPENSE || kind==Kind.CONTRIBUTION,"Tipo de previsão inválido.");
        require(title!=null && !title.trim().isEmpty(),"Informe uma descrição.");
        account(s,accountId);
        if(kind!=Kind.CONTRIBUTION)require(account(s,accountId).kind==AccountKind.CASH,"Receitas e despesas usam contas de movimentação.");
        if(kind==Kind.CONTRIBUTION) {
            require(account(s,accountId).kind==AccountKind.CASH && account(s,targetId).kind==AccountKind.INVESTMENT,
                    "Aporte precisa sair de uma conta para um investimento.");
        }
        for(Forecast f:s.forecasts) if(f.id.equals(stableId)) return f;
        Forecast f=new Forecast(); f.id=stableId; f.kind=kind; f.title=title.trim(); f.amountCents=cents; f.date=due;
        f.accountId=accountId; f.targetAccountId=targetId; f.agendaId=agendaId;
        Cycle c=activeCycle(s); f.cycleId=c==null?null:c.id; s.forecasts.add(f); return f;
    }
    public static void editForecast(FinanceState s,String id,String title,long amount,String due) {
        Forecast f=forecast(s,id); date(due);
        require(!f.cancelled && !f.completed,"Previsão já encerrada.");
        require(amount>0 && amount>=paid(s,f),"Previsão deve cobrir o valor já pago.");
        require(title!=null && !title.trim().isEmpty(),"Informe a descrição.");
        f.title=title.trim(); f.amountCents=amount; f.date=due;
    }
    public static void cancelForecast(FinanceState s,String id,String today) {
        Forecast f=forecast(s,id); require(!f.completed,"Uma previsão liquidada não pode ser cancelada.");
        f.cancelled=true; f.cancelledBySalaryReversal=false; f.closedDate=today;
    }
    public static void ensureSchedule(FinanceState s,String today) {
        if(!s.settings.configured) return;
        LocalDate end=date(today).plusMonths(12); LocalDate first=date(s.settings.validFrom);
        // Previsões do salário não são receita realizada.
        for(YearMonth m=YearMonth.from(first); !m.isAfter(YearMonth.from(end)); m=m.plusMonths(1)) {
            String d=payday(m,s.settings.payDay);
            if(d.compareTo(s.settings.validFrom)<0 || s.settings.salaryCents<=0) continue;
            String key="salary:"+d; Forecast found=null;
            for(Forecast f:s.forecasts) if(f.id.equals(key)) {found=f;break;}
            if(found==null) {
                Forecast f=plan(s,key,Kind.INCOME,"Salário",s.settings.salaryCents,d,s.settings.cashAccountId,null,null);
                f.ruleId="salary";
            }
        }
        for(Rule rule:s.rules) if(rule.active) {
            for(YearMonth m=YearMonth.from(date(rule.startDate)); !m.isAfter(YearMonth.from(end)); m=m.plusMonths(1)) {
                String d=payday(m,rule.day);
                if(d.compareTo(rule.startDate)<0 || (rule.endDate!=null && d.compareTo(rule.endDate)>0)) continue;
                Forecast f=plan(s,"rule:"+rule.id+":"+d,Kind.EXPENSE,rule.title,rule.amountCents,d,rule.accountId,null,null);
                f.ruleId=rule.id;
            }
        }
        s.settings.lastScheduledOn=today;
    }
    public static Rule addRule(FinanceState s,String title,long amount,int day,String start,String accountId,String today) {
        ready(s); date(start); payday(YearMonth.from(date(start)),day);
        require(account(s,accountId).kind==AccountKind.CASH,"Contas recorrentes usam uma conta de movimentação.");
        require(start.compareTo(s.settings.validFrom)>=0,"A recorrência deve começar a partir do início do seu financeiro. Cadastre dívidas anteriores como previsões avulsas.");
        require(amount>0 && title!=null && !title.trim().isEmpty(),"Informe descrição e valor da conta.");
        Rule r=new Rule(); r.id=id(); r.title=title.trim(); r.amountCents=amount; r.day=day; r.startDate=start; r.accountId=accountId;
        s.rules.add(r); ensureSchedule(s,today); return r;
    }
    public static void stopRule(FinanceState s,String id,String today) {
        for(Rule r:s.rules) if(r.id.equals(id)) { r.active=false; r.endDate=today; }
        for(Forecast f:s.forecasts) if(id.equals(f.ruleId) && f.date.compareTo(today)>=0 && paid(s,f)==0) f.cancelled=true;
    }
    private static Cycle openCycle(FinanceState s,String today,String salaryId) {
        Cycle c=new Cycle(); c.id=id(); c.startDate=today; c.nextPayDate=nextPayday(today,s.settings.payDay);
        c.salaryEntryId=salaryId;
        c.openingCash=summarize(s,today).cash;
        s.cycles.add(c); s.activeCycleId=c.id; return c;
    }
    private static void editableDate(FinanceState s,String effective,String today) {
        require(date(effective).compareTo(date(today))<=0,"Uma operação futura deve ser planejada.");
        require(effective.compareTo(s.settings.validFrom)>=0,"Use uma data a partir do saldo inicial cadastrado.");
        for(Cycle c:s.cycles) if(c.closed && effective.compareTo(c.startDate)>=0 && effective.compareTo(c.closedDate)<=0) {
            Cycle active=activeCycle(s);
            require((active!=null && effective.compareTo(active.startDate)>=0)
                    || (active==null && effective.equals(today) && effective.equals(c.closedDate)),
                    "Esse período está fechado. Registre um ajuste no ciclo atual.");
        }
    }
    public static Entry record(FinanceState s,String stableId,Kind kind,String title,long cents,String effective,
                               String from,String to,String forecastId,boolean finalPayment,String today) {
        ready(s); for(Entry e:s.entries) if(e.id.equals(stableId)) return e;
        editableDate(s,effective,today); require(cents>0,"Informe um valor maior que zero.");
        require(title!=null && !title.trim().isEmpty(),"Informe a descrição.");
        require(kind!=Kind.VALUATION,"Use atualizar saldo para registrar uma valorização.");
        if(kind==Kind.INCOME) { require(from==null,"Receita não tem conta de saída.");require(account(s,to).kind==AccountKind.CASH,"Receitas entram em contas de movimentação. Use atualização de saldo para rendimentos aplicados."); }
        else if(kind==Kind.EXPENSE) { require(account(s,from).kind==AccountKind.CASH,"Registre o resgate para uma conta antes de pagar a despesa.");require(to==null,"Despesa não tem conta de destino."); }
        else {
            Account src=account(s,from), dst=account(s,to); require(!from.equals(to),"Escolha contas diferentes.");
            if(kind==Kind.CONTRIBUTION) require(src.kind==AccountKind.CASH && dst.kind==AccountKind.INVESTMENT,"Aporte precisa ter um investimento como destino.");
            if(kind==Kind.TRANSFER && src.kind==AccountKind.CASH && dst.kind==AccountKind.INVESTMENT)kind=Kind.CONTRIBUTION;
            if(src.kind==AccountKind.INVESTMENT) require(balance(s,from,effective)>=cents,"Resgate maior que o saldo investido.");
        }
        if(from!=null) {
            require(account(s,from).openingDate.compareTo(effective)<=0,"A data é anterior ao saldo inicial da conta de saída.");
            long reserved=0;for(Allocation a:s.allocations)if(a.accountId.equals(from))reserved=Math.addExact(reserved,a.amountCents);
            require(reserved==0 || cents<=balance(s,from,effective)-reserved,"Libere a reserva da meta antes de movimentar esse dinheiro.");
        }
        if(to!=null)require(account(s,to).openingDate.compareTo(effective)<=0,"A data é anterior ao saldo inicial da conta de entrada.");
        Forecast f=null;
        if(forecastId!=null) {
            f=forecast(s,forecastId); require(!f.completed && !f.cancelled,"Essa previsão já foi encerrada.");
            require(f.kind==kind,"Movimentação incompatível com a previsão.");
            require(Objects.equals(f.accountId,kind==Kind.INCOME?to:from),"A conta deve corresponder à previsão.");
            if(kind==Kind.CONTRIBUTION)require(Objects.equals(f.targetAccountId,to),"O investimento de destino deve corresponder à previsão.");
        }
        Cycle c=activeCycle(s);
        if(f!=null && "salary".equals(f.ruleId)) {
            if(c!=null && c.salaryEntryId!=null && paid(s,f)==0) {
                require(effective.compareTo(c.startDate)>=0,"O salário não pode iniciar um ciclo antes do atual.");
                for(Entry existing:s.entries)if(!existing.voided && c.id.equals(existing.cycleId))require(existing.date.compareTo(effective)<=0,"Há movimentações posteriores a esse recebimento. Revise o ciclo antes de registrar um salário retroativo.");
                closeCycle(s,today,"Carregado para o próximo ciclo"); c=null;
            }
            if(c==null) c=openCycle(s,effective,stableId);
            else if(c.salaryEntryId==null) {
                boolean hasEntries=false;for(Entry existing:s.entries)if(c.id.equals(existing.cycleId)&&!existing.voided)hasEntries=true;
                for(Account existing:s.accounts)if(c.id.equals(existing.openingCycleId))hasEntries=true;
                if(!hasEntries){c.startDate=effective;c.nextPayDate=nextPayday(effective,s.settings.payDay);c.openingCash=summarize(s,effective).cash;}
                c.salaryEntryId=stableId;
            }
        }
        if(c==null) c=openCycle(s,effective,null);
        Entry e=new Entry(); e.id=stableId; e.kind=kind; e.title=title.trim(); e.amountCents=cents; e.date=effective;
        e.fromAccountId=from; e.toAccountId=to; e.forecastId=forecastId; e.cycleId=c.id; s.entries.add(e);
        if(f!=null) {
            f.completed=finalPayment || paid(s,f)>=f.amountCents;f.settledBelowPlan=finalPayment && paid(s,f)<f.amountCents;
            if(f.completed) f.closedDate=effective;
            if("salary".equals(f.ruleId)) {
                long contribution=plannedContribution(s,paid(s,f));
                if(contribution>0) {
                    String key="salary-contribution:"+f.id; Forecast contributionPlan=null;
                    for(Forecast existing:s.forecasts) if(existing.id.equals(key)) contributionPlan=existing;
                    if(contributionPlan==null) plan(s,key,Kind.CONTRIBUTION,"Aporte do ciclo",contribution,effective,
                            s.settings.cashAccountId,s.settings.investmentAccountId,null);
                    else {
                        if(contributionPlan.cancelledBySalaryReversal) {
                            contributionPlan.cancelled=false;contributionPlan.cancelledBySalaryReversal=false;
                        }
                        if(!contributionPlan.cancelled && (!contributionPlan.completed || !contributionPlan.settledBelowPlan)) {
                            contributionPlan.amountCents=Math.max(contribution,paid(s,contributionPlan));
                            contributionPlan.completed=paid(s,contributionPlan)>=contributionPlan.amountCents;
                            if(!contributionPlan.completed)contributionPlan.closedDate=null;
                        }
                    }
                }
            }
        }
        return e;
    }
    public static Entry settle(FinanceState s,String id,String forecastId,long amount,boolean complete,String today) {
        Forecast f=forecast(s,forecastId);
        return record(s,id,f.kind,f.title,amount,today,f.kind==Kind.INCOME?null:f.accountId,
                f.kind==Kind.INCOME?f.accountId:f.targetAccountId,f.id,complete,today);
    }
    public static void voidEntry(FinanceState s,String id,String reason,String today) {
        require(reason!=null && !reason.trim().isEmpty(),"Informe o motivo do estorno.");
        for(Entry e:s.entries) if(e.id.equals(id)) {
            for(Cycle c:s.cycles) if(c.id.equals(e.cycleId)) require(!c.closed,"Ciclo fechado. Registre um ajuste no ciclo atual.");
            require(!e.voided,"Essa movimentação já foi estornada.");
            e.voided=true; e.voidReason=reason.trim();
            if(e.forecastId!=null) {
                Forecast f=forecast(s,e.forecastId); f.completed=false;f.settledBelowPlan=false; f.closedDate=null;
                if("salary".equals(f.ruleId)) {
                    for(Forecast a:s.forecasts) if(a.id.equals("salary-contribution:"+f.id)) {
                        a.amountCents=Math.max(plannedContribution(s,paid(s,f)),paid(s,a));
                        if(paid(s,f)==0 && !a.cancelled) {a.cancelled=true;a.cancelledBySalaryReversal=true;}
                    }
                    Cycle c=activeCycle(s); if(c!=null && paid(s,f)==0) c.salaryEntryId=null;
                }
            }
            return;
        }
        throw new IllegalArgumentException("Movimentação não encontrada.");
    }
    public static Cycle closeCycle(FinanceState s,String today,String destination) {
        Cycle c=activeCycle(s); require(c!=null,"Nenhum ciclo aberto.");
        require(today.compareTo(c.startDate)>=0,"Data de fechamento inválida.");
        Summary x=summarize(s,today); c.income=x.income; c.expenses=x.expenses; c.contributions=x.contributions;
        c.withdrawals=x.withdrawals; c.adjustments=x.adjustments;c.closingCash=x.cash; c.freeRemainder=x.available;
        c.committed=x.committed;c.pendingContributions=x.pendingContributions;c.cashReservations=x.cashReservations;
        c.closed=true; c.closedDate=today; c.remainderDestination=destination; s.activeCycleId=null; return c;
    }
    public static Cycle finishCycle(FinanceState s,String operation,String today,RemainderDestination destination,long amount,String goalId,String accountId) {
        for(Cycle c:s.cycles)if(operation.equals(c.closeOperationId))return c;
        require(activeCycle(s)!=null,"Nenhum ciclo aberto.");Summary before=summarize(s,today);
        require(destination!=null,"Escolha o destino da sobra.");
        String description="Carregado para o próximo ciclo";
        if(destination!=RemainderDestination.CARRY) {
            require(amount>0 && amount<=before.available,"A destinação deve caber na sobra livre.");
            require(account(s,accountId).kind==AccountKind.CASH,"Escolha uma conta de movimentação.");
            require(balance(s,accountId,today)>=amount,"A conta escolhida não tem saldo suficiente.");
            switch(destination) {
                case GOAL:
                    allocate(s,goalId,accountId,amount,today);description=Money.format(amount)+" destinados a "+goal(s,goalId).title;break;
                case PLAN_CONTRIBUTION:
                    plan(s,operation+":plan",Kind.CONTRIBUTION,"Aporte da sobra do ciclo",amount,today,accountId,INVEST,null);
                    description=Money.format(amount)+" separados para aporte";break;
                case CONTRIBUTION:
                    record(s,operation+":invest",Kind.CONTRIBUTION,"Aporte da sobra do ciclo",amount,today,accountId,INVEST,null,true,today);
                    description=Money.format(amount)+" investidos no fechamento";break;
                default:break;
            }
        }
        Cycle closed=closeCycle(s,today,description);closed.freeRemainder=before.available;closed.closeOperationId=operation;
        closed.remainderAllocated=destination==RemainderDestination.CARRY?0:amount;return closed;
    }
    public static Goal addGoal(FinanceState s,String title,long target,String deadline,String today) {
        ready(s); require(target>0 && title!=null && !title.trim().isEmpty(),"Informe nome e valor da meta.");
        require(date(deadline).compareTo(date(today))>=0,"O prazo da meta deve ser hoje ou no futuro.");
        Goal g=new Goal(); g.id=id(); g.title=title.trim(); g.targetCents=target; g.deadline=deadline; s.goals.add(g); return g;
    }
    public static void editGoal(FinanceState s,String id,String title,long target,String deadline) {
        Goal g=goal(s,id);date(deadline);require(target>0 && title!=null && !title.trim().isEmpty(),"Informe nome e valor da meta.");
        g.title=title.trim();g.targetCents=target;g.deadline=deadline;
    }
    public static void removeGoal(FinanceState s,String id) {
        releaseGoal(s,id);s.goals.removeIf(g->g.id.equals(id));
    }
    public static long allocated(FinanceState s,String goalId) {
        long total=0; for(Allocation a:s.allocations) if(a.goalId.equals(goalId)) total=Math.addExact(total,a.amountCents); return total;
    }
    public static void allocate(FinanceState s,String goalId,String accountId,long amount,String today) {
        goal(s,goalId); Account ac=account(s,accountId); require(amount>0,"Informe um valor maior que zero.");
        long reserved=0; for(Allocation a:s.allocations) if(a.accountId.equals(accountId)) reserved=Math.addExact(reserved,a.amountCents);
        require(amount<=balance(s,accountId,today)-reserved,"Essa conta não tem dinheiro livre suficiente para a reserva.");
        if(ac.kind==AccountKind.CASH) require(amount<=summarize(s,today).available,"Essa reserva usaria dinheiro já comprometido.");
        Allocation a=new Allocation(); a.id=id(); a.goalId=goalId; a.accountId=accountId; a.amountCents=amount; a.date=today; s.allocations.add(a);
    }
    public static void releaseGoal(FinanceState s,String goalId) {
        goal(s,goalId); s.allocations.removeIf(a->a.goalId.equals(goalId));
    }
    public static List<Forecast> pending(FinanceState s) {
        List<Forecast> list=new ArrayList<>(); for(Forecast f:s.forecasts) if(remaining(s,f)>0) list.add(f);
        list.sort(Comparator.comparing((Forecast f)->f.date).thenComparing(f->f.title)); return list;
    }
    /** Projeção de caixa inclui receitas esperadas; nunca é usada como saldo existente. */
    public static SortedMap<String,Long> cashForecast(FinanceState s,String today,int days) {
        require(days>=1 && days<=366,"Use um horizonte de 1 a 366 dias.");
        TreeMap<String,Long> result=new TreeMap<>(), changes=new TreeMap<>(); String end=date(today).plusDays(days).toString();
        long cash=summarize(s,today).cash;
        for(Forecast f:pending(s)) if(f.date.compareTo(end)<=0) {
            String d=f.date.compareTo(today)<0?today:f.date;
            long amount=remaining(s,f)*(f.kind==Kind.INCOME?1:-1);
            changes.put(d,Math.addExact(changes.getOrDefault(d,0L),amount));
            if("salary".equals(f.ruleId)) {
                Forecast contribution=null;
                for(Forecast other:s.forecasts) if(other.id.equals("salary-contribution:"+f.id)) contribution=other;
                long target=plannedContribution(s,Math.addExact(paid(s,f),remaining(s,f))),extra=0;
                if(contribution==null)extra=target;
                else if(!contribution.cancelled && (!contribution.completed || !contribution.settledBelowPlan))extra=Math.max(0,target-contribution.amountCents);
                changes.put(d,Math.subtractExact(changes.get(d),extra));
            }
        }
        result.put(today,cash);
        for(Map.Entry<String,Long> e:changes.entrySet()) { cash=Math.addExact(cash,e.getValue()); result.put(e.getKey(),cash); }
        result.put(end,cash); return result;
    }
    public static AgendaItem addAgenda(FinanceState s,String stableId,String title,String date,String time) {
        date(date); try { LocalTime.parse(time); } catch(Exception e) { throw new IllegalArgumentException("Use um horário como 09:00."); }
        require(title!=null && !title.trim().isEmpty(),"Informe o compromisso.");
        for(AgendaItem a:s.agenda) if(a.id.equals(stableId)) return a;
        AgendaItem a=new AgendaItem(); a.id=stableId; a.title=title.trim(); a.date=date; a.time=time; s.agenda.add(a); return a;
    }
    public static void cancelAgenda(FinanceState s,String id,boolean cancelExpense,String today) {
        for(AgendaItem a:s.agenda) if(a.id.equals(id)) {
            a.cancelled=true;
            if(cancelExpense && a.forecastId!=null) {
                Forecast f=forecast(s,a.forecastId); if(!f.completed) cancelForecast(s,f.id,today);
            }
        }
    }
    public static void stageLegacy(FinanceState s,LegacyItem item) {
        if(s.importedLegacyIds.contains(item.id)) return;
        require(item.id!=null && item.amountCents>=0,"Registro antigo inválido.");
        s.importedLegacyIds.add(item.id); s.legacy.add(item);
    }
    /** Valida o resultado inteiro da transação, inclusive estorno + substituição na mesma gravação. */
    public static void validateState(FinanceState s) {
        require(s.schemaVersion==1 && s.userId!=null && s.settings!=null,"Estado financeiro incompatível.");
        if(!s.settings.configured){require(s.accounts.isEmpty() && s.entries.isEmpty() && s.allocations.isEmpty(),"Configure as contas antes das movimentações.");return;}
        String asOf=s.settings.validFrom;date(asOf);
        if(s.settings.lastScheduledOn!=null && s.settings.lastScheduledOn.compareTo(asOf)>0)asOf=s.settings.lastScheduledOn;
        Set<String> accountIds=new HashSet<>(),entryIds=new HashSet<>();
        for(Account a:s.accounts) {
            if(a.yieldIndex!=null) InvestmentProjection.profile(a,a.yieldIndex,InvestmentProjection.CDI.equals(a.yieldIndex)?a.yieldPercent:a.expectedAnnualRate);
            require(a.id!=null && accountIds.add(a.id) && a.kind!=null,"Conta inválida ou duplicada.");date(a.openingDate);
            if(a.openingDate.compareTo(asOf)>0)asOf=a.openingDate;
        }
        for(Entry e:s.entries) {
            require(e.id!=null && entryIds.add(e.id) && e.kind!=null,"Movimentação inválida ou duplicada.");date(e.date);
            require(e.kind==Kind.VALUATION || e.amountCents>0,"Movimentação precisa ter valor positivo.");
            if(e.fromAccountId!=null)account(s,e.fromAccountId);if(e.toAccountId!=null)account(s,e.toAccountId);
            if(e.date.compareTo(asOf)>0)asOf=e.date;
        }
        for(Account a:s.accounts) {
            long reserved=0;
            for(Allocation allocation:s.allocations) {
                goal(s,allocation.goalId);account(s,allocation.accountId);require(allocation.amountCents>0,"Reserva inválida.");
                if(a.id.equals(allocation.accountId))reserved=Math.addExact(reserved,allocation.amountCents);
            }
            long current=balance(s,a.id,asOf);
            require(a.kind!=AccountKind.INVESTMENT || current>=0,"O estorno deixaria o investimento negativo. Revise os resgates posteriores.");
            require(reserved==0 || current>=reserved,"O estorno usaria valores destinados a metas. Libere ou ajuste essas reservas primeiro.");
        }
        try{summarize(s,asOf);}catch(ArithmeticException e){throw new IllegalArgumentException("A soma dos valores ultrapassa o limite suportado.");}
    }
}
