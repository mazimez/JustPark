package com.example.tillnow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.tillnow.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneOtp extends AppCompatActivity {

    //local
    private String phoneNum;
    private String verificationCode;
    private String userCode;
    private String uid;
    private boolean isAuthenticate = false;

    //EditText
    private EditText otp;

    //Button
    private Button submit;

    //progress bar
    private ProgressBar progressBar;

    //Context
    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_otp);

        //setting up hooks
        otp = findViewById(R.id.otp);
        submit = findViewById(R.id.submit);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        //getting data from caller activity
        Bundle extras = getIntent().getExtras();
        phoneNum = extras.getString("phoneNumber");

        //authentification process start i.e. sending the OTP
        PhoneAuthProvider.getInstance().verifyPhoneNumber( //sensing message
                phoneNum,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                TaskExecutors.MAIN_THREAD,    // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks

        //verifing the code entered manualy by user.
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userCode = otp.getText().toString();
                if (!userCode.isEmpty()) {
                    progressBar.setVisibility(View.VISIBLE);
                    verifyCode(userCode);
                }else{
                    otp.setError("Please enter OTP.");
                }
            }
        });
    }
        //callbacks for phone authentication failed/success
        private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                verificationCode = s; //the actual code that should be recived on phone number
            }

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                String code = phoneAuthCredential.getSmsCode(); //the code that actually recieved by phone number
                //cheking if code is not null
                if (code != null) {
                    //starting the progress bar and verifing the code recieved
                    progressBar.setVisibility(View.VISIBLE);
                    verifyCode(code);
                }
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                //showing the toast of veridication failed and going back
                Toast.makeText(context, "Verification Faild: OTP not recived", Toast.LENGTH_SHORT).show();
                Intent intent = getIntent();
                intent.putExtra("isAuthenticate", isAuthenticate);
                setResult(RESULT_OK, intent);
                Log.d("myTag", "authenticattion failed...going back");
                finish();
            }
        };

        //verifing the OTP
        public void verifyCode(String code) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode, code);
            signin(credential);
        }

        //signing in the User
        private void signin(PhoneAuthCredential credential) {
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            firebaseAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        //showing the toast of verification complete and going back with user id
                        Toast.makeText(context, "Verification Complete", Toast.LENGTH_SHORT).show();
                        isAuthenticate=true;
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        uid=firebaseUser.getUid();
                        Intent intent = getIntent();
                        intent.putExtra("isAuthenticate",isAuthenticate);
                        intent.putExtra("uid",uid);
                        setResult(RESULT_OK,intent);
                        Log.d("myTag", "authenticated...going back");
                        finish();
                    } else {
                        Toast.makeText(context, "Verification Faild: OTP wrong", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
}
