package com.unicorn.unicrypt;

import static android.content.ContentValues.TAG;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ChatActivity extends AppCompatActivity {
    private TextView tvDisplayName;
    private ImageView ivBack;
    private RecyclerView rvChat;
    private FrameLayout flSend;
    private ConstraintLayout chatLayout;
    private EditText etMessage;
    private String chatKey, sender, receiver, message, chatUser, chatId, myPublicKey, myPrivateKey;
    private ChatMessage chatMessage = new ChatMessage();
    private ArrayList<ChatMessage> chatList = new ArrayList<>();
    private ArrayList<User> mUser = new ArrayList<>();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference().child("messages");
    private DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users");
    private LinearLayoutManager manager;
    private ChatAdapter adapter;
    private final int VIEW_TYPE_MESSAGE_SENT = 2;
    private final int VIEW_TYPE_MESSAGE_RECEIVED = 1;
    private ValueEventListener dbListener;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initViews();

        chatUser = getIntent().getStringExtra("chatPhone");
        chatId = getIntent().getStringExtra("chatId");
        chatKey = getIntent().getStringExtra("chatKey");
        tvDisplayName.setText(chatUser);

        prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        editor = prefs.edit();
        myPublicKey = prefs.getString("public_key", null);
        myPrivateKey = prefs.getString("private_key", null);

        getMessages();

        flSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                message = etMessage.getText().toString();
                user.reload();

                if (message.isEmpty()) {
                    showSnackbar("Cannot send blank message!");
                } else {
                    ChatMessage myChatMsg = new ChatMessage(user.getUid(), chatId, encrypt(message, myPublicKey), System.currentTimeMillis());
                    ChatMessage theirChatMsg = new ChatMessage(user.getUid(), chatId, encrypt(message, chatKey), System.currentTimeMillis());

                    messageRef.child(user.getUid()).push().setValue(myChatMsg);
                    messageRef.child(chatId).push().setValue(theirChatMsg);

                    getMessages();
                    etMessage.setText("");
                }
            }
        });

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void initViews() {
        tvDisplayName = findViewById(R.id.tv_displayname);
        rvChat = findViewById(R.id.rv_chat);
        flSend = findViewById(R.id.layoutSend);
        etMessage = findViewById(R.id.et_msg);
        chatLayout = findViewById(R.id.chatlayout);
        ivBack = findViewById(R.id.iv_back);

        sender = user.getPhoneNumber();

        manager = new LinearLayoutManager(ChatActivity.this, RecyclerView.VERTICAL, false);
        //manager.setReverseLayout(true);
        manager.setStackFromEnd(true);
        adapter = new ChatAdapter(ChatActivity.this, chatList);
        rvChat.setLayoutManager(manager);
        rvChat.smoothScrollToPosition(0);
        rvChat.setAdapter(adapter);
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(chatLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
    }

    private void getMessages() {
        dbListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                chatList.clear();
                try {
                    Gson gson = new Gson();
                    String s1 = gson.toJson(dataSnapshot.getValue());
                    JSONObject jsonObject = new JSONObject(s1);
                    JSONArray jsonArray = jsonObject.toJSONArray(jsonObject.names());

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonContent = (JSONObject) jsonArray.get(i);
//                        Log.d("RRRRRRRRRRRRRRR", jsonContent.toString());
                        String dbSender = jsonContent.getString("sender");
                        String dbReceiver = jsonContent.getString("receiver");
                        String dbMessage = decrypt(jsonContent.getString("message"));
                        String dbMsgTime = jsonContent.getString("messageTime");

                        ChatMessage myMessage = new ChatMessage(dbSender, dbReceiver, dbMessage, Long.parseLong(dbMsgTime));
                        if (dbSender.equals(user.getUid())) {
                            myMessage.setType(VIEW_TYPE_MESSAGE_SENT);
                        } else {
                            myMessage.setType(VIEW_TYPE_MESSAGE_RECEIVED);
                        }
                        chatList.add(myMessage);
                    }
                    //Collections.reverse(chatList);
                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Users failed, log a message
                Log.w(TAG, "loadUsers:onCancelled", databaseError.toException());
            }
        };
        user.reload();
        messageRef.child(user.getUid()).addValueEventListener(dbListener);
    }

    public String encrypt(String msg, String public_key) {
        Cipher cipher = null;
        String encrypted = null;
        try {
            byte[] keyBytes = Base64.decode(public_key, Base64.NO_WRAP);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Key publicKey = null;
            publicKey = keyFactory.generatePublic(x509KeySpec);
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(msg.getBytes());
            encrypted = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return (encrypted);
    }


    public String decrypt(String msg) {
        Cipher cipher = null;
        String decrypted = null;
        try {
            byte[] keyBytes = Base64.decode(myPrivateKey, Base64.NO_WRAP);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decode = Base64.decode(msg, Base64.NO_WRAP);
            byte[] decryptedBytes = cipher.doFinal(decode);
            decrypted = new String(decryptedBytes);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return decrypted;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getMessages();
    }
}