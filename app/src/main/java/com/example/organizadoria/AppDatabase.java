package com.example.organizadoria;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Tarefa.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TarefaDao tarefaDao();
}