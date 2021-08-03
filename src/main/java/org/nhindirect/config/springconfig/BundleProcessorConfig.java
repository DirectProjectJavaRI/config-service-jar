package org.nhindirect.config.springconfig;

import org.nhindirect.config.processor.BundleCacheUpdateProcessor;
import org.nhindirect.config.processor.BundleRefreshProcessor;
import org.nhindirect.config.processor.impl.DefaultBundleCacheUpdateProcessorImpl;
import org.nhindirect.config.processor.impl.DefaultBundleRefreshProcessorImpl;
import org.nhindirect.config.repository.TrustBundleAnchorRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BundleProcessorConfig
{	
	@Bean
	public BundleRefreshProcessor bundleRefreshProcessor(TrustBundleRepository trustBundleRepo, TrustBundleAnchorRepository bundleAnchorRepo)
	{
		final DefaultBundleRefreshProcessorImpl retVal = new DefaultBundleRefreshProcessorImpl();
		retVal.setRepositories(trustBundleRepo, bundleAnchorRepo);
		
		return retVal;
	}
	
	@ConditionalOnMissingBean
	@Bean 
	public BundleCacheUpdateProcessor bundleCacheUpdateProcessor(BundleRefreshProcessor refreshProc, TrustBundleRepository trustBundleRepo)
	{
		DefaultBundleCacheUpdateProcessorImpl proc = new DefaultBundleCacheUpdateProcessorImpl();
		proc.setRefreshProcessor(refreshProc);
		proc.setRepository(trustBundleRepo);
		
		return proc;
	}
	

}
