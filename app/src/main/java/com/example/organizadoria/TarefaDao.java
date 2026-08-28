package com.example.organizadoria;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TarefaDao {

    @Insert
    void inserir(Tarefa tarefa);

    // Traz todas as tarefas, ordenadas pela data mais próxima
    @Query("SELECT * FROM tabela_tarefas ORDER BY data ASC")
    List<Tarefa> buscarTodas();

    // Traz apenas o que for do tipo "despesa" ou "receita" para a aba financeira
    @Query("SELECT * FROM tabela_tarefas WHERE tipo != 'tarefa'")
    List<Tarefa> buscarApenasFinancas();
}