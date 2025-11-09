package com.norwood.mcheli.helper.info;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.info.parsers.yaml.YamlParser;

/**
 * <p>
 * Test class for {@link YamlParser}.
 * </p>
 * Note that classloading <strong>must not</strong> cascade into any class under package {@link net.minecraft.init}, be
 * careful with classes' clinit
 */
class YamlParserTest {

    @BeforeAll
    static void setupLogger() {
        MCH_Logger.setLogger(LogManager.getLogger("TestLogger"));
    }

    @TempDir
    Path tempDir;
}
