package com.norwood.mcheli.helper.info;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_SeatRackInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.plane.MCP_PlaneInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>Test class for {@link YamlParser}.</p>
 * Note that classloading <strong>must not</strong> cascade into any class under package {@link net.minecraft.init}, be careful with classes' clinit
 */
class YamlParserTest {

    @TempDir
    Path tempDir;

    private static Method getAccessibleMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = YamlParser.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Expected method not found: " + name, e);
        }
    }

    private static MCH_AircraftInfo newAircraftInfo() {
        return new TestAircraftInfo();
    }

    private Path writeYaml(String fileName, String yaml) throws IOException {
        Path file = tempDir.resolve(fileName);
        //noinspection ReadWriteStringCanBeUsed
        Files.write(file, yaml.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static Double[] vector(double x, double y, double z) {
        return new Double[]{x, y, z};
    }

    private static void assertVecEquals(Vec3d expected, Vec3d actual) {
        assertEquals(expected.x, actual.x, 1.0e-6);
        assertEquals(expected.y, actual.y, 1.0e-6);
        assertEquals(expected.z, actual.z, 1.0e-6);
    }

    @Test
    void parseHelicopter_withMinimalYaml_populatesBasicFields() throws Exception {
        String yaml = "DisplayName: Test Helicopter\nCategory:\n  - transport\nCanRide: true\n";
        Path file = writeYaml("heli.yml", yaml);

        MCH_HeliInfo info = YamlParser.INSTANCE.parseHelicopter(
                new AddonResourceLocation("mcheli:test_pack|helicopters/test_heli"),
                file.toString(),
                Collections.emptyList(),
                false
        );

        assertNotNull(info, "Expected parseHelicopter to build an info object for valid YAML input.");
        assertEquals("Test Helicopter", info.displayName);
        assertEquals("TRANSPORT", info.category);
        assertTrue(info.canRide);
    }

    @Test
    void parsePlane_withMinimalYaml_populatesBasicFields() throws Exception {
        String yaml = "DisplayName: Test Plane\nMaxFuel: 1200\n";
        Path file = writeYaml("plane.yml", yaml);

        MCP_PlaneInfo info = YamlParser.INSTANCE.parsePlane(
                new AddonResourceLocation("mcheli:test_pack|planes/test_plane"),
                file.toString(),
                Collections.emptyList(),
                false
        );

        assertNotNull(info, "Expected parsePlane to build an info object for valid YAML input.");
        assertEquals("Test Plane", info.displayName);
        assertEquals(1200, info.maxFuel);
    }

    @Test
    void parseWeapon_withMinimalYaml_populatesBasicFields() throws Exception {
        String yaml = "DisplayName: Test Weapon\nType: cannon\nPower: 15\n";
        Path file = writeYaml("weapon.yml", yaml);

        MCH_WeaponInfo info = YamlParser.INSTANCE.parseWeapon(
                new AddonResourceLocation("mcheli:test_pack|weapons/test_weapon"),
                file.toString(),
                Collections.emptyList(),
                false
        );

        assertNotNull(info, "Expected parseWeapon to build an info object for valid YAML input.");
        assertEquals("Test Weapon", info.displayName);
        assertEquals("cannon", info.type);
        assertEquals(15, info.power);
    }

    @Test
    void parseVehicle_withMinimalYaml_populatesMovementFields() throws Exception {
        String yaml = "DisplayName: Test Vehicle\nMaxFuel: 900\nCanRide: false\n";
        Path file = writeYaml("vehicle.yml", yaml);

        MCH_VehicleInfo info = YamlParser.INSTANCE.parseVehicle(
                new AddonResourceLocation("mcheli:test_pack|vehicles/test_vehicle"),
                file.toString(),
                Collections.emptyList(),
                false
        );

        assertNotNull(info, "Expected parseVehicle to build an info object for valid YAML input.");
        assertEquals("Test Vehicle", info.displayName);
        assertEquals(900, info.maxFuel);
        assertFalse(info.canRide);
    }

    @Test
    void parseHud_withSimpleYamlBuildsHudCommands() throws Exception {
        String yaml = "Commands:\n  - type: text\n    value: 'Hello'\n";
        Path file = writeYaml("hud.yml", yaml);

        MCH_Hud info = YamlParser.INSTANCE.parseHud(
                new AddonResourceLocation("mcheli:test_pack|hud/test_hud"),
                file.toString(),
                Collections.emptyList(),
                false
        );

        assertNotNull(info, "Expected parseHud to build an HUD definition from YAML.");
        assertFalse(info.list.isEmpty(), "Expected HUD items parsed from YAML.");
    }

    @Test
    void mapToAircraft_appliesCoreFields() throws Exception {
        MCH_AircraftInfo info = newAircraftInfo();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("DisplayName", " Test Name ");
        root.put("Category", new ArrayList<String>() {{
            add("transport");
            add("support");
        }});
        root.put("CanRide", true);
        root.put("CreativeOnly", false);
        root.put("Invulnerable", true);
        root.put("MaxFuel", 5000);
        root.put("FuelConsumption", 2.5f);
        root.put("FuelSupplyRange", 400.0);
        root.put("AmmoSupplyRange", 250.0);
        Map<String, Number> repairMap = new HashMap<>();
        repairMap.put("range", 150.0f);
        repairMap.put("value", 2000);
        root.put("RepairOtherVehicles", repairMap);
        root.put("Textures", new ArrayList<String>() {{
            add("  tex1  ");
            add("tex2");
        }});
        root.put("ParticleScale", 1.5f);
        root.put("EnableSeaSurfaceParticle", true);
        root.put("SplashParticles", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<>() {{
                put("pos", vector(0.0, 0.5, -1.0));
                put("count", 5);
                put("size", 3.0f);
                put("accel", 1.2f);
                put("age", 120);
                put("motion", 0.3f);
                put("gravity", 0.1f);
            }});
        }});
        root.put("SearchLights", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<>() {{
                put("pos", vector(1.0, 2.0, 3.0));
                put("colorStart", "0x00FF00");
                put("colorEnd", "FF0000");
                put("height", 2.5f);
                put("width", 1.5f);
                put("yaw", 45.0f);
                put("pitch", 10.0f);
                put("stRot", 5.0f);
                put("fixedDirection", true);
                put("steering", true);
            }});
        }});
        root.put("LightHatch", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<>() {{
                put("pos", vector(0.0, 1.0, 2.0));
                put("rot", vector(0.0, 0.0, 1.0));
                put("name", "doorLeft");
                put("isSliding", true);
            }});
        }});
        root.put("RepellingHooks", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<>() {{
                put("pos", vector(1.0, 0.0, 0.0));
                put("interval", 5);
            }});
        }});
        root.put("Racks", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<>() {{
                put("pos", vector(0.0, 1.0, 0.0));
                put("camera", new HashMap<String, Object>() {{
                    put("pos", vector(0.0, 1.0, 0.0));
                    put("fixedRot", true);
                    put("yaw", 15.0f);
                    put("pitch", -5.0f);
                }});
                put("names", new ArrayList<String>() {{
                    add("seat1");
                }});
                put("range", 4.0f);
                put("openParaAlt", 10.0f);
                put("yaw", 90.0f);
                put("pitch", 5.0f);
                put("rotSeat", true);
            }});
            add(new HashMap<>() {{
                put("type", YamlParser.RACK_TYPE.RIDING.name());
                put("id", 7);
                put("name", "  Rear Seat   ");
            }});
        }});
        root.put("Wheels", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<>() {{
                put("pos", vector(0.0, 0.2, 1.0));
                put("scale", 1.2f);
            }});
        }});

        Method mapToAircraft = getAccessibleMethod("mapToAircraft", MCH_AircraftInfo.class, Map.class);
        mapToAircraft.invoke(YamlParser.INSTANCE, info, root);

        assertEquals("Test Name", info.displayName);
        assertEquals("TRANSPORT,SUPPORT", info.category);
        assertTrue(info.canRide);
        assertTrue(info.invulnerable);
        assertEquals(5000, info.maxFuel);
        assertEquals(2.5f, info.fuelConsumption);
        assertEquals(400.0f, info.fuelSupplyRange);
        assertEquals(250.0f, info.ammoSupplyRange);
        assertEquals(150.0f, info.repairOtherVehiclesRange);
        assertEquals(2000, info.repairOtherVehiclesValue);
        assertEquals(1.5f, info.particlesScale);
        assertTrue(info.enableSeaSurfaceParticle);

        assertEquals(1, info.particleSplashs.size());
        MCH_AircraftInfo.ParticleSplash splash = info.particleSplashs.get(0);
        assertEquals(5, splash.num);
        assertEquals(3.0f, splash.acceleration);
        assertEquals(1.2f, splash.size);
        assertEquals(120, splash.age);
        assertEquals(0.3f, splash.motionY);
        assertEquals(0.1f, splash.gravity);
        assertVecEquals(new Vec3d(0.0, 0.5, -1.0), splash.pos);

        assertEquals(1, info.searchLights.size());
        MCH_AircraftInfo.SearchLight searchLight = info.searchLights.get(0);
        assertVecEquals(new Vec3d(1.0, 2.0, 3.0), searchLight.pos);
        assertEquals(YamlParser.INSTANCE.parseHexColor("0x00FF00"), searchLight.colorStart);
        assertEquals(YamlParser.INSTANCE.parseHexColor("FF0000"), searchLight.colorEnd);
        assertEquals(2.5f, searchLight.height);
        assertEquals(1.5f, searchLight.width);
        assertTrue(searchLight.fixDir);
        assertTrue(searchLight.steering);
        assertEquals(45.0f, searchLight.yaw);
        assertEquals(10.0f, searchLight.pitch);
        assertEquals(5.0f, searchLight.stRot);

        assertEquals(1, info.lightHatchList.size());
        MCH_AircraftInfo.Hatch hatch = info.lightHatchList.get(0);
        assertVecEquals(new Vec3d(0.0, 1.0, 2.0), hatch.pos);
        assertVecEquals(new Vec3d(0.0, 0.0, 1.0), hatch.rot);
        assertEquals("doorLeft", hatch.modelName);
        assertTrue(hatch.isSlide);

        assertEquals(1, info.repellingHooks.size());
        MCH_AircraftInfo.RepellingHook hook = info.repellingHooks.get(0);
        Field intervalField = MCH_AircraftInfo.RepellingHook.class.getDeclaredField("interval");
        intervalField.setAccessible(true);
        assertEquals(5, intervalField.getInt(hook));
        Field hookPosField = MCH_AircraftInfo.RepellingHook.class.getDeclaredField("pos");
        hookPosField.setAccessible(true);
        assertVecEquals(new Vec3d(1.0, 0.0, 0.0), (Vec3d) hookPosField.get(hook));

        assertEquals(1, info.entityRackList.size());
        MCH_SeatRackInfo seatRack = info.entityRackList.get(0);
        assertVecEquals(new Vec3d(0.0, 1.0, 0.0), seatRack.pos);
        assertArrayEquals(new String[]{"seat1"}, seatRack.names);
        assertEquals(4.0f, seatRack.range);
        assertEquals(10.0f, seatRack.openParaAlt);
        assertTrue(seatRack.rotSeat);
        assertNotNull(seatRack.getCamPos());
        assertEquals(15.0f, seatRack.getCamPos().yaw);
        assertEquals(-5.0f, seatRack.getCamPos().pitch);
        assertVecEquals(new Vec3d(0.0, 1.0, 0.0), seatRack.getCamPos().pos);

        assertEquals(1, info.rideRacks.size());
        MCH_AircraftInfo.RideRack rideRack = info.rideRacks.get(0);
        assertEquals("rear seat", rideRack.name);
        assertEquals(7, rideRack.rackID);

        assertEquals(1, info.wheels.size());
        MCH_AircraftInfo.Wheel wheel = info.wheels.get(0);
        assertVecEquals(new Vec3d(0.0, 0.2, 1.0), wheel.pos);
        assertEquals(1.2f, wheel.size);

        Field texturesField = MCH_AircraftInfo.class.getDeclaredField("textureNameList");
        texturesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> textures = (List<String>) texturesField.get(info);
        assertTrue(textures.contains("tex1"));
        assertTrue(textures.contains("tex2"));
    }

    @Test
    void displayNameTranslationUsesProvidedMap() throws Exception {
        MCH_AircraftInfo info = newAircraftInfo();
        Map<String, Object> root = new HashMap<>();
        HashMap<String, String> translations = new HashMap<>();
        translations.put("en_us", "Helicopter");
        translations.put("ja_jp", "helicopter_jp");
        root.put("DisplayName", translations);

        Method mapToAircraft = getAccessibleMethod("mapToAircraft", MCH_AircraftInfo.class, Map.class);
        mapToAircraft.invoke(YamlParser.INSTANCE, info, root);

        assertSame(translations, info.displayNameLang);
        assertEquals("Helicopter", info.displayNameLang.get("en_us"));
        assertEquals("helicopter_jp", info.displayNameLang.get("ja_jp"));
    }

    @Test
    void parseHexColorSupportsPrefixedAndUnprefixedValues() {
        assertEquals(0xABCDEF, YamlParser.INSTANCE.parseHexColor("0xABCDEF"));
        assertEquals(0x123456, YamlParser.INSTANCE.parseHexColor("123456"));
    }

    @Test
    void parseWheelWithoutPositionThrows() {
        Method parseWheel = getAccessibleMethod("parseWheel", Map.class);
        Map<String, Object> map = new HashMap<>();
        map.put("scale", 1.0f);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> parseWheel.invoke(YamlParser.INSTANCE, map));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertEquals("Wheel must have a position!", ex.getCause().getMessage());
    }

    private static final class TestAircraftInfo extends MCH_AircraftInfo {
        private TestAircraftInfo() {
            super(AddonResourceLocation.EMPTY_LOCATION, "test");
        }

        @Override
        public void onPostReload() {
        }

        @Override
        public String getDirectoryName() {
            return "test";
        }

        @Override
        public String getKindName() {
            return "test";
        }

        @Override
        public String getDefaultHudName(int seatId) {
            return "hud" + seatId;
        }

        @Override
        public Item getItem() {
            return null;
        }
    }
}
