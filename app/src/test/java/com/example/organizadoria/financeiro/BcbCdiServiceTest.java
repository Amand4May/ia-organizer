package com.example.organizadoria.financeiro;
import com.example.organizadoria.financeiro.domain.InvestmentProjection.Quote;
import com.example.organizadoria.financeiro.network.BcbCdiService;
import org.junit.Test;
import static org.junit.Assert.*;
public class BcbCdiServiceTest {
 @Test public void newestObservation(){Quote q=BcbCdiService.decode("[{\"data\":\"17/09/2026\",\"valor\":\"0.050788\"},{\"data\":\"16/09/2026\",\"valor\":\"0.051660\"}]","2026-09-18");assertEquals("2026-09-17",q.date);assertEquals(.050788,q.dailyPercent,.0000001);assertEquals(13.649989315282104,q.annualPercent(),.0000001);}
 @Test public void rejectsInvalid(){for(String raw:new String[]{"[]","{}",row("31/02/2026","0.05"),row("19/09/2026","0.05"),row("17/09/2026","NaN"),row("17/09/2026","13.65")}){try{BcbCdiService.decode(raw,"2026-09-18");fail("Deveria rejeitar: "+raw);}catch(RuntimeException expected){}}}
 private static String row(String date,String value){return "[{\"data\":\""+date+"\",\"valor\":\""+value+"\"}]";}
}
