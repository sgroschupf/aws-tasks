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
package datameer.awstasks.testsupport.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * A junit {@link Runner} which scans test classes for a {@link CheckBefore} annotation and skips
 * all tests of the class checking method throws an exception.
 * <p>
 * 
 * The test class have to be annotated with @RunWith(CheckBeforeRunner.class) and must contain a
 * method annotated with @CheckBefore.
 */
public class CheckBeforeRunner extends BlockJUnit4ClassRunner {

    private static final Logger LOG = Logger.getLogger(CheckBeforeRunner.class);

    public CheckBeforeRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Statement classBlock(final RunNotifier notifier) {
        TestClass testClass = getTestClass();
        Method checkMethod = getMethod(testClass.getJavaClass(), CheckBefore.class);
        Exception exception = execute(newInstance(testClass.getJavaClass()), checkMethod);
        Statement statement;
        if (exception == null) {
            statement = super.classBlock(notifier);
        } else {
            LOG.warn("skip test " + testClass.getJavaClass().getSimpleName() + " cause check faild: " + exception.getMessage());
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Description description = Description.createSuiteDescription(getClass());
                    notifier.fireTestIgnored(description);
                }
            };
        }
        return statement;
    }

    private Object newInstance(Class<?> javaClass) {
        try {
            return javaClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Exception execute(Object target, Method checkMethod) {
        Exception exception = null;
        boolean reThrow = true;
        try {
            checkMethod.invoke(target);
        } catch (IllegalArgumentException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        } catch (InvocationTargetException e) {
            exception = (Exception) e.getCause();
            reThrow = false;
        }
        if (exception != null && reThrow) {
            throw new RuntimeException(exception);
        }
        return exception;
    }

    private Method getMethod(Class<?> declaringClass, Class<? extends Annotation> annotationClass) {
        Method[] declaredMethods = declaringClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getAnnotation(annotationClass) != null) {
                return method;
            }
        }
        throw new IllegalStateException(declaringClass.getName() + " does not have any method annotated with " + annotationClass.getName());
    }

}
