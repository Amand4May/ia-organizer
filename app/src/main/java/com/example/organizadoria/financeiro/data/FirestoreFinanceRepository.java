package com.example.organizadoria.financeiro.data;

import com.example.organizadoria.BuildConfig;
import com.example.organizadoria.financeiro.domain.FinanceState;
import com.example.organizadoria.financeiro.domain.FinanceEngine;
import com.example.organizadoria.financeiro.domain.FinanceState.*;
import com.google.firebase.firestore.*;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Adaptador conectado. Documentos por entidade, revisão global e transação com detecção de conflito. */
public final class FirestoreFinanceRepository implements FinanceRepository {
    private final FirebaseFirestore db;
    private final CollectionReference collection;
    private final Gson gson=new Gson();
    private final String uid;
    private FinanceState state;
    private boolean loaded;
    private String loadError;
    private final Set<Runnable> listeners=new HashSet<>();
    private final ListenerRegistration registration;
    public FirestoreFinanceRepository(String uid) {
        if(BuildConfig.LOCAL_ONLY) throw new IllegalStateException("Firebase desativado na cópia local.");
        this.uid=uid;state=FinanceState.empty(uid);db=FirebaseFirestore.getInstance();
        collection=db.collection("users").document(uid).collection("financeiro_v1");
        registration=collection.addSnapshotListener((snapshot,error)->{
            if(error!=null){synchronized(this){loadError=error.getMessage();}notifyListeners();return;}
            if(snapshot==null || snapshot.getMetadata().hasPendingWrites())return;
            try {
                FinanceState next=decode(snapshot);
                synchronized(this) {
                    if(!loaded || next.revision>=state.revision) state=next;
                    // Ausência só é confirmada pelo servidor; cache vazio não significa conta vazia.
                    loaded=!snapshot.isEmpty() || !snapshot.getMetadata().isFromCache();loadError=null;
                }
                notifyListeners();
            } catch(Exception e) { synchronized(this){loadError="Falha ao ler os dados financeiros.";}notifyListeners(); }
        });
    }
    @Override public synchronized FinanceState snapshot() { return state.copy(); }
    public synchronized String statusMessage() { return loadError!=null?loadError:loaded?null:"Carregando os dados financeiros…"; }
    @Override public void update(Mutation mutation) {
        FinanceState before;
        synchronized(this) {
            if(!loaded || loadError!=null) throw new IllegalStateException(loadError!=null?loadError:"Aguarde o carregamento dos dados.");
            before=state.copy();
        }
        FinanceState next=before.copy();mutation.apply(next);
        if(!uid.equals(next.userId))throw new IllegalStateException("A operação não pode alterar o usuário.");
        FinanceEngine.validateState(next);next.revision=Math.addExact(before.revision,1);
        Map<String,Map<String,Object>> previous=encode(before), current=encode(next);
        Map<String,Map<String,Object>> changed=new HashMap<>();
        for(String key:current.keySet()) if(!current.get(key).equals(previous.get(key)))changed.put(key,current.get(key));
        Set<String> deleted=new HashSet<>(previous.keySet());deleted.removeAll(current.keySet());
        if(changed.size()+deleted.size()>450) throw new IllegalStateException("Muitas alterações de uma vez. Reduza o período ou a quantidade de contas.");
        try {
            Tasks.await(db.runTransaction(tx->{
                DocumentSnapshot meta=tx.get(collection.document("meta"));
                long revision=meta.exists() && meta.getLong("revision")!=null?meta.getLong("revision"):0;
                if(revision!=before.revision) throw new FirebaseFirestoreException("Dados alterados em outro aparelho. Atualize e tente novamente.",FirebaseFirestoreException.Code.ABORTED);
                for(Map.Entry<String,Map<String,Object>> e:changed.entrySet()) tx.set(collection.document(e.getKey()),e.getValue());
                for(String id:deleted)tx.delete(collection.document(id));
                return null;
            }),25,TimeUnit.SECONDS);
            synchronized(this){if(next.revision>=state.revision)state=next;}
            notifyListeners();
        } catch(Exception e) { throw new IllegalStateException("Não foi possível confirmar a gravação. Aguarde a sincronização antes de repetir o comando.",e); }
    }
    private Map<String,Map<String,Object>> encode(FinanceState s) {
        Map<String,Map<String,Object>> docs=new HashMap<>();
        Map<String,Object> meta=new HashMap<>();meta.put("type","meta");meta.put("revision",s.revision);
        meta.put("schemaVersion",1);meta.put("settings",gson.toJson(s.settings));meta.put("activeCycleId",s.activeCycleId);docs.put("meta",meta);
        for(Account a:s.accounts)put(docs,"account",a.id,a);
        for(Forecast a:s.forecasts)put(docs,"forecast",a.id,a);
        for(Entry a:s.entries)put(docs,"entry",a.id,a);
        for(Rule a:s.rules)put(docs,"rule",a.id,a);
        for(Cycle a:s.cycles)put(docs,"cycle",a.id,a);
        for(Goal a:s.goals)put(docs,"goal",a.id,a);
        for(Allocation a:s.allocations)put(docs,"allocation",a.id,a);
        for(AgendaItem a:s.agenda)put(docs,"agenda",a.id,a);
        for(LegacyItem a:s.legacy)put(docs,"legacy",a.id,a);
        for(String a:s.appliedCommands)put(docs,"command",a,a);
        for(String a:s.importedLegacyIds)put(docs,"import",a,a);
        return docs;
    }
    private void put(Map<String,Map<String,Object>> out,String type,String id,Object value) {
        Map<String,Object> data=new HashMap<>();data.put("type",type);data.put("payload",gson.toJson(value));
        // IDs são internos; legados são codificados para nunca virar um caminho arbitrário.
        String safe=android.util.Base64.encodeToString(id.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
        out.put(type+"_"+safe,data);
    }
    private FinanceState decode(QuerySnapshot snapshot) {
        FinanceState s=FinanceState.empty(uid);
        for(DocumentSnapshot d:snapshot.getDocuments()) {
            String type=d.getString("type"), p=d.getString("payload");if(type==null)continue;
            switch(type) {
                case "meta":
                    if(!Long.valueOf(1).equals(d.getLong("schemaVersion")))throw new IllegalStateException("Versão desconhecida");
                    s.revision=d.getLong("revision");s.settings=gson.fromJson(d.getString("settings"),Settings.class);s.activeCycleId=d.getString("activeCycleId");break;
                case "account":s.accounts.add(gson.fromJson(p,Account.class));break;
                case "forecast":s.forecasts.add(gson.fromJson(p,Forecast.class));break;
                case "entry":s.entries.add(gson.fromJson(p,Entry.class));break;
                case "rule":s.rules.add(gson.fromJson(p,Rule.class));break;
                case "cycle":s.cycles.add(gson.fromJson(p,Cycle.class));break;
                case "goal":s.goals.add(gson.fromJson(p,Goal.class));break;
                case "allocation":s.allocations.add(gson.fromJson(p,Allocation.class));break;
                case "agenda":s.agenda.add(gson.fromJson(p,AgendaItem.class));break;
                case "legacy":s.legacy.add(gson.fromJson(p,LegacyItem.class));break;
                case "command":s.appliedCommands.add(gson.fromJson(p,String.class));break;
                case "import":s.importedLegacyIds.add(gson.fromJson(p,String.class));break;
            }
        }
        FinanceEngine.validateState(s);return s;
    }
    private void notifyListeners(){List<Runnable> copy;synchronized(this){copy=new ArrayList<>(listeners);}for(Runnable r:copy)try{r.run();}catch(RuntimeException ignored){/* A gravação confirmada não depende da interface. */}}
    @Override public synchronized void addListener(Runnable listener){listeners.add(listener);}
    @Override public synchronized void removeListener(Runnable listener){listeners.remove(listener);}
    public void close(){registration.remove();}
}
