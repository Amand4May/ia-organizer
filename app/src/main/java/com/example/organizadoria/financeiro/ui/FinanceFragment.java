package com.example.organizadoria.financeiro.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.organizadoria.*;
import com.example.organizadoria.financeiro.data.*;
import com.example.organizadoria.financeiro.domain.*;
import java.time.LocalDate;
import android.widget.ScrollView;
import com.example.organizadoria.financeiro.network.CdiRepository;

public class FinanceFragment extends Fragment {
    private FinanceRepository repository;private LinearLayout content;
    private String page,scheduleAttemptDate;String filter="Tudo";String search="";String fromDate="",toDate="";
    private boolean refreshingSchedule;
    int projectionMonths=12,setupStep=0;long monthlyOverride=-1;boolean compareProjection;
    String investmentAccount=FinanceEngine.INVEST,assistantText="",assistantOperation;
    final Bundle setup=new Bundle();InvestmentProjection.Quote cdi;String cdiError;boolean cdiLoading,cdiAttempted;FinanceAssistant assistant;
    private final Runnable changed=()->{if(isAdded())requireActivity().runOnUiThread(()->{if(getView()!=null)render();});};
    public static FinanceFragment create(String page){FinanceFragment f=new FinanceFragment();Bundle b=new Bundle();b.putString("page",page);f.setArguments(b);return f;}
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){return inflater.inflate(R.layout.fragment_finance,parent,false);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        page=requireArguments().getString("page","overview");content=view.findViewById(R.id.financeContent);
        repository=AppServices.repository(requireContext());
        if(state!=null){filter=state.getString("filter","Tudo");search=state.getString("search","");fromDate=state.getString("fromDate","");toDate=state.getString("toDate","");}
        if(state!=null){projectionMonths=state.getInt("projectionMonths",12);monthlyOverride=state.getLong("monthlyOverride",-1);compareProjection=state.getBoolean("compareProjection");investmentAccount=state.getString("investmentAccount",FinanceEngine.INVEST);assistantText=state.getString("assistantText","");assistantOperation=state.getString("assistantOperation");setupStep=state.getInt("setupStep",0);Bundle draft=state.getBundle("setup");if(draft!=null)setup.putAll(draft);}
        cdi=new CdiRepository(requireContext()).cached();render();
    }
    @Override public void onStart(){super.onStart();scheduleAttemptDate=null;repository.addListener(changed);render();}
    @Override public void onStop(){repository.removeListener(changed);super.onStop();}
    @Override public void onSaveInstanceState(@NonNull Bundle state){super.onSaveInstanceState(state);state.putString("filter",filter);state.putString("search",search);state.putString("fromDate",fromDate);state.putString("toDate",toDate);state.putInt("projectionMonths",projectionMonths);state.putLong("monthlyOverride",monthlyOverride);state.putBoolean("compareProjection",compareProjection);state.putString("investmentAccount",investmentAccount);state.putString("assistantText",assistantText);state.putString("assistantOperation",assistantOperation);state.putInt("setupStep",setupStep);state.putBundle("setup",setup);}
    @Override public void onDestroyView(){if(assistant!=null)assistant.close();assistant=null;content=null;super.onDestroyView();}
    public String today(){return LocalDate.now().toString();}
    FinanceState state(){return repository.snapshot();}
    void open(String page){((FinanceiroActivity)requireActivity()).open(page);}
    void home(){((FinanceiroActivity)requireActivity()).home();}
    void title(String title){((FinanceiroActivity)requireActivity()).title(title);}
    void render(){
        if(content==null||assistant!=null&&"assistant".equals(page))return;ScrollView scroll=getView().findViewById(R.id.financeScroll);int position=scroll.getScrollY();content.removeAllViews();
        boolean ready=true;
        if(repository instanceof FirestoreFinanceRepository){String status=((FirestoreFinanceRepository)repository).statusMessage();if(status!=null){FinanceUi.note(content,status);ready=false;}}
        FinanceState current=state();FinanceScreens.render(this,content,current,page);
        scroll.post(()->{if(getView()!=null)scroll.scrollTo(0,position);});
        if(ready && !refreshingSchedule && current.settings.configured && !today().equals(current.settings.lastScheduledOn) && !today().equals(scheduleAttemptDate)) {
            scheduleAttemptDate=today();refreshingSchedule=true;
            AppServices.mutate(requireContext(),s->FinanceEngine.ensureSchedule(s,today()),e->{refreshingSchedule=false;if(isAdded() && e!=null)error(e);});
        }
    }
    void error(Exception e){if(isAdded())new AlertDialog.Builder(requireContext(),R.style.Theme_Finance_Dialog).setTitle("Não foi possível concluir").setMessage(e.getMessage()).setPositiveButton("Entendi",null).show();}
    void refreshCdi(boolean force){if(cdiLoading||!force&&cdiAttempted)return;cdiLoading=true;cdiAttempted=true;new CdiRepository(requireContext()).refresh(force,(quote,error)->{cdiLoading=false;cdi=quote;cdiError=error;if(isAdded()&&getView()!=null)render();});}
    void mutate(FinanceRepository.Mutation action,AlertDialog dialog){mutate(action,dialog,null);}
    void mutate(FinanceRepository.Mutation action,AlertDialog dialog,Runnable after){
        String operation;
        if(dialog!=null){Object tag=dialog.getWindow().getDecorView().getTag();operation=tag instanceof String?(String)tag:FinanceEngine.id();dialog.getWindow().getDecorView().setTag(operation);}
        else operation=FinanceEngine.id();
        if(dialog!=null){dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);dialog.setCancelable(false);}
        AppServices.mutate(requireContext(),state->{if(state.appliedCommands.contains(operation))return;action.apply(state);state.appliedCommands.add(operation);},e->{
            if(!isAdded())return;
            if(e==null){if(dialog!=null)dialog.dismiss();Toast.makeText(requireContext(),"Salvo",Toast.LENGTH_SHORT).show();if(after!=null)after.run();else render();}
            else {if(dialog!=null){dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);dialog.setCancelable(true);}error(e);}
        });
    }
}
