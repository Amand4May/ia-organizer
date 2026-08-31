package com.example.organizadoria;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;
import java.util.Locale;

public class FinanceiroActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView listaFinancas;
    private TextView textSalario, textGastosFixos, textInvestimentos, textGastosMes;
    private TarefaAdapter adapter;
    private AppDatabase db;
    private TarefaDao tarefaDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financeiro);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton btnAbrirMenu = findViewById(R.id.btnAbrirMenu);
        NavigationView navView = findViewById(R.id.navView);
        listaFinancas = findViewById(R.id.listaFinancas);
        textSalario = findViewById(R.id.textSalario);
        textGastosFixos = findViewById(R.id.textGastosFixos);
        textInvestimentos = findViewById(R.id.textInvestimentos);
        textGastosMes = findViewById(R.id.textGastosMes);

        btnAbrirMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        if (navView != null) {
            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_inicio) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else if (id == R.id.nav_agenda) {
                    startActivity(new Intent(this, AgendaActivity.class));
                    finish();
                } else if (id == R.id.nav_financas) {
                    // Já está no financeiro
                } else if (id == R.id.nav_perfil) {
                    startActivity(new Intent(this, PerfilActivity.class));
                    finish();
                }
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return true;
            });
        }

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
            String currentUserId = FirebaseAuth.getInstance().getUid();
            List<Tarefa> financas = tarefaDao.buscarApenasFinancas(currentUserId);
            
            // Buscar dados do Perfil (SharedPreferences)
            android.content.SharedPreferences prefs = getSharedPreferences("DadosPerfil_" + currentUserId, MODE_PRIVATE);
            String rendaStr = prefs.getString("renda", "0").replace(",", ".");
            String investStr = prefs.getString("investimentos", "0").replace(",", ".");
            
            double salarioBase = 0;
            double investBase = 0;
            try {
                salarioBase = Double.parseDouble(rendaStr.isEmpty() ? "0" : rendaStr);
                investBase = Double.parseDouble(investStr.isEmpty() ? "0" : investStr);
            } catch (NumberFormatException e) {
                // Caso o usuário digite algo inválido
            }

            double salario = salarioBase;
            double assinaturas = 0;
            double investimentos = investBase;
            double gastosMes = 0;

            for (Tarefa f : financas) {
                String desc = f.getDescricao().toLowerCase();
                if (f.getTipo().equalsIgnoreCase("receita")) {
                    salario += f.getValor();
                } else if (f.getTipo().equalsIgnoreCase("despesa")) {
                    if (desc.contains("investimento") || desc.contains("aporte") || desc.contains("poupança")) {
                        investimentos += f.getValor();
                    } else if (desc.contains("aluguel") || desc.contains("internet") || desc.contains("luz") || 
                               desc.contains("água") || desc.contains("assinatura") || desc.contains("mensalidade") || 
                               desc.contains("plano") || desc.contains("fixo")) {
                        assinaturas += f.getValor();
                    } else {
                        gastosMes += f.getValor();
                    }
                }
            }
            
            double finalSalario = salario;
            double finalAssinaturas = assinaturas;
            double finalInvest = investimentos;
            double finalGastosMes = gastosMes;

            runOnUiThread(() -> {
                adapter.carregarListaCompleta(financas);
                textSalario.setText(String.format(Locale.getDefault(), "R$ %.2f", finalSalario));
                textGastosFixos.setText(String.format(Locale.getDefault(), "R$ %.2f", finalAssinaturas));
                textInvestimentos.setText(String.format(Locale.getDefault(), "R$ %.2f", finalInvest));
                textGastosMes.setText(String.format(Locale.getDefault(), "R$ %.2f", finalGastosMes));
            });
        }).start();
    }
}