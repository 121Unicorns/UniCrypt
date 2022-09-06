package com.unicorn.unicrypt;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.protobuf.Value;

import java.util.concurrent.TimeUnit;

public class VerifyActivity extends AppCompatActivity {
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private ConstraintLayout VerifyLayout;
    private EditText etCode;
    private Button btnVerify;
    private String phoneNumber, verCode, mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private CircularProgressIndicator cipProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        initViews();

        mAuth = FirebaseAuth.getInstance();
        phoneNumber = getIntent().getStringExtra("PhoneNo");
        //Log.d("yyyyyy", phoneNumber);

        //*********************************************** WE DEFINE THE CALLBACK BEFORE WE REQUEST A VERIFICATION CODE ***********************************************
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    showSnackbar(e.getMessage());
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    showSnackbar(e.getMessage());
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                //Log.d(TAG, "onCodeSent:" + verificationId);
                mVerificationId = verificationId;
                mResendToken = token;
            }
        };

        //*********************************************** REQUEST A VERIFICATION CODE TO BE SENT TO THIS PHONE NUMBER ***********************************************
        if (phoneNumber != null) {
            startPhoneNumberVerification(phoneNumber);
        }

        etCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                btnVerify.setVisibility(View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                btnVerify.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //phoneNumber = getIntent().getStringExtra("PhoneNo");
                verCode = etCode.getText().toString();
                if (verCode.isEmpty() || verCode.length() < 6) {
                    showSnackbar("Enter valid code");
                    closeKeyboard();
                    etCode.requestFocus();
                } else {
                    verifyPhoneNumberWithCode(mVerificationId, verCode);
                }
            }
        });
    }

    //*********************************************** ESTABLISHES THE CREDENTIAL USED TO VERIFY THE PHONE NUMBER ***********************************************
    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        btnVerify.setVisibility(View.GONE);
        cipProgress.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        // [START start_phone_auth]
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(120L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        // [END start_phone_auth]
    }

    //*********************************************** SIGN THE USER INTO FIREBASE WITH THE COLLECTED CREDENTIAL ***********************************************
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(VerifyActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            btnVerify.setEnabled(false);
                            FirebaseUser user = task.getResult().getUser();
                            Log.d(TAG, "signInWithCredential:success");

                            //Once the phone number is verified, we update the profile with the details collected and proceed to MainActivity
                            Intent intent = new Intent(VerifyActivity.this, PasscodeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            String message = "Something is wrong, we will fix it soon...";

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                message = "Invalid code entered...";
                            }
                            showSnackbar(message);
                        }
                    }
                });
    }

    private void initViews() {
        etCode = findViewById(R.id.et_code);
        VerifyLayout = findViewById(R.id.verifyLayout);
        btnVerify = findViewById(R.id.btn_verify);
        cipProgress = findViewById(R.id.progress);
    }

    //*********************************************** THEMING THE SNACKBAR ***********************************************
    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(VerifyLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
    }

    //*********************************************** CLOSES THE KEYBOARD ***********************************************
    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}