package com.example.organizadoria.financeiro.data;

import com.example.organizadoria.financeiro.domain.FinanceState;

/** A interface deixa telas e regras independentes da implementação de armazenamento. */
public interface FinanceRepository {
    interface Mutation { void apply(FinanceState state); }
    FinanceState snapshot();
    void update(Mutation mutation);
    void addListener(Runnable listener);
    void removeListener(Runnable listener);
}
