package com.example.organizadoria;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FinanceiroActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private RecyclerView listaFinancas;
    private TextView textSalario, textGastosFixos, textInvestimentos, textGastosMes;
    private TarefaAdapter adapter;
    private AppDatabase db;
    private TarefaDao tarefaDao;
    private FirebaseFirestore firestoreDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financeiro);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_financas);

        listaFinancas = findViewById(R.id.listaFinancas);
        textSalario = findViewById(R.id.textSalario);
        textGastosFixos = findViewById(R.id.textGastosFixos);
        textInvestimentos = findViewById(R.id.textInvestimentos);
        textGastosMes = findViewById(R.id.textGastosMes);

        firestoreDb = FirebaseFirestore.getInstance();

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_agenda) {
                startActivity(new Intent(this, AgendaActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_financas) {
                // Já está no financeiro
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            return true;
        });

        listaFinancas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TarefaAdapter();
        listaFinancas.setAdapter(adapter);

        adapter.setOnTarefaLongClickListener(tarefa -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Apagar")
                    .setMessage("Deseja apagar este registro financeiro?")
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

        escutarFinancasFirestore();
    }

    private void escutarFinancasFirestore() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        firestoreDb.collection("users")
                .document(currentUserId)
                .collection("tarefas")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIRESTORE_ERRO", "Erro ao carregar finanças", error);
                        return;
                    }
                    if (value != null) {
                        List<Tarefa> financas = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Tarefa t = doc.toObject(Tarefa.class);
                            if (t != null && !"tarefa".equalsIgnoreCase(t.getTipo())) {
                                t.setDocId(doc.getId());
                                financas.add(t);
                            }
                        }
                        processarEExibirFinancas(financas, currentUserId);
                    }
                });
    }

    private void processarEExibirFinancas(List<Tarefa> financas, String currentUserId) {
        // Buscar dados do Perfil (SharedPreferences)
        android.content.SharedPreferences prefs = getSharedPreferences("DadosPerfil_" + currentUserId, MODE_PRIVATE);
        String rendaStr = prefs.getString("renda", "0").replace(",", ".");
        String investStr = prefs.getString("investimentos", "0").replace(",", ".");
        String assinaturasStr = prefs.getString("assinaturas", "0").replace(",", ".");

        double salarioBase = 0;
        double investBase = 0;
        double assinaturasBase = 0;
        try {
            salarioBase = Double.parseDouble(rendaStr.isEmpty() ? "0" : rendaStr);
            investBase = Double.parseDouble(investStr.isEmpty() ? "0" : investStr);
            assinaturasBase = Double.parseDouble(assinaturasStr.isEmpty() ? "0" : assinaturasStr);
        } catch (NumberFormatException ignored) {}

        double salario = salarioBase;
        double assinaturas = assinaturasBase;
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
                           desc.contains("plano") || desc.contains("fixo") || desc.contains("curso") || 
                           desc.contains("spotify") || desc.contains("netflix") || desc.contains("assinar") || 
                           desc.contains("prime") || desc.contains("hbo") || desc.contains("disney") || 
                           desc.contains("clube") || desc.contains("academia")) {
                    // Já no assinaturasBase do Perfil
                } else {
                    gastosMes += f.getValor();
                }
            }
        }

        double finalSalario = salario;
        double finalAssinaturas = assinaturas;
        double finalInvest = investimentos;
        double finalGastosMes = gastosMes;
        double finalSaldo = salario - (assinaturas + gastosMes);

        runOnUiThread(() -> {
            adapter.carregarListaCompleta(financas);

            TextView textSaldoDisponivel = findViewById(R.id.textSaldoDisponivel);
            if (textSaldoDisponivel != null) {
                textSaldoDisponivel.setText(String.format(Locale.getDefault(), "R$ %.2f", finalSaldo));
            }
            TextView badgeSalarioResumo = findViewById(R.id.badgeSalarioResumo);
            if (badgeSalarioResumo != null) {
                badgeSalarioResumo.setText(String.format(Locale.getDefault(), "Salário +R$%.0f", finalSalario));
            }
            TextView badgeAssinaturasResumo = findViewById(R.id.badgeAssinaturasResumo);
            if (badgeAssinaturasResumo != null) {
                badgeAssinaturasResumo.setText(String.format(Locale.getDefault(), "Assinaturas -R$%.0f", finalAssinaturas));
            }

            textSalario.setText(String.format(Locale.getDefault(), "R$ %.2f", finalSalario));
            textGastosFixos.setText(String.format(Locale.getDefault(), "R$ %.2f", finalAssinaturas));
            textInvestimentos.setText(String.format(Locale.getDefault(), "R$ %.2f", finalInvest));
            textGastosMes.setText(String.format(Locale.getDefault(), "R$ %.2f", finalGastosMes));
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
}