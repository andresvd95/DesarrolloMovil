package com.example.layouts;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.layouts.auth.SessionManager;
import com.example.layouts.ui.InsetsUtil;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private ImageButton buttonTogglePassword;
    private TextView textViewRegister;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        InsetsUtil.applyTopInset(findViewById(R.id.topBar));

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonTogglePassword = findViewById(R.id.buttonTogglePassword);
        textViewRegister = findViewById(R.id.textViewRegister);

        buttonTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                editTextEmail.setError("Ingresa tu correo o usuario");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                editTextPassword.setError("Ingresa tu contraseña");
                return;
            }

            new SessionManager(this).markLoggedIn();
            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        });

        textViewRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        int selection = editTextPassword.getSelectionEnd();
        editTextPassword.setInputType(passwordVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        buttonTogglePassword.setImageResource(passwordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
        if (selection >= 0) {
            editTextPassword.setSelection(selection);
        }
    }
}
