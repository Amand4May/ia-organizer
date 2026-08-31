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

    @Query("SELECT * FROM tabela_tarefas WHERE userId = :uId ORDER BY data ASC, id DESC")
    List<Tarefa> buscarTodas(String uId);

    @Query("SELECT * FROM tabela_tarefas WHERE userId = :uId AND tipo != 'tarefa' ORDER BY data DESC, id DESC")
    List<Tarefa> buscarApenasFinancas(String uId);

    @Query("SELECT * FROM tabela_tarefas WHERE userId = :uId AND tipo = 'tarefa' ORDER BY data ASC, horario ASC")
    List<Tarefa> buscarApenasTarefas(String uId);
}