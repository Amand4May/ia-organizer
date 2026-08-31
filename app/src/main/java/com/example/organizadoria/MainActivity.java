package com.example.organizadoria;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.google.android.material.navigation.NavigationView;
import android.content.Intent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnAbrirMenu;
    private NavigationView navView;

    private EditText inputComando;
    private ImageButton btnEnviar;
    private RecyclerView listaTarefas;

    private ApiService apiService;
    private TarefaAdapter tarefaAdapter;

    // Variáveis do Banco de Dados
    private AppDatabase db;
    private TarefaDao tarefaDao;

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

        drawerLayout = findViewById(R.id.drawerLayout);
        btnAbrirMenu = findViewById(R.id.btnAbrirMenu);
        navView = findViewById(R.id.navView);

        inputComando = findViewById(R.id.inputComando);
        btnEnviar = findViewById(R.id.btnEnviar);
        listaTarefas = findViewById(R.id.listaTarefas);

        btnAbrirMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_agenda) {
                startActivity(new Intent(this, AgendaActivity.class));
            } else if (id == R.id.nav_financas) {
                startActivity(new Intent(this, FinanceiroActivity.class));
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
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
                        new Thread(() -> {
                            tarefaDao.deletar(tarefa);
                            List<Tarefa> atualizada = tarefaDao.buscarTodas(FirebaseAuth.getInstance().getUid());
                            runOnUiThread(() -> tarefaAdapter.carregarListaCompleta(atualizada));
                        }).start();
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });

        // INICIALIZAÇÃO DO BANCO DE DADOS
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "banco_organizadoria")
                .fallbackToDestructiveMigration()
                .build();
        tarefaDao = db.tarefaDao();

        // BUSCA AS TAREFAS SALVAS AO ABRIR O APP
        new Thread(() -> {
            List<Tarefa> tarefasSalvas = tarefaDao.buscarTodas(FirebaseAuth.getInstance().getUid());
            runOnUiThread(() -> tarefaAdapter.carregarListaCompleta(tarefasSalvas));
        }).start();

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarSaudacao();
    }

    private void atualizarSaudacao() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;
        
        String nomeUsuario = getSharedPreferences("DadosPerfil_" + currentUserId, MODE_PRIVATE).getString("nome", "");
        TextView textSaudacao = findViewById(R.id.textSaudacao);
        
        if (!nomeUsuario.isEmpty()) {
            String primeiroNome = nomeUsuario.split(" ")[0];
            textSaudacao.setText("Vamos organizar, " + primeiroNome + "?");
        } else {
            textSaudacao.setText("Vamos organizar?");
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

        String tokenAuth = "Bearer " + BuildConfig.GROQ_API_KEY;

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
                        com.google.gson.JsonElement element = new JsonParser().parse(respostaIA);
                        JsonArray jsonArray;
                        
                        if (element.isJsonArray()) {
                            jsonArray = element.getAsJsonArray();
                        } else {
                            jsonArray = new JsonArray();
                            jsonArray.add(element.getAsJsonObject());
                        }

                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject jsonRecebido = jsonArray.get(i).getAsJsonObject();
                            String tipo = jsonRecebido.get("tipo").getAsString();
                            String descricao = jsonRecebido.get("descricao").getAsString();
                            double valor = jsonRecebido.get("valor").getAsDouble();
                            String data = jsonRecebido.get("data").getAsString();
                            String horario = jsonRecebido.has("horario") ? jsonRecebido.get("horario").getAsString() : "09:00";

                            Tarefa novaTarefa = new Tarefa(FirebaseAuth.getInstance().getUid(), tipo, descricao, valor, data, horario);

                            // SALVA NO BANCO E ATUALIZA A TELA
                            new Thread(() -> {
                                tarefaDao.inserir(novaTarefa);
                                runOnUiThread(() -> tarefaAdapter.adicionarTarefa(novaTarefa));
                            }).start();
                        }

                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Erro ao ler JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ERRO_JSON", "Falha no parser", e);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Erro de API. Olhe o Logcat.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Erro de conexão", Toast.LENGTH_LONG).show();
            }
        });
    }
}