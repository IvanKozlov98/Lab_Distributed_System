import CacheApi.TCacheManager;
import org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.TouchedExpiryPolicy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static javax.cache.expiry.Duration.ONE_HOUR;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TCacheTest {

    private static Cache<Integer, String> cache;
    private static int INIT_SIZE_CACHE = 30;
    private final int innerIndex = INIT_SIZE_CACHE / 2;
    private final String PREFIX = "word_";

    @BeforeClass
    public static void setupCache() {
        //configure the cache
        MutableConfiguration<Integer, String> config =
                new MutableConfiguration<>();
        config.setTypes(Integer.class, String.class);
        config.setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(ONE_HOUR));

        config.setStatisticsEnabled(true);
        //create the cache
        cache = new TCacheManager().createCache("simpleCache", config);
    }

    @Before
    public void setCache() {
        // put entries
        for (int i = 0; i < INIT_SIZE_CACHE; ++i) {
            cache.put(i, PREFIX + i);
        }
    }


    @Test
    public void get() {
        for (int i = 0; i < INIT_SIZE_CACHE; ++i) {
            String expectedValue = PREFIX + i;
            assertEquals(cache.get(i), expectedValue);
        }
    }

    @Test
    public void getAll() {
        Set<Integer> keys = new HashSet<>();
        for (int i = 0; i < 10; ++i) {
            keys.add(i);
        }
        Map<Integer, String> cacheAll = cache.getAll(keys);
        for (int i = 0; i < 10; ++i) {
            String expectedValue = PREFIX + i;
            String value = cacheAll.get(i);
            assertEquals(expectedValue, value);
        }
    }

    @Test
    public void containsKey() {
        for (int i = -INIT_SIZE_CACHE; i < 0; ++i) {
            assertFalse(cache.containsKey(i));
        }
        for (int i = 0; i < INIT_SIZE_CACHE; ++i) {
            assertTrue(cache.containsKey(i));
        }
        for (int i = INIT_SIZE_CACHE; i < 2 * INIT_SIZE_CACHE; ++i) {
            assertFalse(cache.containsKey(i));
        }
    }

    @Test
    public void put() {
        for (int i = INIT_SIZE_CACHE; i < INIT_SIZE_CACHE + 10; ++i) {
            cache.put(i, PREFIX + i);
        }
        for (int i = INIT_SIZE_CACHE; i < INIT_SIZE_CACHE + 10; ++i) {
            String expectedValue = PREFIX + i;
            assertEquals(expectedValue, cache.get(i));
        }
    }

    @Test
    public void getAndPut() {
        final String expectedNewValue = PREFIX + 300;
        final String expectedOldValue = cache.get(innerIndex);
        final String oldValue = cache.getAndPut(innerIndex, expectedNewValue);
        final String newValue = cache.get(innerIndex);
        assertEquals(expectedOldValue, oldValue);
        assertEquals(expectedNewValue, newValue);
    }

    @Test
    public void putAll() {
        Map<Integer, String> entries = new HashMap<>();
        for (int i = INIT_SIZE_CACHE; i < INIT_SIZE_CACHE + 10; ++i) {
            entries.put(i, PREFIX + i);
        }
        cache.putAll(entries);
        for (int i = INIT_SIZE_CACHE; i < INIT_SIZE_CACHE + 10; ++i) {
            String expectedValue = PREFIX + i;
            assertEquals(expectedValue, cache.get(i));
        }
    }

    @Test
    public void putIfAbsent() {
        // if Absent
        cache.putIfAbsent(INIT_SIZE_CACHE, PREFIX + INIT_SIZE_CACHE);
        final String expectedValue = PREFIX + INIT_SIZE_CACHE;
        assertEquals(expectedValue, cache.get(INIT_SIZE_CACHE));
        // if not Absent
        String newValue = "newValue";
        String expectedValue2 = cache.get(innerIndex);
        cache.putIfAbsent(innerIndex, newValue);
        assertEquals(expectedValue2, cache.get(innerIndex));
    }

    @Test
    public void remove() {
        cache.remove(innerIndex);
        assertFalse(cache.containsKey(innerIndex));
    }

    @Test
    public void remove1() {
        final String omissionValue = "newValue";
        final String truthValue = PREFIX + innerIndex;
        cache.remove(innerIndex, omissionValue);
        assertTrue(cache.containsKey(innerIndex));
        cache.remove(innerIndex, truthValue);
        assertFalse(cache.containsKey(innerIndex));
    }

    @Test
    public void getAndRemove() {
        String oldValue = cache.getAndRemove(innerIndex);
        String expectedValue = PREFIX + innerIndex;
        assertEquals(oldValue, expectedValue);
        assertFalse(cache.containsKey(innerIndex));
    }

    @Test
    public void replace() {

    }

    @Test
    public void replace1() {
    }

    @Test
    public void getAndReplace() {
    }

    @Test
    public void removeAll() {
        cache.removeAll();
        for (int i = 0; i < INIT_SIZE_CACHE; ++i) {
            assertFalse(cache.containsKey(i));
        }
    }

    @Test
    public void clear() {
        cache.clear();
        for (int i = 0; i < INIT_SIZE_CACHE; ++i) {
            assertFalse(cache.containsKey(i));
        }
    }
}