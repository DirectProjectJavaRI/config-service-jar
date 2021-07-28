package org.nhindirect.config.springconfig;

import org.nhindirect.config.processor.BundleCacheUpdateProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BundleRefreshTask 
{
	@Autowired
	protected BundleCacheUpdateProcessor updateProcessor;
	
	@Scheduled(fixedRateString = "${direct.trustbundles.refresh.period:3600000}")
	public void refreshBundles()
	{
		updateProcessor.updateBundleCache();
	}
}
