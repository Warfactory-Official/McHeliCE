package com.norwood.mcheli.helper.info;

import com.norwood.mcheli.MCH_CommonProxy;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.info.emitters.YamlEmitter;
import com.norwood.mcheli.helper.info.parsers.txt.TxtParser;
import com.norwood.mcheli.helper.info.parsers.yaml.YamlParser;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * Test class for {@link YamlParser}.
 * </p>
 * Note that classloading <strong>must not</strong> cascade into any class under package {@link net.minecraft.init}, be
 * careful with classes' clinit
 */
class YamlParserTest {
//
//    static YamlEmitter emitter = new YamlEmitter();
//    static class DummyProxy extends MCH_CommonProxy {
//        @Override public boolean isRemote() { return true; }
//    }
//    @BeforeAll
//    static void setupProxy() {
//        MCH_MOD.proxy = new DummyProxy();
//        new MCH_Config("", "");
//    }
//
//    @BeforeAll
//    static void setupHudRegistry() throws Exception {
//        Field hudField = ContentRegistries.class.getDeclaredField("REGISTORY_HUD");
//        hudField.setAccessible(true);
//
//        ContentRegistry<MCH_Hud> emptyRegistry =
//                ContentRegistry.builder(MCH_Hud.class, "hud").build();
//        hudField.set(null, emptyRegistry);
//
//        Field weaponField = ContentRegistries.class.getDeclaredField("REGISTORY_WEAPON");
//        weaponField.setAccessible(true);
//
//        ContentRegistry<MCH_WeaponInfo> emptyWeapon =
//                ContentRegistry.builder(MCH_WeaponInfo.class, "weapons").build();
//
//        weaponField.set(null, emptyWeapon);
//    }
//
//
//
//
//
//
//
//    @TempDir
//    Path tempDir;
//    String AIRCRAFT_ALL_VARS = "temp";
//    String HELI_ALL_VARS = "temp";
//    String PLANE_ALL_VARS = "temp";
//
//    @BeforeAll
//    static void setupLogger() {
//        MCH_Logger.setLogger(LogManager.getLogger("TestLogger"));
//    }
//
//    @Test
//    void convertMekava() {
//        String finConfig = TestStrings.ABRAMS;
//        MCH_TankInfo tankInfoTXT;
//
//        try {
//            tankInfoTXT = TxtParser.INSTANCE.parseTank(null, "Test", Arrays.asList(finConfig.split("\n")).stream().filter(s -> !s.equals("")).collect(Collectors.toList()), false);
//            tankInfoTXT.validate();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        //Tank asserts
//
//
//        String emittedYML = emitter.emitTank(tankInfoTXT);
//
//
//        MCH_TankInfo tankInfoYML;
//
//        try {
//            tankInfoYML = YamlParser.INSTANCE.parseTank(null, "Test", Arrays.asList(emittedYML.split("\n")), false);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        //Compare objects
//
//        float FLOAT_EPSILON = 0.001f;
//        double DOUBLE_EPSILON = 0.001;
//
//        Comparator<Float> floatComparator = (a, b) ->
//                Math.abs(a - b) < FLOAT_EPSILON ? 0 : Float.compare(a, b);
//
//        Comparator<Double> doubleComparator = (a, b) ->
//                Math.abs(a - b) < DOUBLE_EPSILON ? 0 : Double.compare(a, b);
//
//        Comparator<String> caseInsensitiveString = (a, b) -> a.equalsIgnoreCase(b) ? 0 : a.compareToIgnoreCase(b);
//
//        assertThat( tankInfoYML)
//                .usingRecursiveComparison()
//                .withComparatorForType(floatComparator, Float.class)
//                .withComparatorForType(caseInsensitiveString, String.class)
//                .withComparatorForType(doubleComparator, Double.class)
//                .isEqualTo(tankInfoTXT);
//
//    }
//    @Test
//    void convertHeli() {
//        String finConfig = TestStrings.EC665_TIGER_UHT;
//        MCH_HeliInfo heliInfoTXT;
//
//        try {
//            heliInfoTXT = TxtParser.INSTANCE.parseHelicopter(null, "Test", Arrays.asList(finConfig.split("\n")).stream().filter(s -> !s.equals("")).collect(Collectors.toList()), false);
//            heliInfoTXT.validate();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        //Tank asserts
//
//
//        String emittedYML = emitter.emitHelicopter(heliInfoTXT);
//
//
//        MCH_HeliInfo heliInfoYML;
//
//        try {
//            heliInfoYML = YamlParser.INSTANCE.parseHelicopter(null, "Test", Arrays.asList(emittedYML.split("\n")), false);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        //Compare objects
//
//        float FLOAT_EPSILON = 0.001f;
//        double DOUBLE_EPSILON = 0.001;
//
//        Comparator<Float> floatComparator = (a, b) ->
//                Math.abs(a - b) < FLOAT_EPSILON ? 0 : Float.compare(a, b);
//
//        Comparator<Double> doubleComparator = (a, b) ->
//                Math.abs(a - b) < DOUBLE_EPSILON ? 0 : Double.compare(a, b);
//
//        Comparator<String> caseInsensitiveString = (a, b) -> a.equalsIgnoreCase(b) ? 0 : a.compareToIgnoreCase(b);
//
//        assertThat(heliInfoYML)
//                .usingRecursiveComparison()
//                .withComparatorForType(floatComparator, Float.class)
//                .withComparatorForType(caseInsensitiveString, String.class)
//                .withComparatorForType(doubleComparator, Double.class)
//                .isEqualTo(heliInfoTXT
//                        );
//
//    }
//

}
