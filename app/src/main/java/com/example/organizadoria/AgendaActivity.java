package com.example.organizadoria;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AgendaActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private RecyclerView listaAgenda;
    private TextView textVazio;
    private CalendarView calendarView;
    private TarefaAdapter adapter;
    private AppDatabase db;
    private TarefaDao tarefaDao;
    private FirebaseFirestore firestoreDb;
    private List<Tarefa> todasAsTarefas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_agenda);

        listaAgenda = findViewById(R.id.listaAgenda);
        textVazio = findViewById(R.id.textVazio);
        calendarView = findViewById(R.id.calendarView);
        
        firestoreDb = FirebaseFirestore.getInstance();

        ImageButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("focarInput", true);
            startActivity(intent);
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_agenda) {
                // Já está na agenda
            } else if (id == R.id.nav_financas) {
                startActivity(new Intent(this, FinanceiroActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            return true;
        });

        listaAgenda.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TarefaAdapter();
        listaAgenda.setAdapter(adapter);

        adapter.setOnTarefaLongClickListener(tarefa -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Apagar")
                    .setMessage("Deseja apagar este compromisso?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        deletarTarefa(tarefa);
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

        escutarAgendaFirestore();
    }

    private void escutarAgendaFirestore() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        firestoreDb.collection("users")
                .document(userId)
                .collection("tarefas")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIRESTORE_ERRO", "Erro ao carregar agenda", error);
                        return;
                    }
                    if (value != null) {
                        todasAsTarefas.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Tarefa t = doc.toObject(Tarefa.class);
                            if (t != null && "tarefa".equalsIgnoreCase(t.getTipo())) {
                                t.setDocId(doc.getId());
                                todasAsTarefas.add(t);
                            }
                        }

                        long date = calendarView.getDate();
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTimeInMillis(date);
                        String hoje = String.format(Locale.getDefault(), "%04d-%02d-%02d", 
                                cal.get(java.util.Calendar.YEAR), 
                                cal.get(java.util.Calendar.MONTH) + 1, 
                                cal.get(java.util.Calendar.DAY_OF_MONTH));

                        filtrarTarefasPorData(hoje);
                    }
                });
    }

    private void deletarTarefa(Tarefa tarefa) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        if (tarefa.getDocId() != null) {
            firestoreDb.collection("users")
                    .document(userId)
                    .collection("tarefas")
                    .document(tarefa.getDocId())
                    .delete();
        }
        new Thread(() -> tarefaDao.deletar(tarefa)).start();
    }

    private void filtrarTarefasPorData(String data) {
        List<Tarefa> filtradas = new ArrayList<>();
        for (Tarefa t : todasAsTarefas) {
            if (t.getData() != null && t.getData().equals(data)) {
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