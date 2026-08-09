package com.musicpod.catalog.track;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.musicpod.config.CacheNames;

class TrackCacheContractTests {

    @Test
    void getByIdIsCachedByTrackId()
            throws Exception {

        Method method =
                TrackService.class
                        .getMethod(
                                "getById",
                                UUID.class
                        );

        Cacheable cacheable =
                method.getAnnotation(
                        Cacheable.class
                );

        assertNotNull(
                cacheable
        );

        assertArrayEquals(
                new String[] {
                        CacheNames.TRACKS
                },
                cacheable.cacheNames()
        );

        assertEquals(
                "#trackId",
                cacheable.key()
        );
    }

    @Test
    void updateEvictsTrackCache()
            throws Exception {

        Method method =
                TrackService.class
                        .getMethod(
                                "update",
                                UUID.class,
                                UpdateTrackRequest.class
                        );

        CacheEvict cacheEvict =
                method.getAnnotation(
                        CacheEvict.class
                );

        assertNotNull(
                cacheEvict
        );

        assertArrayEquals(
                new String[] {
                        CacheNames.TRACKS
                },
                cacheEvict.cacheNames()
        );

        assertEquals(
                "#trackId",
                cacheEvict.key()
        );
    }

    @Test
    void deleteEvictsTrackCache()
            throws Exception {

        Method method =
                TrackService.class
                        .getMethod(
                                "delete",
                                UUID.class
                        );

        CacheEvict cacheEvict =
                method.getAnnotation(
                        CacheEvict.class
                );

        assertNotNull(
                cacheEvict
        );

        assertArrayEquals(
                new String[] {
                        CacheNames.TRACKS
                },
                cacheEvict.cacheNames()
        );

        assertEquals(
                "#trackId",
                cacheEvict.key()
        );
    }
}