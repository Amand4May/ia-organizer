package com.example.organizadoria;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.organizadoria.financeiro.data.*;
import com.example.organizadoria.financeiro.domain.FinanceEngine;
import java.io.File;
import java.time.LocalDate;
import java.util.concurrent.*;

public final class AppServices {
    private static FinanceRepository repo;
    private static String owner;
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private AppServices() {}
    public static synchronized FinanceRepository repository(Context context) {
        String uid=AppSession.uid(context);
        if(uid==null) throw new IllegalStateException("Entre na sua conta novamente.");
        if(repo==null || !uid.equals(owner)) {
            release();owner=uid;
            if(BuildConfig.LOCAL_ONLY) repo=new FileFinanceRepository(new File(context.getFilesDir(),"financeiro/"+uid+".bin"),uid);
            else repo=new FirestoreFinanceRepository(uid);
        }
        return repo;
    }
    public static synchronized void release() {
        if(repo instanceof FirestoreFinanceRepository) ((FirestoreFinanceRepository)repo).close();
        repo=null;owner=null;
    }
    public interface Result { void done(Exception error); }
    public static void mutate(Context context,FinanceRepository.Mutation mutation,Result callback) {
        FinanceRepository target=repository(context);
        worker.execute(()->{
            Exception error=null;try {target.update(state->{
                String today=LocalDate.now().toString();
                if(state.settings.configured && !today.equals(state.settings.lastScheduledOn)) FinanceEngine.ensureSchedule(state,today);
                mutation.apply(state);
            });}catch(Exception e){error=e;}
            Exception result=error;main.post(()->callback.done(result));
        });
    }
}
