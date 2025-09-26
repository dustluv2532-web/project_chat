package com.iot.chat001;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ChatApi {

    // === DTO ===
    public static class LoginReq {
        public String name;
        public LoginReq(String name) { this.name = name; }
    }
    public static class LoginRes { public String token; public String name; public Integer expires_in; }
    public static class ChatMsg { public long ts; public String user; public String text; }
    public static class MessagesRes { public List<ChatMsg> messages; }
    public static class SendReq { public String text; public SendReq(String text){ this.text=text; } }
    public static class SendRes { public Boolean ok; public ChatMsg message; }

    // 회원가입(쓰면 유지)
    @POST("/register")
    Call<Map<String, Object>> register(@Body Map<String, String> body);

    // 🔁 로그인: DTO 사용 (ChatActivity의 new ChatApi.LoginReq(name)과 호환)
    @POST("/login")
    Call<LoginRes> login(@Body LoginReq body);

    @GET("/messages")
    Call<MessagesRes> messages(@Query("limit") int limit);

    @POST("/messages")
    Call<SendRes> send(@Header("Authorization") String bearer, @Body SendReq body);
}
