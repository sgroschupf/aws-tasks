/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datameer.awstasks.ant.junit;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitVersionHelper;
import org.apache.tools.ant.util.StringUtils;

/**
 * Enhanced version of the
 * {@link org.apache.tools.ant.taskdefs.optional.junit.BriefJUnitResultFormatter}. Inspired by
 * http://shaman-sir.wikidot.com/one-liner-output-formatter.
 */
public class EnhancedJunitFormatter implements JUnitResultFormatter, TestListener {
    // BuildListener
    private final String TAB_STR = "    ";

    private final boolean showCausesLines = true;
    // (\w+\.)+(\w+)\((\w+).(?:\w+):(\d+)\)
    private final Pattern traceLinePattern = Pattern.compile("(\\w+\\.)+(\\w+)\\((\\w+).(?:\\w+):(\\d+)\\)");

    private OutputStream out;
    private PrintWriter output;
    // private StringWriter results;
    // private PrintWriter resultWriter;
    private NumberFormat numberFormat = NumberFormat.getInstance();

    private String _systemOutput = null;
    private String _systemError = null;

    private Map<Test, Throwable> failedTests = new LinkedHashMap<Test, Throwable>();
    private Hashtable testStarts = new Hashtable();
    private final boolean _showOutput;

    public EnhancedJunitFormatter() {
        // results = new StringWriter();
        // resultWriter = new PrintWriter(results);
        _showOutput = System.getProperty("showOutput", "false").equals("true");
    }

    @Override
    public void setOutput(OutputStream out) {
        this.out = out;
        output = new PrintWriter(out);
    }

    @Override
    public void setSystemOutput(String out) {
        _systemOutput = out;
    }

    @Override
    public void setSystemError(String err) {
        _systemError = err;
    }

    @Override
    public void startTestSuite(JUnitTest suite) {
        output.write(fillWithWhiteSpace(String.format("Running %s... ", suite.getName()), 60));
    }

    @Override
    public void endTestSuite(JUnitTest suite) {
        if (suite.failureCount() > 0 || suite.errorCount() > 0) {
            output.write("FAILED");
        } else if (suite.runCount() == 0) {
            output.write("IGNORED");
        } else {
            output.write("SUCCEED");
        }
        // output.write(StringUtils.LINE_SEP);
        StringBuilder sb = new StringBuilder("\t- Tests: ");
        sb.append(suite.runCount());
        sb.append(", Failures: ");
        sb.append(suite.failureCount());
        sb.append(", Errors: ");
        sb.append(suite.errorCount());
        sb.append(", Time: ");
        sb.append(formatTimeDuration(suite.getRunTime()));
        sb.append(StringUtils.LINE_SEP);

        // append the err and output streams to the log
        if (_showOutput) {
            if (_systemOutput != null && _systemOutput.length() > 0) {
                sb.append("------------- Standard Output ---------------").append(StringUtils.LINE_SEP).append(_systemOutput).append("------------- ---------------- ---------------").append(
                        StringUtils.LINE_SEP);
            }

            if (_systemError != null && _systemError.length() > 0) {
                sb.append("------------- Standard Error -----------------").append(StringUtils.LINE_SEP).append(_systemError).append("------------- ---------------- ---------------").append(
                        StringUtils.LINE_SEP);
            }
        }

        output.write(sb.toString());
        // resultWriter.close();
        // output.write(results.toString());
        if (!failedTests.isEmpty()) {
            for (Entry<Test, Throwable> entry : failedTests.entrySet()) {
                output.write("\t" + getTestName(entry.getKey()) + "\t" + entry.getValue().getClass().getSimpleName() + ": " + entry.getValue().getMessage() + "\n");
            }
            failedTests.clear();
        }
        output.flush();
    }

    @Override
    public void startTest(Test test) {
        testStarts.put(test, new Long(System.currentTimeMillis()));
    }

    @Override
    public void endTest(Test test) {
        // Fix for bug #5637 - if a junit.extensions.TestSetup is
        // used and throws an exception during setUp then startTest
        // would never have been called
        // if (!testStarts.containsKey(test)) {
        // startTest(test);
        // }
        //
        // boolean failed = failedTests.containsKey(test);
        //
        // Long l = (Long) testStarts.get(test);
        //
        // output.write("Ran [");
        // output.write((formatTimeDuration(System.currentTimeMillis() - l.longValue())) + "] ");
        // output.write(getTestName(test) + " ... " + (failed ? "FAILED" : "OK"));
        // output.write(StringUtils.LINE_SEP);
        // output.flush();
    }

