package com.example.organizadoria;

import com.example.organizadoria.financeiro.domain.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Exercita JSON sintético; nunca chama a API da IA. Requer Gson resolvido pelo Gradle. */
public class RemoteCommandCodecTest {
    private static final String TODAY="2026-09-05";
    @Test public void taskKeepsApostropheAndLinksExpense() {
        List<OrganizerCommand> commands=RemoteCommandCodec.parse("```json\n[{\"acao\":\"TASK\",\"descricao\":\"Corte d'água\",\"valor\":45,\"data\":\"2026-09-06\",\"horario\":\"09:00\"}]\n```",TODAY);
        FinanceState state=FinanceState.empty("test");CommandRouter.apply(state,commands,"one",TODAY);
        assertEquals("Corte d'água",state.agenda.get(0).title);
        assertEquals(4500,FinanceEngine.summarize(state,TODAY).committed);
        assertEquals(0,state.entries.size());assertNotNull(state.agenda.get(0).forecastId);
    }
    @Test public void salaryRuleDoesNotInventReceipt() {
        String json="[{\"acao\":\"CONFIG_SALARY\",\"valor\":3000,\"dia\":5},{\"acao\":\"CONFIG_CONTRIBUTION\",\"percentual\":10}]";
        FinanceState state=FinanceState.empty("test");CommandRouter.apply(state,RemoteCommandCodec.parse(json,TODAY),"setup",TODAY);
        assertEquals(300000,state.settings.salaryCents);assertEquals(1000,state.settings.contributionBasisPoints);
        assertEquals(0,FinanceEngine.summarize(state,TODAY).cash);
    }
    @Test public void rejectsMalformedOrUnknownActionsBeforeApplyingAnything() {
        for(String json:Arrays.asList("{}","[]","[{\"acao\":\"DELETE_EVERYTHING\"}]","[{\"acao\":\"EXPENSE\",\"valor\":1.001}]",
                "[{\"acao\":\"EXPENSE\",\"valor\":-1}]","[{\"acao\":\"TASK\",\"data\":\"2026-02-30\"}]","[{\"acao\":\"CONFIG_CONTRIBUTION\",\"percentual\":101}]")) {
            try{RemoteCommandCodec.parse(json,TODAY);fail("Deveria rejeitar JSON inválido");}catch(RuntimeException expected){}
        }
    }
    @Test public void explicitZeroMonthlyContributionIsPreserved() {
        OrganizerCommand command=RemoteCommandCodec.parse("[{\"acao\":\"SIMULATE\",\"valor\":0,\"meses\":60}]",TODAY).get(0);
        assertTrue(command.amountSpecified);assertEquals(0,command.amount);
    }
}
