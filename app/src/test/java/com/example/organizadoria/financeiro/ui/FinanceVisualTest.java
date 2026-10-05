package com.example.organizadoria.financeiro.ui;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.example.organizadoria.AppServices;
import com.example.organizadoria.AppSession;
import com.example.organizadoria.BuildConfig;
import com.example.organizadoria.FinanceiroActivity;
import com.example.organizadoria.R;
import com.example.organizadoria.financeiro.data.FinanceRepository;
import com.example.organizadoria.financeiro.domain.FinanceState;
import com.example.organizadoria.financeiro.domain.Money;
import com.example.organizadoria.financeiro.domain.InvestmentProjection;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static com.example.organizadoria.financeiro.domain.FinanceEngine.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FinanceVisualTest {
 private FinanceRepository repo;private ActivityController<FinanceiroActivity> controller;private FinanceiroActivity activity;private final String today=LocalDate.now().toString();
 @Before public void prepare(){Assume.assumeTrue(BuildConfig.LOCAL_ONLY);AppServices.release();Context context=RuntimeEnvironment.getApplication();AppSession.enterLocal(context);File file=new File(context.getFilesDir(),"financeiro/"+AppSession.uid(context)+".bin");if(file.exists())assertTrue(file.delete());repo=AppServices.repository(context);repo.update(s->{configure(s,0,800000,300000,LocalDate.now().getDayOfMonth(),false,50000,0,8,today);settle(s,"salary","salary:"+today,300000,true,today);plan(s,"bills",FinanceState.Kind.EXPENSE,"Contas do ciclo",80000,today,CASH,null,null);addGoal(s,"Reserva de emergência",2000000,LocalDate.now().plusYears(2).toString(),today);});}
 private void launch(){controller=Robolectric.buildActivity(FinanceiroActivity.class).setup();activity=controller.get();idle();}
 private FinanceFragment fragment(){return (FinanceFragment)activity.getSupportFragmentManager().findFragmentById(R.id.financeContainer);}
 private void open(String page){activity.open(page);idle();}
 private void idle(){activity.getSupportFragmentManager().executePendingTransactions();shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(250));layout();shadowOf(Looper.getMainLooper()).idle();}
 private void layout(){View v=activity.getWindow().getDecorView();v.measure(View.MeasureSpec.makeMeasureSpec(393,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(852,View.MeasureSpec.EXACTLY));v.layout(0,0,393,852);}
 @After public void cleanup(){if(controller!=null)controller.pause().stop().destroy();AppServices.release();}
 @Test public void allPagesRenderWithoutChangingBalances()throws Exception {
  launch();long revision=repo.snapshot().revision;assertNotNull(find(activity.getWindow().getDecorView(),Money.format(170000)));capture("overview");
  for(String page:new String[]{"investments","cycle","goals","statement","planning","settings","accounts"}){open(page);capture(page);activity.getSupportFragmentManager().popBackStackImmediate();idle();}
  assertEquals(revision,repo.snapshot().revision);assertEquals(170000,summarize(repo.snapshot(),today).available);
 }
 @Test public void draftSurvivesRotationWithoutRecordingMoney()throws Exception {
  AppServices.release();Context context=RuntimeEnvironment.getApplication();File file=new File(context.getFilesDir(),"financeiro/"+AppSession.uid(context)+".bin");assertTrue(file.delete());repo=AppServices.repository(context);launch();open("setup");capture("onboarding");fragment().setup.putString("cash","100,00");click("Continuar");assertEquals(1,fragment().setupStep);controller.recreate();activity=controller.get();idle();assertEquals(1,fragment().setupStep);assertEquals("100,00",fragment().setup.getString("cash"));assertFalse(repo.snapshot().settings.configured);
 }
 @Test public void assistantAndScenarioOnlyPreview()throws Exception {
  launch();long revision=repo.snapshot().revision;open("assistant");click("Gastei R$40 no corte");click("Revisar com o Cogni");idle();assertNotNull(find(activity.getWindow().getDecorView(),"Confirmar registro"));assertEquals(revision,repo.snapshot().revision);capture("assistant");
  open("investments");FinanceFragment f=fragment();f.monthlyOverride=70000;f.projectionMonths=60;f.compareProjection=true;f.render();idle();capture("investment-comparison");assertEquals(revision,repo.snapshot().revision);assertEquals(800000,summarize(repo.snapshot(),today).invested);
  open("assistant");findEdit(activity.getWindow().getDecorView(),"O que aconteceu?").setText("Quanto terei em 5 anos?");click("Revisar com o Cogni");assertNotNull(find(activity.getWindow().getDecorView(),Money.format(4822695)));assertNull(find(activity.getWindow().getDecorView(),"Confirmar registro"));assertEquals(revision,repo.snapshot().revision);capture("simulation");
 }
 @Test public void cdiProfileUsesItsOwnRate()throws Exception {
  launch();open("investments");FinanceFragment f=fragment();f.cdi=new InvestmentProjection.Quote(today,.050788);f.cdiAttempted=true;repo.update(s->InvestmentProjection.profile(account(s,INVEST),InvestmentProjection.CDI,110));idle();assertNotNull(find(activity.getWindow().getDecorView(),"110,00% do CDI"));long revision=repo.snapshot().revision;android.widget.ScrollView scroll=activity.findViewById(R.id.financeScroll);scroll.scrollTo(0,360);idle();capture("cdi-chart");assertEquals(revision,repo.snapshot().revision);
 }
 @Test public void confirmedExpenseAndPartialContributionPersist()throws Exception {
  launch();open("assistant");click("Gastei R$40 no corte");click("Revisar com o Cogni");long revision=repo.snapshot().revision;click("Confirmar registro");assertEquals(revision,repo.snapshot().revision);confirmLatest();assertEquals(4000,summarize(repo.snapshot(),today).expenses);assertEquals(166000,summarize(repo.snapshot(),today).available);
  open("investments");click("Registrar aporte");AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();EditText amount=findEdit(dialog.getWindow().getDecorView(),"Valor realizado (R$)");assertNotNull(amount);amount.setText("350,00");confirmLatest();Summary result=summarize(repo.snapshot(),today);assertEquals(835000,result.invested);assertEquals(261000,result.cash);assertEquals(15000,result.pendingContributions);assertEquals(166000,result.available);assertEquals(4000,result.expenses);AppServices.release();FinanceState persisted=AppServices.repository(activity).snapshot();assertEquals(835000,summarize(persisted,today).invested);
 }
 private void confirmLatest()throws Exception {AlertDialog d=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(d);CountDownLatch saved=new CountDownLatch(1);Runnable listener=saved::countDown;repo.addListener(listener);try{d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertFalse(d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());assertTrue(saved.await(5,TimeUnit.SECONDS));}finally{repo.removeListener(listener);}idle();}
 private void click(String label){View v=find(activity.getWindow().getDecorView(),label);assertNotNull(label,v);assertTrue(v.performClick());idle();}
 private static View find(View root,String label){if(root instanceof TextView&&label.contentEquals(((TextView)root).getText()))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View result=find(g.getChildAt(i),label);if(result!=null)return result;}}return null;}
 private static EditText findEdit(View root,String hint){if(root instanceof EditText&&hint.contentEquals(((EditText)root).getHint()))return (EditText)root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){EditText result=findEdit(g.getChildAt(i),hint);if(result!=null)return result;}}return null;}
 private static void invalidateTree(View view){view.invalidate();if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)invalidateTree(g.getChildAt(i));}}
 private void capture(String name)throws Exception {layout();invalidateTree(activity.getWindow().getDecorView());Bitmap bitmap=Bitmap.createBitmap(393,852,Bitmap.Config.ARGB_8888);activity.getWindow().getDecorView().draw(new Canvas(bitmap));File dir=new File("build/finance-preview");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream stream=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}bitmap.recycle();}
}
