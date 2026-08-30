package com.example.organizadoria;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TarefaDao {

    @Insert
    void inserir(Tarefa tarefa);

    @Delete
    void deletar(Tarefa tarefa);

    // Traz todas as tarefas, ordenadas pela data (mais próxima primeiro)
    // Se a data for igual, o id DESC mostra o mais recente adicionado antes
    @Query("SELECT * FROM tabela_tarefas ORDER BY data ASC, id DESC")
    List<Tarefa> buscarTodas();

    // Traz apenas o que for do tipo "despesa" ou "receita" para a aba financeira
    @Query("SELECT * FROM tabela_tarefas WHERE tipo != 'tarefa' ORDER BY data DESC, id DESC")
    List<Tarefa> buscarApenasFinancas();

    // Traz apenas o que for do tipo "tarefa" para a agenda
    @Query("SELECT * FROM tabela_tarefas WHERE tipo = 'tarefa' ORDER BY data ASC, horario ASC")
    List<Tarefa> buscarApenasTarefas();
}