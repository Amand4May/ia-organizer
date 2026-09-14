package com.example.organizadoria;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.Exclude;

@Entity(tableName = "tabela_tarefas")
public class Tarefa {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @Exclude
    @Ignore
    public String docId; // ID do documento no Firestore

    public String userId; // ID do usuário para separar os dados
    public String tipo;
    public String descricao;
    public double valor;
    public String data;
    public String horario;

    // Construtor vazio necessário para o Firestore deserializar o objeto
    public Tarefa() {}

    public Tarefa(String userId, String tipo, String descricao, double valor, String data, String horario) {
        this.userId = userId;
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.horario = horario;
    }

    public String getTipo() { return tipo; }
    public String getDescricao() {
        if (descricao == null || descricao.trim().isEmpty()) {
            return "";
        }
        String d = descricao.trim();
        return d.substring(0, 1).toUpperCase() + d.substring(1);
    }
    public double getValor() { return valor; }
    public String getData() { return data; }
    public String getHorario() { return horario; }
    public String getUserId() { return userId; }

    @Exclude
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getDataExibicao() {
        if (data == null || !data.contains("-")) return data;
        String[] partes = data.split("-");
        if (partes.length == 3) {
            return partes[2] + "/" + partes[1] + "/" + partes[0];
        }
        return data;
    }
}