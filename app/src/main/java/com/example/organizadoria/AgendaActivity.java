package com.example.organizadoria;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.example.organizadoria.financeiro.domain.FinanceState;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AgendaActivity extends AppCompatActivity {
    private TarefaAdapter adapter;private AgendaFeed feed;private RecyclerView list;private TextView empty;
    private List<Tarefa> tasks=new ArrayList<>();private String selected;private MonthCalendarView calendar;
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);setContentView(R.layout.activity_agenda);if(!Navigation.requireSession(this))return;
        Navigation.bind(this,R.id.nav_agenda);selected=saved==null?LocalDate.now().toString():saved.getString("date",LocalDate.now().toString());
        list=findViewById(R.id.listaAgenda);empty=findViewById(R.id.textVazio);list.setLayoutManager(new LinearLayoutManager(this));adapter=new TarefaAdapter();list.setAdapter(adapter);
        calendar=findViewById(R.id.calendarView);calendar.setSelectedDate(LocalDate.parse(selected));
        calendar.setOnDateSelected(date->{selected=date.toString();filter();});
        feed=new AgendaFeed(this,items->{tasks=items;filter();updateDots();});adapter.setOnTarefaLongClickListener(task->feed.delete(task));
        findViewById(R.id.btnAdd).setOnClickListener(v->startActivity(new Intent(this,MainActivity.class).putExtra("focarInput",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP)));
    }
    private void filter(){
        List<Tarefa> filtered=new ArrayList<>();for(Tarefa t:tasks)if(selected.equals(t.data))filtered.add(t);
        filtered.addAll(financeOfDay(selected));
        adapter.carregarListaCompleta(filtered);empty.setVisibility(filtered.isEmpty()?View.VISIBLE:View.GONE);list.setVisibility(filtered.isEmpty()?View.GONE:View.VISIBLE);
        ((TextView)findViewById(R.id.labelHoje)).setText(selected.equals(LocalDate.now().toString())?"Hoje":LocalDate.parse(selected).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }
    /** Receitas e gastos do dia como cards na lista. Gastos já ligados a um compromisso aparecem no próprio card do compromisso. */
    private List<Tarefa> financeOfDay(String day){
        List<Tarefa> out=new ArrayList<>();FinanceState s=AppServices.repository(this).snapshot();
        Set<String> linked=new HashSet<>();for(FinanceState.AgendaItem a:s.agenda)if(!a.cancelled&&a.forecastId!=null)linked.add(a.forecastId);
        for(FinanceState.Entry e:s.entries){
            if(e.voided||e.date==null||!e.date.startsWith(day))continue;
            if(e.kind==FinanceState.Kind.INCOME)out.add(financeRow(s,"receita",e.title,e.amountCents,true));
            else if(e.kind==FinanceState.Kind.EXPENSE)out.add(financeRow(s,"despesa",e.title,e.amountCents,true));
        }
        for(FinanceState.Forecast f:s.forecasts){
            if(f.cancelled||f.completed||linked.contains(f.id)||f.date==null||!f.date.startsWith(day))continue;
            if(f.kind==FinanceState.Kind.INCOME)out.add(financeRow(s,"receita",f.title,f.amountCents,false));
            else if(f.kind==FinanceState.Kind.EXPENSE)out.add(financeRow(s,"despesa",f.title,f.amountCents,false));
        }
        return out;
    }
    private static Tarefa financeRow(FinanceState s,String tipo,String title,long cents,boolean realizado){
        Tarefa t=new Tarefa(s.userId,tipo,title,cents/100.0,null,null);t.gastoCentavos=cents;t.gastoPago=realizado;return t;
    }
    /** Uma bolinha por categoria por dia: compromisso, gasto previsto, recebimento e gasto realizado. */
    private void updateDots(){
        Map<LocalDate,Integer> dots=new HashMap<>();
        for(Tarefa t:tasks)mark(dots,t.data,MonthCalendarView.DOT_APPOINTMENT);
        FinanceState s=AppServices.repository(this).snapshot();
        for(FinanceState.Forecast f:s.forecasts){
            if(f.cancelled)continue;
            if(f.kind==FinanceState.Kind.INCOME)mark(dots,f.date,MonthCalendarView.DOT_INCOME);
            else if(f.kind==FinanceState.Kind.EXPENSE&&!f.completed)mark(dots,f.date,MonthCalendarView.DOT_PLANNED_EXPENSE);
        }
        for(FinanceState.Entry e:s.entries){
            if(e.voided)continue;
            if(e.kind==FinanceState.Kind.INCOME)mark(dots,e.date,MonthCalendarView.DOT_INCOME);
            else if(e.kind==FinanceState.Kind.EXPENSE)mark(dots,e.date,MonthCalendarView.DOT_EXPENSE);
        }
        calendar.setDots(dots);
    }
    private static void mark(Map<LocalDate,Integer> dots,String date,int flag){
        if(date==null||date.length()<10)return;
        try{LocalDate d=LocalDate.parse(date.substring(0,10));Integer cur=dots.get(d);dots.put(d,(cur==null?0:cur)|flag);}
        catch(java.time.format.DateTimeParseException ignored){/* Data inválida em registro antigo: sem bolinha. */}
    }
    @Override protected void onStart(){super.onStart();if(feed!=null)feed.start();}
    @Override protected void onStop(){if(feed!=null)feed.stop();super.onStop();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("date",selected);}
}