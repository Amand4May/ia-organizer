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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.List;

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

    private final String PROMPT_SISTEMA = "Você é um assistente financeiro. O usuário vai te mandar uma frase. " +
            "Sua única função é extrair os dados e me devolver EXATAMENTE um JSON, sem nenhuma outra palavra antes ou depois. " +
            "O JSON deve ter as chaves: 'tipo' (escreva 'despesa', 'receita' ou 'tarefa'), 'descricao', 'valor' (apenas numero), " +
            "e 'data' (formato YYYY-MM-DD, hoje é 2026-08-28).";

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

        listaTarefas.setLayoutManager(new LinearLayoutManager(this));
        tarefaAdapter = new TarefaAdapter();
        listaTarefas.setAdapter(tarefaAdapter);

        // INICIALIZAÇÃO DO BANCO DE DADOS
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "banco_organizadoria").build();
        tarefaDao = db.tarefaDao();

        // BUSCA AS TAREFAS SALVAS AO ABRIR O APP
        new Thread(() -> {
            List<Tarefa> tarefasSalvas = tarefaDao.buscarTodas();
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

    private void chamarIA(String comandoUsuario) {
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", PROMPT_SISTEMA);
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
                        JsonObject jsonRecebido = new JsonParser().parse(respostaIA).getAsJsonObject();

                        String tipo = jsonRecebido.get("tipo").getAsString();
                        String descricao = jsonRecebido.get("descricao").getAsString();
                        double valor = jsonRecebido.get("valor").getAsDouble();
                        String data = jsonRecebido.get("data").getAsString();

                        Tarefa novaTarefa = new Tarefa(tipo, descricao, valor, data);

                        // SALVA A TAREFA NO BANCO ANTES DE MOSTRAR NA TELA
                        new Thread(() -> {
                            tarefaDao.inserir(novaTarefa);
                            runOnUiThread(() -> tarefaAdapter.adicionarTarefa(novaTarefa));
                        }).start();

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