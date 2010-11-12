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
package datameer.awstasks.ssh;

import java.io.IOException;
import java.text.NumberFormat;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Session;

public abstract class JschCommand {

    protected static final Logger LOG = Logger.getLogger(JschCommand.class);

    public abstract void execute(Session session) throws IOException;

    protected final static void logStats(long timeStarted, long timeEnded, long totalLength) {
        double durationInSec = (timeEnded - timeStarted) / 1000.0;
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(1);
        LOG.debug("File transfer time: " + format.format(durationInSec) + " Average Rate: " + format.format(totalLength / durationInSec) + " B/s");
    }
}
