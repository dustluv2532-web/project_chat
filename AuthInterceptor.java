package com.iot.chat001;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final SharedPreferences prefs;

    public AuthInterceptor(SharedPreferences prefs) { this.prefs = prefs; }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = prefs.getString("token", null);
        Request req = chain.request();
        if (token != null && !token.isEmpty()) {
            req = req.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
        }
        return chain.proceed(req);
    }

    // helper: create EncryptedSharedPreferences (call from Activity/Context)
    public static SharedPreferences createEncryptedPrefs(Context ctx) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    "auth_secure_prefs",
                    masterKeyAlias,
                    ctx,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Android 버전/라이브러리 이슈 시 일반 SharedPreferences fallback
            return ctx.getSharedPreferences("auth", Context.MODE_PRIVATE);
        }
    }
}
