package com.dm.awstasks;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;

public class AbstractTest {

    private Map<String, File> _testName2rootFolder = new HashMap<String, File>();

    @After
    public final void cleanUpTempFolders() throws Exception {
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

}
