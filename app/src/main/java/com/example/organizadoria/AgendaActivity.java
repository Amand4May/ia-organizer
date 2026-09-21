package com.example.organizadoria;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AgendaActivity extends AppCompatActivity {
    private TarefaAdapter adapter;private AgendaFeed feed;private RecyclerView list;private TextView empty;
    private List<Tarefa> tasks=new ArrayList<>();private String selected;
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);setContentView(R.layout.activity_agenda);if(!Navigation.requireSession(this))return;
        Navigation.bind(this,R.id.nav_agenda);selected=saved==null?LocalDate.now().toString():saved.getString("date",LocalDate.now().toString());
        list=findViewById(R.id.listaAgenda);empty=findViewById(R.id.textVazio);list.setLayoutManager(new LinearLayoutManager(this));adapter=new TarefaAdapter();list.setAdapter(adapter);
        feed=new AgendaFeed(this,items->{tasks=items;filter();});adapter.setOnTarefaLongClickListener(task->feed.delete(task));
        CalendarView calendar=findViewById(R.id.calendarView);calendar.setDate(LocalDate.parse(selected).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        calendar.setOnDateChangeListener((v,year,month,day)->{selected=LocalDate.of(year,month+1,day).toString();filter();});
        findViewById(R.id.btnAdd).setOnClickListener(v->startActivity(new Intent(this,MainActivity.class).putExtra("focarInput",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP)));
    }
    private void filter(){
        List<Tarefa> filtered=new ArrayList<>();for(Tarefa t:tasks)if(selected.equals(t.data))filtered.add(t);
        adapter.carregarListaCompleta(filtered);empty.setVisibility(filtered.isEmpty()?View.VISIBLE:View.GONE);list.setVisibility(filtered.isEmpty()?View.GONE:View.VISIBLE);
        ((TextView)findViewById(R.id.labelHoje)).setText(selected.equals(LocalDate.now().toString())?"Hoje":LocalDate.parse(selected).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }
    @Override protected void onStart(){super.onStart();if(feed!=null)feed.start();}
    @Override protected void onStop(){if(feed!=null)feed.stop();super.onStop();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("date",selected);}
}
