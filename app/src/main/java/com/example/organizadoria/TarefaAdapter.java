package com.example.organizadoria;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TarefaAdapter extends RecyclerView.Adapter<TarefaAdapter.TarefaViewHolder> {

    private List<Tarefa> listaTarefas = new ArrayList<>();

    // Método para adicionar um item novo e avisar a tela para atualizar a interface
    public void adicionarTarefa(Tarefa novaTarefa) {
        listaTarefas.add(0, novaTarefa); // Adiciona no topo da lista
        notifyItemInserted(0);
    }

    // Método para carregar a lista do banco de dados ao abrir o app
    public void carregarListaCompleta(List<Tarefa> tarefasDoBanco) {
        this.listaTarefas.clear();
        this.listaTarefas.addAll(tarefasDoBanco);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TarefaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Infla" (renderiza) o arquivo XML na memória
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tarefa, parent, false);
        return new TarefaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TarefaViewHolder holder, int position) {
        Tarefa tarefa = listaTarefas.get(position);

        holder.textTipo.setText(tarefa.getTipo());
        holder.textDescricao.setText(tarefa.getDescricao());
        holder.textValor.setText(String.format("R$ %.2f", tarefa.getValor()));
        holder.textData.setText(tarefa.getData());
    }

    @Override
    public int getItemCount() {
        return listaTarefas.size();
    }

    // Classe interna que mapeia as textviews do XML para a memória do Java
    static class TarefaViewHolder extends RecyclerView.ViewHolder {
        TextView textTipo, textDescricao, textValor, textData;

        public TarefaViewHolder(@NonNull View itemView) {
            super(itemView);
            textTipo = itemView.findViewById(R.id.textTipo);
            textDescricao = itemView.findViewById(R.id.textDescricao);
            textValor = itemView.findViewById(R.id.textValor);
            textData = itemView.findViewById(R.id.textData);
        }
    }
}