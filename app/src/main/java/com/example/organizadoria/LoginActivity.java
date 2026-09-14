package com.example.organizadoria;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editSenha, editNome, editDataNascimento;
    private MaterialButton btnEntrar;
    private TextView btnIrParaCadastro, btnEsqueceuSenha;
    private FirebaseAuth mAuth;
    private boolean isModoCadastro = false;

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

        editNome = findViewById(R.id.editNome);
        editDataNascimento = findViewById(R.id.editDataNascimento);
        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnIrParaCadastro = findViewById(R.id.btnIrParaCadastro);
        btnEsqueceuSenha = findViewById(R.id.btnEsqueceuSenha);

        configurarMascaraData();

        btnEntrar.setOnClickListener(v -> {
            if (isModoCadastro) {
                registrarUsuario();
            } else {
                loginUsuario();
            }
        });

        if (btnEsqueceuSenha != null) {
            btnEsqueceuSenha.setOnClickListener(v -> redefinirSenha());
        }
        
        // Configurar estado inicial (Login)
        isModoCadastro = true; // Forçamos true para que o alternarModo mude para false (Login)
        alternarModo(); 
        
        btnIrParaCadastro.setOnClickListener(v -> alternarModo());
    }

    private void redefinirSenha() {
        String email = editEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Digite seu e-mail para redefinir a senha", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "E-mail de redefinição enviado para " + email, Toast.LENGTH_LONG).show();
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Erro";
                        Toast.makeText(this, "Erro: " + err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void configurarMascaraData() {
        editDataNascimento.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private String ddmmyyyy = "DDMMYYYY";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d.]|\\.", "");
                    String cleanC = current.replaceAll("[^\\d.]|\\.", "");

                    int cl = clean.length();
                    int sel = cl;
                    for (int i = 2; i <= cl && i < 6; i += 2) {
                        sel++;
                    }
                    if (clean.equals(cleanC)) sel--;

                    if (clean.length() < 8) {
                        clean = clean + ddmmyyyy.substring(clean.length());
                    } else {
                        clean = clean.substring(0, 8);
                    }

                    clean = String.format("%s/%s/%s", clean.substring(0, 2),
                            clean.substring(2, 4),
                            clean.substring(4, 8));

                    sel = Math.max(0, sel);
                    current = clean;
                    editDataNascimento.setText(current);
                    editDataNascimento.setSelection(Math.min(sel, current.length()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void alternarModo() {
        isModoCadastro = !isModoCadastro;
        
        int visibilidade = isModoCadastro ? View.VISIBLE : View.GONE;
        
        View imgPlaceholder = findViewById(R.id.imgPerfilPlaceholder);
        if (imgPlaceholder != null) imgPlaceholder.setVisibility(visibilidade);

        View labelNome = findViewById(R.id.labelNome);
        if (labelNome != null) labelNome.setVisibility(visibilidade);

        View layoutNome = findViewById(R.id.layoutNome);
        if (layoutNome != null) layoutNome.setVisibility(visibilidade);

        View labelData = findViewById(R.id.labelDataNascimento);
        if (labelData != null) labelData.setVisibility(visibilidade);

        View layoutData = findViewById(R.id.layoutDataNascimento);
        if (layoutData != null) layoutData.setVisibility(visibilidade);

        if (btnEsqueceuSenha != null) {
            btnEsqueceuSenha.setVisibility(isModoCadastro ? View.GONE : View.VISIBLE);
        }
        
        if (isModoCadastro) {
            btnEntrar.setText("Criar Conta");
            
            String texto = "Já tem conta? Entrar";
            SpannableString ss = new SpannableString(texto);
            int inicio = texto.indexOf("Entrar");
            int fim = inicio + "Entrar".length();
            
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.azul_safira)), inicio, fim, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new UnderlineSpan(), inicio, fim, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            btnIrParaCadastro.setText(ss);
        } else {
            btnEntrar.setText("Entrar");
            
            String texto = "Ainda não tem conta? Criar conta";
            SpannableString ss = new SpannableString(texto);
            int inicio = texto.indexOf("Criar conta");
            int fim = inicio + "Criar conta".length();
            
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.azul_safira)), inicio, fim, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new UnderlineSpan(), inicio, fim, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            btnIrParaCadastro.setText(ss);
        }
    }

    private void loginUsuario() {
        String email = editEmail.getText().toString().trim();
        String senha = editSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        irParaHome();
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Erro ao entrar";
                        Toast.makeText(LoginActivity.this, "Erro ao entrar: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registrarUsuario() {
        String nome = editNome.getText().toString().trim();
        String nascimento = editDataNascimento.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String senha = editSenha.getText().toString().trim();

        if (nome.isEmpty() || nascimento.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos para criar conta", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String currentUserId = mAuth.getUid();
                        // Salvar nome no SharedPreferences para usar na saudação
                        getSharedPreferences("DadosPerfil_" + currentUserId, MODE_PRIVATE)
                                .edit()
                                .putString("nome", nome)
                                .putString("nascimento", nascimento)
                                .apply();

                        Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();
                        irParaHome();
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Erro ao criar";
                        Toast.makeText(LoginActivity.this, "Erro ao criar: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void irParaHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}