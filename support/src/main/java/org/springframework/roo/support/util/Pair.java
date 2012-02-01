package org.springframework.roo.support.util;

import org.apache.commons.lang3.ObjectUtils;

/**
 * A pair with a key of type "K" and a value of type "V". Instances are
 * immutable.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 * @param <K> the key type
 * @param <V> the value type
 */
public class Pair<K, V> {

    private final K key;
    private final V value;

    /**
     * Constructor
     * 
     * @param key can be <code>null</code>
     * @param value can be <code>null</code>
     */
    public Pair(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Pair)) {
            return false;
        }
        final Pair<?, ?> otherPair = (Pair<?, ?>) obj;
        return ObjectUtils.equals(key, otherPair.getKey())
                && ObjectUtils.equals(value, otherPair.getValue());
    }

    /**
     * Returns the key
     * 
     * @return <code>null</code> if it is
     */
    public K getKey() {
        return key;
    }

    /**
     * Returns the value
     * 
     * @return <code>null</code> if it is
     */
    public V getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCodeMulti(getKey(), getValue());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("key: ").append(key);
        sb.append(", value: ").append(value);
        return sb.toString();
    }
}
