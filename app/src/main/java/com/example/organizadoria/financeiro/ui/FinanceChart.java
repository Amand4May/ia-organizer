package com.example.organizadoria.financeiro.ui;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.MotionEvent;
import com.example.organizadoria.financeiro.domain.Money;
import java.util.Locale;
import static com.example.organizadoria.financeiro.ui.FinanceUi.*;
/** Desenha os pontos reais do domínio; permite consultar a curva sem alterar a simulação. */
final class FinanceChart extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final double[] values,capital,alternative;private final String unit;private final java.util.function.IntConsumer selected;
 private int cursor=-1;private float touchX,touchY;private boolean dragging;
 FinanceChart(Context c,double[] v,double[] capital,double[] alt,java.util.function.IntConsumer selected){this(c,v,capital,alt,"meses",selected);}
 FinanceChart(Context c,double[] v,double[] capital,double[] alt,String unit,java.util.function.IntConsumer selected){super(c);values=v;this.capital=capital;alternative=alt;this.unit=unit;this.selected=selected;setFocusable(true);setContentDescription("Evolução estimada em "+(v.length-1)+" "+unit+". Inicial "+Money.format(Math.round(v[0]*100))+". Final "+Money.format(Math.round(v[v.length-1]*100))+". Consulte os detalhes abaixo.");}
 @Override protected void onMeasure(int width,int height){setMeasuredDimension(MeasureSpec.getSize(width),resolveSize(dp(getContext(),230),height));}
 private float left(){return dp(getContext(),8);}private float right(){return getWidth()-dp(getContext(),8);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(values.length<2)return;float top=dp(getContext(),28),bottom=getHeight()-dp(getContext(),30),left=left(),right=right();double min=0,max=1;for(double[] points:new double[][]{values,capital,alternative})if(points!=null)for(double v:points){min=Math.min(min,v);max=Math.max(max,v);}double range=Math.max(1,max-min);paint.setShader(null);paint.setColor(BORDER);paint.setStrokeWidth(dp(getContext(),1));for(int i=0;i<4;i++){float y=top+(bottom-top)*i/3;c.drawLine(left,y,right,y,paint);}paint.setTextSize(dp(getContext(),10));paint.setColor(MUTED);c.drawText(compact(max),left,top-dp(getContext(),10),paint);c.drawText("Hoje",left,getHeight()-dp(getContext(),6),paint);String end=(values.length-1)+" "+unit;c.drawText(end,right-paint.measureText(end),getHeight()-dp(getContext(),6),paint);
  Path area=path(values,min,range,top,bottom,left,right);area.lineTo(right,bottom);area.lineTo(left,bottom);area.close();paint.setShader(new LinearGradient(0,top,0,bottom,Color.argb(65,0,229,255),Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawPath(area,paint);paint.setShader(null);line(c,capital,min,range,top,bottom,left,right,MUTED,1.5f);line(c,alternative,min,range,top,bottom,left,right,BLUE,2);line(c,values,min,range,top,bottom,left,right,CYAN,2.5f);
  if(cursor>=0){float x=left+(right-left)*cursor/(values.length-1),y=(float)(bottom-(values[cursor]-min)/range*(bottom-top));paint.setColor(WHITE);paint.setStrokeWidth(dp(getContext(),1));c.drawLine(x,top,x,bottom,paint);c.drawCircle(x,y,dp(getContext(),4),paint);}
 }
 private Path path(double[] data,double min,double range,float top,float bottom,float left,float right){Path p=new Path();for(int i=0;i<data.length;i++){float x=left+(right-left)*i/(data.length-1),y=(float)(bottom-(data[i]-min)/range*(bottom-top));if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}return p;}
 private void line(Canvas c,double[] points,double min,double range,float top,float bottom,float left,float right,int color,float width){if(points==null)return;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width*getResources().getDisplayMetrics().density);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setColor(color);c.drawPath(path(points,min,range,top,bottom,left,right),paint);paint.setStyle(Paint.Style.FILL);}
 private static String compact(double v){return Math.abs(v)>=1000000?String.format(new Locale("pt","BR"),"R$ %.1f mi",v/1000000):Math.abs(v)>=1000?String.format(new Locale("pt","BR"),"R$ %.1f mil",v/1000):Money.format(Math.round(v*100));}
 private void select(float x){cursor=Math.max(0,Math.min(values.length-1,Math.round((x-left())/Math.max(1,right()-left())*(values.length-1))));if(selected!=null)selected.accept(cursor);invalidate();}
 @Override public boolean onTouchEvent(MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:touchX=e.getX();touchY=e.getY();dragging=false;return true;case MotionEvent.ACTION_MOVE:if(Math.abs(e.getX()-touchX)>dp(getContext(),10)&&Math.abs(e.getX()-touchX)>Math.abs(e.getY()-touchY)){dragging=true;getParent().requestDisallowInterceptTouchEvent(true);select(e.getX());}return true;case MotionEvent.ACTION_UP:if(dragging||Math.abs(e.getY()-touchY)<dp(getContext(),10)){select(e.getX());performClick();}getParent().requestDisallowInterceptTouchEvent(false);return true;case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;default:return super.onTouchEvent(e);}}
 @Override public boolean performClick(){super.performClick();return true;}
}
