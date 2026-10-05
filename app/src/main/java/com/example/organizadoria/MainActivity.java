package com.example.organizadoria;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.*;
import androidx.recyclerview.widget.*;
import com.example.organizadoria.financeiro.domain.*;
import com.google.gson.*;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import java.time.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private EditText input;private ImageButton send;private TarefaAdapter adapter;private AgendaFeed feed;
    private Call<JsonObject> request;private boolean sending;private String pendingOperation,pendingText;
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);setContentView(R.layout.activity_main);if(!Navigation.requireSession(this))return;
        Navigation.bind(this,R.id.nav_inicio);input=findViewById(R.id.inputComando);send=findViewById(R.id.btnEnviar);
        if(saved!=null){pendingOperation=saved.getString("pendingOperation");pendingText=saved.getString("pendingText");}
        RecyclerView list=findViewById(R.id.listaTarefas);list.setLayoutManager(new LinearLayoutManager(this));adapter=new TarefaAdapter();list.setAdapter(adapter);
        feed=new AgendaFeed(this,tasks->{List<Tarefa> upcoming=new ArrayList<>();String today=LocalDate.now().toString();for(Tarefa t:tasks)if(t.data!=null&&t.data.compareTo(today)>=0)upcoming.add(t);adapter.carregarListaCompleta(upcoming);});
        adapter.setOnTarefaLongClickListener(t->feed.delete(t));
        findViewById(R.id.labelVerTudo).setOnClickListener(v->startActivity(new Intent(this,AgendaActivity.class)));
        send.setOnClickListener(v->submit());if(getIntent().getBooleanExtra("focarInput",false))input.requestFocus();
    }
    @Override protected void onStart(){super.onStart();if(feed!=null)feed.start();}
    @Override protected void onStop(){if(feed!=null)feed.stop();super.onStop();}
    @Override protected void onResume(){super.onResume();if(!Navigation.requireSession(this))return;Navigation.bind(this,R.id.nav_inicio);greeting();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(input!=null&&intent.getBooleanExtra("focarInput",false))input.requestFocus();}
    @Override protected void onDestroy(){if(request!=null)request.cancel();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("pendingOperation",pendingOperation);state.putString("pendingText",pendingText);}
    private void greeting(){
        int hour=LocalTime.now().getHour();((TextView)findViewById(R.id.textBomDia)).setText(hour>=5&&hour<12?"BOM DIA":hour>=12&&hour<18?"BOA TARDE":"BOA NOITE");
        String uid=AppSession.uid(this);String name=getSharedPreferences("DadosPerfil_"+uid,MODE_PRIVATE).getString("nome","").trim();
        ((TextView)findViewById(R.id.textSaudacao)).setText(name.isEmpty()?"Vamos organizar?":"Vamos organizar,\n"+name.split("\\s+")[0]+"?");
    }
    private void busy(boolean value){sending=value;send.setEnabled(!value);input.setEnabled(!value);send.setAlpha(value?.4f:1);}
    private void failure(String text){if(isFinishing()||isDestroyed())return;busy(false);new AlertDialog.Builder(this).setTitle("Não foi possível registrar").setMessage(text).setPositiveButton("Entendi",null).show();}
    private void submit(){
        if(sending)return;String text=input.getText().toString().trim();if(text.isEmpty())return;
        if(!text.equals(pendingText) || pendingOperation==null){pendingText=text;pendingOperation=FinanceEngine.id();}
        String today=LocalDate.now().toString(),operation=pendingOperation,uid=AppSession.uid(this);busy(true);
        if(BuildConfig.LOCAL_ONLY){try{execute(LocalCommandParser.parse(text,today),operation,uid,today);}catch(Exception e){failure(e.getMessage());}return;}
        String key=BuildConfig.GROQ_API_KEY.trim();if(key.isEmpty()){failure("A chave da IA não está configurada nesta instalação. Você pode registrar os dados pelas telas do Financeiro.");return;}
        JsonArray messages=new JsonArray();JsonObject system=new JsonObject();system.addProperty("role","system");system.addProperty("content",RemoteCommandCodec.prompt(today));messages.add(system);
        JsonObject user=new JsonObject();user.addProperty("role","user");user.addProperty("content",text);messages.add(user);
        JsonObject body=new JsonObject();body.addProperty("model","openai/gpt-oss-120b");body.addProperty("temperature",0);body.add("messages",messages);
        ApiService api=new Retrofit.Builder().baseUrl("https://api.groq.com/").addConverterFactory(GsonConverterFactory.create()).build().create(ApiService.class);
        request=api.mandarParaIA("Bearer "+key,body);request.enqueue(new Callback<JsonObject>(){
            @Override public void onResponse(Call<JsonObject> call,Response<JsonObject> response){
                if(isDestroyed()||!Objects.equals(uid,AppSession.uid(MainActivity.this)))return;
                if(!response.isSuccessful()||response.body()==null){failure("A IA não respondeu corretamente (HTTP "+response.code()+"). Seu texto foi preservado.");return;}
                try{String content=response.body().getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();execute(RemoteCommandCodec.parse(content,today),operation,uid,today);}
                catch(Exception e){failure("Não consegui validar a resposta da IA: "+e.getMessage());}
            }
            @Override public void onFailure(Call<JsonObject> call,Throwable error){if(!call.isCanceled())failure("Não foi possível conectar à IA. Seu texto foi preservado.");}
        });
    }
    private void execute(List<OrganizerCommand> commands,String operation,String uid,String today){
        if(!Objects.equals(uid,AppSession.uid(this))){failure("A sessão mudou. Entre novamente.");return;}
        final List<String> messages=new ArrayList<>();
        AppServices.mutate(this,state->messages.addAll(CommandRouter.apply(state,commands,operation,today)),error->{
            if(isDestroyed()||isFinishing()||!Objects.equals(uid,AppSession.uid(this)))return;
            if(error instanceof CommandRouter.AmbiguousMatch){
                CommandRouter.AmbiguousMatch match=(CommandRouter.AmbiguousMatch)error;FinanceState s=AppServices.repository(this).snapshot();
                String[] options=new String[match.forecastIds.size()];for(int i=0;i<options.length;i++){FinanceState.Forecast p=FinanceEngine.forecast(s,match.forecastIds.get(i));options[i]=p.title+" · "+p.date+" · "+Money.format(FinanceEngine.remaining(s,p));}
                new AlertDialog.Builder(this).setTitle("Qual previsão você quer confirmar?").setItems(options,(d,index)->{commands.get(match.commandIndex).targetId=match.forecastIds.get(index);execute(commands,operation,uid,today);}).setNegativeButton("Voltar",(d,w)->busy(false)).setOnCancelListener(d->busy(false)).show();return;
            }
            if(error!=null){failure(error.getMessage());return;}
            busy(false);input.setText("");pendingOperation=null;pendingText=null;new AlertDialog.Builder(this).setTitle("Cogni").setMessage(android.text.TextUtils.join("\n\n",messages)).setPositiveButton("Entendi",null).show();
        });
    }
}
