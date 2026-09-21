package com.example.organizadoria.financeiro.domain;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/** Interpretação conservadora; confirmação separada antes de gravar. */
public final class InvestmentDraft {
 public long amount,monthly;public double cdiPercent=100;public int months=12;
 public boolean monthlySpecified,percentageAssumed=true,simulation;
 private static Matcher match(String regex,String text){Matcher m=Pattern.compile(regex).matcher(text);return m.find()?m:null;}
 public static boolean accepts(String text){return CommandRouter.normalize(text).matches("(?s).*\\bcdi\\b.*");}
 public static InvestmentDraft parse(String text){
  String n=CommandRouter.normalize(text);if(!accepts(n))throw new IllegalArgumentException("Informe o CDI.");
  if(n.contains("amanha")||n.contains("ontem")||n.matches(".*\\d{1,2}/\\d{1,2}.*"))throw new IllegalArgumentException("Para outra data, use a movimentação detalhada. A projeção parte do valor atual.");
  InvestmentDraft d=new InvestmentDraft();d.simulation=n.contains("simul")||n.contains("e se")||n.contains("quanto");String number="([\\d.,]+)(?:\\s*(mil))?";
  Matcher m=match("(?:tenho|investi|investir|apliquei|aplicar|simule|simular)\\s*(?:r\\$\\s*)?"+number,n);if(m==null)m=match("r\\$\\s*"+number,n);
  if(m==null)throw new IllegalArgumentException("Informe o valor: ‘Tenho R$8.000 a 100% do CDI’.");d.amount=money(m);
  m=match("([+-]?[\\d.,]+)\\s*%\\s*(?:do|de)?\\s*cdi",n);if(m!=null){d.cdiPercent=Double.parseDouble(m.group(1).replace(',','.'));d.percentageAssumed=false;}
  if(!Double.isFinite(d.cdiPercent)||d.cdiPercent<0||d.cdiPercent>300)throw new IllegalArgumentException("Use de 0 a 300% do CDI.");
  m=match("(?:aporto|aportando|aporte(?:s)?(?: mensal)?(?: de)?|investir)\\s*(?:r\\$\\s*)?"+number+"\\s*(?:por mes|/mes|mensais|ao mes)",n);if(m!=null){d.monthly=money(m);d.monthlySpecified=true;}
  m=match("(?:em|por|durante|daqui a)\\s*(\\d+)\\s*(anos?|mes(?:es)?)",n);if(m!=null){long count=Long.parseLong(m.group(1))*(m.group(2).startsWith("ano")?12L:1);if(count<1||count>1200)throw new IllegalArgumentException("Use de 1 a 1200 meses.");d.months=(int)count;}
  if(n.matches(".*(?:gastei|paguei|recebi|salario|resgatei|vou pagar).*"))throw new IllegalArgumentException("Registre um investimento por vez para conferir os valores.");
  Projection.calculate(d.amount,d.monthly,0,d.months);return d;
 }
 private static long money(Matcher m){return Math.multiplyExact(Money.parse(m.group(1)),m.group(2)==null?1:1000);}
}
