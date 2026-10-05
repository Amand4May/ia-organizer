package com.example.organizadoria;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.organizadoria.financeiro.data.*;
import com.example.organizadoria.financeiro.domain.*;
import com.google.firebase.FirebaseApp;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.example.organizadoria.financeiro.domain.FinanceEngine.*;
import static com.example.organizadoria.financeiro.domain.FinanceState.*;
import static org.junit.Assert.*;

/** Somente androidTestLocal: dados sintéticos, sem permissão INTERNET ou backend real. */
@RunWith(AndroidJUnit4.class)
public class FinanceLocalFlowTest {
    private Context context;private FinanceRepository repository;private String today;
    @Before public void seedLocalCopy() {
        assertTrue("Estes testes só podem rodar na variante local",BuildConfig.LOCAL_ONLY);
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();today=LocalDate.now().toString();
        AppServices.release();
        File data=new File(context.getFilesDir(),"financeiro/local-user.bin");
        assertTrue(!data.exists() || data.delete());
        AppSession.enterLocal(context);repository=AppServices.repository(context);
        repository.update(s->{
            configure(s,0,800000,300000,LocalDate.now().getDayOfMonth(),false,50000,0,8,today);
            settle(s,"salary-test","salary:"+today,300000,true,today);
            plan(s,"bills-test",Kind.EXPENSE,"Contas do ciclo",80000,LocalDate.now().plusDays(1).toString(),CASH,null,null);
        });
    }
    @After public void release() { AppServices.release(); }
    @Test public void localVariantHasNoNetworkOrFirebaseInitialization() {
        assertEquals("com.example.organizadoria.local",context.getPackageName());
        assertEquals(PackageManager.PERMISSION_DENIED,context.checkSelfPermission(Manifest.permission.INTERNET));
        assertTrue(repository instanceof FileFinanceRepository);assertTrue(FirebaseApp.getApps(context).isEmpty());
    }
    @Test public void mainRemainsEntryAndEveryFinancialPageCanReturn() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.inputComando)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_financas)).perform(click());
            onView(withText(Money.format(170000))).check(matches(isDisplayed()));
            String[][] pages={{"Ciclo","Ciclo financeiro"},{"Investimentos","Investimentos"},{"Metas","Metas"},{"Extrato","Extrato"},{"Planejamento","Planejamento"}};
            for(String[] page:pages) {
                onView(withText(page[0])).perform(scrollTo(),click());
                onView(withId(R.id.tituloFinanceiro)).check(matches(withText(page[1])));
                pressBack();onView(withId(R.id.tituloFinanceiro)).check(matches(withText("Financeiro")));
            }
            onView(withId(R.id.nav_agenda)).perform(click());onView(withId(R.id.calendarView)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_inicio)).perform(click());scenario.recreate();
            onView(withId(R.id.inputComando)).check(matches(isDisplayed()));
            assertEquals(170000,summarize(repository.snapshot(),today).available);
        }
    }
    @Test public void contributionFormPreservesAssetsAndReleasesOnlyPaidPart() throws Exception {
        try(ActivityScenario<FinanceiroActivity> scenario=ActivityScenario.launch(FinanceiroActivity.class)) {
            onView(withText("Investimentos")).perform(scrollTo(),click());
            onView(withText("Registrar aporte")).perform(scrollTo(),click());
            onView(withHint("Valor realizado (R$)")).perform(scrollTo(),replaceText("350,00"),closeSoftKeyboard());
            CountDownLatch saved=new CountDownLatch(1);
            Runnable listener=()->{if(summarize(repository.snapshot(),today).contributions==35000)saved.countDown();};
            repository.addListener(listener);
            try {
                onView(withText("Registrar")).perform(click());assertTrue("O aporte não foi salvo",saved.await(5,TimeUnit.SECONDS));
                onView(withId(R.id.tituloFinanceiro)).check(matches(withText("Investimentos")));
                Summary result=summarize(repository.snapshot(),today);
                assertEquals(15000,result.pendingContributions);assertEquals(170000,result.available);assertEquals(1100000,result.assets);
                scenario.recreate();onView(withId(R.id.tituloFinanceiro)).check(matches(withText("Investimentos")));
            }finally{repository.removeListener(listener);}
        }
    }
    @Test public void commandCreatesAgendaAndPlannedExpenseThenReconciles() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            sendCommand("Hoje vou cortar o cabelo às 9h por R$45",()->repository.snapshot().agenda.size()==1);
            assertEquals(84500,summarize(repository.snapshot(),today).committed);
            onView(withId(R.id.nav_agenda)).perform(click());onView(withText("Corte de cabelo")).check(matches(isDisplayed()));
            onView(withId(R.id.nav_inicio)).perform(click());
            sendCommand("Gastei R$40 no corte",()->summarize(repository.snapshot(),today).expenses==4000);
            Summary result=summarize(repository.snapshot(),today);assertEquals(80000,result.committed);assertEquals(166000,result.available);
        }
    }
    private void sendCommand(String text,java.util.function.BooleanSupplier condition)throws Exception {
        CountDownLatch saved=new CountDownLatch(1);Runnable listener=()->{if(condition.getAsBoolean())saved.countDown();};repository.addListener(listener);
        try {
            onView(withId(R.id.inputComando)).perform(replaceText(text),closeSoftKeyboard());onView(withId(R.id.btnEnviar)).perform(click());
            assertTrue("O comando não foi salvo",saved.await(5,TimeUnit.SECONDS));onView(withText("Entendi")).perform(click());
        }finally{repository.removeListener(listener);}
    }
}
