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
package datameer.awstasks.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Util class providing some file-/stream operations and some units of measurement.
 */
public class IoUtil {

    /**
     * The size of one kilobyte in byte.
     */
    public static final long KB_BYTE = 1024;

    /**
     * The size of one megabyte in byte.
     */
    public static final long MB_Byte = KB_BYTE * KB_BYTE;

    /**
     * The size of one gigabyte in byte.
     */
    public static final long GB_BYTE = KB_BYTE * MB_Byte;

    /**
     * The size of one terrabyte in byte.
     */
    public static final long TB_BYTE = KB_BYTE * GB_BYTE;

    /**
     * A default buffer size, could be used f.e. by copying bytes from stream to stream.
     */
    public static final int DEFAULT_BUFFER_SIZE = 4 * 1024;

    /**
     * Write all bytes available from Input- to OutputStream in pieces of DEFAULT_BUFFER_SIZE. <br/>
     * Block until it reads -1.
     * 
     * 
     * @param iStream
     * @param oStream
     * @return total bytes copied
     * @throws IOException
     */
    public final static long copyBytes(InputStream iStream, OutputStream oStream) throws IOException {
        long totalLength = 0;
        int length = -1;
        byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
        while ((length = iStream.read(bytes)) != -1) {
            oStream.write(bytes, 0, length);
            totalLength += length;
        }
        return totalLength;
    }

    /**
     * Write length bytes available from Input- to OutputStream in pieces of DEFAULT_BUFFER_SIZE.
     * 
     * @param iStream
     * @param oStream
     * @param length
     * @throws IOException
     */
    public static void copyBytes(InputStream iStream, OutputStream oStream, long length) throws IOException {
        byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
        int pieceLength = bytes.length;
        while (length > 0) {
            if (length < bytes.length) {
                pieceLength = iStream.read(bytes, 0, (int) length);
            } else {
                pieceLength = iStream.read(bytes, 0, bytes.length);
            }

            oStream.write(bytes, 0, pieceLength);
            length -= pieceLength;
        }
    }

    public static void writeToFile(File file, String... lines) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String line : lines) {
            writer.write(line);
            writer.newLine();
        }
        writer.close();
    }

    public static OutputStream closeProtectedStream(final OutputStream outputStream) {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }

            @Override
            public void close() throws IOException {
                // ignore
            }
        };
    }

}
