package com.example.organizadoria;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TarefaAdapter extends RecyclerView.Adapter<TarefaAdapter.TarefaViewHolder> {

    private List<Tarefa> listaTarefas = new ArrayList<>();
    private OnTarefaLongClickListener longClickListener;

    public interface OnTarefaLongClickListener {
        void onTarefaLongClick(Tarefa tarefa);
    }

    public void setOnTarefaLongClickListener(OnTarefaLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void adicionarTarefa(Tarefa novaTarefa) {
        listaTarefas.add(0, novaTarefa);
        notifyItemInserted(0);
    }

    public void carregarListaCompleta(List<Tarefa> tarefasDoBanco) {
        this.listaTarefas.clear();
        this.listaTarefas.addAll(tarefasDoBanco);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TarefaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tarefa, parent, false);
        return new TarefaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TarefaViewHolder holder, int position) {
        Tarefa tarefa = listaTarefas.get(position);

        holder.textDescricao.setText(tarefa.getDescricao());
        holder.textSubtitulo.setText(tarefa.getDataExibicao()); // Usando a data como subtitulo padrão

        String tipo = tarefa.getTipo() == null ? "tarefa" : tarefa.getTipo().toLowerCase(java.util.Locale.ROOT);

        if (tipo.contains("receita")) {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_icon_receita);
            holder.imgIcon.setImageResource(R.drawable.ic_target_blue);
            holder.badgeContainer.setBackgroundResource(R.drawable.bg_badge_receita);
            holder.textBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.cor_receita));
            holder.textBadge.setText("+" + com.example.organizadoria.financeiro.domain.Money.format(Math.round(tarefa.getValor() * 100)));
            if (tarefa.data == null) holder.textSubtitulo.setText(tarefa.gastoPago ? "Receita · recebida" : "Recebimento previsto");

        } else if (tipo.contains("despesa")) {
            // Mesmas cores das bolinhas: amarelo = previsto, vermelho = realizado
            int cor = ContextCompat.getColor(holder.itemView.getContext(), tarefa.gastoPago ? R.color.cor_vermelho : R.color.cor_previsto);
            holder.iconContainer.setBackgroundResource(R.drawable.bg_icon_despesa);
            holder.imgIcon.setImageResource(R.drawable.ic_card_yellow);
            holder.badgeContainer.setBackgroundResource(R.drawable.bg_badge_despesa);
            holder.textBadge.setTextColor(cor);
            holder.textBadge.setText(com.example.organizadoria.financeiro.domain.Money.format(Math.round(tarefa.getValor() * 100)));
            if (tarefa.data == null) holder.textSubtitulo.setText(tarefa.gastoPago ? "Gasto realizado" : "Gasto previsto");

        } else {
            // Tarefa
            holder.iconContainer.setBackgroundResource(R.drawable.bg_icon_tarefa);
            holder.imgIcon.setImageResource(R.drawable.ic_calendar_cyan);
            holder.badgeContainer.setBackgroundResource(R.drawable.bg_badge_tarefa);
            holder.textBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.ciano));

            String hoje = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            String amanha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            String dataTarefa = tarefa.getData();
            String sufixo = "";
            if (dataTarefa != null) {
                if (dataTarefa.equals(hoje)) {
                    sufixo = " hoje";
                } else if (dataTarefa.equals(amanha)) {
                    sufixo = " amanhã";
                }
            }

            holder.textBadge.setText(tarefa.getHorario() + sufixo);
            String agendado = "Agendado para " + tarefa.getDataExibicao();
            if (tarefa.gastoCentavos > 0) {
                // Valor na cor da bolinha da agenda: amarelo = previsto, vermelho = pago
                String valor = com.example.organizadoria.financeiro.domain.Money.format(tarefa.gastoCentavos)
                        + (tarefa.gastoPago ? " pago" : " previsto");
                android.text.SpannableString texto = new android.text.SpannableString(agendado + " · " + valor);
                int cor = ContextCompat.getColor(holder.itemView.getContext(), tarefa.gastoPago ? R.color.cor_vermelho : R.color.cor_previsto);
                texto.setSpan(new android.text.style.ForegroundColorSpan(cor), texto.length() - valor.length(), texto.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.textSubtitulo.setText(texto);
            } else {
                holder.textSubtitulo.setText(agendado);
            }
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onTarefaLongClick(tarefa);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return listaTarefas.size();
    }

    static class TarefaViewHolder extends RecyclerView.ViewHolder {
        FrameLayout iconContainer;
        ImageView imgIcon;
        TextView textDescricao, textSubtitulo, textBadge;
        LinearLayout badgeContainer;

        public TarefaViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            textDescricao = itemView.findViewById(R.id.textDescricao);
            textSubtitulo = itemView.findViewById(R.id.textSubtitulo);
            textBadge = itemView.findViewById(R.id.textBadge);
            badgeContainer = itemView.findViewById(R.id.badgeContainer);
        }
    }
}