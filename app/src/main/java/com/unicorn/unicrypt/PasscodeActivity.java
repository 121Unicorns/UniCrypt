package com.unicorn.unicrypt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class PasscodeActivity extends AppCompatActivity {
    private TextView tvPass;
    private EditText etPass;
    private Button btnPass;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private String savedPassword, pass;
    private ConstraintLayout passLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passcode);

        initViews();

        prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        editor = prefs.edit();
        savedPassword = prefs.getString("password", null);

        if (savedPassword == null){
            tvPass.setText("Please create a new password");
            btnPass.setText("CREATE");
        } else {
            tvPass.setText("Please enter your password");
            btnPass.setText("ENTER");
        }

        btnPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pass = etPass.getText().toString();
                if (pass.length()<4){
                    closeKeyboard();
                    showSnackbar("Your passcode cannot be less that 4 digits! Please enter your passcode.");
                    etPass.requestFocus();
                } else {
                    if (savedPassword == null){
                        editor.putString("password", pass);
                        editor.apply();
                        editor.commit();
                        startActivity(new Intent(PasscodeActivity.this, MainActivity.class));
                        finish();
                    } else {
                        if (savedPassword.equals(pass)){
                            closeKeyboard();
                            showSnackbar("Password verified. Welcome!");
                            startActivity(new Intent(PasscodeActivity.this, MainActivity.class));
                            finish();
                        } else {
                            closeKeyboard();
                            showSnackbar("Incorrect password!");
                            etPass.requestFocus();
                        }
                    }
                }
            }
        });

    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(passLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
    }

    private void initViews(){
        tvPass = findViewById(R.id.tv_password);
        etPass = findViewById(R.id.et_password);
        btnPass = findViewById(R.id.btn_pass);
        passLayout = findViewById(R.id.pass_layout);
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}