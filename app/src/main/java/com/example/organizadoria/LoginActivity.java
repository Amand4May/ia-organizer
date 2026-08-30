package com.example.organizadoria;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editSenha;
    private MaterialButton btnEntrar;
    private TextView btnIrParaCadastro;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Verificar se o usuário já está logado (Manter Login)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            irParaHome();
        }

        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnIrParaCadastro = findViewById(R.id.btnIrParaCadastro);

        btnEntrar.setOnClickListener(v -> loginUsuario());
        
        btnIrParaCadastro.setOnClickListener(v -> registrarUsuario());
    }

    private void loginUsuario() {
        String email = editEmail.getText().toString();
        String senha = editSenha.getText().toString();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        irParaHome();
                    } else {
                        Toast.makeText(LoginActivity.this, "Erro ao entrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registrarUsuario() {
        String email = editEmail.getText().toString();
        String senha = editSenha.getText().toString();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha para criar conta", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();
                        irParaHome();
                    } else {
                        Toast.makeText(LoginActivity.this, "Erro ao criar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void irParaHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}