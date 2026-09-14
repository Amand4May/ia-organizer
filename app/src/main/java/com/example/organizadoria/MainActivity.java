package com.example.organizadoria;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    private EditText inputComando;
    private ImageButton btnEnviar;
    private RecyclerView listaTarefas;

    private ApiService apiService;
    private TarefaAdapter tarefaAdapter;

    // Variáveis do Banco de Dados
    private AppDatabase db;
    private TarefaDao tarefaDao;
    private FirebaseFirestore firestoreDb;

    private String getPromptSistema() {
        String hoje = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return "Você é um organizador pessoal inteligente. O usuário vai te mandar uma frase e você deve extrair os dados. " +
                "Sua única função é devolver EXATAMENTE um JSON ARRAY (uma lista []), sem nenhuma outra palavra. " +
                "Cada objeto da lista deve ter as chaves: " +
                "'tipo' (escreva 'tarefa' para compromissos; 'despesa' para gastos; 'receita' para ganhos), " +
                "'descricao', 'valor' (apenas numero), " +
                "'data' (formato YYYY-MM-DD), " +
                "'horario' (formato HH:mm). " +
                "Se o usuário mencionar um valor para um compromisso (ex: dentista 200 reais), coloque o valor tanto na 'tarefa' quanto na 'despesa'. " +
                "Se não houver valor, use 0. " +
                "Hoje é " + hoje + ".";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_inicio);

        inputComando = findViewById(R.id.inputComando);
        btnEnviar = findViewById(R.id.btnEnviar);
        listaTarefas = findViewById(R.id.listaTarefas);

        firestoreDb = FirebaseFirestore.getInstance();

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_agenda) {
                startActivity(new Intent(this, AgendaActivity.class));
                overridePendingTransition(0, 0);
            } else if (id == R.id.nav_financas) {
                startActivity(new Intent(this, FinanceiroActivity.class));
                overridePendingTransition(0, 0);
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
            }
            return true;
        });

        listaTarefas.setLayoutManager(new LinearLayoutManager(this));
        tarefaAdapter = new TarefaAdapter();
        listaTarefas.setAdapter(tarefaAdapter);

        tarefaAdapter.setOnTarefaLongClickListener(tarefa -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Apagar")
                    .setMessage("Deseja apagar este item?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        deletarTarefa(tarefa);
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });

        // INICIALIZAÇÃO DO BANCO DE DADOS LOCAL
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "banco_organizadoria")
                .fallbackToDestructiveMigration()
                .build();
        tarefaDao = db.tarefaDao();

        // ESCUTAR TAREFAS DA NUVEM (FIRESTORE) EM TEMPO REAL
        escutarTarefasFirestore();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.groq.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        btnEnviar.setOnClickListener(v -> {
            String textoDigitado = inputComando.getText().toString();
            if (!textoDigitado.isEmpty()) {
                Toast.makeText(MainActivity.this, "Processando...", Toast.LENGTH_SHORT).show();
                chamarIA(textoDigitado);
                inputComando.setText("");
            }
        });

        TextView labelVerTudo = findViewById(R.id.labelVerTudo);
        if (labelVerTudo != null) {
            labelVerTudo.setOnClickListener(v -> {
                startActivity(new Intent(this, AgendaActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (getIntent().getBooleanExtra("focarInput", false)) {
            inputComando.requestFocus();
        }
    }

    private void escutarTarefasFirestore() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        firestoreDb.collection("users")
                .document(userId)
                .collection("tarefas")
                .orderBy("data", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIRESTORE_ERRO", "Erro ao carregar do Firestore", error);
                        return;
                    }
                    if (value != null) {
                        List<Tarefa> lista = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Tarefa t = doc.toObject(Tarefa.class);
                            if (t != null) {
                                t.setDocId(doc.getId());
                                lista.add(t);
                            }
                        }
                        tarefaAdapter.carregarListaCompleta(lista);
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

        // Também apaga localmente
        new Thread(() -> tarefaDao.deletar(tarefa)).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarSaudacao();
    }

    private void atualizarSaudacao() {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hora = cal.get(java.util.Calendar.HOUR_OF_DAY);
        String periodo;
        if (hora >= 5 && hora < 12) {
            periodo = "BOM DIA";
        } else if (hora >= 12 && hora < 18) {
            periodo = "BOA TARDE";
        } else {
            periodo = "BOA NOITE";
        }

        TextView textBomDia = findViewById(R.id.textBomDia);
        if (textBomDia != null) {
            textBomDia.setText(periodo);
        }

        if (currentUserId == null) return;
        
        String nomeUsuario = getSharedPreferences("DadosPerfil_" + currentUserId, MODE_PRIVATE).getString("nome", "");
        TextView textSaudacao = findViewById(R.id.textSaudacao);
        
        if (!nomeUsuario.isEmpty()) {
            String primeiroNome = nomeUsuario.split(" ")[0];
            textSaudacao.setText("Vamos organizar,\n" + primeiroNome + "?");
        } else {
            textSaudacao.setText("Vamos organizar?");
        }
    }

    private void verificarEAtualizarAssinaturaNoPerfil(String tipo, String descricao, double valor) {
        if (tipo == null || descricao == null || valor <= 0) return;

        String descLower = descricao.toLowerCase();
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        android.content.SharedPreferences prefs = getSharedPreferences("DadosPerfil_" + currentUserId, MODE_PRIVATE);

        // 1. Assinaturas
        boolean isAssinatura = descLower.contains("assinatura") || descLower.contains("assinar") || 
                               descLower.contains("mensalidade") || descLower.contains("plano") || 
                               descLower.contains("fixo") || descLower.contains("aluguel") || 
                               descLower.contains("internet") || descLower.contains("luz") || 
                               descLower.contains("água") || descLower.contains("curso") || 
                               descLower.contains("spotify") || descLower.contains("netflix") || 
                               descLower.contains("prime") || descLower.contains("hbo") || 
                               descLower.contains("disney") || descLower.contains("clube") || 
                               descLower.contains("academia");

        if (tipo.equalsIgnoreCase("despesa") && isAssinatura) {
            String assinaturasAtuaisStr = prefs.getString("assinaturas", "0").replace(",", ".");
            double assinaturasAtuais = 0;
            try { assinaturasAtuais = Double.parseDouble(assinaturasAtuaisStr.isEmpty() ? "0" : assinaturasAtuaisStr); } catch (Exception ignored) {}
            double novoTotal = assinaturasAtuais + valor;
            String valStr = String.format(Locale.US, "%.2f", novoTotal);
            prefs.edit().putString("assinaturas", valStr).apply();
            firestoreDb.collection("users").document(currentUserId).update("assinaturas", valStr);
        }

        // 2. Salário / Renda
        boolean isSalario = tipo.equalsIgnoreCase("receita") || 
                            descLower.contains("salário") || descLower.contains("salario") || 
                            descLower.contains("renda") || descLower.contains("pagamento");

        if (isSalario) {
            String rendaAtualStr = prefs.getString("renda", "0").replace(",", ".");
            double rendaAtual = 0;
            try { rendaAtual = Double.parseDouble(rendaAtualStr.isEmpty() ? "0" : rendaAtualStr); } catch (Exception ignored) {}
            double novoTotal = rendaAtual + valor;
            String valStr = String.format(Locale.US, "%.2f", novoTotal);
            prefs.edit().putString("renda", valStr).apply();
            firestoreDb.collection("users").document(currentUserId).update("renda", valStr);
        }

        // 3. Investimentos
        boolean isInvestimento = descLower.contains("investimento") || descLower.contains("investir") || 
                                descLower.contains("aporte") || descLower.contains("poupança") || 
                                descLower.contains("poupanca") || descLower.contains("ações") || 
                                descLower.contains("cdb") || descLower.contains("tesouro");

        if (isInvestimento) {
            String investAtualStr = prefs.getString("investimentos", "0").replace(",", ".");
            double investAtual = 0;
            try { investAtual = Double.parseDouble(investAtualStr.isEmpty() ? "0" : investAtualStr); } catch (Exception ignored) {}
            double novoTotal = investAtual + valor;
            String valStr = String.format(Locale.US, "%.2f", novoTotal);
            prefs.edit().putString("investimentos", valStr).apply();
            firestoreDb.collection("users").document(currentUserId).update("investimentos", valStr);
        }
    }

    private void chamarIA(String comandoUsuario) {
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", getPromptSistema());
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", comandoUsuario);
        messages.add(userMessage);

        JsonObject corpoRequisicao = new JsonObject();
        corpoRequisicao.addProperty("model", "openai/gpt-oss-120b");
        corpoRequisicao.add("messages", messages);

        String apiKey = BuildConfig.GROQ_API_KEY != null ? BuildConfig.GROQ_API_KEY.trim() : "";
        String tokenAuth = "Bearer " + apiKey;

        apiService.mandarParaIA(tokenAuth, corpoRequisicao).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String respostaIA = response.body()
                                .getAsJsonArray("choices").get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString();

                        respostaIA = respostaIA.replace("'", "\"");
                        if (respostaIA.contains("```")) {
                            respostaIA = respostaIA.replaceAll("```json", "").replaceAll("```", "").trim();
                        }
                        com.google.gson.JsonElement element = new JsonParser().parse(respostaIA);
                        JsonArray jsonArray;
                        
                        if (element.isJsonArray()) {
                            jsonArray = element.getAsJsonArray();
                        } else {
                            jsonArray = new JsonArray();
                            jsonArray.add(element.getAsJsonObject());
                        }

                        String userId = FirebaseAuth.getInstance().getUid();

                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject jsonRecebido = jsonArray.get(i).getAsJsonObject();
                            String tipo = jsonRecebido.get("tipo").getAsString();
                            String descricao = jsonRecebido.get("descricao").getAsString();
                            if (descricao != null && !descricao.trim().isEmpty()) {
                                descricao = descricao.trim();
                                descricao = descricao.substring(0, 1).toUpperCase() + descricao.substring(1);
                            }
                            double valor = jsonRecebido.get("valor").getAsDouble();
                            String data = jsonRecebido.get("data").getAsString();
                            String horario = jsonRecebido.has("horario") ? jsonRecebido.get("horario").getAsString() : "09:00";

                            // Verificar se é uma assinatura para atualizar o Perfil
                            verificarEAtualizarAssinaturaNoPerfil(tipo, descricao, valor);

                            Tarefa novaTarefa = new Tarefa(userId, tipo, descricao, valor, data, horario);

                            // SALVAR NA NUVEM (FIRESTORE)
                            if (userId != null) {
                                firestoreDb.collection("users")
                                        .document(userId)
                                        .collection("tarefas")
                                        .add(novaTarefa);
                            }

                            // SALVA TAMBÉM LOCALMENTE
                            new Thread(() -> tarefaDao.inserir(novaTarefa)).start();
                        }

                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Erro ao ler JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ERRO_JSON", "Falha no parser", e);
                    }
                } else {
                    try {
                        String errBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        Log.e("ERRO_GROQ", "HTTP " + response.code() + ": " + errBody);
                        Toast.makeText(MainActivity.this, "Erro de API (" + response.code() + "): " + errBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e("ERRO_GROQ", "Erro de API", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Erro de conexão", Toast.LENGTH_LONG).show();
            }
        });
    }
}