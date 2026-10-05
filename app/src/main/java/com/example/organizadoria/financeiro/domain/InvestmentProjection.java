package com.example.organizadoria.financeiro.domain;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.example.organizadoria.financeiro.domain.FinanceState.Account;
/** Valores brutos hipotéticos; nunca atualiza o saldo real. */
public final class InvestmentProjection {
 public static final String CDI="CDI",FIXED="FIXED";
 private InvestmentProjection(){}
 public static final class Quote {
  public final String date;public final double dailyPercent;
  public Quote(String date,double dailyPercent){FinanceEngine.date(date);annualFromDaily(dailyPercent,100);this.date=date;this.dailyPercent=dailyPercent;}
  public double annualPercent(){return annualFromDaily(dailyPercent,100);}
  public boolean stale(String today){return ChronoUnit.DAYS.between(LocalDate.parse(date),LocalDate.parse(today))>7;}
  public void validateOn(String today){if(date.compareTo(today)>0)throw new IllegalArgumentException("Cotação com data futura.");}
 }
 public static double annualFromDaily(double daily,double percent){
  if(!Double.isFinite(daily)||daily<0||daily>1||!Double.isFinite(percent)||percent<0||percent>300)throw new IllegalArgumentException("Taxa de CDI inválida. Use de 0 a 300% do índice.");
  double annual=Math.expm1(252*Math.log1p(daily/100*percent/100))*100;Projection.calculate(0,0,annual,1);return annual;
 }
 public static double annual(Account a,FinanceState s,Quote quote){
  if(CDI.equals(a.yieldIndex)){if(quote==null)throw new IllegalStateException("Abra o assistente do Financeiro ou Investimentos para consultar o CDI e calcular esta projeção.");return annualFromDaily(quote.dailyPercent,a.yieldPercent);}
  return FIXED.equals(a.yieldIndex)?a.expectedAnnualRate:s.settings.expectedAnnualReturn;
 }
 public static void profile(Account a,String index,double value){
  if(a.kind!=FinanceState.AccountKind.INVESTMENT)throw new IllegalArgumentException("Escolha uma aplicação.");
  if(CDI.equals(index)){if(!Double.isFinite(value)||value<0||value>300)throw new IllegalArgumentException("Informe de 0 a 300% do CDI.");a.yieldPercent=value;}
  else if(FIXED.equals(index)){Projection.calculate(0,0,value,1);a.expectedAnnualRate=value;}else throw new IllegalArgumentException("Indexador desconhecido.");a.yieldIndex=index;
 }
 public static double[] series(long initial,long monthly,double annual,int months){Projection.calculate(initial,monthly,annual,months);double[] out=new double[months+1];out[0]=initial/100.0;for(int i=1;i<=months;i++)out[i]=Projection.calculate(initial,monthly,annual,i).finalValue;return out;}
 public static double[] capital(long initial,long monthly,int months){return series(initial,monthly,0,months);}
 public static double[] portfolio(FinanceState state,long monthly,double override,int months,Quote quote,String today){
  if(!Double.isNaN(override))return series(FinanceEngine.summarize(state,today).invested,monthly,override,months);
  Projection.calculate(0,monthly,0,months);double[] result=new double[months+1];
  for(Account a:state.accounts)if(a.kind==FinanceState.AccountKind.INVESTMENT){double[] part=series(FinanceEngine.balance(state,a.id,today),FinanceEngine.INVEST.equals(a.id)?monthly:0,annual(a,state,quote),months);for(int i=0;i<result.length;i++){result[i]+=part[i];if(!Double.isFinite(result[i])||result[i]>9e13)throw new IllegalArgumentException("Projeção fora do limite numérico.");}}
  return result;
 }
 public static long requiredPortfolioMonthly(FinanceState state,long target,double override,int months,Quote quote,String today){
  if(target<0)throw new IllegalArgumentException("Informe uma meta positiva.");double starting=portfolio(state,0,override,months,quote,today)[months];double rate=Double.isNaN(override)?annual(FinanceEngine.account(state,FinanceEngine.INVEST),state,quote):override;
  double cents=Math.max(0,(target/100.0-starting)/Projection.calculate(0,100,rate,months).finalValue*100);if(!Double.isFinite(cents)||cents>9e15)throw new IllegalArgumentException("Meta fora do limite numérico.");return (long)Math.ceil(cents);
 }
}
