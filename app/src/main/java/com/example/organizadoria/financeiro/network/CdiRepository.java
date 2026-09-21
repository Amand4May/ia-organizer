package com.example.organizadoria.financeiro.network;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.example.organizadoria.BuildConfig;
import com.example.organizadoria.financeiro.domain.InvestmentProjection.Quote;
import java.time.LocalDate;
import java.util.concurrent.*;
public final class CdiRepository {
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();private static final Handler main=new Handler(Looper.getMainLooper());
 private final SharedPreferences cache;
 public interface Result{void done(Quote quote,String error);}
 public CdiRepository(Context c){cache=c.getApplicationContext().getSharedPreferences("finance_cdi_public_v1",Context.MODE_PRIVATE);}
 public Quote cached(){try{String date=cache.getString("date",null),daily=cache.getString("daily",null);if(date==null||daily==null)return null;Quote q=new Quote(date,Double.parseDouble(daily));q.validateOn(LocalDate.now().toString());return q;}catch(Exception e){return null;}}
 public void refresh(boolean force,Result callback){
  if(BuildConfig.LOCAL_ONLY){callback.done(null,"A cópia local não acessa a internet. Escolha uma taxa personalizada para testar; o CDI automático está na variante conectada.");return;}
  Quote previous=cached();long age=System.currentTimeMillis()-cache.getLong("fetched",0);
  if(!force&&previous!=null&&!previous.stale(LocalDate.now().toString())&&age>=0&&age<21600000){callback.done(previous,null);return;}
  worker.execute(()->{Quote quote;String error=null;try{quote=BcbCdiService.fetch(LocalDate.now().toString());cache.edit().putString("date",quote.date).putString("daily",Double.toString(quote.dailyPercent)).putLong("fetched",System.currentTimeMillis()).apply();}catch(Exception e){quote=cached();error=quote==null?"Não foi possível consultar o CDI. Tente novamente quando estiver conectado.":"Sem atualização. Usando a taxa salva na data indicada.";}Quote result=quote;String message=error;main.post(()->callback.done(result,message));});
 }
}
