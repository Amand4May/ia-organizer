package com.example.organizadoria;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

// Novos imports necessários para a internet e a IA
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private EditText inputComando;
    private Button btnEnviar;
    private RecyclerView listaTarefas;

    // COLE SUA CHAVE DENTRO DAS ASPAS AQUI
    private final String API_KEY = "CHAVE_REMOVIDA";

    private final String PROMPT_SISTEMA = "Você é um assistente financeiro. O usuário vai te mandar uma frase. " +
            "Sua única função é extrair os dados e me devolver EXATAMENTE um JSON, sem nenhuma outra palavra antes ou depois. " +
            "O JSON deve ter as chaves: 'tipo' (escreva 'despesa', 'receita' ou 'tarefa'), 'descricao', 'valor' (apenas numero), " +
            "e 'data' (formato YYYY-MM-DD, hoje é 2026-08-26).";

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputComando = findViewById(R.id.inputComando);
        btnEnviar = findViewById(R.id.btnEnviar);
        listaTarefas = findViewById(R.id.listaTarefas);

        // 1. Configurando a Base da API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // 2. Ação do Botão
        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textoDigitado = inputComando.getText().toString();
                if (!textoDigitado.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Pensando...", Toast.LENGTH_SHORT).show();
                    chamarIA(textoDigitado); // Chama o método que criamos abaixo
                    inputComando.setText("");
                } else {
                    Toast.makeText(MainActivity.this, "Digite algo primeiro!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 3. Método que monta o pacote de dados e manda pra internet
    private void chamarIA(String comandoUsuario) {
        // O Gemini exige que os dados sejam montados nessa estrutura específica de "parts" e "contents"
        JsonObject part = new JsonObject();
        part.addProperty("text", PROMPT_SISTEMA + "\nComando do usuário: " + comandoUsuario);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject corpoRequisicao = new JsonObject();
        corpoRequisicao.add("contents", contents);

        // 4. Disparando a requisição
        apiService.mandarParaIA(API_KEY, corpoRequisicao).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Navega no JSON de resposta do Gemini para extrair apenas o texto que queremos
                        String respostaIA = response.body()
                                .getAsJsonArray("candidates").get(0).getAsJsonObject()
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts").get(0).getAsJsonObject()
                                .get("text").getAsString();

                        // Mostra o JSON extraído na tela!
                        Toast.makeText(MainActivity.this, "IA: " + respostaIA, Toast.LENGTH_LONG).show();

                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Erro ao ler a resposta", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Erro na API: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Erro de conexão: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}