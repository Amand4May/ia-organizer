package com.example.organizadoria.financeiro.domain;

public final class OrganizerCommand {
    public enum Action { TASK, PLAN_EXPENSE, EXPENSE, INCOME, SALARY, CONFIG_SALARY, CONFIG_CONTRIBUTION,
        CONTRIBUTION, PLAN_CONTRIBUTION, WITHDRAWAL, RECURRING_BILL, SET_INVESTED, CONFIG_RETURN,
        SIMULATE, REQUIRED_CONTRIBUTION, GOAL }
    public Action action;
    public String title="", date, time="09:00", targetId;
    public long amount;
    public int day, months=60, basisPoints;
    public double annualRate=Double.NaN;
    public boolean percent, partial, amountSpecified;
    public OrganizerCommand(Action action) { this.action=action; }
}
