import org.junit.Test;

/** A mesma suíte pode rodar via Gradle ou pelo script Java sem Android SDK. */
public class FinanceRegressionTest {
    @Test public void financialBehaviorAndPersistence() throws Exception {
        FinanceTestSuite.main(new String[0]);
    }
}
