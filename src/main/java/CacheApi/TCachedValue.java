package CacheApi;

public class TCachedValue <V> {

    /**
     * The internal representation of Cache Entry value
     */
    private V internalValue;

    /**
     * The time (since the Epoc) in milliseconds since the internal value was created.
     */
    private long creationTime;

    /**
     * The time (since the Epoc) in milliseconds when the Cache Entry associated
     * with this value should be considered expired.
     * <p>
     * A value of -1 indicates that the Cache Entry should never expire.
     * </p>
     */
    private long expiryTime;

    /**
     * The time (since the Epoc) in milliseconds since the internal value was
     * last modified.
     */
    private long modificationTime;

    /**
     * The time (since the Epoc) in milliseconds since the internal value was
     * last accessed.
     */
    private long accessTime;

    /**
     * Constructs an CacheApi.TCachedValue with the creation, access and
     * modification times being the current time.
     *
     * @param internalValue the internal representation of the value
     * @param creationTime  the time when the cache entry was created
     * @param expiryTime    the time when the cache entry should expire
     */
    public TCachedValue(V internalValue, long creationTime, long expiryTime) {
        this.internalValue = internalValue;
        this.creationTime = creationTime;
        this.expiryTime = expiryTime;
        this.accessTime = creationTime;
        this.modificationTime = creationTime;
    }

    /**
     * Sets the internal value with the additional side-effect of updating the
     * modification time to that which is specified and incrementing the
     * modification count.
     *
     * @param internalValue    the new internal value
     * @param modificationTime the time when the value was modified
     */
    public void setInternalValue(V internalValue, long modificationTime) {
        this.modificationTime = modificationTime;
        this.internalValue = internalValue;
    }

    /**
     * Sets the time (since the Epoc) in milliseconds when the Cache Entry
     * associated with this value should be considered expired.
     *
     * @param expiryTime time in milliseconds (since the Epoc)
     */
    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public V getInternalValue(long accessTime) {
        this.accessTime = accessTime;
        return internalValue;
    }

    public boolean equalsValue(V value) {
        return internalValue.equals(value);
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    /**
     * Determines if the Cache Entry associated with this value would be expired
     * at the specified time
     *
     * @param now time in milliseconds (since the Epoc)
     * @return true if the value would be expired at the specified time
     */
    public boolean isExpiredAt(long now) {
        return expiryTime > -1 && expiryTime <= now;
    }
}
