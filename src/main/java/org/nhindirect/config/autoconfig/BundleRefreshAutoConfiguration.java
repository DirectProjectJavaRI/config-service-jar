package org.nhindirect.config.autoconfig;

import org.nhindirect.config.processor.BundleCacheUpdateProcessor;
import org.nhindirect.config.processor.BundleRefreshProcessor;
import org.nhindirect.config.processor.impl.DefaultBundleCacheUpdateProcessorImpl;
import org.nhindirect.config.processor.impl.DefaultBundleRefreshProcessorImpl;
import org.nhindirect.config.repository.TrustBundleAnchorRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@AutoConfiguration
@ComponentScan({"org.nhindirect.config.resources"})
@EnableR2dbcRepositories("org.nhindirect.config.repository")
public class BundleRefreshAutoConfiguration
{	
	@ConditionalOnMissingBean
	@Bean
	BundleRefreshProcessor bundleRefreshProcessor(TrustBundleRepository trustBundleRepo, TrustBundleAnchorRepository bundleAnchorRepo)
	{
		final DefaultBundleRefreshProcessorImpl retVal = new DefaultBundleRefreshProcessorImpl();
		retVal.setRepositories(trustBundleRepo, bundleAnchorRepo);
		
		return retVal;
	}
	
	@ConditionalOnMissingBean
	@Bean 
	BundleCacheUpdateProcessor bundleCacheUpdateProcessor(BundleRefreshProcessor refreshProc, TrustBundleRepository trustBundleRepo)
	{
		DefaultBundleCacheUpdateProcessorImpl proc = new DefaultBundleCacheUpdateProcessorImpl();
		proc.setRefreshProcessor(refreshProc);
		proc.setRepository(trustBundleRepo);
		
		return proc;
	}
	
	@ConditionalOnMissingBean
	@Bean
	BundleRefreshTask bundleRefreshTask() {
		
		return new BundleRefreshTask();
	}

}