    /**
     * Interface TestListener for JUnit <= 3.4.
     * 
     * <p>
     * A Test failed.
     * 
     * @param test
     *            a test
     * @param t
     *            the exception thrown by the test
     */
    public void addFailure(Test test, Throwable t) {
        formatError("\tFAILED", test, t);
    }

    /**
     * Interface TestListener for JUnit > 3.4.
     * 
     * <p>
     * A Test failed.
     * 
     * @param test
     *            a test
     * @param t
     *            the assertion failed by the test
     */
    @Override
    public void addFailure(Test test, AssertionFailedError t) {
        addFailure(test, (Throwable) t);
    }

    /**
     * A test caused an error.
     * 
     * @param test
     *            a test
     * @param error
     *            the error thrown by the test
     */
    @Override
    public void addError(Test test, Throwable error) {
        formatError("\tCaused an ERROR", test, error);
    }

    /**
     * Get test name
     * 
     * @param test
     *            a test
     * @return test name
     */
    protected String getTestName(Test test) {
        if (test == null) {
            return "null";
        } else {
            return /* JUnitVersionHelper.getTestCaseClassName(test) + ": " + */
            JUnitVersionHelper.getTestCaseName(test);
        }
    }

    /**
     * Get test case full class name
     * 
     * @param test
     *            a test
     * @return test full class name
     */
    protected String getTestCaseClassName(Test test) {
        if (test == null) {
            return "null";
        } else {
            return JUnitVersionHelper.getTestCaseClassName(test);
        }
    }

    /**
     * Format the test for printing..
     * 
     * @param test
     *            a test
     * @return the formatted testname
     */
    protected String formatTest(Test test) {
        if (test == null) {
            return "Null Test: ";
        } else {
            return "Testcase: " + test.toString() + ":";
        }
    }

    /**
     * Format an error and print it.
     * 
     * @param type
     *            the type of error
     * @param test
     *            the test that failed
     * @param error
     *            the exception that the test threw
     */
    protected synchronized void formatError(String type, Test test, Throwable error) {
        failedTests.put(test, error);
        // if (test != null) {
        // endTest(test);
        // }

        // resultWriter.println(formatTest(test) + type);
        // resultWriter.println(TAB_STR + "(" + error.getClass().getSimpleName() + "): " +
        // ((error.getMessage() != null) ? error.getMessage() : error));
        //
        // if (showCausesLines) {
        // resultWriter.append(StringUtils.LINE_SEP);
        // resultWriter.println(filterErrorTrace(test, error));
        // }
        //
        // resultWriter.println();

        /*
         * String strace = JUnitTestRunner.getFilteredTrace(error); resultWriter.println(strace);
         * resultWriter.println();
         */
    }

    protected String filterErrorTrace(Test test, Throwable error) {
        String trace = StringUtils.getStackTrace(error);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StringReader sr = new StringReader(trace);
        BufferedReader br = new BufferedReader(sr);

        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (line.indexOf(getTestCaseClassName(test)) != -1) {
                    Matcher matcher = traceLinePattern.matcher(line);
                    // pw.println(matcher + ": " + matcher.find());
                    if (matcher.find()) {
                        pw.print(TAB_STR);
                        pw.print("(" + matcher.group(3) + ") ");
                        pw.print(matcher.group(2) + ": ");
                        pw.println(matcher.group(4));
                    } else {
                        pw.println(line);
                    }

                }
            }
        } catch (Exception e) {
            return trace; // return the treca unfiltered
        }

        return sw.toString();
    }

    private static String formatTimeDuration(long timeDuration) {
        StringBuilder builder = new StringBuilder();
        long hours = timeDuration / (60 * 60 * 1000);
        long rem = (timeDuration % (60 * 60 * 1000));
        long minutes = rem / (60 * 1000);
        rem = rem % (60 * 1000);
        long seconds = rem / 1000;

        if (hours != 0) {
            builder.append(hours);
            builder.append(" hrs, ");
        }
        if (minutes != 0) {
            builder.append(minutes);
            builder.append(" mins, ");
        }
        // return "0sec if no difference
        builder.append(seconds);
        builder.append(" sec");
        return builder.toString();
    }

    /**
     * 
     * @param string
     * @param length
     * @return the given path + as many whitespace that the given string reaches the given length
     */
    private static String fillWithWhiteSpace(String string, int length) {
        int neededWhiteSpace = length - string.length();
        if (neededWhiteSpace > 0) {
            StringBuilder builder = new StringBuilder(string);
            for (int i = 0; i < neededWhiteSpace; i++) {
                builder.append(" ");
            }
            return builder.toString();
        }
        return string;
    }

    @Override
    protected void finalize() throws Throwable {
        output.write("bye bye");
        super.finalize();
    }

}
