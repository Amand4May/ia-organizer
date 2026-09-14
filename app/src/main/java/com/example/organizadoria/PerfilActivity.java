package com.example.organizadoria;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PerfilActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private EditText editNome, editDataNascimento, editRenda, editTotalInvestido, editAssinaturas, editNovoEmail;
    private TextView textIniciais;
    private MaterialButton btnSalvarInfo, btnSair;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_perfil);

        mAuth = FirebaseAuth.getInstance();
        String currentUserId = mAuth.getUid();
        prefs = getSharedPreferences("DadosPerfil_" + currentUserId, MODE_PRIVATE);

        editNome = findViewById(R.id.editNome);
        editDataNascimento = findViewById(R.id.editDataNascimento);
        editRenda = findViewById(R.id.editRenda);
        editTotalInvestido = findViewById(R.id.editTotalInvestido);
        editAssinaturas = findViewById(R.id.editAssinaturas);
        editNovoEmail = findViewById(R.id.editNovoEmail);
        btnSalvarInfo = findViewById(R.id.btnSalvarInfo);
        btnSair = findViewById(R.id.btnSair);
        textIniciais = findViewById(R.id.textIniciais);

        configurarPincéisEEdicao();
        configurarMascaraData();

        // Carregar dados salvos
        String nomeCarregado = prefs.getString("nome", "");
        editNome.setText(nomeCarregado);
        atualizarIniciais(nomeCarregado);
        
        editDataNascimento.setText(prefs.getString("nascimento", ""));
        editRenda.setText(prefs.getString("renda", ""));
        editTotalInvestido.setText(prefs.getString("investimentos", ""));
        editAssinaturas.setText(prefs.getString("assinaturas", ""));

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            editNovoEmail.setText(user.getEmail());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_agenda) {
                startActivity(new Intent(this, AgendaActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_financas) {
                startActivity(new Intent(this, FinanceiroActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_perfil) {
                // Já está no perfil
            }
            return true;
        });
        
        btnSalvarInfo.setOnClickListener(v -> salvarInformacoes());
        
        btnSair.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void configurarPincéisEEdicao() {
        ImageView iconEditNome = findViewById(R.id.iconEditNome);
        ImageView iconEditData = findViewById(R.id.iconEditData);
        ImageView iconEditRenda = findViewById(R.id.iconEditRenda);
        ImageView iconEditAssinaturas = findViewById(R.id.iconEditAssinaturas);
        ImageView iconEditInvest = findViewById(R.id.iconEditInvest);
        ImageView iconEditEmail = findViewById(R.id.iconEditEmail);

        View.OnClickListener clickNome = v -> focarECampo(editNome);
        if (iconEditNome != null) iconEditNome.setOnClickListener(clickNome);

        View.OnClickListener clickData = v -> focarECampo(editDataNascimento);
        if (iconEditData != null) iconEditData.setOnClickListener(clickData);

        View.OnClickListener clickRenda = v -> focarECampo(editRenda);
        if (iconEditRenda != null) iconEditRenda.setOnClickListener(clickRenda);

        View.OnClickListener clickAssinaturas = v -> focarECampo(editAssinaturas);
        if (iconEditAssinaturas != null) iconEditAssinaturas.setOnClickListener(clickAssinaturas);

        View.OnClickListener clickInvest = v -> focarECampo(editTotalInvestido);
        if (iconEditInvest != null) iconEditInvest.setOnClickListener(clickInvest);

        View.OnClickListener clickEmail = v -> focarECampo(editNovoEmail);
        if (iconEditEmail != null) iconEditEmail.setOnClickListener(clickEmail);
    }

    private void focarECampo(EditText campo) {
        campo.requestFocus();
        campo.setSelection(campo.getText().length());
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(campo, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void atualizarIniciais(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            textIniciais.setText("?");
            return;
        }
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            textIniciais.setText(partes[0].substring(0, 1).toUpperCase());
        } else {
            textIniciais.setText((partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase());
        }
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

    private void salvarInformacoes() {
        String nome = editNome.getText().toString().trim();
        String nascimento = editDataNascimento.getText().toString().trim();
        String renda = editRenda.getText().toString().trim();
        String invest = editTotalInvestido.getText().toString().trim();
        String assinaturas = editAssinaturas.getText().toString().trim();
        String novoEmail = editNovoEmail.getText().toString().trim();

        // Salvar localmente
        prefs.edit()
                .putString("nome", nome)
                .putString("nascimento", nascimento)
                .putString("renda", renda)
                .putString("investimentos", invest)
                .putString("assinaturas", assinaturas)
                .apply();

        atualizarIniciais(nome);

        // Atualizar e-mail no Firebase se alterado
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !novoEmail.isEmpty() && !novoEmail.equals(user.getEmail())) {
            user.updateEmail(novoEmail).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "E-mail e perfil atualizados no Firebase!", Toast.LENGTH_SHORT).show();
                } else {
                    String msg = task.getException() != null ? task.getException().getMessage() : "Erro";
                    Toast.makeText(this, "Perfil salvo! (E-mail não atualizado no Firebase: " + msg + ")", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "Alterações salvas com sucesso!", Toast.LENGTH_SHORT).show();
        }
    }
}