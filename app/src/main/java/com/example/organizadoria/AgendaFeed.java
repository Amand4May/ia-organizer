package com.example.organizadoria;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import com.example.organizadoria.financeiro.data.FinanceRepository;
import com.example.organizadoria.financeiro.domain.*;
import com.google.firebase.firestore.*;
import java.util.*;
import java.time.LocalDate;
import java.util.function.Consumer;

/** Une compromissos novos e legados sem expor lançamentos financeiros na Agenda. */
public final class AgendaFeed {
    private final Activity activity;private final FinanceRepository repo;private final Consumer<List<Tarefa>> consumer;
    private final List<Tarefa> legacy=new ArrayList<>();private ListenerRegistration cloud;private volatile boolean active;
    private final Runnable listener=()->publish();
    public AgendaFeed(Activity activity,Consumer<List<Tarefa>> consumer){this.activity=activity;this.consumer=consumer;repo=AppServices.repository(activity);}
    public void start(){
        active=true;repo.addListener(listener);publish();
        if(!BuildConfig.LOCAL_ONLY){
            String uid=AppSession.uid(activity);if(uid==null)return;
            cloud=FirebaseFirestore.getInstance().collection("users").document(uid).collection("tarefas").addSnapshotListener((value,error)->{
                if(!active)return;
                if(error!=null){android.widget.Toast.makeText(activity,"Não foi possível atualizar os compromissos antigos.",android.widget.Toast.LENGTH_SHORT).show();return;}
                synchronized(legacy){legacy.clear();if(value!=null)for(DocumentSnapshot doc:value.getDocuments()){
                    Tarefa t=doc.toObject(Tarefa.class);if(t!=null&&"tarefa".equalsIgnoreCase(t.tipo)){t.setDocId("legacy:"+doc.getId());legacy.add(t);}
                }}publish();
            });
        }
    }
    public void stop(){active=false;repo.removeListener(listener);if(cloud!=null){cloud.remove();cloud=null;}}
    private void publish(){
        if(!active)return;
        List<Tarefa> tasks=new ArrayList<>();synchronized(legacy){tasks.addAll(legacy);}
        FinanceState state=repo.snapshot();
        for(FinanceState.AgendaItem a:state.agenda)if(!a.cancelled){
            Tarefa t=new Tarefa(state.userId,"tarefa",a.title,0,a.date,a.time);t.setDocId("v1:"+a.id);tasks.add(t);
        }
        tasks.sort(Comparator.comparing((Tarefa t)->t.data==null?"":t.data).thenComparing(t->t.horario==null?"":t.horario));
        activity.runOnUiThread(()->{if(active&&!activity.isFinishing())consumer.accept(tasks);});
    }
    public void delete(Tarefa task){
        String id=task.getDocId();if(id==null)return;
        boolean[] cancelLinked={true};AlertDialog.Builder dialog=new AlertDialog.Builder(activity).setTitle("Cancelar compromisso").setMessage("Deseja cancelar este compromisso?");
        if(id.startsWith("v1:")){
            boolean linked=false;for(FinanceState.AgendaItem a:repo.snapshot().agenda)if(("v1:"+a.id).equals(id)&&a.forecastId!=null)linked=true;
            if(linked)dialog.setMessage(null).setMultiChoiceItems(new String[]{"Cancelar também a despesa ainda prevista"},cancelLinked,(d,index,checked)->cancelLinked[0]=checked);
            dialog.setPositiveButton("Cancelar compromisso",(d,w)->AppServices.mutate(activity,s->FinanceEngine.cancelAgenda(s,id.substring(3),cancelLinked[0],LocalDate.now().toString()),error->{
                if(error!=null&&!activity.isFinishing())new AlertDialog.Builder(activity).setMessage(error.getMessage()).setPositiveButton("Entendi",null).show();
            }));
        }else if(!BuildConfig.LOCAL_ONLY){
            String uid=AppSession.uid(activity);if(uid==null)return;
            dialog.setPositiveButton("Cancelar compromisso",(d,w)->FirebaseFirestore.getInstance().collection("users").document(uid).collection("tarefas").document(id.substring(7)).delete().addOnFailureListener(e->{if(!activity.isFinishing())new AlertDialog.Builder(activity).setMessage("Não foi possível cancelar.").setPositiveButton("Entendi",null).show();}));
        }
        dialog.setNegativeButton("Voltar",null).show();
    }
}
