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

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister;
    private ImageButton buttonTogglePassword, buttonToggleConfirmPassword;
    private TextView textViewLogin;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        InsetsUtil.applyTopInset(findViewById(R.id.topBar));

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonTogglePassword = findViewById(R.id.buttonTogglePassword);
        buttonToggleConfirmPassword = findViewById(R.id.buttonToggleConfirmPassword);
        textViewLogin = findViewById(R.id.textViewLogin);

        buttonTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            applyPasswordVisibility(editTextPassword, buttonTogglePassword, passwordVisible);
        });
        buttonToggleConfirmPassword.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            applyPasswordVisibility(editTextConfirmPassword, buttonToggleConfirmPassword, confirmPasswordVisible);
        });

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

            new SessionManager(this).markLoggedIn();
            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        textViewLogin.setOnClickListener(v -> finish());
    }

    private void applyPasswordVisibility(EditText field, ImageButton toggle, boolean visible) {
        int selection = field.getSelectionEnd();
        field.setInputType(visible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        toggle.setImageResource(visible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
        if (selection >= 0) {
            field.setSelection(selection);
        }
    }
}
