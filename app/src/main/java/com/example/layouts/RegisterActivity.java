package com.example.layouts;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);

        buttonRegister.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                editTextName.setError("Ingresa tu nombre");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                editTextEmail.setError("Ingresa tu correo");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                editTextPassword.setError("Ingresa una contraseña");
                return;
            }
            if (TextUtils.isEmpty(confirmPassword)) {
                editTextConfirmPassword.setError("Confirma tu contraseña");
                return;
            }
            if (!password.equals(confirmPassword)) {
                editTextConfirmPassword.setError("Las contraseñas no coinciden");
                return;
            }

            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        textViewLogin.setOnClickListener(v -> finish());
    }
}
