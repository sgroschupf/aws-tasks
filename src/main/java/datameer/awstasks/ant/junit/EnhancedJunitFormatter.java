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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

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

    // TODO combine with BuildListener ?

    private PrintWriter _console;
    private String _systemOutput = null;
    private String _systemError = null;

    private Map<Test, Throwable> failedTests = new LinkedHashMap<Test, Throwable>();
    private final boolean _showOutput;
    private final boolean _showCurrentTest;

    public EnhancedJunitFormatter() {
        _showOutput = System.getProperty("showOutput", "false").equals("true");
        _showCurrentTest = System.getProperty("showCurrentTest", "true").equals("true");
    }

    @Override
    public void setOutput(OutputStream out) {
        _console = new PrintWriter(out);
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
        _console.write(fillWithWhiteSpace(String.format("Running %s...", suite.getName()), 80));
        if (_showCurrentTest) {
            _console.write('\n');
            _console.flush();
        }
    }

    @Override
    public void endTestSuite(JUnitTest suite) {
        if (_showCurrentTest) {
            _console.write("\t");
        }
        if (suite.failureCount() > 0 || suite.errorCount() > 0) {
            _console.write("FAILED");
        } else if (suite.runCount() == 0) {
            _console.write("IGNORED");
        } else {
            _console.write("SUCCEED");
        }
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

        _console.write(sb.toString());
        if (!failedTests.isEmpty()) {
            for (Entry<Test, Throwable> entry : failedTests.entrySet()) {
                if (_showCurrentTest) {
                    _console.write('\t');
                }
                _console.write("\t" + getTestName(entry.getKey()) + "() \t- " + entry.getValue().getClass().getSimpleName() + ": " + entry.getValue().getMessage() + "\n");
            }
            failedTests.clear();
        }
        _console.flush();
    }

    @Override
    public void startTest(Test test) {
    }

    @Override
    public void endTest(Test test) {

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
        _console.write("bye bye");
        super.finalize();
    }

}
