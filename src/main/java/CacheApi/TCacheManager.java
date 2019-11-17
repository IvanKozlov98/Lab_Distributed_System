package CacheApi;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCacheManager implements CacheManager {

    private static final Logger LOGGER = Logger.getLogger("javax.cache");
    private final HashMap<String, TCache<?, ?>> caches = new HashMap<String, TCache<?, ?>>();

    private volatile boolean isClosed;

    /**
     * Constructs a new RICacheManager with the specified name
     */
    public TCacheManager() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CachingProvider getCachingProvider() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        if (!isClosed()) {
            //first releaseCacheManager the CacheManager from the CacheProvider so that
            //future requests for this CacheManager won't return this o
            //cachingProvider.releaseCacheManager(getURI(), ClassLoader.getSystemClassLoader());

            isClosed = true;

            ArrayList<Cache<?, ?>> cacheList;
            synchronized (caches) {
                cacheList = new ArrayList<Cache<?, ?>>(caches.values());
                caches.clear();
            }
            for (Cache<?, ?> cache : cacheList) {
                try {
                    cache.close();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error stopping cache: " + cache, e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getURI() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getProperties() {
        return null;
    }

    @Override
    public <K, V> Cache<K, V> createCache(String cacheName, Configuration<K, V> configuration) throws IllegalArgumentException {
        if (isClosed()) {
            throw new IllegalStateException();
        }

        if (cacheName == null) {
            throw new NullPointerException("cacheName must not be null");
        }

        if (configuration == null) {
            throw new NullPointerException("configuration must not be null");
        }

        synchronized (caches) {
            TCache<?, ?> cache = caches.get(cacheName);

            if (cache == null) {
                cache = new TCache<>(this, cacheName, configuration);
                caches.put(cache.getName(), cache);

                return (Cache<K, V>) cache;
            } else {
                throw new CacheException("A cache named " + cacheName + " already exists.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        if (isClosed()) {
            throw new IllegalStateException();
        }

        if (keyType == null) {
            throw new NullPointerException("keyType can not be null");
        }

        if (valueType == null) {
            throw new NullPointerException("valueType can not be null");
        }

        synchronized (caches) {
            TCache<?, ?> cache = caches.get(cacheName);

            if (cache == null) {
                return null;
            } else {
                Configuration<?, ?> configuration = cache.getConfiguration();

                if (configuration.getKeyType() != null &&
                        configuration.getKeyType().equals(keyType)) {

                    if (configuration.getValueType() != null &&
                            configuration.getValueType().equals(valueType)) {

                        return (Cache<K, V>) cache;
                    } else {
                        throw new ClassCastException("Incompatible cache value types specified, expected " +
                                configuration.getValueType() + " but " + valueType + " was specified");
                    }
                } else {
                    throw new ClassCastException("Incompatible cache key types specified, expected " +
                            configuration.getKeyType() + " but " + keyType + " was specified");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cache getCache(String cacheName) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        synchronized (caches) {
            return caches.get(cacheName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<String> getCacheNames() {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        synchronized (caches) {
            HashSet<String> set = new HashSet<String>();
            for (Cache<?, ?> cache : caches.values()) {
                set.add(cache.getName());
            }
            return Collections.unmodifiableSet(set);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroyCache(String cacheName) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException();
        }

        Cache<?, ?> cache;
        synchronized (caches) {
            cache = caches.get(cacheName);
        }

        if (cache != null) {
            cache.close();
        }
    }

    /**
     * Releases the Cache with the specified name from being managed by
     * this CacheManager.
     *
     * @param cacheName the name of the Cache to releaseCacheManager
     */
    void releaseCache(String cacheName) {
        if (cacheName == null) {
            throw new NullPointerException();
        }
        synchronized (caches) {
            caches.remove(cacheName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableStatistics(String cacheName, boolean enabled) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException();
        }
        //((CacheApi.TCache) caches.get(cacheName)).setStatisticsEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableManagement(String cacheName, boolean enabled) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException();
        }
        //((CacheApi.TCache) caches.get(cacheName)).setManagementEnabled(enabled);
    }

    @Override
    public <T> T unwrap(java.lang.Class<T> cls) {
        if (cls.isAssignableFrom(getClass())) {
            return cls.cast(this);
        }

        throw new IllegalArgumentException("Unwapping to " + cls + " is not a supported by this implementation");
    }

    /**
     * Obtain the logger.
     *
     * @return the logger.
     */
    Logger getLogger() {
        return LOGGER;
    }
}