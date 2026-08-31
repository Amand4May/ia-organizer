package com.example.organizadoria;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tabela_tarefas")
public class Tarefa {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId; // ID do usuário para separar os dados
    public String tipo;
    public String descricao;
    public double valor;
    public String data;
    public String horario;

    public Tarefa(String userId, String tipo, String descricao, double valor, String data, String horario) {
        this.userId = userId;
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.horario = horario;
    }

    public String getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public double getValor() { return valor; }
    public String getData() { return data; }
    public String getHorario() { return horario; }
    public String getUserId() { return userId; }

    public String getDataExibicao() {
        if (data == null || !data.contains("-")) return data;
        String[] partes = data.split("-");
        if (partes.length == 3) {
            return partes[2] + "/" + partes[1] + "/" + partes[0];
        }
        return data;
    }
}