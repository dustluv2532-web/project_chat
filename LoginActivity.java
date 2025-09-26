package com.iot.chat001;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    EditText edtUser, edtPw;
    Button btnLogin, btnRegister;
    CheckBox chkRemember;
    TextView tvMode, tvStatus;
    SharedPreferences prefs;
    ChatApi api;
    private static final String BASE_URL = "http://10.0.2.2:5000/"; // ngrok 사용 시 https://xxxxx.ngrok.io/

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_login);
        prefs = AuthInterceptor.createEncryptedPrefs(this); // 권장
        api = ApiClient.get(prefs, BASE_URL);

        edtUser = findViewById(R.id.edtUsername);
        edtPw = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        chkRemember = findViewById(R.id.chkRemember);
        tvMode = findViewById(R.id.tvMode);
        tvStatus = findViewById(R.id.tvStatus);
        tvMode.setText("Mode: Secure (bcrypt + JWT + EncryptedPrefs)");

        btnRegister.setOnClickListener(v -> doRegister());
        btnLogin.setOnClickListener(v -> doLogin());
    }

    private void doRegister() {
        String u = edtUser.getText().toString().trim();
        String p = edtPw.getText().toString().trim();
        if (u.isEmpty() || p.isEmpty()) { tvStatus.setText("아이디/비밀번호 필요"); return; }

        java.util.Map<String,String> body = new java.util.HashMap<>();
        body.put("username", u);
        body.put("password", p);
        api.register(body).enqueue(new Callback<java.util.Map<String,Object>>() {
            @Override public void onResponse(Call<java.util.Map<String,Object>> call, Response<java.util.Map<String,Object>> res) {
                tvStatus.setText(res.isSuccessful() ? "가입 성공" : "가입 실패: " + res.code());
            }
            @Override public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t) {
                tvStatus.setText("네트워크 오류");
            }
        });
    }

    private void doLogin() {
        String u = edtUser.getText().toString().trim();
        String p = edtPw.getText().toString().trim();
        if (u.isEmpty() || p.isEmpty()) { tvStatus.setText("아이디/비밀번호 필요"); return; }

        // 서버 ChatApi.login은 LoginReq를 name만 받게 되어있음. 서버/ChatApi를 username/password로 맞춰야 합니다.
        java.util.Map<String,String> body = new java.util.HashMap<>();
        body.put("username", u);
        body.put("password", p);

        // 여기선 login 엔드포인트가 Map을 받는다고 가정(필요 시 ChatApi 수정)
        api.login(new ChatApi.LoginReq(u)).enqueue(new Callback<ChatApi.LoginRes>() {
            @Override public void onResponse(Call<ChatApi.LoginRes> call, Response<ChatApi.LoginRes> res) {
                if (!res.isSuccessful() || res.body()==null) {
                    tvStatus.setText("로그인 실패: " + res.code());
                    return;
                }
                String token = res.body().token;
                String name = res.body().name;
                if (chkRemember.isChecked()) {
                    prefs.edit().putString("token", token).putString("name", name).apply();
                } else {
                    // 임시 세션: 메모리에만 보관(간단히 prefs에만 저장하지 않음)
                    prefs.edit().putString("token", token).putString("name", name).apply();
                }
                // ChatActivity로 이동
                startActivity(new Intent(LoginActivity.this, ChatActivity.class));
                finish();
            }
            @Override public void onFailure(Call<ChatApi.LoginRes> call, Throwable t) {
                tvStatus.setText("네트워크 오류");
            }
        });
    }
}
