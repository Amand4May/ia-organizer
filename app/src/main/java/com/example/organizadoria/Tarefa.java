package com.example.organizadoria;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tabela_tarefas")
public class Tarefa {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String tipo;
    public String descricao;
    public double valor;
    public String data;
    public String horario;

    public Tarefa(String tipo, String descricao, double valor, String data, String horario) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.horario = horario;
    }

    // Os métodos GET (getTipo, getDescricao...) que você já tinha continuam aqui embaixo
    public String getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public double getValor() { return valor; }
    public String getData() { return data; }
    public String getHorario() { return horario; }
}