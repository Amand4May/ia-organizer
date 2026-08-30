package com.example.organizadoria;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import java.util.List;
import java.util.Locale;

public class FinanceiroActivity extends AppCompatActivity {

    private RecyclerView listaFinancas;
    private TextView textSalario, textGastosFixos, textInvestimentos;
    private TarefaAdapter adapter;
    private AppDatabase db;
    private TarefaDao tarefaDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financeiro);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        listaFinancas = findViewById(R.id.listaFinancas);
        textSalario = findViewById(R.id.textSalario);
        textGastosFixos = findViewById(R.id.textGastosFixos);
        textInvestimentos = findViewById(R.id.textInvestimentos);

        btnVoltar.setOnClickListener(v -> finish());

        listaFinancas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TarefaAdapter();
        listaFinancas.setAdapter(adapter);

        adapter.setOnTarefaLongClickListener(tarefa -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Apagar")
                    .setMessage("Deseja apagar este registro financeiro?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        new Thread(() -> {
                            tarefaDao.deletar(tarefa);
                            carregarFinancas();
                        }).start();
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "banco_organizadoria")
                .fallbackToDestructiveMigration()
                .build();
        tarefaDao = db.tarefaDao();

        carregarFinancas();
    }

    private void carregarFinancas() {
        new Thread(() -> {
            List<Tarefa> financas = tarefaDao.buscarApenasFinancas();
            
            double salario = 0;
            double gastos = 0;
            double investimentos = 0;

            for (Tarefa f : financas) {
                String desc = f.getDescricao().toLowerCase();
                if (f.getTipo().equalsIgnoreCase("receita")) {
                    salario += f.getValor();
                } else if (f.getTipo().equalsIgnoreCase("despesa")) {
                    if (desc.contains("investimento") || desc.contains("aporte") || desc.contains("poupança")) {
                        investimentos += f.getValor();
                    } else {
                        gastos += f.getValor();
                    }
                }
            }
            
            double finalSalario = salario;
            double finalGastos = gastos;
            double finalInvest = investimentos;

            runOnUiThread(() -> {
                adapter.carregarListaCompleta(financas);
                textSalario.setText(String.format(Locale.getDefault(), "R$ %.2f", finalSalario));
                textGastosFixos.setText(String.format(Locale.getDefault(), "R$ %.2f", finalGastos));
                textInvestimentos.setText(String.format(Locale.getDefault(), "R$ %.2f", finalInvest));
            });
        }).start();
    }
}