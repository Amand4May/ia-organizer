import com.example.organizadoria.financeiro.domain.*;
import com.example.organizadoria.financeiro.data.*;
import static com.example.organizadoria.financeiro.domain.FinanceState.*;
import static com.example.organizadoria.financeiro.domain.FinanceEngine.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Testes de comportamento sobre as mesmas classes Java usadas no Android. Sem cópias das fórmulas. */
public class FinanceTestSuite {
    static int checks;
    static final String TODAY="2026-09-05";
    static void eq(long wanted,long got) { checks++; if(wanted!=got) throw new AssertionError("Esperado "+wanted+", obtido "+got); }
    static void ok(boolean value) { checks++; if(!value) throw new AssertionError("Condição não atendida"); }
    static void near(double wanted,double got) { checks++; if(Math.abs(wanted-got)>.011) throw new AssertionError(wanted+" != "+got); }
    static void rejects(Runnable action) { checks++; try{action.run();}catch(IllegalArgumentException|IllegalStateException e){return;} throw new AssertionError("Deveria rejeitar"); }
    static FinanceState fresh(long cash) {
        FinanceState s=FinanceState.empty("alice"); configure(s,cash,0,300000,5,false,50000,0,8,TODAY); return s;
    }
    static Forecast salary(FinanceState s) { return forecast(s,"salary:"+TODAY); }
    public static void main(String[] args)throws Exception {
        money(); scenarios(); salaryCycle(); forecasts(); investments(); goals(); recurrence(); repository();
        commands(); commandTransactions(); corrections(); conservation();
        closingDestinations(); scheduleChanges(); reversalIntegrity(); concurrentPersistence();
        cdiProjection(); investmentLanguage(); investmentConfirmation(); portfolioProjection();
        System.out.println("PASS: "+checks+" verificações de domínio e persistência Java.");
    }
    static void money() {
        eq(300000,Money.parse("R$ 3.000,00")); eq(15050,Money.parse("150.50")); eq(50000,Money.parse("500"));
        eq(-250,Money.parse("-2,50")); eq(150000,Money.parse("1.500"));
        rejects(()->Money.parse("abc")); rejects(()->Money.parse("12,345")); rejects(()->Money.parse(""));
        eq(3333,Money.percent(10000,3333)); rejects(()->Money.percent(100,10001));
    }
    static void scenarios() {
        FinanceState s=fresh(0); settle(s,"sal1",salary(s).id,300000,true,TODAY);
        plan(s,"bills",Kind.EXPENSE,"Contas",80000,"2026-09-20",CASH,null,null);
        Summary x=summarize(s,TODAY); eq(300000,x.cash);eq(80000,x.committed);eq(50000,x.pendingContributions);eq(170000,x.available);
        String aporte="salary-contribution:"+salary(s).id;
        settle(s,"a350",aporte,35000,false,TODAY); x=summarize(s,TODAY);
        eq(265000,x.cash);eq(35000,x.invested);eq(15000,x.pendingContributions);eq(170000,x.available);eq(300000,x.assets);
        settle(s,"a150",aporte,15000,true,TODAY);eq(170000,summarize(s,TODAY).available);eq(0,summarize(s,TODAY).pendingContributions);
        long assets=summarize(s,TODAY).assets;
        record(s,"resgate",Kind.TRANSFER,"Resgate",10000,TODAY,INVEST,CASH,null,true,TODAY);
        eq(assets,summarize(s,TODAY).assets);eq(40000,summarize(s,TODAY).invested);
        rejects(()->record(s,"badresgate",Kind.TRANSFER,"Resgate",90000,TODAY,INVEST,CASH,null,true,TODAY));
        FinanceState low=fresh(10000);plan(low,"x",Kind.EXPENSE,"Conta",15000,TODAY,CASH,null,null);eq(-5000,summarize(low,TODAY).available);
    }
    static void salaryCycle() {
        FinanceState s=fresh(0);eq(0,summarize(s,TODAY).cash);eq(0,summarize(s,TODAY).pendingContributions);
        settle(s,"salary",salary(s).id,300000,true,TODAY);settle(s,"salary",salary(s).id,300000,true,TODAY);eq(300000,summarize(s,TODAY).cash);
        rejects(()->settle(s,"duplicate",salary(s).id,300000,true,TODAY));
        String c=activeCycle(s).id;
        record(s,"extra",Kind.INCOME,"Extra",50000,TODAY,null,CASH,null,true,TODAY);ok(c.equals(activeCycle(s).id));
        plan(s,"expense",Kind.EXPENSE,"Gastos",210000,TODAY,CASH,null,null);settle(s,"paid", "expense",210000,true,TODAY);
        settle(s,"aporte","salary-contribution:"+salary(s).id,50000,true,TODAY);
        Goal g=addGoal(s,"Reserva",100000,"2027-01-01",TODAY);allocate(s,g.id,CASH,30000,TODAY);
        Cycle closed=closeCycle(s,TODAY,"Carregado");eq(350000,closed.income);eq(210000,closed.expenses);eq(50000,closed.contributions);
        eq(90000,closed.closingCash);eq(60000,closed.freeRemainder);eq(90000,summarize(s,TODAY).cash);
        rejects(()->closeCycle(s,TODAY,"Novamente"));rejects(()->voidEntry(s,"paid","Correção",TODAY));
        settle(s,"nextsalary","salary:2026-10-05",300000,true,"2026-10-05");eq(390000,summarize(s,"2026-10-05").cash);
        eq(300000,summarize(s,"2026-10-05").income);eq(90000,activeCycle(s).openingCash);
        FinanceState percent=fresh(0);configure(percent,0,0,300000,5,true,0,1000,8,TODAY);
        settle(percent,"p1",salary(percent).id,200000,false,TODAY);eq(20000,summarize(percent,TODAY).pendingContributions);
        settle(percent,"p2",salary(percent).id,100000,true,TODAY);eq(30000,summarize(percent,TODAY).pendingContributions);
        voidEntry(percent,"p2","Correção",TODAY);eq(20000,summarize(percent,TODAY).pendingContributions);
        FinanceState paidEarly=fresh(0);configure(paidEarly,0,0,300000,5,true,0,1000,8,TODAY);
        settle(paidEarly,"firstPart",salary(paidEarly).id,200000,false,TODAY);
        settle(paidEarly,"firstContribution","salary-contribution:"+salary(paidEarly).id,20000,true,TODAY);
        eq(270000,cashForecast(paidEarly,TODAY,1).get(TODAY));
        settle(paidEarly,"lastPart",salary(paidEarly).id,100000,true,TODAY);
        eq(10000,summarize(paidEarly,TODAY).pendingContributions);eq(270000,summarize(paidEarly,TODAY).available);
    }
    static void forecasts() {
        FinanceState s=fresh(10000);plan(s,"cut",Kind.EXPENSE,"Corte",4500,"2026-09-06",CASH,null,null);
        eq(10000,summarize(s,TODAY).cash);eq(5500,summarize(s,TODAY).available);
        settle(s,"cutpaid","cut",4000,true,"2026-09-06");eq(6000,summarize(s,"2026-09-06").available);eq(0,remaining(s,forecast(s,"cut")));
        voidEntry(s,"cutpaid","Valor incorreto","2026-09-06");eq(10000,summarize(s,"2026-09-06").cash);eq(4500,remaining(s,forecast(s,"cut")));
        settle(s,"cutpart","cut",2000,false,"2026-09-06");eq(2500,remaining(s,forecast(s,"cut")));ok(status(s,forecast(s,"cut"))==PlanStatus.PARTIAL);
        cancelForecast(s,"cut","2026-09-06");eq(8000,summarize(s,"2026-09-06").available);eq(2000,summarize(s,"2026-09-06").expenses);
        rejects(()->settle(s,"bad","cut",1000,true,"2026-09-06"));
        plan(s,"nextcycle",Kind.EXPENSE,"Conta futura",30000,"2026-10-10",CASH,null,null);eq(0,summarize(s,"2026-09-06").committed);
        plan(s,"late",Kind.EXPENSE,"Conta atrasada",5000,TODAY,CASH,null,null);eq(35000,summarize(s,"2026-10-06").committed);
        rejects(()->record(s,"future",Kind.EXPENSE,"Futuro",100,"2026-09-10",CASH,null,null,true,TODAY));
        AgendaItem a=addAgenda(s,"event","Corte","2026-09-08","09:00");
        Forecast f=plan(s,"linked",Kind.EXPENSE,"Corte",4500,"2026-09-08",CASH,null,a.id);a.forecastId=f.id;
        cancelAgenda(s,a.id,true,TODAY);ok(a.cancelled);ok(f.cancelled);
    }
    static void investments() {
        near(14856.94,Projection.calculate(800000,50000,8,12).finalValue);
        near(48226.95,Projection.calculate(800000,50000,8,60).finalValue);
        near(107333.54,Projection.calculate(800000,50000,8,120).finalValue);
        near(62815.88,Projection.calculate(800000,70000,8,60).finalValue);
        near(14000,Projection.calculate(800000,50000,0,12).finalValue);
        ok(Projection.calculate(800000,0,-10,12).finalValue<8000);
        long needed=Projection.requiredMonthly(800000,10000000,8,60);eq(120976,needed);
        ok(Projection.calculate(800000,needed,8,60).finalValue>=100000);
        eq(0,Projection.requiredMonthly(10000000,5000000,8,60));
        rejects(()->Projection.calculate(0,500,8,0));rejects(()->Projection.calculate(0,500,-100,60));
        rejects(()->Projection.calculate(0,500,Double.NaN,60));rejects(()->Projection.calculate(-1,0,8,12));
        FinanceState s=fresh(50000);record(s,"aporte",Kind.CONTRIBUTION,"Aporte",10000,TODAY,CASH,INVEST,null,true,TODAY);
        reconcileBalance(s,INVEST,11000,TODAY);eq(51000,summarize(s,TODAY).assets);eq(10000,summarize(s,TODAY).contributions);
        eq(0,summarize(s,TODAY).expenses);
    }
    static void goals() {
        FinanceState s=fresh(100000);Goal g=addGoal(s,"Viagem",200000,"2027-02-01",TODAY);
        allocate(s,g.id,CASH,10000,TODAY);eq(90000,summarize(s,TODAY).available);eq(100000,summarize(s,TODAY).assets);
        record(s,"inv",Kind.CONTRIBUTION,"Aplicar",20000,TODAY,CASH,INVEST,null,true,TODAY);
        allocate(s,g.id,INVEST,20000,TODAY);eq(70000,summarize(s,TODAY).available);eq(30000,allocated(s,g.id));
        rejects(()->allocate(s,g.id,INVEST,1,TODAY));rejects(()->allocate(s,g.id,CASH,90000,TODAY));
        releaseGoal(s,g.id);eq(80000,summarize(s,TODAY).available);eq(100000,summarize(s,TODAY).assets);
    }
    static void recurrence() {
        ok(nextPayday("2026-01-31",31).equals("2026-02-28"));ok(nextPayday("2028-01-31",31).equals("2028-02-29"));
        ok(nextPayday("2026-12-31",31).equals("2027-01-31"));
        FinanceState s=fresh(100000);Rule r=addRule(s,"Internet",10000,10,TODAY,CASH,TODAY);
        int count=s.forecasts.size();ensureSchedule(s,TODAY);eq(count,s.forecasts.size());eq(10000,summarize(s,TODAY).committed);
        stopRule(s,r.id,TODAY);eq(0,summarize(s,TODAY).committed);ensureSchedule(s,TODAY);eq(count,s.forecasts.size());
        SortedMap<String,Long> points=cashForecast(s,TODAY,35);ok(points.size()>=2);
        eq(100000,summarize(s,TODAY).cash); // Projeção não altera dinheiro existente.
        LegacyItem old=new LegacyItem();old.id="old:1";old.amountCents=4500;old.title="Corte";stageLegacy(s,old);stageLegacy(s,old);eq(1,s.legacy.size());
    }
    static void repository()throws Exception {
        File path=Files.createTempDirectory("cogni-test").resolve("alice.bin").toFile();
        FileFinanceRepository repo=new FileFinanceRepository(path,"alice");
        repo.update(s->configure(s,100000,0,0,5,false,0,0,8,TODAY));long revision=repo.snapshot().revision;
        rejects(()->repo.update(s->{record(s,"a",Kind.EXPENSE,"Compra",10000,TODAY,CASH,null,null,true,TODAY);throw new IllegalArgumentException("Falha posterior");}));
        eq(100000,summarize(repo.snapshot(),TODAY).cash);eq(revision,repo.snapshot().revision);
        FinanceState copy=repo.snapshot();copy.accounts.clear();eq(2,repo.snapshot().accounts.size());
        FileFinanceRepository reopened=new FileFinanceRepository(path,"alice");eq(100000,summarize(reopened.snapshot(),TODAY).cash);
        rejects(()->new FileFinanceRepository(path,"bob"));
        repo.update(s->record(s,"purchase",Kind.EXPENSE,"Compra",10000,TODAY,CASH,null,null,true,TODAY));
        repo.update(s->record(s,"purchase",Kind.EXPENSE,"Compra",10000,TODAY,CASH,null,null,true,TODAY));eq(90000,summarize(repo.snapshot(),TODAY).cash);
        rejects(()->repo.update(s->addAccount(s,"Valor excessivo",AccountKind.CASH,Long.MAX_VALUE,TODAY)));eq(90000,summarize(repo.snapshot(),TODAY).cash);
        Files.write(path.toPath(),new byte[]{1,2,3});rejects(()->new FileFinanceRepository(path,"alice"));eq(3,path.length());
        Files.delete(path.toPath());Files.delete(path.getParentFile().toPath());
    }
    static void command(FinanceState s,String text,String operation,String today) {
        CommandRouter.apply(s,LocalCommandParser.parse(text,today),operation,today);
    }
    static void commands() {
        FinanceState s=FinanceState.empty("alice");
        command(s,"Todo dia 5 recebo R$3.000 e quero investir R$500","setup",TODAY);
        eq(300000,s.settings.salaryCents);eq(50000,s.settings.contributionCents);eq(0,summarize(s,TODAY).cash);ok(!s.settings.balancesConfirmed);
        command(s,"Recebi R$3.000 de salário","received",TODAY);eq(300000,summarize(s,TODAY).cash);eq(50000,summarize(s,TODAY).pendingContributions);
        command(s,"Amanhã vou cortar o cabelo às 9h por 45 reais","hair",TODAY);
        eq(1,s.agenda.size());ok(s.agenda.get(0).time.equals("09:00"));ok(s.agenda.get(0).date.equals("2026-09-06"));
        eq(4500,summarize(s,TODAY).committed);eq(245500,summarize(s,TODAY).available);
        command(s,"Gastei R$40 no corte","hairPaid","2026-09-06");eq(0,summarize(s,"2026-09-06").committed);eq(246000,summarize(s,"2026-09-06").available);
        command(s,"Gastei R$40 no corte","hairPaid","2026-09-06");eq(296000,summarize(s,"2026-09-06").cash);
        command(s,"Investi R$350","partial","2026-09-06");eq(15000,summarize(s,"2026-09-06").pendingContributions);eq(246000,summarize(s,"2026-09-06").available);
        command(s,"Todo mês quero investir R$700","raise","2026-09-06");eq(35000,summarize(s,"2026-09-06").pendingContributions);
        command(s,"Todo mês quero investir 10% do salário","percentage","2026-09-06");eq(1000,s.settings.contributionBasisPoints);eq(0,summarize(s,"2026-09-06").pendingContributions);
        command(s,"Todo mês quero investir R$500","restore","2026-09-06");eq(15000,summarize(s,"2026-09-06").pendingContributions);
        long assets=summarize(s,"2026-09-06").assets;int ledger=s.entries.size();
        command(s,"E se eu investir R$700 por mês?","whatIf","2026-09-06");
        command(s,"Quanto terei em 5 anos?","future","2026-09-06");
        command(s,"Quanto preciso investir mensalmente para chegar a R$100 mil em 5 anos?","inverse","2026-09-06");
        eq(assets,summarize(s,"2026-09-06").assets);eq(ledger,s.entries.size());
        configure(s,261000,35000,300000,5,false,50000,0,8,"2026-09-06");
        eq(261000,summarize(s,"2026-09-06").cash);eq(35000,summarize(s,"2026-09-06").invested);ok(s.settings.balancesConfirmed);
        FinanceState investment=FinanceState.empty("alice");command(investment,"Tenho R$8.000 investidos, aporto R$500/mês e espero retorno médio de 8% a.a.","invested",TODAY);
        eq(800000,summarize(investment,TODAY).invested);eq(50000,investment.settings.contributionCents);near(8,investment.settings.expectedAnnualReturn);
        rejects(()->LocalCommandParser.parse("nonsense",TODAY));
        FinanceState noBudget=fresh(0);configure(noBudget,0,0,300000,5,false,0,0,8,TODAY);settle(noBudget,"s1",salary(noBudget).id,300000,true,TODAY);
        command(noBudget,"Todo mês quero investir R$500","newplan",TODAY);eq(50000,summarize(noBudget,TODAY).pendingContributions);
    }
    static void commandTransactions()throws Exception {
        File path=Files.createTempDirectory("cogni-command-test").resolve("alice.bin").toFile();FileFinanceRepository repo=new FileFinanceRepository(path,"alice");
        OrganizerCommand task=new OrganizerCommand(OrganizerCommand.Action.TASK);task.title="Dentista";task.amount=20000;task.date=TODAY;
        OrganizerCommand invalid=new OrganizerCommand(OrganizerCommand.Action.EXPENSE);invalid.amount=-100;invalid.title="Inválido";invalid.date=TODAY;
        rejects(()->repo.update(s->CommandRouter.apply(s,Arrays.asList(task,invalid),"compound",TODAY)));
        eq(0,repo.snapshot().agenda.size());eq(0,repo.snapshot().entries.size());eq(0,repo.snapshot().forecasts.size());
        repo.update(s->{configure(s,100000,0,0,5,false,0,0,8,TODAY);plan(s,"one",Kind.EXPENSE,"Corte de cabelo",4500,TODAY,CASH,null,null);plan(s,"two",Kind.EXPENSE,"Corte de cabelo",5000,"2026-09-10",CASH,null,null);});
        List<OrganizerCommand> commands=LocalCommandParser.parse("Gastei R$40 no corte",TODAY);
        checks++;try{repo.update(s->CommandRouter.apply(s,commands,"ambiguous",TODAY));throw new AssertionError("Deveria pedir escolha");}catch(CommandRouter.AmbiguousMatch e){eq(2,e.forecastIds.size());commands.get(e.commandIndex).targetId="one";}
        eq(0,repo.snapshot().entries.size());repo.update(s->CommandRouter.apply(s,commands,"ambiguous",TODAY));
        eq(1,repo.snapshot().entries.size());eq(5000,summarize(repo.snapshot(),TODAY).committed);
        repo.addListener(()->{throw new IllegalStateException("Falha da interface");});
        repo.update(s->record(s,"another",Kind.EXPENSE,"Almoço",2000,TODAY,CASH,null,null,true,TODAY));eq(94000,summarize(repo.snapshot(),TODAY).cash);
        Files.delete(path.toPath());Files.delete(path.getParentFile().toPath());
    }
    static void corrections() {
        FinanceState s=fresh(0);settle(s,"salary1",salary(s).id,300000,true,TODAY);
        voidEntry(s,"salary1","Corrigir valor",TODAY);eq(0,summarize(s,TODAY).cash);eq(0,summarize(s,TODAY).pendingContributions);
        settle(s,"salaryCorrection",salary(s).id,320000,true,TODAY);eq(320000,summarize(s,TODAY).cash);eq(50000,summarize(s,TODAY).pendingContributions);
        Goal g=addGoal(s,"Meta",100000,"2027-01-01",TODAY);allocate(s,g.id,CASH,20000,TODAY);
        rejects(()->record(s,"tooMuch",Kind.EXPENSE,"Compra",310000,TODAY,CASH,null,null,true,TODAY));
        rejects(()->reconcileBalance(s,CASH,10000,TODAY));
        Account extra=addAccount(s,"Outra conta",AccountKind.CASH,10000,TODAY);Summary x=summarize(s,TODAY);
        eq(x.cash,activeCycle(s).openingCash+x.income-x.expenses-x.contributions+x.withdrawals+x.adjustments);
        record(s,"transfer",Kind.TRANSFER,"Transferência",5000,TODAY,CASH,extra.id,null,true,TODAY);eq(330000,summarize(s,TODAY).assets);
        record(s,"actualExpense",Kind.EXPENSE,"Compra",1000,"2026-09-20",CASH,null,null,true,"2026-09-20");
        rejects(()->record(s,"retroSalary",Kind.INCOME,"Salário",300000,"2026-09-15",null,CASH,"salary:2026-10-05",true,"2026-09-20"));
    }
    static void conservation() {
        FinanceState s=fresh(10000000);Random random=new Random(9071);long expectedAssets=10000000;
        for(int i=0;i<250;i++) {
            long value=random.nextInt(50000)+1;int operation=random.nextInt(4);
            if(operation==0){record(s,"random:"+i,Kind.INCOME,"Receita",value,TODAY,null,CASH,null,true,TODAY);expectedAssets+=value;}
            else if(operation==1){record(s,"random:"+i,Kind.EXPENSE,"Despesa",value,TODAY,CASH,null,null,true,TODAY);expectedAssets-=value;}
            else if(operation==2)record(s,"random:"+i,Kind.CONTRIBUTION,"Aporte",value,TODAY,CASH,INVEST,null,true,TODAY);
            else if(balance(s,INVEST,TODAY)>=value)record(s,"random:"+i,Kind.TRANSFER,"Resgate",value,TODAY,INVEST,CASH,null,true,TODAY);
            Summary x=summarize(s,TODAY);eq(expectedAssets,x.assets);
            eq(x.cash,activeCycle(s).openingCash+x.income-x.expenses-x.contributions+x.withdrawals+x.adjustments);
        }
    }
    static void closingDestinations() {
        for(RemainderDestination destination:RemainderDestination.values()) {
            FinanceState s=fresh(100000);Goal g=addGoal(s,"Reserva",100000,"2027-01-01",TODAY);
            Cycle closed=finishCycle(s,"close",TODAY,destination,30000,g.id,CASH);
            eq(100000,closed.freeRemainder);eq(100000,summarize(s,TODAY).assets);ok(activeCycle(s)==null);
            eq(destination==RemainderDestination.CARRY?100000:70000,summarize(s,TODAY).available);
            eq(destination==RemainderDestination.CONTRIBUTION?70000:100000,summarize(s,TODAY).cash);
            eq(destination==RemainderDestination.CONTRIBUTION?30000:0,summarize(s,TODAY).invested);
            eq(destination==RemainderDestination.GOAL?30000:0,closed.cashReservations);
            eq(destination==RemainderDestination.PLAN_CONTRIBUTION?30000:0,closed.pendingContributions);
            eq(destination==RemainderDestination.CARRY?0:30000,closed.remainderAllocated);
            int entries=s.entries.size(),cycles=s.cycles.size();finishCycle(s,"close",TODAY,destination,30000,g.id,CASH);
            eq(entries,s.entries.size());eq(cycles,s.cycles.size());
            record(s,"afterClose",Kind.EXPENSE,"Gasto após fechamento",1000,TODAY,CASH,null,null,true,TODAY);
            ok(activeCycle(s)!=null);eq(closed.closingCash,activeCycle(s).openingCash);eq(1000,summarize(s,TODAY).expenses);
        }
        FinanceState s=fresh(10000);rejects(()->finishCycle(s,"bad",TODAY,RemainderDestination.CONTRIBUTION,10001,null,CASH));
        eq(1,s.cycles.size());ok(!s.cycles.get(0).closed);
        Goal g=addGoal(s,"Meta",20000,"2027-01-01",TODAY);allocate(s,g.id,CASH,2000,TODAY);
        editGoal(s,g.id,"Viagem",30000,"2027-02-01");eq(2000,allocated(s,g.id));eq(30000,goal(s,g.id).targetCents);
        removeGoal(s,g.id);eq(0,s.goals.size());eq(10000,summarize(s,TODAY).available);
    }
    static void scheduleChanges() {
        FinanceState s=fresh(0);cancelForecast(s,"salary:2026-10-05",TODAY);
        ensureSchedule(s,"2026-09-06");ok(forecast(s,"salary:2026-10-05").cancelled);
        configure(s,0,0,300000,5,false,50000,0,10,"2026-09-06");ok(forecast(s,"salary:2026-10-05").cancelled);
        editForecast(s,"salary:2026-11-05","Salário com ajuste",310000,"2026-11-05");ensureSchedule(s,"2026-09-07");eq(310000,forecast(s,"salary:2026-11-05").amountCents);
        configure(s,0,0,320000,10,false,50000,0,10,"2026-09-07");ok(forecast(s,"salary:2026-11-05").cancelled);eq(320000,forecast(s,"salary:2026-10-10").amountCents);
        Rule r=addRule(s,"Aluguel",80000,31,"2026-09-07",CASH,"2026-09-07");
        ok(forecast(s,"rule:"+r.id+":2026-09-30")!=null);ensureSchedule(s,"2027-10-01");
        ok(forecast(s,"rule:"+r.id+":2028-02-29")!=null);ok("2027-10-01".equals(s.settings.lastScheduledOn));
        int count=s.forecasts.size();ensureSchedule(s,"2027-10-01");eq(count,s.forecasts.size());
        FinanceState a=fresh(0);Account other=addAccount(a,"Outra conta",AccountKind.CASH,10000,TODAY);settle(a,"received",salary(a).id,300000,true,TODAY);
        Summary x=summarize(a,TODAY);eq(x.cash,activeCycle(a).openingCash+x.income-x.expenses-x.contributions+x.withdrawals+x.adjustments);
        configure(a,0,0,300000,5,false,70000,0,8,TODAY);eq(70000,summarize(a,TODAY).pendingContributions);
        FinanceState b=FinanceState.empty("alice");command(b,"Recebi R$100","first",TODAY);addAccount(b,"Segunda",AccountKind.CASH,20000,TODAY);
        command(b,"Todo dia 5 recebo R$3000","setup",TODAY);eq(30000,summarize(b,TODAY).cash);ok(!b.settings.balancesConfirmed);
        List<OrganizerCommand> zero=LocalCommandParser.parse("E se eu investir R$0 por mês?",TODAY);ok(zero.get(0).amountSpecified);
        List<String> result=CommandRouter.apply(a,zero,"zero",TODAY);ok(result.get(0).contains(Money.format(0)+"/mês"));
    }
    static void reversalIntegrity()throws Exception {
        File path=Files.createTempDirectory("cogni-reversal").resolve("alice.bin").toFile();FileFinanceRepository repo=new FileFinanceRepository(path,"alice");
        repo.update(s->{configure(s,100000,0,0,5,false,0,0,8,TODAY);record(s,"invest",Kind.CONTRIBUTION,"Aporte",50000,TODAY,CASH,INVEST,null,true,TODAY);record(s,"withdraw",Kind.TRANSFER,"Resgate",10000,TODAY,INVEST,CASH,null,true,TODAY);});
        long revision=repo.snapshot().revision;rejects(()->repo.update(s->voidEntry(s,"invest","Estorno inválido",TODAY)));
        eq(revision,repo.snapshot().revision);eq(40000,summarize(repo.snapshot(),TODAY).invested);
        repo.update(s->{voidEntry(s,"invest","Correção",TODAY);record(s,"corrected",Kind.CONTRIBUTION,"Aporte corrigido",60000,TODAY,CASH,INVEST,null,true,TODAY);});
        eq(50000,summarize(repo.snapshot(),TODAY).invested);eq(100000,summarize(repo.snapshot(),TODAY).assets);
        repo.update(s->{Goal g=addGoal(s,"Meta",100000,"2027-01-01",TODAY);allocate(s,g.id,INVEST,40000,TODAY);});
        rejects(()->repo.update(s->{voidEntry(s,"corrected","Correção",TODAY);record(s,"bad",Kind.CONTRIBUTION,"Aporte corrigido",20000,TODAY,CASH,INVEST,null,true,TODAY);}));
        eq(50000,summarize(repo.snapshot(),TODAY).invested);rejects(()->repo.update(s->s.userId="bob"));ok(repo.snapshot().userId.equals("alice"));
        Files.delete(path.toPath());Files.delete(path.getParentFile().toPath());
        FinanceState manual=fresh(0);settle(manual,"salary",salary(manual).id,300000,true,TODAY);
        cancelForecast(manual,"salary-contribution:"+salary(manual).id,TODAY);voidEntry(manual,"salary","Correção",TODAY);settle(manual,"salary2",salary(manual).id,320000,true,TODAY);
        eq(0,summarize(manual,TODAY).pendingContributions);
        FinanceState partial=fresh(0);settle(partial,"salary",salary(partial).id,300000,true,TODAY);settle(partial,"partial","salary-contribution:"+salary(partial).id,35000,false,TODAY);
        voidEntry(partial,"salary","Correção",TODAY);settle(partial,"salary2",salary(partial).id,320000,true,TODAY);eq(15000,summarize(partial,TODAY).pendingContributions);eq(35000,summarize(partial,TODAY).invested);
        FinanceState finalized=fresh(0);settle(finalized,"salary",salary(finalized).id,300000,true,TODAY);settle(finalized,"less","salary-contribution:"+salary(finalized).id,35000,true,TODAY);
        voidEntry(finalized,"salary","Correção",TODAY);settle(finalized,"salary2",salary(finalized).id,320000,true,TODAY);eq(0,summarize(finalized,TODAY).pendingContributions);
    }
    static void concurrentPersistence()throws Exception {
        File dir=Files.createTempDirectory("cogni-concurrent").toFile(),path=new File(dir,"alice.bin");FileFinanceRepository repo=new FileFinanceRepository(path,"alice");
        repo.update(s->configure(s,0,0,0,5,false,0,0,8,TODAY));
        java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(4);
        List<java.util.concurrent.Future<?>> writes=new ArrayList<>();
        try {
            for(int i=0;i<20;i++){String key="income:"+i;writes.add(workers.submit(()->repo.update(s->record(s,key,Kind.INCOME,"Receita",10000,TODAY,null,CASH,null,true,TODAY))));}
            for(java.util.concurrent.Future<?> write:writes)write.get(5,java.util.concurrent.TimeUnit.SECONDS);
        }finally{workers.shutdownNow();}
        eq(200000,summarize(repo.snapshot(),TODAY).cash);eq(20,repo.snapshot().entries.size());eq(21,repo.snapshot().revision);
        eq(200000,summarize(new FileFinanceRepository(path,"alice").snapshot(),TODAY).cash);
        File obstruction=new File(dir,"parent-is-file");Files.write(obstruction.toPath(),new byte[]{1});
        FileFinanceRepository failed=new FileFinanceRepository(new File(obstruction,"bob.bin"),"bob");rejects(()->failed.update(s->configure(s,100,0,0,5,false,0,0,8,TODAY)));ok(!failed.snapshot().settings.configured);
        Files.delete(obstruction.toPath());Files.delete(path.toPath());Files.delete(dir.toPath());
    }
    static void cdiProjection() {
        InvestmentProjection.Quote q=new InvestmentProjection.Quote("2026-09-17",.050788);
        near(13.649989315282104,q.annualPercent());double contracted=InvestmentProjection.annualFromDaily(q.dailyPercent,110);
        near(15.113110066727445,contracted);near(8594.382428614945,Projection.calculate(800000,50000,contracted,1).finalValue);
        ok(Math.abs(contracted-q.annualPercent()*1.1)>.09);near(0,InvestmentProjection.annualFromDaily(0,110));near(0,InvestmentProjection.annualFromDaily(.05,0));
        rejects(()->InvestmentProjection.annualFromDaily(Double.NaN,100));rejects(()->InvestmentProjection.annualFromDaily(.05,301));rejects(()->new InvestmentProjection.Quote("2026-09-17",-.01));
        rejects(()->q.validateOn("2026-09-16"));ok(!q.stale("2026-09-18"));ok(q.stale("2026-09-25"));
        FinanceState s=fresh(0);Account a=account(s,INVEST);near(8,InvestmentProjection.annual(a,s,null));InvestmentProjection.profile(a,InvestmentProjection.CDI,110);
        near(contracted,InvestmentProjection.annual(a,s,q));rejects(()->InvestmentProjection.annual(a,s,null));InvestmentProjection.profile(a,InvestmentProjection.FIXED,-5);near(-5,InvestmentProjection.annual(a,s,null));
        rejects(()->InvestmentProjection.profile(account(s,CASH),InvestmentProjection.CDI,100));double[] values=InvestmentProjection.series(800000,50000,8,60);
        eq(61,values.length);near(8000,values[0]);near(48226.95,values[60]);near(38000,InvestmentProjection.capital(800000,50000,60)[60]);
        for(int i=1;i<values.length;i++)ok(values[i]>values[i-1]);eq(1201,InvestmentProjection.series(0,0,0,1200).length);rejects(()->InvestmentProjection.series(0,0,0,1201));
    }
    static void investmentLanguage() {
        InvestmentDraft d=InvestmentDraft.parse("Tenho R$8.000 a 110% do CDI e aporto R$500 por mês por 5 anos");
        eq(800000,d.amount);eq(50000,d.monthly);eq(60,d.months);near(110,d.cdiPercent);ok(!d.percentageAssumed);ok(!d.simulation);ok(d.monthlySpecified);
        d=InvestmentDraft.parse("Investi 2 mil em CDI");eq(200000,d.amount);near(100,d.cdiPercent);ok(d.percentageAssumed);ok(!d.monthlySpecified);
        d=InvestmentDraft.parse("Simule R$1.500,50 a 100% do CDI por 12 meses");eq(150050,d.amount);ok(d.simulation);eq(12,d.months);
        d=InvestmentDraft.parse("Tenho 8000 em CDI e aporto 0 por mês");ok(d.monthlySpecified);eq(0,d.monthly);
        rejects(()->InvestmentDraft.parse("CDI"));rejects(()->InvestmentDraft.parse("Tenho 100 a 400% do CDI"));rejects(()->InvestmentDraft.parse("Investi 100 ontem em CDI"));
        rejects(()->InvestmentDraft.parse("Simule R$100 em CDI por 101 anos"));rejects(()->InvestmentDraft.parse("Tenho 100 em CDI e gastei 20"));
    }
    static void investmentConfirmation() throws Exception {
        FinanceState s=fresh(0);settle(s,"salary",salary(s).id,300000,true,TODAY);InvestmentEntryPlan plan=new InvestmentEntryPlan();plan.amount=35000;plan.cdiPercent=110;plan.apply(s,"investment",TODAY);
        Summary x=summarize(s,TODAY);eq(265000,x.cash);eq(35000,x.invested);eq(15000,x.pendingContributions);eq(300000,x.assets);eq(0,x.expenses);
        plan.apply(s,"investment",TODAY);eq(35000,summarize(s,TODAY).invested);plan.existingBalance=true;plan.amount=800000;plan.apply(s,"balance",TODAY);x=summarize(s,TODAY);
        eq(800000,x.invested);eq(265000,x.cash);eq(35000,x.contributions);eq(300000,x.income);near(110,account(s.copy(),INVEST).yieldPercent);ok(InvestmentProjection.CDI.equals(account(s.copy(),INVEST).yieldIndex));
        plan.accountId=null;plan.amount=10000;plan.apply(s,"new-account",TODAY);eq(3,s.accounts.size());eq(810000,summarize(s,TODAY).invested);
        File path=Files.createTempDirectory("cdi-test").resolve("alice.bin").toFile();FileFinanceRepository repo=new FileFinanceRepository(path,"alice");repo.update(state->configure(state,300000,0,0,5,false,0,0,8,TODAY));long revision=repo.snapshot().revision;
        plan.updateMonthly=true;plan.monthly=50000;rejects(()->repo.update(state->plan.apply(state,"invalid-account",TODAY)));eq(revision,repo.snapshot().revision);eq(2,repo.snapshot().accounts.size());
        plan.accountId=INVEST;plan.amount=800000;repo.update(state->plan.apply(state,"saved",TODAY));FinanceState reopened=new FileFinanceRepository(path,"alice").snapshot();eq(800000,balance(reopened,INVEST,TODAY));near(110,account(reopened,INVEST).yieldPercent);eq(50000,reopened.settings.contributionCents);
    }
    static void portfolioProjection() {
        FinanceState s=fresh(0);reconcileBalance(s,INVEST,800000,TODAY);Account other=addAccount(s,"Outra aplicação",AccountKind.INVESTMENT,200000,TODAY);
        InvestmentProjection.profile(account(s,INVEST),InvestmentProjection.CDI,110);InvestmentProjection.profile(other,InvestmentProjection.FIXED,0);InvestmentProjection.Quote q=new InvestmentProjection.Quote(TODAY,.050788);
        double annual=InvestmentProjection.annualFromDaily(q.dailyPercent,110);double[] values=InvestmentProjection.portfolio(s,50000,Double.NaN,60,q,TODAY);
        near(10000,values[0]);near(Projection.calculate(800000,50000,annual,60).finalValue+2000,values[60]);rejects(()->InvestmentProjection.portfolio(s,50000,Double.NaN,60,null,TODAY));
        near(Projection.calculate(1000000,50000,8,60).finalValue,InvestmentProjection.portfolio(s,50000,8,60,null,TODAY)[60]);
        long required=InvestmentProjection.requiredPortfolioMonthly(s,10000000,Double.NaN,60,q,TODAY);ok(InvestmentProjection.portfolio(s,required,Double.NaN,60,q,TODAY)[60]>=100000);ok(InvestmentProjection.portfolio(s,required-1,Double.NaN,60,q,TODAY)[60]<100000);
        eq(0,InvestmentProjection.requiredPortfolioMonthly(s,100,Double.NaN,60,q,TODAY));near(10000,summarize(s,TODAY).invested/100.0);eq(0,s.entries.stream().filter(e->e.kind==Kind.CONTRIBUTION).count());
        OrganizerCommand simulation=new OrganizerCommand(OrganizerCommand.Action.SIMULATE);simulation.months=60;rejects(()->CommandRouter.apply(s.copy(),Collections.singletonList(simulation),"no-cdi-quote",TODAY));
        OrganizerCommand rate=new OrganizerCommand(OrganizerCommand.Action.CONFIG_RETURN);rate.annualRate=7;CommandRouter.apply(s,Collections.singletonList(rate),"custom-rate",TODAY);
        ok(InvestmentProjection.FIXED.equals(account(s,INVEST).yieldIndex));near(7,account(s,INVEST).expectedAnnualRate);near(Projection.calculate(800000,50000,7,60).finalValue+2000,InvestmentProjection.portfolio(s,50000,Double.NaN,60,null,TODAY)[60]);
    }

}
