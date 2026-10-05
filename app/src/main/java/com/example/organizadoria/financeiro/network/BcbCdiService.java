package com.example.organizadoria.financeiro.network;
import com.example.organizadoria.financeiro.domain.InvestmentProjection.Quote;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
/** Consulta pública SGS 12 (% por dia útil). Sem dados pessoais nem chave de API. */
public final class BcbCdiService {
 public static final String ENDPOINT="https://api.bcb.gov.br/dados/serie/bcdata.sgs.12/dados/ultimos/10?formato=json";
 private BcbCdiService(){}
 public static Quote fetch(String today)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(ENDPOINT).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setRequestProperty("Accept","application/json");
  try{if(c.getResponseCode()!=200)throw new IllegalStateException("CDI indisponível.");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){if(out.size()+n>32768)throw new IllegalStateException("Resposta inválida.");out.write(b,0,n);}return decode(new String(out.toByteArray(),StandardCharsets.UTF_8),today);}}finally{c.disconnect();}
 }
 public static Quote decode(String raw,String today){
  JsonElement root=new JsonParser().parse(raw);if(!root.isJsonArray()||root.getAsJsonArray().size()==0||root.getAsJsonArray().size()>20)throw new IllegalArgumentException("Cotação não encontrada.");DateTimeFormatter format=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);Quote latest=null;
  for(JsonElement element:root.getAsJsonArray()){JsonObject o=element.getAsJsonObject();Quote q=new Quote(LocalDate.parse(o.get("data").getAsString(),format).toString(),o.get("valor").getAsDouble());q.validateOn(today);if(latest==null||q.date.compareTo(latest.date)>0)latest=q;}return latest;
 }
}
