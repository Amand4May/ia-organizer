package com.example.organizadoria.financeiro.domain;
import static com.example.organizadoria.financeiro.domain.FinanceState.*;
import static com.example.organizadoria.financeiro.domain.FinanceEngine.*;
public final class InvestmentEntryPlan {
 public boolean existingBalance,updateMonthly;public String accountId=INVEST,cashId=CASH,name="CDI";
 public long amount,monthly;public double cdiPercent=100;
 public void apply(FinanceState s,String operation,String today){
  if(s.appliedCommands.contains(operation))return;
  if(!s.settings.configured)throw new IllegalArgumentException("Configure o Financeiro primeiro.");
  if(amount<0||monthly<0)throw new IllegalArgumentException("Valores negativos não são aceitos.");
  Account target=accountId==null?addAccount(s,name,AccountKind.INVESTMENT,0,today):account(s,accountId);InvestmentProjection.profile(target,InvestmentProjection.CDI,cdiPercent);
  if(existingBalance)reconcileBalance(s,target.id,amount,today);
  else{String link=null;for(Forecast p:pending(s))if(p.kind==Kind.CONTRIBUTION&&target.id.equals(p.targetAccountId)&&cashId.equals(p.accountId)){if(link!=null)throw new IllegalArgumentException("Há mais de um aporte previsto. Use ‘Registrar aporte’ para escolher.");link=p.id;}record(s,operation+":aporte",Kind.CONTRIBUTION,"Aporte · "+target.name,amount,today,cashId,target.id,link,false,today);}
  if(updateMonthly){if(!INVEST.equals(target.id))throw new IllegalArgumentException("O plano mensal geral usa a aplicação principal.");setContribution(s,false,monthly,0,today);}
  validateState(s);s.appliedCommands.add(operation);
 }
}
