package com.unicorn.unicrypt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hbb20.CountryCodePicker;

public class LoginActivity extends AppCompatActivity {
    private String phoneNumber, phone, phoneStore;
    private Button btnSend;
    private CountryCodePicker ccp;
    private EditText etPhone, etCode;
    private ConstraintLayout loginLayout;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser user = auth.getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (user != null){
            startActivity(new Intent(LoginActivity.this, PasscodeActivity.class));
            finish();
        }

        initViews();

        ccp.registerCarrierNumberEditText(etPhone);
        phoneNumber = ccp.getFullNumberWithPlus();
        phoneStore = ccp.getFullNumber();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                phone = etPhone.getText().toString();
                if (phone.length() <9 ||phone.length() >10 ){
                    etPhone.requestFocus();
                    showSnackbar("Please enter a valid phone number!");
                } else {
                    ccp.registerCarrierNumberEditText(etPhone);
                    phoneNumber = ccp.getFullNumberWithPlus();
                    Intent intent = new Intent(LoginActivity.this, VerifyActivity.class);
                    intent.putExtra("PhoneNo", phoneNumber);
                    startActivity(intent);
                }
            }
        });
    }

    private void initViews(){
        ccp = findViewById(R.id.ccp_code);
        etPhone = findViewById(R.id.et_phone);
        btnSend = findViewById(R.id.btn_send);
        loginLayout = findViewById(R.id.loginLayout);
    }

    private void showSnackbar(String message){
        Snackbar snackbar = Snackbar.make(loginLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
    }
}