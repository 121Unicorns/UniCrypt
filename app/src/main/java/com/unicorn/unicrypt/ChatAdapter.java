package com.unicorn.unicrypt;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ChatAdapter extends RecyclerView.Adapter {

    ArrayList<ChatMessage> messageChatModelList;
    Context context;
    private static final int VIEW_TYPE_MESSAGE_SENT = 2;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 1;

    public ChatAdapter(Context context, ArrayList<ChatMessage> messageChatModelList) {
        this.context = context;
        this.messageChatModelList = messageChatModelList;
    }


    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageChatModelList.get(position);
        if ((message.getType()) == 2) {
            // If the current user is the sender of the message
            //Log.e("getItemViewType", "2");
            return VIEW_TYPE_MESSAGE_SENT;
        } /*else if ((message.getType()) == 1) {
            // If some other user sent the message
            Log.e("getItemViewType", "1");
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }*/ else {
            // If some other user sent the message
            //Log.e("getItemViewType", "1");
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.receive_layout, parent, false);
            return new ReceivedMessageHolder(view);
        } /* else if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.send_layout, parent, false);
            return new SentMessageHolder(view);
        }*/ else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.send_layout, parent, false);
            return new SentMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageChatModelList.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messageChatModelList.size();
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView message;
        TextView time;
        String date;

        public SentMessageHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.tv_sentmessage);
            time = itemView.findViewById(R.id.tv_senttime);
        }

        void bind(ChatMessage messageModel) {
            message.setText(messageModel.getMessage());
            Date mydate = new Date(messageModel.getMessageTime());
            DateFormat dateFormat = new SimpleDateFormat("E dd/MM/yyyy HH:mm");
            date = dateFormat.format(mydate);
            time.setText(date);
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView msg;
        TextView time;
        String date;

        public ReceivedMessageHolder(@NonNull View itemView) {
            super(itemView);
            msg = itemView.findViewById(R.id.tv_receivemessage);
            time = itemView.findViewById(R.id.tv_receivetime);
        }

        void bind(ChatMessage messageModel) {
            msg.setText(messageModel.getMessage());
            Date mydate = new Date(messageModel.getMessageTime());
            DateFormat dateFormat = new SimpleDateFormat("E dd/MM/yyyy HH:mm");
            date = dateFormat.format(mydate);
            time.setText(date);
        }
    }
}