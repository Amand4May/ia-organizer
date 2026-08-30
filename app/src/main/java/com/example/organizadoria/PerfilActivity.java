package com.example.organizadoria;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class PerfilActivity extends AppCompatActivity {

    private TextInputEditText editNovoEmail, editRenda, editTotalInvestido;
    private MaterialButton btnAtualizarEmail, btnSalvarInfo, btnSair;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("DadosPerfil", MODE_PRIVATE);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        editNovoEmail = findViewById(R.id.editNovoEmail);
        editRenda = findViewById(R.id.editRenda);
        editTotalInvestido = findViewById(R.id.editTotalInvestido);
        btnAtualizarEmail = findViewById(R.id.btnAtualizarEmail);
        btnSalvarInfo = findViewById(R.id.btnSalvarInfo);
        btnSair = findViewById(R.id.btnSair);

        // Carregar dados salvos
        if (mAuth.getCurrentUser() != null) {
            editNovoEmail.setText(mAuth.getCurrentUser().getEmail());
        }
        editRenda.setText(prefs.getString("renda", ""));
        editTotalInvestido.setText(prefs.getString("investimentos", ""));

        btnVoltar.setOnClickListener(v -> finish());
        
        btnAtualizarEmail.setOnClickListener(v -> atualizarEmail());
        
        btnSalvarInfo.setOnClickListener(v -> salvarInformacoes());
        
        btnSair.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void atualizarEmail() {
        String novoEmail = editNovoEmail.getText().toString();
        if (novoEmail.isEmpty()) return;

        mAuth.getCurrentUser().updateEmail(novoEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "E-mail atualizado!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void salvarInformacoes() {
        String renda = editRenda.getText().toString();
        String invest = editTotalInvestido.getText().toString();
        
        prefs.edit()
                .putString("renda", renda)
                .putString("investimentos", invest)
                .apply();
        
        Toast.makeText(this, "Informações salvas!", Toast.LENGTH_SHORT).show();
    }
}