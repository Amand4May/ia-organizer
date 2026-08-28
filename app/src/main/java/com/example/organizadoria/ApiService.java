package com.example.organizadoria;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("openai/v1/chat/completions")
    Call<JsonObject> mandarParaIA(
            @Header("Authorization") String authHeader,
            @Body JsonObject corpoRequisicao
    );
}