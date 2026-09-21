package com.example.organizadoria.financeiro.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** BRL em centavos. Parsing estrito evita transformar valores inválidos em zero. */
public final class Money {
    private Money() {}
    public static long parse(String text) {
        if (text == null) throw new IllegalArgumentException("Informe um valor.");
        String s = text.trim().replace("R$", "").replace("\u00a0", "").replace(" ", "");
        if (s.matches("-?\\d{1,3}(\\.\\d{3})+(,\\d{1,2})?")) s = s.replace(".", "");
        s = s.replace(',', '.');
        if (!s.matches("-?\\d+(\\.\\d{1,2})?")) throw new IllegalArgumentException("Use um valor como 1.500,00.");
        try { return new BigDecimal(s).movePointRight(2).longValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Valor fora do limite."); }
    }
    public static long percent(long cents, int basisPoints) {
        if (basisPoints < 0 || basisPoints > 10000) throw new IllegalArgumentException("Percentual deve estar entre 0 e 100%.");
        return BigDecimal.valueOf(cents).multiply(BigDecimal.valueOf(basisPoints))
                .divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).longValueExact();
    }
    public static String format(long cents) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(BigDecimal.valueOf(cents, 2));
    }
    public static String input(long cents) { return BigDecimal.valueOf(cents, 2).toPlainString().replace('.', ','); }
}
