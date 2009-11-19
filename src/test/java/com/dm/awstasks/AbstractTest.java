package com.dm.awstasks;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class AbstractTest {

    @Rule
    public PrintTestNameRule _printPrintTestNameRule = new PrintTestNameRule();
    private static String TEST_CLASS_NAME;

    @AfterClass
    public static void printOutTestClassName() {
        System.out.println("~~~~~~~~~~~~~~~ FIN " + TEST_CLASS_NAME + " ~~~~~~~~~~~~~~~");
    }

    @Before
    public final void setClassName() throws Exception {
        TEST_CLASS_NAME = getClass().getName();
    }

    static class PrintTestNameRule implements MethodRule {
        public Statement apply(Statement base, FrameworkMethod method, Object target) {
            System.out.println("~~~~~~~~~~~~~~~ " + target.getClass().getName() + "#" + method.getName() + "() ~~~~~~~~~~~~~~~");
            return base;
        }
    }

}
