package org.springframework.roo.felix;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides levels for the Felix "log" command.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class LogLevel implements Comparable<LogLevel> {

    public static final LogLevel DEBUG = new LogLevel("DEBUG", "debug");
    public static final LogLevel ERROR = new LogLevel("ERROR", "error");

    public static final LogLevel INFORMATION = new LogLevel("INFORMATION",
            "info");
    public static final LogLevel WARNING = new LogLevel("WARNING", "warn");
    private final String felixCode;
    private final String key;

    public LogLevel(final String key, final String felixCode) {
        Assert.hasText(key, "Key required");
        Assert.hasText(felixCode, "Felix code required");
        this.key = key;
        this.felixCode = felixCode;
    }

    public final int compareTo(final LogLevel o) {
        if (o == null) {
            return -1;
        }
        final int result = key.compareTo(o.key);
        if (result == 0) {
            return felixCode.compareTo(o.felixCode);
        }
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        return obj instanceof LogLevel && compareTo((LogLevel) obj) == 0;
    }

    public String getFelixCode() {
        return felixCode;
    }

    public String getKey() {
        return key;
    }

    @Override
    public final int hashCode() {
        return key.hashCode() * felixCode.hashCode();
    }

    @Override
    public String toString() {
        final ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("key", key);
        tsc.append("felixCode", felixCode);
        return tsc.toString();
    }
}
