package no.difi.meldingsutveksling.serviceregistry;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {

    public static final String SVARUT_CACHE = "svarUtCache";
    public static final String BRREG_CACHE = "brregCache";
    public static final String KRR_CACHE = "krrCache";
    public static final String DSF_CACHE = "dsfCache";
    public static final String FIKSIO_CACHE = "fiksIoCache";

    @Override
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new CaffeineCache(SVARUT_CACHE,
                        Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .build()),
                new CaffeineCache(FIKSIO_CACHE,
                        Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .build()),
                new CaffeineCache(BRREG_CACHE,
                        Caffeine.newBuilder()
                                .expireAfterWrite(1, TimeUnit.HOURS)
                                .build()),
                new CaffeineCache(KRR_CACHE,
                        Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .build()),
                new CaffeineCache(DSF_CACHE,
                        Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .build())
        ));
        return cacheManager;
    }

}
