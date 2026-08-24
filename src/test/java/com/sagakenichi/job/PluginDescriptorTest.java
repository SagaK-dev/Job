package com.sagakenichi.job;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PluginDescriptorTest {

    @Test
    void pluginDescriptorDoesNotExposeFixedUsageHint() throws IOException {
        String descriptor = descriptor();
        assertFalse(descriptor.contains("usage:"), "plugin.yml must not force Bukkit's fixed usage hint");
    }

    @Test
    void pluginDescriptorRegistersBothJobCommandsAndRequiresVault() throws IOException {
        String descriptor = descriptor();
        assertTrue(descriptor.contains("version: 2.1.0"));
        assertTrue(descriptor.contains("depend: [Vault]"));
        assertTrue(descriptor.contains("  job:\n"));
        assertTrue(descriptor.contains("  jobmenu:\n"));
        assertTrue(descriptor.contains("aliases: [jmenu]"));
    }

    private static String descriptor() throws IOException {
        try (InputStream input = PluginDescriptorTest.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            if (input == null) {
                throw new IOException("plugin.yml was not available on the test classpath");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
