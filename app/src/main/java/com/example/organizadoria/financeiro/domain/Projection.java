package com.example.organizadoria.financeiro.domain;

/** Simulações nominais, antes de custos, com aporte ao final do mês. Não altera o livro financeiro. */
public final class Projection {
    public final double finalValue, contributed, estimatedReturn, monthlyRate;
    public final int months;
    private Projection(double value,double contributed,double rate,int months) {
        finalValue=value; this.contributed=contributed; estimatedReturn=value-contributed; monthlyRate=rate; this.months=months;
    }
    private static double rate(double annualPercent,int months) {
        if (!Double.isFinite(annualPercent) || annualPercent <= -100 || annualPercent > 1000)
            throw new IllegalArgumentException("Rentabilidade deve ser maior que -100% e no máximo 1000% a.a.");
        if (months < 1 || months > 1200) throw new IllegalArgumentException("Prazo deve ser de 1 a 1200 meses.");
        return Math.expm1(Math.log1p(annualPercent/100)/12);
    }
    public static Projection calculate(long initialCents,long monthlyCents,double annualPercent,int months) {
        if (initialCents<0 || monthlyCents<0) throw new IllegalArgumentException("Investimento e aporte não podem ser negativos.");
        double r=rate(annualPercent,months), growth=Math.exp(months*Math.log1p(r));
        double factor=Math.abs(r)<1e-12 ? months : Math.expm1(months*Math.log1p(r))/r;
        double value=initialCents/100.0*growth + monthlyCents/100.0*factor;
        if (!Double.isFinite(value) || value>9e13) throw new IllegalArgumentException("Projeção fora do limite numérico.");
        return new Projection(value,initialCents/100.0+monthlyCents/100.0*months,r,months);
    }
    public static long requiredMonthly(long initialCents,long targetCents,double annualPercent,int months) {
        if(initialCents<0 || targetCents<0) throw new IllegalArgumentException("Valores devem ser positivos.");
        double r=rate(annualPercent,months), growth=Math.exp(months*Math.log1p(r));
        double factor=Math.abs(r)<1e-12 ? months : Math.expm1(months*Math.log1p(r))/r;
        double cents=Math.max(0,(targetCents-initialCents*growth)/factor);
        if(!Double.isFinite(cents) || cents>9e15) throw new IllegalArgumentException("Meta fora do limite numérico.");
        return (long)Math.ceil(cents);
    }
}
