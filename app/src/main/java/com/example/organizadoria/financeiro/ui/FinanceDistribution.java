package com.example.organizadoria.financeiro.ui;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import static com.example.organizadoria.financeiro.ui.FinanceUi.*;
final class FinanceDistribution extends View {
 private final long[] amounts;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 FinanceDistribution(Context c,long free,long bills,long contributions,long goals){super(c);amounts=new long[]{Math.max(0,free),bills,contributions,goals};setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),dp(getContext(),9));}
 @Override protected void onDraw(Canvas c){double total=0;for(long value:amounts)total+=value;paint.setColor(BORDER);c.drawRoundRect(new RectF(0,0,getWidth(),getHeight()),getHeight()/2f,getHeight()/2f,paint);if(total<=0)return;int[] colors={CYAN,ORANGE,BLUE,GREEN};float x=0;for(int i=0;i<amounts.length;i++){float width=(float)(getWidth()*amounts[i]/total);if(width<=0)continue;paint.setColor(colors[i]);c.drawRoundRect(new RectF(x,0,Math.max(x,x+width-dp(getContext(),2)),getHeight()),getHeight()/2f,getHeight()/2f,paint);x+=width;}}
}
