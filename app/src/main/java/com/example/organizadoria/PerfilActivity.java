package com.example.organizadoria;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class PerfilActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextInputEditText editNome, editDataNascimento, editNovoEmail, editRenda, editTotalInvestido;
    private MaterialButton btnAtualizarEmail, btnSalvarInfo, btnSair;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        mAuth = FirebaseAuth.getInstance();
        String currentUserId = mAuth.getUid();
        prefs = getSharedPreferences("DadosPerfil_" + currentUserId, MODE_PRIVATE);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton btnAbrirMenu = findViewById(R.id.btnAbrirMenu);
        NavigationView navView = findViewById(R.id.navView);
        
        editNome = findViewById(R.id.editNome);
        editDataNascimento = findViewById(R.id.editDataNascimento);
        editNovoEmail = findViewById(R.id.editNovoEmail);
        editRenda = findViewById(R.id.editRenda);
        editTotalInvestido = findViewById(R.id.editTotalInvestido);
        btnAtualizarEmail = findViewById(R.id.btnAtualizarEmail);
        btnSalvarInfo = findViewById(R.id.btnSalvarInfo);
        btnSair = findViewById(R.id.btnSair);

        configurarMascaraData();

        // Carregar dados salvos
        editNome.setText(prefs.getString("nome", ""));
        editDataNascimento.setText(prefs.getString("nascimento", ""));
        if (mAuth.getCurrentUser() != null) {
            editNovoEmail.setText(mAuth.getCurrentUser().getEmail());
        }
        editRenda.setText(prefs.getString("renda", ""));
        editTotalInvestido.setText(prefs.getString("investimentos", ""));

        btnAbrirMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        if (navView != null) {
            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_inicio) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else if (id == R.id.nav_agenda) {
                    startActivity(new Intent(this, AgendaActivity.class));
                    finish();
                } else if (id == R.id.nav_financas) {
                    startActivity(new Intent(this, FinanceiroActivity.class));
                    finish();
                } else if (id == R.id.nav_perfil) {
                    // Já está no perfil
                }
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return true;
            });
        }
        
        btnAtualizarEmail.setOnClickListener(v -> atualizarEmail());
        
        btnSalvarInfo.setOnClickListener(v -> salvarInformacoes());
        
        btnSair.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
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

    private void atualizarEmail() {
        String novoEmail = editNovoEmail.getText().toString();
        if (novoEmail.isEmpty()) return;

        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().updateEmail(novoEmail)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "E-mail atualizado!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Erro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void salvarInformacoes() {
        String nome = editNome.getText().toString();
        String nascimento = editDataNascimento.getText().toString();
        String renda = editRenda.getText().toString();
        String invest = editTotalInvestido.getText().toString();
        
        prefs.edit()
                .putString("nome", nome)
                .putString("nascimento", nascimento)
                .putString("renda", renda)
                .putString("investimentos", invest)
                .apply();
        
        Toast.makeText(this, "Informações salvas!", Toast.LENGTH_SHORT).show();
    }
}