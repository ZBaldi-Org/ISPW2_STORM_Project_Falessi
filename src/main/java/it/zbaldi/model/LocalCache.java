package it.zbaldi.model;

import java.util.HashMap;
import java.util.Map;

public class LocalCache {

    /** Map storing cached key-value pairs. Keys are strings, values are integers. */
    private static HashMap<String, Integer> releasesMap = new HashMap<>();

    /** Map storing cached key-value pairs. Keys are strings, values are integers. */
    private static HashMap<String, Integer> totalReleaseMap = new HashMap<>();

    /** Hide constructor */
    private LocalCache() {}

    /**
     * Adds a key-value pair to the cache.
     * This method is synchronized to ensure thread safety.
     *
     * @param key   the string key to store the value under
     * @param value the integer value to store
     */
    public static synchronized void addRelease(String key, int value) {
        releasesMap.put(key, value);
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the string key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
     */
    public static Integer getReleaseValue(String key) {
        return releasesMap.get(key);
    }

    /**
     * Returns the first key associated with the specified value, or null if no key is mapped to this value.
     * Note: if multiple keys map to the same value, only one is returned (the specific key is not guaranteed).
     *
     * @param value the value whose associated key is to be returned
     * @return the key associated with the specified value, or null if this map contains no mapping for the value
     */
    public static String getReleaseKey(Integer value) {
        return releasesMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the number of key-value mappings in the cache.
     *
     * @return the size of the cache (number of entries)
     */
    public static int getReleaseSize() {
        return releasesMap.size();
    }

    /**
     * Adds a key-value pair to the total releases cache.
     * This method is synchronized to ensure thread safety.
     *
     * @param key   the string key to store the value under
     * @param value the integer value to store
     */
    public static synchronized void addTotalRelease(String key, int value) {
        totalReleaseMap.put(key, value);
    }

    /**
     * Retrieves the value associated with the specified key from the total releases cache.
     *
     * @param key the string key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
     */
    public static Integer getTotalReleaseValue(String key) {
        return totalReleaseMap.get(key);
    }

    /**
     * Returns the first key associated with the specified value in the total releases cache, or null if no key is mapped to this value.
     * Note: if multiple keys map to the same value, only one is returned (the specific key is not guaranteed).
     *
     * @param value the value whose associated key is to be returned
     * @return the key associated with the specified value, or null if this map contains no mapping for the value
     */
    public static String getTotalReleaseKey(Integer value) {
        return totalReleaseMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the number of key-value mappings in the total releases cache.
     *
     * @return the size of the total releases cache (number of entries)
     */
    public static int getTotalReleaseSize() {
        return totalReleaseMap.size();
    }
}