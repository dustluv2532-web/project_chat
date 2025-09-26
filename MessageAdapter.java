package com.iot.chat001;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final List<ChatApi.ChatMsg> data = new ArrayList<>();
    private final String myName;

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageAdapter(String myName) { this.myName = myName; }

    public void setAll(List<ChatApi.ChatMsg> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public void addOne(ChatApi.ChatMsg m) {
        data.add(m);
        notifyItemInserted(data.size() - 1);
    }

    @Override public int getItemViewType(int position) {
        ChatApi.ChatMsg m = data.get(position);
        return (m.user != null && m.user.equals(myName)) ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            View v = inf.inflate(R.layout.item_message_me, parent, false);
            return new MeVH(v);
        } else {
            View v = inf.inflate(R.layout.item_message_other, parent, false);
            return new OtherVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        ChatApi.ChatMsg m = data.get(pos);
        String t = timeFmt.format(new Date(m.ts)); // ts(ms) -> "HH:mm"

        if (h instanceof MeVH) {
            MeVH vh = (MeVH) h;
            vh.txtMsg.setText(m.text);
            vh.txtTime.setText(t);
            // (선택) 내 닉네임 표시하고 싶으면:
            // vh.txtUserMe.setText(m.user != null ? m.user : myName);
        } else {
            OtherVH vh = (OtherVH) h;
            vh.txtUser.setText(m.user != null && !m.user.isEmpty() ? m.user : "unknown");
            vh.txtMsg.setText(m.text);
            vh.txtTime.setText(t);
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class MeVH extends RecyclerView.ViewHolder {
        // (선택) TextView txtUserMe;
        TextView txtMsg, txtTime;
        MeVH(@NonNull View itemView) {
            super(itemView);
            // txtUserMe = itemView.findViewById(R.id.txtUserMe);
            txtMsg  = itemView.findViewById(R.id.txtMsg);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
    static class OtherVH extends RecyclerView.ViewHolder {
        TextView txtUser, txtMsg, txtTime;
        OtherVH(@NonNull View itemView) {
            super(itemView);
            txtUser = itemView.findViewById(R.id.txtUser);
            txtMsg  = itemView.findViewById(R.id.txtMsg);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
}
