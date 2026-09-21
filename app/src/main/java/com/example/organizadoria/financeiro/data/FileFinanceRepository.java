package com.example.organizadoria.financeiro.data;

import com.example.organizadoria.financeiro.domain.FinanceState;
import com.example.organizadoria.financeiro.domain.FinanceEngine;
import java.io.*;
import java.util.*;

/** Persistência transacional da cópia local. Sem rede. Arquivo privado, separado por usuário. */
public final class FileFinanceRepository implements FinanceRepository {
    private final File file;
    private FinanceState state;
    private final Set<Runnable> listeners=new HashSet<>();
    public FileFinanceRepository(File file,String userId) {
        this.file=file;
        if(file.exists()) {
            try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(file))) {
                state=(FinanceState)in.readObject();
                if(state.schemaVersion!=1 || !Objects.equals(userId,state.userId)) throw new IOException("Versão ou usuário incompatível.");
                FinanceEngine.validateState(state);
            } catch(IOException | ClassNotFoundException | ClassCastException e) {
                throw new IllegalStateException("Não foi possível abrir os dados financeiros. O arquivo original foi preservado.",e);
            }
        } else state=FinanceState.empty(userId);
    }
    @Override public synchronized FinanceState snapshot() { return state.copy(); }
    @Override public void update(Mutation mutation) {
        List<Runnable> notify;
        synchronized(this) {
            FinanceState next=state.copy();
            mutation.apply(next); // Erros abortam a transação inteira, incluindo comando composto Agenda + despesa.
            if(!Objects.equals(state.userId,next.userId))throw new IllegalStateException("A operação não pode alterar o usuário.");
            FinanceEngine.validateState(next);
            next.revision=Math.addExact(state.revision,1);
            File parent=file.getParentFile();
            if(parent!=null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Não foi possível criar o armazenamento.");
            File tmp=new File(file.getPath()+".pending");
            try {
                try(FileOutputStream bytes=new FileOutputStream(tmp); ObjectOutputStream out=new ObjectOutputStream(bytes)) {
                    out.writeObject(next); out.flush(); bytes.getFD().sync();
                }
                if(!tmp.renameTo(file)) throw new IOException("Não foi possível substituir o arquivo de forma atômica.");
            } catch(IOException e) {
                tmp.delete(); throw new IllegalStateException("Não foi possível salvar. Os dados anteriores foram preservados.",e);
            }
            state=next; notify=new ArrayList<>(listeners);
        }
        for(Runnable r:notify) try { r.run(); } catch(RuntimeException ignored) { /* Observer não pode invalidar uma gravação já confirmada. */ }
    }
    @Override public synchronized void addListener(Runnable listener) { listeners.add(listener); }
    @Override public synchronized void removeListener(Runnable listener) { listeners.remove(listener); }
}
