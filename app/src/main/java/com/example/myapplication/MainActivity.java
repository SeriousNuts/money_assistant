package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.mindrot.jbcrypt.BCrypt;

import static com.example.myapplication.R.id.sign_in;
import static com.example.myapplication.R.id.sign_up;
import static com.google.android.gms.auth.api.signin.GoogleSignIn.getClient;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout mDotsLayout;
    private TextView[] mDots;
    private EditText Password;
    private FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 123456;
    private GoogleSignInClient mGoogleSignInClient;
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser!=null){
            Intent intentGoogleSignUp = new Intent(MainActivity.this,ChartGenerator.class);
            startActivity(intentGoogleSignUp);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();



        ViewPager mSlidePageViewer = (ViewPager) findViewById(R.id.Pager);
        mDotsLayout = (LinearLayout) findViewById(R.id.LinearLayout);
        authorization_slider_layout sliderAdapter = new authorization_slider_layout(this);
        mSlidePageViewer.setAdapter(sliderAdapter);
        addDotsInditcator(0);
        mSlidePageViewer.addOnPageChangeListener(ViewListener);
        }
    private void createRequest() {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);
        builder.requestIdToken(getString(R.string.default_web_client_id));
        builder.requestEmail();
        GoogleSignInOptions gso = builder
                .build();
        mGoogleSignInClient = getClient(MainActivity.this, gso);
    }

    private void signIn() {
        createRequest();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent,
                RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Toast.makeText(MainActivity.this, "Firebase with google succes",
                        Toast.LENGTH_SHORT).show();
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(MainActivity.this, "Authentification failed" + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(MainActivity.this, "Authentification succes",
                                    Toast.LENGTH_SHORT).show();
                            FirebaseUser user = mAuth.getCurrentUser();
                            Intent intentGoogleSignUp = new Intent(MainActivity.this,ChartGenerator.class);
                            startActivity(intentGoogleSignUp);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentification failed" + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case sign_in:
                EditText email = (EditText) findViewById(R.id.email_toggle);
                Password = (EditText) findViewById(R.id.password_toggle);
                if((email.getText().toString().length()!=0) || (Password.getText().toString().length()!=0)) {
                    //вход
                    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                    firebaseAuth.signInWithEmailAndPassword(email.getText().toString(), Password.getText().toString()).addOnCompleteListener(
                            new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Intent intentMainWindows = new Intent(MainActivity.this, ChartGenerator.class);
                                        startActivity(intentMainWindows);
                                    } else {
                                        Toast.makeText(MainActivity.this, "Authentication failed." + task.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
                else
                {
                    Toast.makeText(MainActivity.this,"Поля логин и пароль должны быть заполнены", Toast.LENGTH_SHORT).show();
                }


                break;
            case sign_up:
                Intent intentSignUpActitvity = new Intent(MainActivity.this,SignUpActivity.class);
                startActivity(intentSignUpActitvity);
                finish();
            case R.id.googleSignIn:
                signIn();

                break;

            default:
                break;
        }
    }

    private void addDotsInditcator(int position) {
        mDots = new TextView[3];
        mDotsLayout.removeAllViews();
        for (int i = 0; i < mDots.length; i++) {
            mDots[i] = new TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226"));
            mDots[i].setTextSize(35);
            mDots[i].setTextColor(getResources().getColor(R.color.colorPrimaryDark));

            mDotsLayout.addView(mDots[i]);
        }
        if (mDots.length > 0) {
            mDots[position].setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    ViewPager.OnPageChangeListener ViewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addDotsInditcator(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

}

