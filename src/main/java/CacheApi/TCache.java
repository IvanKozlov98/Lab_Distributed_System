package CacheApi;


import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * The reference implementation for JSR107.
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Kozlov Ivan
 */
public class TCache<K, V> implements Cache<K, V> {

    /**
     * The name of the {@link Cache} as used with in the scope of the
     * Cache Manager.
     */
    private final String cacheName;

    /**
     * The {@link CacheManager} that created this implementation
     */
    private final TCacheManager cacheManager;

    /**
     * The Configuration for the Cache.
     */
    private final MutableConfiguration<K, V> configuration;

    /**
     * This map used to store cache entries, keyed by the
     * internal representation of a key.
     */
    private final ConcurrentHashMap<Object, TCachedValue<V>> entries;

    /**
     * The {@link ExpiryPolicy} for the {@link Cache}.
     */
    private final ExpiryPolicy expiryPolicy;

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    /**
     * Constructs a cache.
     *
     * @param cacheManager  the CacheManager that's creating the RICache
     * @param cacheName     the name of the Cache
     //* @param classLoader   the ClassLoader the RICache will use for loading classes
     * @param configuration the Configuration of the Cache
     */
    TCache(TCacheManager cacheManager,
            String cacheName,
            //ClassLoader classLoader,
            Configuration<K, V> configuration) {
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;

        //we make a copy of the configuration here so that the provided one
        //may be changed and or used independently for other caches.  we do this
        //as we don't know if the provided configuration is mutable

        //support use of Basic Configuration
        MutableConfiguration mutableConfiguration = new MutableConfiguration();
        mutableConfiguration.setStoreByValue(configuration.isStoreByValue());
        mutableConfiguration.setTypes(configuration.getKeyType(), configuration.getValueType());
        mutableConfiguration.setExpiryPolicyFactory(configuration.getExpiryPolicyFactory());
        this.configuration = new MutableConfiguration<K, V>(mutableConfiguration);
        this.expiryPolicy = this.configuration.getExpiryPolicyFactory().create();
        this.entries = new ConcurrentHashMap<>();
    }

    /**
     * The default Duration to use when a Duration can't be determined.
     *
     * @return the default Duration
     */

    @Override
    public V get(K k) {
        TCachedValue<V> cachedValue = entries.get(k);
        if(null != cachedValue) {
            long now = System.currentTimeMillis();
            if (!cachedValue.isExpiredAt(now)) {
                return cachedValue.getInternalValue(now);
            } else {
                entries.remove(k);
            }
        }
        return null;
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> setKeys) {
        Map<K, V> resultMap = new HashMap<>();
        try {
            readLock.lock();
            setKeys.forEach((key) -> resultMap.put(key, this.get(key)));
        } finally {
            readLock.unlock();
        }
        return resultMap;
    }

    @Override
    public boolean containsKey(K k) {
        return entries.containsKey(k);
    }

    @Override
    public void loadAll(Set<? extends K> set, boolean b, CompletionListener completionListener) {
    }

    /**
     * factory method
     * Not thread-safe method
     * @param value the internal value of building entry
     * @return the new building entry
     */
    private TCachedValue<V> createNewEntry(V value) {
        long now = System.currentTimeMillis();
        Duration duration = expiryPolicy.getExpiryForCreation();
        long expiryTime = duration.getAdjustedTime(now);
        return new TCachedValue<>(value, now, expiryTime);
    }

    /**
     * Not thread-safe method
     * @param cachedValue the cachedValue will update
     * @param newValue the new internal value for updating value
     */
    private void updateEntry(TCachedValue<V> cachedValue, V newValue) {
        long now = System.currentTimeMillis();
        Duration duration = expiryPolicy.getExpiryForUpdate();
        long expiryTime = duration.getAdjustedTime(now);
        cachedValue.setExpiryTime(expiryTime);
        cachedValue.setInternalValue(newValue, now);
    }

    @Override
    public void put(K key, V value) {
       entries.compute(key, (k, cachedValue) -> {
            if (null != cachedValue) {
                updateEntry(cachedValue, value);
            } else {
                cachedValue = createNewEntry(value);
            }
            return cachedValue;
        });
    }

    @Override
    public V getAndPut(K k, V v) {
        V oldValue = this.get(k);
        if (null != oldValue)
            this.put(k, v);
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        try {
            writeLock.lock();
            map.forEach(this::put);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        TCachedValue<V> oldCachedValue = entries.get(key);
        TCachedValue<V> possibleCachedValue = entries.computeIfAbsent(key, k -> createNewEntry(value));
        return oldCachedValue != possibleCachedValue;
    }

    @Override
    public boolean remove(K k) {
        TCachedValue<V> oldValue = entries.remove(k);
        return (null != oldValue);
    }

    @Override
    public boolean remove(K key, V value) {
        TCachedValue oldValue = entries.computeIfPresent(key, (k, v) -> {
            if (v.equalsValue(value))
                return null;
            return v;
        });
        return (null != oldValue);
    }

    @Override
    public V getAndRemove(K k) {
        TCachedValue<V> oldValue = entries.remove(k);
        return null != oldValue ? oldValue.getInternalValue(-1) : null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        TCachedValue<V> oldCachedValue = entries.computeIfPresent(key, (k, v) -> {
            if (v.equalsValue(oldValue))
                updateEntry(v, newValue);
            return v;
        });
        return null != oldCachedValue;
    }

    @Override
    public boolean replace(K key, V value) {
        TCachedValue<V> oldCachedValue = entries.computeIfPresent(key, (k, v) -> {
            updateEntry(v, value);
            return v;
        });
        return null != oldCachedValue;
    }

    @Override
    public V getAndReplace(K key, V value) {
        TCachedValue<V> oldCachedValue = entries.computeIfPresent(key, (k, v) -> {
            updateEntry(v, value);
            return v;
        });
        return null != oldCachedValue ? oldCachedValue.getInternalValue(-1) : null;
    }

    @Override
    public void removeAll(Set<? extends K> set) {
        try{
            writeLock.lock();
            set.forEach(entries::remove);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeAll() {
        entries.clear();
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public Configuration<K, V> getConfiguration() {
        return configuration;
    }

    @Override
    public <T> T invoke(K k, EntryProcessor<K, V, T> entryProcessor, Object... objects) throws EntryProcessorException {
        return null;
    }

    @Override
    public <T> Map<K, T> invokeAll(Set<? extends K> set, EntryProcessor<K, V, T> entryProcessor, Object... objects) {
        return null;
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> aClass) {
        return null;
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {

    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {

    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return null;
    }
}
