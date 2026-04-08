package org.nhindirect.config.autoconfig;

import org.nhindirect.config.processor.BundleCacheUpdateProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class BundleRefreshTask
{
	@Autowired
	protected BundleCacheUpdateProcessor updateProcessor;

	@Scheduled(fixedRateString = "${direct.trustbundles.refresh.period:3600000}")
	public void refreshBundles()
	{
		updateProcessor.updateBundleCache()
			.subscribeOn(Schedulers.boundedElastic())
			.subscribe(
				null,
				ex -> log.error("Error refreshing bundle cache", ex)
			);
	}
}
