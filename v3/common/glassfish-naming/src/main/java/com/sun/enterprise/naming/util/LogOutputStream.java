/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * Header Notice in each file and include the License file 
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.  
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.enterprise.naming.util;

import java.io.*;
import java.util.logging.*;


/**
 * Capture output lines and send them to the system error log.
 */
public class LogOutputStream extends OutputStream {
    protected Logger logger;
    protected Level level;

    private int lastb = -1;
    private byte[] buf = new byte[80];
    private int pos = 0;

    /**
     * Log to the specified facility at the default FINE level.
     */
    public LogOutputStream(String facility) {
        this(facility, Level.FINE);
    }

    /**
     * Log to the specified facility at the specified level.
     */
    public LogOutputStream(String facility, Level level) {
        logger = Logger.getLogger(facility);
        this.level = level;
    }

    public void write(int b) throws IOException {
        if (!logger.isLoggable(level))
            return;

        if (b == '\r') {
            logBuf();
        } else if (b == '\n') {
            if (lastb != '\r')
                logBuf();
        } else {
            expandCapacity(1);
            buf[pos++] = (byte) b;
        }
        lastb = b;
    }

    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
        int start = off;

        if (!logger.isLoggable(level))
            return;
        len += off;
        for (int i = start; i < len; i++) {
            if (b[i] == '\r') {
                expandCapacity(i - start);
                System.arraycopy(b, start, buf, pos, i - start);
                pos += i - start;
                logBuf();
                start = i + 1;
            } else if (b[i] == '\n') {
                if (lastb != '\r') {
                    expandCapacity(i - start);
                    System.arraycopy(b, start, buf, pos, i - start);
                    pos += i - start;
                    logBuf();
                }
                start = i + 1;
            }
            lastb = b[i];
        }
        if ((len - start) > 0) {
            expandCapacity(len - start);
            System.arraycopy(b, start, buf, pos, len - start);
            pos += len - start;
        }
    }

    /**
     * Log the specified message.
     * Can be overridden by subclass to do different logging.
     */
    protected void log(String msg) {
        logger.log(level, msg);
    }

    /**
     * Convert the buffer to a string and log it.
     */
    private void logBuf() {
        String msg = new String(buf, 0, pos);
        pos = 0;
        log(msg);
    }

    /**
     * Ensure that the buffer can hold at least len bytes
     * beyond the current position.
     */
    private void expandCapacity(int len) {
        while (pos + len > buf.length) {
            byte[] nb = new byte[buf.length * 2];
            System.arraycopy(buf, 0, nb, 0, pos);
            buf = nb;
        }
    }
}
