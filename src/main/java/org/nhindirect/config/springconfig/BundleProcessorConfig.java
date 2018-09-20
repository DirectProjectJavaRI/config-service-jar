package org.nhindirect.config.springconfig;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.nhindirect.config.processor.BundleCacheUpdateProcessor;
import org.nhindirect.config.processor.BundleRefreshProcessor;
import org.nhindirect.config.processor.impl.DefaultBundleCacheUpdateProcessorImpl;
import org.nhindirect.config.processor.impl.DefaultBundleRefreshProcessorImpl;
import org.nhindirect.config.store.dao.TrustBundleDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class BundleProcessorConfig
{
	@Autowired
	protected CamelContext camelContext;

	@Value("${direct.config.bundlerefresh.endpointuri:seda:refresh-start}")
	protected String refreshEndpoint;
	
	@Bean
	public BundleRefreshProcessor bundleRefreshProcessor(TrustBundleDao trustBundleDao)
	{
		final DefaultBundleRefreshProcessorImpl retVal = new DefaultBundleRefreshProcessorImpl();
		retVal.setDao(trustBundleDao);
		
		return retVal;
	}
	
	@Bean 
	public BundleCacheUpdateProcessor bundleCacheUpdateProcessor(BundleRefreshProcessor refreshProc, TrustBundleDao trustBundleDao)
	{
		DefaultBundleCacheUpdateProcessorImpl proc = new DefaultBundleCacheUpdateProcessorImpl();
		proc.setRefreshProcessor(refreshProc);
		proc.setDao(trustBundleDao);
		
		return proc;
	}

    @Bean
    public ProducerTemplate bundleRefresh() throws Exception 
    {
    	final ProducerTemplate template = camelContext.createProducerTemplate();
    	template.setDefaultEndpointUri(refreshEndpoint);
        return template;
    }
	
}
