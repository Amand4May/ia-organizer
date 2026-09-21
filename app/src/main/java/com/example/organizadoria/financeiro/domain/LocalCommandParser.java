package com.example.organizadoria.financeiro.domain;

import java.time.*;
import java.util.*;
import java.util.regex.*;
import static com.example.organizadoria.financeiro.domain.OrganizerCommand.Action.*;

/** Interpretador determinístico para exercitar a cópia sem enviar frases para a internet. */
public final class LocalCommandParser {
    private LocalCommandParser() {}
    private static Matcher find(String regex,String s) { Matcher m=Pattern.compile(regex).matcher(s);return m.find()?m:null; }
    private static long amount(String s) {
        Matcher m=find("r\\$\\s*([\\d.,]+)(?:\\s*(mil))?",s);
        if(m==null) m=find("([\\d.,]+)\\s*(mil)?\\s*(?:reais|por mes|/mes)",s);
        if(m==null) m=find("(?:investir|investi|aporto|recebo|recebi|gastei|paguei|tenho|aporte de|chegar a|chegar em)\\s*(?:r\\$\\s*)?([\\d.,]+)(?:\\s*(mil))?",s);
        if(m==null) throw new IllegalArgumentException("Informe um valor, por exemplo R$45.");
        return Math.multiplyExact(Money.parse(m.group(1)),m.group(2)!=null?1000:1);
    }
    private static OrganizerCommand command(OrganizerCommand.Action action,String title,long amount,String date) {
        OrganizerCommand c=new OrganizerCommand(action);c.title=title;c.amount=amount;c.date=date;return c;
    }
    public static List<OrganizerCommand> parse(String text,String today) {
        if(text==null || text.trim().isEmpty()) throw new IllegalArgumentException("Digite um comando.");
        String n=CommandRouter.normalize(text); List<OrganizerCommand> out=new ArrayList<>();
        String due=n.contains("amanha")?LocalDate.parse(today).plusDays(1).toString():today;
        Matcher explicit=find("(\\d{2})/(\\d{2})/(\\d{4})",n);
        if(explicit!=null) due=LocalDate.of(Integer.parseInt(explicit.group(3)),Integer.parseInt(explicit.group(2)),Integer.parseInt(explicit.group(1))).toString();
        Matcher day=find("(?:todo dia|dia)\\s+(\\d{1,2})",n);
        if(n.contains("quanto") || n.startsWith("e se")) {
            boolean inverse=n.contains("preciso") || n.contains("chegar");
            OrganizerCommand c=command(inverse?REQUIRED_CONTRIBUTION:SIMULATE,"Simulação",0,today);
            Matcher period=find("(?:em|daqui a)\\s+(\\d+)\\s*(anos?|mes(?:es)?)",n);
            if(period!=null) c.months=Math.multiplyExact(Integer.parseInt(period.group(1)),period.group(2).startsWith("ano")?12:1);
            if(inverse || n.contains("r$") || n.contains("investir")) {c.amount=amount(n);c.amountSpecified=true;}
            Matcher rate=find("([+-]?[\\d.,]+)\\s*%",n);if(rate!=null)c.annualRate=Double.parseDouble(rate.group(1).replace(',','.'));
            out.add(c);return out;
        }
        if(n.contains("recebo") && day!=null) {
            OrganizerCommand c=command(CONFIG_SALARY,"Salário",amount(n),today);c.day=Integer.parseInt(day.group(1));out.add(c);
            if(n.contains("investir") || n.contains("aportar")) out.add(contributionConfig(n.substring(Math.max(n.indexOf("investir"),n.indexOf("aportar"))),today));
            return out;
        }
        if(n.contains("tenho") && n.contains("investid")) {
            out.add(command(SET_INVESTED,"Investimentos",amount(n),today));
            if(n.contains("aporto")) out.add(contributionConfig(n.substring(n.indexOf("aporto")),today));
            Matcher rate=find("([+-]?[\\d.,]+)\\s*%",n);if(rate!=null){OrganizerCommand c=new OrganizerCommand(CONFIG_RETURN);c.annualRate=Double.parseDouble(rate.group(1).replace(',','.'));out.add(c);}
            return out;
        }
        if((n.contains("todo mes") || n.contains("mensal") || n.contains("/mes")) && (n.contains("invest") || n.contains("aport"))) {
            out.add(contributionConfig(n,today));return out;
        }
        if(n.contains("todo dia") && day!=null) {
            OrganizerCommand c=command(RECURRING_BILL,description(n),amount(n),today);c.day=Integer.parseInt(day.group(1));out.add(c);return out;
        }
        if(n.contains("gastei") || n.contains("paguei")) {
            OrganizerCommand c=command(EXPENSE,description(n),amount(n),due);c.partial=n.contains("parcial") || n.contains("de uma conta");out.add(c);return out;
        }
        if(n.contains("recebi")) { out.add(command(n.contains("salario")?SALARY:INCOME,n.contains("salario")?"Salário":description(n),amount(n),due));return out; }
        if(n.contains("investi") && !n.contains("investir")) {out.add(command(CONTRIBUTION,"Aporte",amount(n),due));return out;}
        if(n.contains("resgatei")) {out.add(command(WITHDRAWAL,"Resgate",amount(n.replace("resgatei","recebi")),due));return out;}
        if(n.contains("vou investir") || n.contains("vou aportar")) {out.add(command(PLAN_CONTRIBUTION,"Aporte planejado",amount(n),due));return out;}
        if(n.contains("vou pagar") || n.contains("vou gastar")) {out.add(command(PLAN_EXPENSE,description(n),amount(n),due));return out;}
        if(n.contains("amanha") || n.contains("reuniao") || n.contains("consulta") || n.contains("vou cortar") || n.contains("agend")) {
            long money=0;if(n.contains("r$") || n.contains("reais"))money=amount(n);
            OrganizerCommand c=command(TASK,description(n),money,due);
            Matcher time=find("(?:as\\s+|\\b)(\\d{1,2})(?:h(?:(\\d{2}))?|:(\\d{2}))",n);
            if(time==null) time=find("as\\s+(\\d{1,2})(?:\\s|$)",n);
            if(time!=null) {
                String minutes=time.groupCount()>=3 && time.group(3)!=null?time.group(3):time.groupCount()>=2&&time.group(2)!=null?time.group(2):"00";
                c.time=String.format(Locale.ROOT,"%02d:%s",Integer.parseInt(time.group(1)),minutes);
            }
            out.add(c);return out;
        }
        throw new IllegalArgumentException("Não entendi. Tente ‘Gastei R$40 no corte’ ou registre pela área Financeiro.");
    }
    private static OrganizerCommand contributionConfig(String n,String today) {
        OrganizerCommand c=command(CONFIG_CONTRIBUTION,"Aporte mensal",0,today);
        Matcher p=find("([+-]?[\\d.,]+)\\s*%",n);
        // Percentual após indicação de rentabilidade não altera o plano de aporte.
        if(p!=null && (n.contains("salario") || n.contains("da renda"))) {
            long bps=Money.parse(p.group(1));if(bps<0 || bps>10000)throw new IllegalArgumentException("Percentual deve estar entre 0 e 100%.");
            c.percent=true;c.basisPoints=(int)bps;
        }
        else c.amount=amount(n.replace("aportar","investir"));
        return c;
    }
    private static String description(String n) {
        if(n.matches(".*(cabelo|corte|barbeiro).*")) return "Corte de cabelo";
        if(n.contains("dentista")) return "Dentista";
        String s=n.replaceAll("r\\$\\s*[\\d.,]+|[\\d.,]+\\s*reais","")
                .replaceAll("\\b(amanha|hoje|gastei|paguei|recebi|vou|todo dia \\d+|por|as \\d+(?:h|:\\d+)?)\\b","")
                .replaceAll("\\s+"," ").trim();
        if(s.isEmpty())s="Movimentação";return Character.toUpperCase(s.charAt(0))+s.substring(1);
    }
}
