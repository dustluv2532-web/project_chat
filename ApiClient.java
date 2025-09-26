package com.iot.chat001;

import android.content.SharedPreferences;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static ChatApi instance;

    public static ChatApi get(SharedPreferences prefs, String baseUrl) {
        if (instance == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(prefs))
                    .build();
            Retrofit r = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            instance = r.create(ChatApi.class);
        }
        return instance;
    }
}
