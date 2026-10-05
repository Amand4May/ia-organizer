package com.example.organizadoria.financeiro.domain;

import java.io.*;
import java.util.*;

/** Estado de domínio serializável. Só é alterado dentro de uma transação do repositório. */
public final class FinanceState implements Serializable {
    private static final long serialVersionUID = 1L;
    public int schemaVersion = 1;
    public long revision;
    public String userId;
    public Settings settings = new Settings();
    public List<Account> accounts = new ArrayList<>();
    public List<Forecast> forecasts = new ArrayList<>();
    public List<Entry> entries = new ArrayList<>();
    public List<Rule> rules = new ArrayList<>();
    public List<Cycle> cycles = new ArrayList<>();
    public List<Goal> goals = new ArrayList<>();
    public List<Allocation> allocations = new ArrayList<>();
    public List<AgendaItem> agenda = new ArrayList<>();
    public Set<String> appliedCommands = new HashSet<>();
    public Set<String> importedLegacyIds = new HashSet<>();
    public List<LegacyItem> legacy = new ArrayList<>();
    public String activeCycleId;

    public enum AccountKind { CASH, INVESTMENT }
    public enum Kind { INCOME, EXPENSE, CONTRIBUTION, TRANSFER, VALUATION }
    public enum PlanStatus { PLANNED, PARTIAL, SETTLED, CANCELLED }
    public enum RemainderDestination { CARRY, GOAL, PLAN_CONTRIBUTION, CONTRIBUTION }

    public static final class Settings implements Serializable {
        private static final long serialVersionUID = 1L;
        public boolean configured, balancesConfirmed;
        public long salaryCents, contributionCents;
        public int payDay = 5, contributionBasisPoints;
        public boolean percentContribution;
        public double expectedAnnualReturn = 8;
        public String validFrom, cashAccountId, investmentAccountId, lastScheduledOn;
    }
    public static final class Account implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, name, openingDate, openingCycleId;
        public AccountKind kind;
        public String yieldIndex;
        public double yieldPercent,expectedAnnualRate;
        public long openingCents;
        public Account() {}
        public Account(String id, String name, AccountKind kind, long opening, String date) {
            this.id=id; this.name=name; this.kind=kind; openingCents=opening; openingDate=date;
        }
    }
    public static final class Forecast implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, title, date, accountId, targetAccountId, ruleId, agendaId, cycleId;
        public Kind kind;
        public long amountCents;
        public boolean completed, cancelled, cancelledBySalaryReversal, settledBelowPlan;
        public String closedDate;
    }
    public static final class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, title, date, fromAccountId, toAccountId, forecastId, cycleId;
        public Kind kind;
        public long amountCents;
        public boolean voided;
        public String voidReason;
    }
    public static final class Rule implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, title, startDate, endDate, accountId;
        public long amountCents;
        public int day;
        public boolean active = true;
    }
    public static final class Cycle implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, startDate, nextPayDate, closedDate, salaryEntryId, closeOperationId;
        public boolean closed;
        public long openingCash, income, expenses, contributions, withdrawals, adjustments, closingCash, freeRemainder;
        public long committed, pendingContributions, cashReservations, remainderAllocated;
        public String remainderDestination = "Carregado para o próximo ciclo";
    }
    public static final class Goal implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, title, deadline;
        public long targetCents;
    }
    public static final class Allocation implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, goalId, accountId, date;
        public long amountCents;
    }
    public static final class AgendaItem implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, title, date, time, forecastId;
        public boolean cancelled;
    }
    public static final class LegacyItem implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, title, date, type;
        public long amountCents;
        public boolean reviewed;
    }
    public static FinanceState empty(String uid) {
        FinanceState s=new FinanceState(); s.userId=Objects.requireNonNull(uid); return s;
    }
    public FinanceState copy() {
        try {
            ByteArrayOutputStream b=new ByteArrayOutputStream();
            try (ObjectOutputStream o=new ObjectOutputStream(b)) { o.writeObject(this); }
            try (ObjectInputStream i=new ObjectInputStream(new ByteArrayInputStream(b.toByteArray()))) {
                return (FinanceState)i.readObject();
            }
        } catch (IOException | ClassNotFoundException e) { throw new IllegalStateException("Não foi possível copiar os dados.",e); }
    }
}
