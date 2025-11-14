package no.difi.meldingsutveksling.serviceregistry.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableAspectJAutoProxy // Enables defining aspects using @Aspect annotations
@Profile("yt")
@Aspect
@RequiredArgsConstructor
public class EvictCacheBeforeCreateDigitalpostServiceRecords {

    private final SRRequestScope requestScope;
    private final CacheManager cacheManager;

    @Before("execution(* no.difi.meldingsutveksling.serviceregistry.record.ServiceRecordService.createDigitalpostServiceRecords(..)) && args(identifier, ..)")
    public void evictCacheBeforeCreateDigitalpostServiceRecords(String identifier) {
        System.out.println("Evicting cache");
        Cache krrCache = cacheManager.getCache(CacheConfig.KRR_CACHE);
        if (krrCache != null) {
            krrCache.invalidate();
        }

        Cache dsfCache = cacheManager.getCache(CacheConfig.DSF_CACHE);
        if (dsfCache != null) {
            dsfCache.invalidate();
        }
    }

}
