package com.example.organizadoria.financeiro.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.app.DatePickerDialog;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import com.example.organizadoria.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.*;
import java.util.*;

final class FinanceUi {
    static final int WHITE=Color.rgb(227,227,227), MUTED=Color.rgb(156,164,181), BLUE=Color.rgb(41,98,255),
            GREEN=Color.rgb(0,230,118), ORANGE=Color.rgb(255,152,0), RED=Color.rgb(255,100,100), CYAN=Color.rgb(0,229,255);
    static final int SURFACE=Color.rgb(20,28,42),BORDER=Color.rgb(36,49,70);
    static GradientDrawable rounded(int color,int radius,Context c){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(c,radius));return b;}
    static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    static TextView text(Context c,String value,int size,int color,boolean bold){
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(null,Typeface.BOLD);t.setLineSpacing(dp(c,2),1);return t;
    }
    static void add(LinearLayout parent,View view,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(parent.getContext(),bottom);parent.addView(view,p);}
    static LinearLayout card(LinearLayout parent){
        Context c=parent.getContext();MaterialCardView card=new MaterialCardView(c);card.setCardBackgroundColor(SURFACE);
        card.setRadius(dp(c,24));card.setCardElevation(0);card.setStrokeColor(BORDER);card.setStrokeWidth(dp(c,1));
        LinearLayout inside=column(c);inside.setPadding(dp(c,20),dp(c,20),dp(c,20),dp(c,20));card.addView(inside);add(parent,card,14);return inside;
    }
    static void heading(LinearLayout parent,String title){TextView t=text(parent.getContext(),title,18,WHITE,true);t.setPadding(0,dp(parent.getContext(),12),0,0);add(parent,t,12);}
    static void note(LinearLayout p,String value){add(p,text(p.getContext(),value,13,MUTED,false),12);}
    static void runAction(Context context,Runnable action){
        try{action.run();}catch(IllegalArgumentException|IllegalStateException e){
            new AlertDialog.Builder(context,R.style.Theme_Finance_Dialog).setTitle("Confira os dados").setMessage(e.getMessage()).setPositiveButton("Entendi",null).show();
        }
    }
    static MaterialButton button(LinearLayout p,String title,Runnable action,boolean primary){
        Context c=p.getContext();MaterialButton b=new MaterialButton(c);b.setText(title);b.setTextSize(14);b.setAllCaps(false);b.setCornerRadius(dp(c,16));b.setMinHeight(dp(c,52));b.setInsetTop(0);b.setInsetBottom(0);
        b.setBackgroundTintList(ColorStateList.valueOf(primary?BLUE:Color.rgb(30,34,45)));b.setTextColor(primary?Color.WHITE:WHITE);
        b.setOnClickListener(v->runAction(c,action));add(p,b,8);return b;
    }
    static void metric(LinearLayout p,String title,String value,int color){
        LinearLayout box=card(p);add(box,text(p.getContext(),title,13,MUTED,false),6);add(box,text(p.getContext(),value,24,color,true),0);
    }
    static void row(LinearLayout p,String title,String detail,String value,int color,Runnable action){
        LinearLayout box=card(p);Context c=p.getContext();
        LinearLayout top=new LinearLayout(c);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text(c,title,15,WHITE,true);top.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView amount=text(c,value,14,color,true);amount.setPadding(dp(c,8),0,0,0);amount.setMaxWidth(dp(c,155));top.addView(amount);add(box,top,4);
        add(box,text(c,detail,12,MUTED,false),0);
        if(action!=null){View clickable=(View)box.getParent();clickable.setOnClickListener(v->runAction(c,action));clickable.setFocusable(true);clickable.setContentDescription(title+", "+detail+", "+value);}
    }
    static void gridMetrics(LinearLayout parent,String[] titles,String[] values,int[] colors){
        for(int i=0;i<titles.length;i+=2){
            LinearLayout row=new LinearLayout(parent.getContext());
            for(int j=i;j<Math.min(i+2,titles.length);j++){
                LinearLayout cell=column(parent.getContext());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(j==i)p.rightMargin=dp(parent.getContext(),8);else p.leftMargin=dp(parent.getContext(),8);row.addView(cell,p);
                LinearLayout box=card(cell);add(box,text(parent.getContext(),titles[j],12,MUTED,false),8);add(box,text(parent.getContext(),values[j],18,colors[j],true),0);
            }
            add(parent,row,0);
        }
    }
    static LinearLayout hero(LinearLayout p){LinearLayout box=card(p);GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(20,51,81),Color.rgb(15,30,50)});bg.setCornerRadius(dp(p.getContext(),24));box.setBackground(bg);return box;}
    static void eyebrow(LinearLayout p,String value){TextView t=text(p.getContext(),value,11,CYAN,true);t.setLetterSpacing(.12f);add(p,t,10);}
    static void pill(LinearLayout p,String value,int color){TextView t=text(p.getContext(),value,12,color,true);t.setPadding(dp(p.getContext(),12),dp(p.getContext(),7),dp(p.getContext(),12),dp(p.getContext(),7));t.setBackground(rounded(Color.rgb(25,40,56),20,p.getContext()));add(p,t,12);}
    static void section(LinearLayout p,String title,String action,Runnable callback){LinearLayout line=new LinearLayout(p.getContext());line.setGravity(Gravity.CENTER_VERTICAL);line.addView(text(p.getContext(),title,17,WHITE,true),new LinearLayout.LayoutParams(0,-2,1));if(callback!=null){TextView link=text(p.getContext(),action,12,CYAN,true);link.setMinHeight(dp(p.getContext(),48));link.setGravity(Gravity.CENTER);link.setPadding(dp(p.getContext(),12),0,0,0);link.setOnClickListener(v->runAction(p.getContext(),callback));line.addView(link);}add(p,line,8);}
    static void segments(LinearLayout p,String[] labels,int selected,java.util.function.IntConsumer select){HorizontalScrollView scroll=new HorizontalScrollView(p.getContext());scroll.setHorizontalScrollBarEnabled(false);LinearLayout line=new LinearLayout(p.getContext());for(int i=0;i<labels.length;i++){final int index=i;TextView t=text(p.getContext(),labels[i],13,i==selected?WHITE:MUTED,i==selected);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(p.getContext(),48));t.setPadding(dp(p.getContext(),16),0,dp(p.getContext(),16),0);t.setBackground(rounded(i==selected?BLUE:SURFACE,14,p.getContext()));t.setSelected(i==selected);t.setFocusable(true);t.setContentDescription(labels[i]+(i==selected?", selecionado":""));t.setOnClickListener(v->runAction(p.getContext(),()->select.accept(index)));LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-2,-2);size.rightMargin=dp(p.getContext(),8);line.addView(t,size);}scroll.addView(line);add(p,scroll,16);}
    static void navigation(LinearLayout p,String[] titles,String[] subtitles,String[] icons,int[] colors,Runnable[] actions){for(int i=0;i<titles.length;i+=2){LinearLayout line=new LinearLayout(p.getContext());for(int j=i;j<Math.min(i+2,titles.length);j++){LinearLayout slot=column(p.getContext());LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-2,1);if(j==i)size.rightMargin=dp(p.getContext(),6);else size.leftMargin=dp(p.getContext(),6);line.addView(slot,size);LinearLayout box=card(slot);box.setPadding(dp(p.getContext(),16),dp(p.getContext(),16),dp(p.getContext(),16),dp(p.getContext(),16));add(box,text(p.getContext(),icons[j],24,colors[j],true),10);add(box,text(p.getContext(),titles[j],15,WHITE,true),4);add(box,text(p.getContext(),subtitles[j],11,MUTED,false),0);Runnable action=actions[j];View target=(View)box.getParent();target.setOnClickListener(v->runAction(p.getContext(),action));target.setFocusable(true);target.setContentDescription(titles[j]+", "+subtitles[j]);}add(p,line,0);}}
    static void inlineMetrics(LinearLayout parent,String[] titles,String[] values,int[] colors){LinearLayout line=new LinearLayout(parent.getContext());for(int i=0;i<titles.length;i++){LinearLayout cell=column(parent.getContext());LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-2,1);if(i>0)size.leftMargin=dp(parent.getContext(),12);line.addView(cell,size);add(cell,text(parent.getContext(),titles[i],11,MUTED,false),6);add(cell,text(parent.getContext(),values[i],16,colors[i],true),0);}add(parent,line,16);}
    static final class Form {
        final Context context;final LinearLayout body;final ScrollView scroll;
        final Map<String,TextInputEditText> inputs=new LinkedHashMap<>();
        Form(Context c){context=new ContextThemeWrapper(c,R.style.Theme_Finance_Dialog);body=column(context);body.setPadding(dp(c,20),dp(c,12),dp(c,20),dp(c,12));scroll=new ScrollView(context);scroll.addView(body);}
        TextInputEditText field(String key,String label,String value,int type){
            TextInputLayout wrap=new TextInputLayout(context);wrap.setHint(label);wrap.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);wrap.setBoxBackgroundColor(Color.rgb(12,20,33));wrap.setBoxCornerRadii(dp(context,14),dp(context,14),dp(context,14),dp(context,14));wrap.setDefaultHintTextColor(ColorStateList.valueOf(MUTED));wrap.setHintTextColor(ColorStateList.valueOf(CYAN));wrap.setBoxStrokeColor(CYAN);
            TextInputEditText edit=new TextInputEditText(wrap.getContext());edit.setTextColor(WHITE);edit.setInputType(type);edit.setText(value);edit.setSingleLine(true);
            edit.setId(0x01000000+(key.hashCode()&0x00ffffff));wrap.addView(edit);add(body,wrap,10);inputs.put(key,edit);return edit;
        }
        String value(String key){return inputs.get(key).getText().toString().trim();}
        void textField(String key,String label,String value){field(key,label,value,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        void moneyField(String key,String label,String value){field(key,label,value,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);}
        void numberField(String key,String label,String value){field(key,label,value,InputType.TYPE_CLASS_NUMBER);}
        void dateField(String key,String label,String value){TextInputEditText edit=field(key,label.replace(" (AAAA-MM-DD)",""),value,InputType.TYPE_CLASS_DATETIME|InputType.TYPE_DATETIME_VARIATION_DATE);edit.setFocusable(false);edit.setOnClickListener(v->{java.time.LocalDate date;try{date=java.time.LocalDate.parse(value(key));}catch(Exception e){date=java.time.LocalDate.now();}DatePickerDialog picker=new DatePickerDialog(context,(view,y,m,d)->edit.setText(java.time.LocalDate.of(y,m+1,d).toString()),date.getYear(),date.getMonthValue()-1,date.getDayOfMonth());if(label.contains("opcional"))picker.setButton(android.content.DialogInterface.BUTTON_NEUTRAL,"Limpar",(d,w)->edit.setText(""));picker.show();});}
        CheckBox check(String label,boolean checked){CheckBox b=new CheckBox(context);b.setText(label);b.setTextColor(WHITE);b.setChecked(checked);add(body,b,10);return b;}
        Spinner select(String label,List<String> options){
            add(body,text(context,label,13,MUTED,false),4);Spinner spinner=new Spinner(context);
            ArrayAdapter<String> adapter=new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,options){
                @Override public View getView(int position,View convert,ViewGroup parent){TextView v=(TextView)super.getView(position,convert,parent);v.setTextColor(WHITE);v.setPadding(dp(context,8),dp(context,12),dp(context,8),dp(context,12));return v;}
                @Override public View getDropDownView(int position,View convert,ViewGroup parent){TextView v=(TextView)super.getDropDownView(position,convert,parent);v.setTextColor(WHITE);v.setBackgroundColor(Color.rgb(30,34,45));v.setPadding(dp(context,12),dp(context,14),dp(context,12),dp(context,14));return v;}
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);add(body,spinner,12);return spinner;
        }
        AlertDialog dialog(String title,String save,java.util.function.Consumer<AlertDialog> onSave){
            AlertDialog dialog=new AlertDialog.Builder(context).setTitle(title).setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton(save,null).create();
            dialog.setOnShowListener(v->{if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawable(rounded(SURFACE,28,context));dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);dialog.getWindow().setLayout(Math.min(context.getResources().getDisplayMetrics().widthPixels-dp(context,32),dp(context,560)),WindowManager.LayoutParams.WRAP_CONTENT);}dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(CYAN);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(MUTED);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
                try{onSave.accept(dialog);}catch(Exception e){new AlertDialog.Builder(context).setMessage(e.getMessage()).setPositiveButton("Entendi",null).show();}
            });});dialog.show();return dialog;
        }
    }
}
