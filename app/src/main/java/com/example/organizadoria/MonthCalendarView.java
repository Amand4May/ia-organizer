package com.example.organizadoria;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Calendário mensal com até uma bolinha por categoria em cada dia.
 * Ordem das bolinhas: compromisso (azul), gasto previsto (amarelo), recebimento (verde), gasto realizado (vermelho).
 */
public class MonthCalendarView extends View {
    public static final int DOT_APPOINTMENT = 1, DOT_PLANNED_EXPENSE = 2, DOT_INCOME = 4, DOT_EXPENSE = 8;
    private static final int[] DOT_ORDER = {DOT_APPOINTMENT, DOT_PLANNED_EXPENSE, DOT_INCOME, DOT_EXPENSE};
    private static final String[] MONTHS = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho","Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
    private static final String[] WEEKDAYS = {"D","S","T","Q","Q","S","S"};

    public interface OnDateSelected { void onDateSelected(LocalDate date); }

    private final float dp;
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG), muted = new Paint(Paint.ANTI_ALIAS_FLAG),
            title = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent, white, secondary;
    private final int[] dotColors = new int[4];
    private final float headerH, weekH, cellH;
    private final GestureDetector gestures;

    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();
    private Map<LocalDate, Integer> dots = new HashMap<>();
    private OnDateSelected listener;

    public MonthCalendarView(Context c) { this(c, null); }
    public MonthCalendarView(Context c, AttributeSet a) {
        super(c, a);
        dp = getResources().getDisplayMetrics().density;
        accent = ContextCompat.getColor(c, R.color.azul_safira);
        white = ContextCompat.getColor(c, R.color.white);
        secondary = ContextCompat.getColor(c, R.color.cor_texto_secundario);
        dotColors[0] = ContextCompat.getColor(c, R.color.cor_tarefa);
        dotColors[1] = ContextCompat.getColor(c, R.color.cor_previsto);
        dotColors[2] = ContextCompat.getColor(c, R.color.cor_receita);
        dotColors[3] = ContextCompat.getColor(c, R.color.cor_vermelho);
        headerH = 48 * dp; weekH = 32 * dp; cellH = 48 * dp;
        text.setTextAlign(Paint.Align.CENTER); text.setTextSize(15 * dp);
        muted.setTextAlign(Paint.Align.CENTER); muted.setTextSize(13 * dp); muted.setColor(secondary);
        title.setTextSize(17 * dp); title.setColor(white); title.setTypeface(Typeface.DEFAULT_BOLD);
        gestures = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapUp(MotionEvent e) { tap(e.getX(), e.getY()); return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || Math.abs(vx) < Math.abs(vy) || Math.abs(e2.getX() - e1.getX()) < 48 * dp) return false;
                showMonth(vx < 0 ? month.plusMonths(1) : month.minusMonths(1)); return true;
            }
        });
    }

    public void setOnDateSelected(OnDateSelected l) { listener = l; }
    public void setSelectedDate(LocalDate d) { selected = d; month = YearMonth.from(d); requestLayout(); invalidate(); }
    /** Bitmask por dia, combinando as constantes DOT_*. */
    public void setDots(Map<LocalDate, Integer> d) { dots = d == null ? new HashMap<>() : d; invalidate(); }

    private void showMonth(YearMonth m) { month = m; requestLayout(); invalidate(); }
    private int leadingBlanks() { return month.atDay(1).getDayOfWeek().getValue() % 7; } // domingo primeiro
    private int rows() { return (leadingBlanks() + month.lengthOfMonth() + 6) / 7; }

    @Override protected void onMeasure(int w, int h) {
        int width = MeasureSpec.getSize(w);
        setMeasuredDimension(width, (int) (headerH + weekH + rows() * cellH + getPaddingTop() + getPaddingBottom()));
    }

    @Override protected void onDraw(Canvas canvas) {
        float left = getPaddingLeft(), top = getPaddingTop(), width = getWidth() - left - getPaddingRight(), col = width / 7f;
        // Cabeçalho: mês/ano e setas
        canvas.drawText(MONTHS[month.getMonthValue() - 1] + " " + month.getYear(), left + 12 * dp, top + headerH / 2 + 6 * dp, title);
        Paint arrow = new Paint(text); arrow.setTextSize(22 * dp); arrow.setColor(white);
        canvas.drawText("‹", left + width - col * 1.5f, top + headerH / 2 + 7 * dp, arrow);
        canvas.drawText("›", left + width - col * 0.5f, top + headerH / 2 + 7 * dp, arrow);
        // Dias da semana
        for (int i = 0; i < 7; i++) canvas.drawText(WEEKDAYS[i], left + col * (i + .5f), top + headerH + weekH / 2 + 5 * dp, muted);
        // Dias
        LocalDate today = LocalDate.now();
        int blanks = leadingBlanks();
        float gridTop = top + headerH + weekH, radius = Math.min(col, cellH) * .36f, dotR = 2.6f * dp, dotGap = 4 * dp;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            int index = blanks + day - 1;
            float cx = left + col * (index % 7 + .5f), cy = gridTop + cellH * (index / 7) + cellH / 2 - 4 * dp;
            LocalDate date = month.atDay(day);
            boolean isSelected = date.equals(selected);
            if (isSelected) { fill.setColor(accent); canvas.drawCircle(cx, cy, radius, fill); }
            text.setColor(isSelected ? white : date.equals(today) ? accent : white);
            text.setTypeface(date.equals(today) ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            canvas.drawText(String.valueOf(day), cx, cy + 5 * dp, text);
            Integer mask = dots.get(date);
            if (mask == null || mask == 0) continue;
            int count = Integer.bitCount(mask);
            float x = cx - (count - 1) * (dotR * 2 + dotGap) / 2f, y = cy + radius + 5 * dp;
            for (int i = 0; i < DOT_ORDER.length; i++) if ((mask & DOT_ORDER[i]) != 0) {
                fill.setColor(dotColors[i]); canvas.drawCircle(x, y, dotR, fill); x += dotR * 2 + dotGap;
            }
        }
    }

    private void tap(float x, float y) {
        float left = getPaddingLeft(), top = getPaddingTop(), width = getWidth() - left - getPaddingRight(), col = width / 7f;
        if (y < top + headerH) {
            if (x > left + width - col * 2 && x < left + width - col) showMonth(month.minusMonths(1));
            else if (x >= left + width - col) showMonth(month.plusMonths(1));
            return;
        }
        float gy = y - (top + headerH + weekH);
        if (gy < 0) return;
        int index = (int) (gy / cellH) * 7 + (int) ((x - left) / col), day = index - leadingBlanks() + 1;
        if (day < 1 || day > month.lengthOfMonth()) return;
        selected = month.atDay(day); invalidate();
        performClick();
        if (listener != null) listener.onDateSelected(selected);
    }

    @Override public boolean onTouchEvent(MotionEvent e) { return gestures.onTouchEvent(e) || super.onTouchEvent(e); }
    @Override public boolean performClick() { return super.performClick(); }
}