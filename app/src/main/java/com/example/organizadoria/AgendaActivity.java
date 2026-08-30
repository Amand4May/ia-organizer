package com.example.organizadoria;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AgendaActivity extends AppCompatActivity {

    private RecyclerView listaAgenda;
    private TextView textVazio;
    private CalendarView calendarView;
    private TarefaAdapter adapter;
    private AppDatabase db;
    private TarefaDao tarefaDao;
    private List<Tarefa> todasAsTarefas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        listaAgenda = findViewById(R.id.listaAgenda);
        textVazio = findViewById(R.id.textVazio);
        calendarView = findViewById(R.id.calendarView);

        btnVoltar.setOnClickListener(v -> finish());

        listaAgenda.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TarefaAdapter();
        listaAgenda.setAdapter(adapter);

        adapter.setOnTarefaLongClickListener(tarefa -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Apagar")
                    .setMessage("Deseja apagar este compromisso?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        new Thread(() -> {
                            tarefaDao.deletar(tarefa);
                            carregarAgenda();
                        }).start();
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "banco_organizadoria")
                .fallbackToDestructiveMigration()
                .build();
        tarefaDao = db.tarefaDao();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String dataSelecionada = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            filtrarTarefasPorData(dataSelecionada);
        });

        carregarAgenda();
    }

    private void carregarAgenda() {
        new Thread(() -> {
            todasAsTarefas = tarefaDao.buscarApenasTarefas();
            
            long date = calendarView.getDate();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(date);
            String hoje = String.format(Locale.getDefault(), "%04d-%02d-%02d", 
                    cal.get(java.util.Calendar.YEAR), 
                    cal.get(java.util.Calendar.MONTH) + 1, 
                    cal.get(java.util.Calendar.DAY_OF_MONTH));
            
            runOnUiThread(() -> filtrarTarefasPorData(hoje));
        }).start();
    }

    private void filtrarTarefasPorData(String data) {
        List<Tarefa> filtradas = new ArrayList<>();
        for (Tarefa t : todasAsTarefas) {
            if (t.getData().equals(data)) {
                filtradas.add(t);
            }
        }
        
        if (filtradas.isEmpty()) {
            textVazio.setVisibility(View.VISIBLE);
            listaAgenda.setVisibility(View.GONE);
        } else {
            textVazio.setVisibility(View.GONE);
            listaAgenda.setVisibility(View.VISIBLE);
            adapter.carregarListaCompleta(filtradas);
        }
    }
}