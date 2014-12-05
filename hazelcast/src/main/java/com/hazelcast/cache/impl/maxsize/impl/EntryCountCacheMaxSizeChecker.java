package com.hazelcast.cache.impl.maxsize.impl;

import com.hazelcast.cache.impl.maxsize.CacheMaxSizeChecker;
import com.hazelcast.cache.impl.record.CacheRecordMap;
import com.hazelcast.config.CacheMaxSizeConfig;

/**
 * Cache max-size policy implementation for
 * {@link com.hazelcast.config.CacheMaxSizeConfig.CacheMaxSizePolicy#ENTRY_COUNT}
 */
public class EntryCountCacheMaxSizeChecker implements CacheMaxSizeChecker {

    private static final float ENTRY_COUNT_FACTOR = 1.0F;

    private final int maxEntryCount;
    private final CacheRecordMap cacheRecordMap;
    private final int partitionCount;

    public EntryCountCacheMaxSizeChecker(CacheMaxSizeConfig maxSizeConfig,
            CacheRecordMap cacheRecordMap, int partitionCount) {
        this.maxEntryCount = maxSizeConfig.getSize();
        this.cacheRecordMap = cacheRecordMap;
        this.partitionCount = partitionCount;
    }

    @Override
    public boolean isReachedToMaxSize() {
        /**
         *  Estimated entry count can be calculated dynamically as "e = (s x p) + sqrt(s)"
         *  where
         *      e : entry count
         *      s : size of current record store
         *      p : partition count
         *
         * Like this:
         *      final int s = cacheRecordMap.size();
         *      final int p = partitionCount;
         *      final int estimatedSize = (int) ((s * p) + Math.sqrt(s));
         *
         * See discussion at "https://hazelcast.atlassian.net/wiki/display/EN/JCache+Eviction".
         */

        final int estimatedSize = (int) ((cacheRecordMap.size() * partitionCount) * ENTRY_COUNT_FACTOR);
        return estimatedSize >= maxEntryCount;
    }

}
