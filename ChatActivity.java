package com.iot.chat001;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:5000/";

    private ChatApi api;
    private RecyclerView recycler;
    private EditText edtMessage;
    private Button btnSend, btnLogout;  // 🔴 로그아웃 버튼 추가
    private TextView txtTitle;

    private MessageAdapter adapter;
    private String token;
    private String myName;

    private boolean isLoggingIn = false;
    private String pendingText = null;
    private long lastShownTs = -1L;

    private final Handler pollHandler = new Handler();
    private final Runnable pollTask = new Runnable() {
        @Override public void run() {
            fetchMessages(false);
            pollHandler.postDelayed(this, 2000);
        }
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1) 뷰 바인딩
        txtTitle   = findViewById(R.id.txtTitle);
        recycler   = findViewById(R.id.recycler);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend    = findViewById(R.id.btnSend);
        btnLogout  = findViewById(R.id.btnLogout);   // 🔴 로그아웃 버튼

        // 2) Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(ChatApi.class);

        // 3) SharedPreferences에서 토큰 확인
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        token  = sp.getString("token", null);
        myName = sp.getString("name",  "guest");

        if (token == null) {   // 🔴 토큰 없으면 로그인 화면으로
            goLoginAndFinish();
            return;
        }

        // 4) RecyclerView
        adapter = new MessageAdapter(myName);
        LinearLayoutManager lm = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);

        if (txtTitle != null) txtTitle.setText("Chat (" + myName + ")");
        btnSend.setEnabled(token != null);

        // 5) 버튼 동작
        btnSend.setOnClickListener(v -> sendMessage());
        btnLogout.setOnClickListener(v -> logout());  // 🔴 로그아웃 버튼 동작

        // 6) 채팅 시작
        startChat();
    }

    /** 채팅 시작 */
    private void startChat() {
        fetchMessages(true);
        pollHandler.postDelayed(pollTask, 500);
    }

    /** 메시지 불러오기 */
    private void fetchMessages(boolean forceScrollToBottom) {
        api.messages(50).enqueue(new Callback<ChatApi.MessagesRes>() {
            @Override public void onResponse(Call<ChatApi.MessagesRes> call, Response<ChatApi.MessagesRes> res) {
                if (!res.isSuccessful() || res.body() == null) return;
                java.util.List<ChatApi.ChatMsg> list = res.body().messages;
                adapter.setAll(list);
                long newLastTs = (list != null && !list.isEmpty()) ? list.get(list.size() - 1).ts : -1L;
                if ((forceScrollToBottom || newLastTs > lastShownTs) && adapter.getItemCount() > 0) {
                    recycler.scrollToPosition(adapter.getItemCount() - 1);
                }
                if (newLastTs > lastShownTs) lastShownTs = newLastTs;
            }
            @Override public void onFailure(Call<ChatApi.MessagesRes> call, Throwable t) { }
        });
    }

    /** 메시지 전송 */
    private void sendMessage() {
        String text = edtMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        if (token == null) {
            toast("로그인이 필요합니다");
            goLoginAndFinish();
            return;
        }

        actuallySend(text);
    }

    private void actuallySend(String text) {
        api.send("Bearer " + token, new ChatApi.SendReq(text))
                .enqueue(new Callback<ChatApi.SendRes>() {
                    @Override public void onResponse(Call<ChatApi.SendRes> call, Response<ChatApi.SendRes> res) {
                        if (res.code() == 401) {
                            toast("세션 만료. 다시 로그인하세요.");
                            logout();
                            return;
                        }
                        ChatApi.SendRes body = res.body();
                        boolean success = res.isSuccessful() && body != null && body.message != null;
                        if (!success) { toast("전송 실패 (" + res.code() + ")"); return; }
                        edtMessage.setText("");
                        adapter.addOne(body.message);
                        recycler.scrollToPosition(adapter.getItemCount() - 1);
                        if (body.message.ts > lastShownTs) lastShownTs = body.message.ts;
                    }
                    @Override public void onFailure(Call<ChatApi.SendRes> call, Throwable t) {
                        toast("네트워크 오류");
                    }
                });
    }

    /** 로그아웃 */
    private void logout() {
        pollHandler.removeCallbacks(pollTask);
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        sp.edit().clear().apply(); // 토큰/닉네임 삭제
        goLoginAndFinish();
    }

    /** 로그인 화면으로 이동 */
    private void goLoginAndFinish() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    @Override protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacks(pollTask);
    }
}
