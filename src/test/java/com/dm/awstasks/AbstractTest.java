package com.dm.awstasks;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class AbstractTest {

    @Rule
    public PrintTestNameRule _printPrintTestNameRule = new PrintTestNameRule();
    private static String TEST_CLASS_NAME;
    private Map<String, File> _testName2rootFolder = new HashMap<String, File>();

    @AfterClass
    public static void printOutTestClassName() {
        System.out.println("~~~~~~~~~~~~~~~ FIN " + TEST_CLASS_NAME + " ~~~~~~~~~~~~~~~");
    }

    @After
    public final void cleanUpTempFolders() throws Exception {
        TEST_CLASS_NAME = getClass().getName();
        Collection<File> files = _testName2rootFolder.values();
        for (File file : files) {
            deleteDirectory(file);
        }
    }

    /**
     * Creates a file in a temporary empty folder. On tearing the test down this folder will be
     * deleted.
     */
    protected File createTmpFile(String name) {
        File rootFolder = getTmpRootFolder();
        return new File(rootFolder, name);
    }

    private File getTmpRootFolder() {
        String testName = this.getClass().getSimpleName();
        File rootFolder = _testName2rootFolder.get(testName);
        if (rootFolder == null) {
            try {
                rootFolder = File.createTempFile(testName + "_", ".tmp");
            } catch (IOException e) {
                throw new RuntimeException("could not create tmp file", e);
            }
            rootFolder.delete();
            rootFolder.mkdirs();
            _testName2rootFolder.put(testName, rootFolder);
        }

        return rootFolder;
    }

    protected final static void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                files[i].delete();
            } else {
                deleteDirectory(files[i]);
            }
        }

        if (!directory.delete()) {
            throw new IOException("directory could not be fully deleted");
        }
    }

    static class PrintTestNameRule implements MethodRule {
        public Statement apply(Statement base, FrameworkMethod method, Object target) {
            System.out.println("~~~~~~~~~~~~~~~ " + target.getClass().getName() + "#" + method.getName() + "() ~~~~~~~~~~~~~~~");
            return base;
        }
    }

}
