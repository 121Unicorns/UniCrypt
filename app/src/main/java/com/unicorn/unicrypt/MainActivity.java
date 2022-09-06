package com.unicorn.unicrypt;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ConstraintLayout inboxLayout;
    private ListView lvConvos;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser user = auth.getCurrentUser();
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private ArrayList<User> messageUsers = new ArrayList<>();
    private InboxAdapter inboxAdapter;
    private ValueEventListener dbListener;
    private String userPrivateKey, userPublicKey;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private LinearProgressIndicator lipMsg;
    private ImageView ivMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        user.reload();

        //Logout if the user doesn't exist or is anonymous
        if (user == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        } else if (user.isAnonymous()) {
            auth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        } else if (user != null && !user.isAnonymous()) {
            getUsers();
            mDatabase.child("users").orderByChild("userID").equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        loadUserKey();
                    } else {
                        generatePairs(user);
                        //User myUser = new User(user.getUid(), user.getPhoneNumber(), userKey);
                        //mDatabase.child("users").child(user.getUid()).setValue(myUser);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        inboxAdapter = new InboxAdapter(this, messageUsers);
        lvConvos.setAdapter(inboxAdapter);

        lvConvos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String chatPhone = messageUsers.get(position).getPhone();
                String chatId = messageUsers.get(position).getUserID();
                String publicKey = messageUsers.get(position).getPublicKey();
                Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
                chatIntent.putExtra("chatPhone", chatPhone);
                chatIntent.putExtra("chatId", chatId);
                chatIntent.putExtra("chatKey", publicKey);
                startActivity(chatIntent);
            }
        });

        ivMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("SIGN OUT")
                        .setMessage("This action will log you out of your account. Do you want to continue?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
                                editor = prefs.edit();
                                editor.putString("public_key", null);
                                editor.putString("private_key", null);
                                editor.putString("password", null);
                                editor.apply();
                                editor.commit();
                                user.reload();
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(MainActivity.this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                finish();
                            }
                        })
                        .setNegativeButton("Cancel", null);
                AlertDialog alert11 = builder.create();
                alert11.show();
            }
        });
    }

    private void initViews() {
        inboxLayout = findViewById(R.id.inboxlayout);
        lvConvos = findViewById(R.id.lv_list);
        lipMsg = findViewById(R.id.lip_msg);
        ivMenu = findViewById(R.id.iv_menu);
    }


    private void loadUserKey() {
        prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        editor = prefs.edit();
        userPublicKey = prefs.getString("public_key", null);
        userPrivateKey = prefs.getString("private_key", null);

        if (userPublicKey == null || userPrivateKey == null) {
            mDatabase.child("users").child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        //Log.d("firebase", String.valueOf(task.getResult().getValue()));
                        User post = task.getResult().getValue(User.class);
                        editor.putString("public_key", post.getPublicKey());
                        editor.putString("private_key", post.getPrivateKey());
                        editor.apply();
                        editor.commit();
                    }
                }
            });
        }
    }

    private void generatePairs(FirebaseUser user) {
        KeyPair keyPair = generateKeys();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        byte[] privateKey = keyPair.getPrivate().getEncoded();
        String private_key = Base64.encodeToString(privateKey, Base64.NO_WRAP);
        String public_key = Base64.encodeToString(publicKey, Base64.NO_WRAP);
        Log.d("private_key_gen", "key-" + keyPair.getPrivate());
        Log.d("public_key_gen", "key-" + keyPair.getPublic());
        savePrivateKey(private_key);
        savePublicKey(public_key);

        Log.d("private_key_base64", Base64.encodeToString(privateKey, Base64.NO_WRAP));
        Log.d("public_key_base64", Base64.encodeToString(publicKey, Base64.NO_WRAP));

        User newUser = new User(user.getUid(), user.getPhoneNumber(), public_key, private_key);
        mDatabase.child("users").child(user.getUid()).setValue(newUser);
    }

    public KeyPair generateKeys() {
        KeyPair keyPair = null;
        try {
            // get instance of rsa cipher
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);            // initialize key generator
            keyPair = keyGen.generateKeyPair(); // generate pair of keys
        } catch (GeneralSecurityException e) {
            System.out.println(e);
        }
        return keyPair;
    }

    private void getUsers() {
        messageUsers.clear();
        dbListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    Gson gson = new Gson();
                    String s1 = gson.toJson(dataSnapshot.getValue());
                    JSONObject jsonObject = new JSONObject(s1);
                    JSONArray jsonArray = jsonObject.toJSONArray(jsonObject.names());

                    //loop through the array getting details and adding them to an arraylist
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonContent = (JSONObject) jsonArray.get(i);
                        String dbPhone = jsonContent.getString("phone");
                        String dbUserID = jsonContent.getString("userID");
                        String dbPublicKey = jsonContent.getString("publicKey");
                        User myUser = new User();
                        myUser.setUserID(dbUserID);
                        myUser.setPhone(dbPhone);
                        myUser.setPublicKey(dbPublicKey);
                        if (!dbUserID.equals(user.getUid())) {
                            messageUsers.add(myUser);
                        }
                    }
                    inboxAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                lipMsg.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Users failed, log a message
                Log.w(TAG, "loadUsers:onCancelled", databaseError.toException());
            }
        };
        //Since I only wanted the listener to fire once then stop, I use addListenerForSingleValueEvent instead
        mDatabase.child("users").addValueEventListener(dbListener);
    }

    private void savePrivateKey(String private_key) {
        editor.putString("private_key", private_key);
        editor.apply();
        editor.commit();
    }

    private void savePublicKey(String public_key) {
        editor.putString("public_key", public_key);
        editor.apply();
        editor.commit();
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(inboxLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDatabase.removeEventListener(dbListener);
    }
}