package com.dm.awstasks.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Test;

public class IoUtilTest {

    @Test
    public void testWriteBytes() throws IOException {
        // set size not over 1024 otherwise PipedOutputStream will hang
        long size = IoUtil.KB_BYTE;
        byte transferValue = 8;

        // fill iStream1
        PipedOutputStream oStream1 = new PipedOutputStream();
        InputStream iStream1 = new PipedInputStream(oStream1);
        for (int i = 0; i < size; i++) {
            oStream1.write(transferValue);
        }
        oStream1.close();

        // connect iStream2 with oStream 2
        PipedOutputStream oStream2 = new PipedOutputStream();
        InputStream iStream2 = new PipedInputStream(oStream2);

        assertEquals(size, iStream1.available());
        assertEquals(0, iStream2.available());

        // copy bytes from iStream1 to oStream2
        IoUtil.copyBytes(iStream1, oStream2);

        assertEquals(0, iStream1.available());
        assertEquals(size, iStream2.available());

        iStream1.close();
        iStream2.close();
        oStream2.close();
    }

    @Test
    public void testWriteBytesWithLength() throws IOException {
        // set size not over 1024 otherwise PipedOutputStream will hang
        long size = IoUtil.KB_BYTE;
        byte transferValue = 8;

        // fill iStream1
        PipedOutputStream oStream1 = new PipedOutputStream();
        InputStream iStream1 = new PipedInputStream(oStream1);
        for (int i = 0; i < size; i++) {
            oStream1.write(transferValue);
        }

        // connect iStream2 with oStream 2
        PipedOutputStream oStream2 = new PipedOutputStream();
        InputStream iStream2 = new PipedInputStream(oStream2);

        assertEquals(size, iStream1.available());
        assertEquals(0, iStream2.available());

        // copy bytes from iStream1 to oStream2
        IoUtil.copyBytes(iStream1, oStream2, iStream1.available());

        assertEquals(0, iStream1.available());
        assertEquals(size, iStream2.available());

        iStream1.close();
        oStream1.close();
        iStream2.close();
        oStream2.close();
    }

}
