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
    void pluginDescriptorRegistersJobCommandsAndVaultBridgeOrdering() throws IOException {
        String descriptor = descriptor();
        assertTrue(descriptor.contains("version: 2.0.2"));
        assertTrue(descriptor.contains("softdepend: [Vault]"));
        assertTrue(descriptor.contains("loadbefore: [BdayoLand]"));
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
