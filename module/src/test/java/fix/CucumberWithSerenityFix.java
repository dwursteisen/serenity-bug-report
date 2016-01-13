package fix;

import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import gherkin.formatter.model.DataTableRow;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Step;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import net.serenitybdd.cucumber.SerenityReporter;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.webdriver.Configuration;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CucumberWithSerenityFix extends CucumberWithSerenity {
    public CucumberWithSerenityFix(Class clazz) throws InitializationError, IOException {
        super(clazz);
    }

    /**
     * Create the Runtime. Sets the Serenity runtime.
     */
    protected cucumber.runtime.Runtime createRuntime(ResourceLoader resourceLoader, ClassLoader classLoader,
                                                     RuntimeOptions runtimeOptions) throws InitializationError, IOException {
        return createSerenityEnabledRuntime(resourceLoader, classLoader, runtimeOptions);
    }

    private Runtime createSerenityEnabledRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, RuntimeOptions runtimeOptions) {
        Configuration systemConfiguration = Injectors.getInjector().getInstance(Configuration.class);
        return createSerenityEnabledRuntime(resourceLoader, classLoader, runtimeOptions, systemConfiguration);
    }

    public static Runtime createSerenityEnabledRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, RuntimeOptions runtimeOptions, Configuration systemConfiguration) {
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        SerenityReporter reporter = new SerenityReporterWithPendingSupport(systemConfiguration);
        runtimeOptions.addPlugin(reporter);
        return new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }

    private static class SerenityReporterWithPendingSupport extends SerenityReporter {

        private final Queue<Step> stepQueueFromSuper;

        public SerenityReporterWithPendingSupport(Configuration systemConfiguration) {
            super(systemConfiguration);
            Queue<Step> queue;
            try {
                Field field = SerenityReporter.class.getDeclaredField("stepQueue");
                field.setAccessible(true);
                queue = (Queue<Step>) field.get(this);
                field.setAccessible(false);
            } catch (NoSuchFieldException e) {
                queue = new LinkedList<Step>();
            } catch (IllegalAccessException e) {
                queue = new LinkedList<Step>();
            }
            stepQueueFromSuper = queue;
        }

        @Override
        public void result(Result result) {
            Step currentStep = stepQueueFromSuper.peek();

            if ("pending".equals(result.getStatus())) {
                StepEventBus.getEventBus().stepStarted(ExecutedStepDescription.withTitle(stepTitleFrom(currentStep)));
                StepEventBus.getEventBus().stepPending();
            }
            super.result(result);
        }

        /* COPIED FROM SerenityReporter class */
        private String stepTitleFrom(Step currentStep) {
            return currentStep.getKeyword()
                    + currentStep.getName()
                    + embeddedTableDataIn(currentStep);
        }

        private String embeddedTableDataIn(Step currentStep) {
            return (currentStep.getRows() == null || currentStep.getRows().isEmpty()) ?
                    "" : convertToTextTable(currentStep.getRows());
        }

        private String convertToTextTable(List<DataTableRow> rows) {
            StringBuilder textTable = new StringBuilder();
            textTable.append(System.lineSeparator());
            for (DataTableRow row : rows) {
                textTable.append("|");
                for (String cell : row.getCells()) {
                    textTable.append(" ");
                    textTable.append(cell);
                    textTable.append(" |");
                }
                if (row != rows.get(rows.size() - 1)) {
                    textTable.append(System.lineSeparator());
                }
            }
            return textTable.toString();
        }

    }
}
