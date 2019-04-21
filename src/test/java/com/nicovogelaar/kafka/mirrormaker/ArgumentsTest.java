package com.nicovogelaar.kafka.mirrormaker;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArgumentsTest {

    @Test
    public void testParseArgs() {
        final String args = "sourceUrl=http://schema-registry-1:8081,targetUrl=http://schema-registry-2:8081";
        Arguments arguments = Arguments.parseArgs(args);

        assertEquals("http://schema-registry-1:8081", arguments.sourceUrl);
        assertEquals("http://schema-registry-2:8081", arguments.targetUrl);
        assertFalse(arguments.includeKeys);
        assertTrue(arguments.whitelist.isEmpty());
    }

    @Test
    public void testParseArgs_WithIncludeKeys() {
        final String args = "sourceUrl=http://schema-registry-1:8081,targetUrl=http://schema-registry-2:8081,includeKeys=true";
        Arguments arguments = Arguments.parseArgs(args);

        assertEquals("http://schema-registry-1:8081", arguments.sourceUrl);
        assertEquals("http://schema-registry-2:8081", arguments.targetUrl);
        assertTrue(arguments.includeKeys);
        assertTrue(arguments.whitelist.isEmpty());
    }

    @Test
    public void testParseArgs_WithWhitelist() {
        final String args = "sourceUrl=http://schema-registry-1:8081,targetUrl=http://schema-registry-2:8081,whitelist=topic1;topic2";
        Arguments arguments = Arguments.parseArgs(args);

        assertEquals("http://schema-registry-1:8081", arguments.sourceUrl);
        assertEquals("http://schema-registry-2:8081", arguments.targetUrl);
        assertEquals(2, arguments.whitelist.size());
        assertTrue(arguments.whitelist.contains("topic1"));
        assertTrue(arguments.whitelist.contains("topic2"));
        assertFalse(arguments.whitelist.contains("topic3"));
    }
}
