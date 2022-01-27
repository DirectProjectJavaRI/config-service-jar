package org.nhindirect.config.processor.impl;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;

import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nhindirect.config.repository.TrustBundleAnchorRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.nhindirect.config.store.BundleThumbprint;
import org.nhindirect.config.store.ConfigurationStoreException;
import org.nhindirect.config.store.TrustBundle;
import org.nhindirect.config.store.TrustBundleAnchor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public class DefaultBundleRefreshProcessorImpl_refreshBundleTest
{
	protected TrustBundleRepository repo;
	protected TrustBundleAnchorRepository anchorRepo;
	
	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setUp()
	{
		repo = mock(TrustBundleRepository.class);
		anchorRepo = mock(TrustBundleAnchorRepository.class);
		
		when(repo.save(any())).thenReturn(Mono.empty());
		when(anchorRepo.deleteByTrustBundleId(any())).thenReturn(Mono.empty());
		when(anchorRepo.saveAll((Collection<TrustBundleAnchor>)any())).thenReturn(Flux.empty());
	}
	
	@Test
	public void testRefreshBundle_validBundle_noCheckSum_needsRefreshed_assertUpdateCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepositories(repo, anchorRepo);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Junit Bundle");
		String bundleURL = getClass().getClassLoader().getResource("bundles/signedbundle.p7b").toString();
		bundle.setBundleURL(bundleURL);
	
		processor.refreshBundle(bundle).block();
	
		verify(repo, times(1)).save((TrustBundle)any());
	}	
	
	@Test
	public void testRefreshBundle_validBundle_unmatchedChecksum_needsRefreshed_assertUpdateCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepositories(repo, anchorRepo);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Junit Bundle");
		String bundleURL = getClass().getClassLoader().getResource("bundles/signedbundle.p7b").toString();
		bundle.setBundleURL(bundleURL);
		bundle.setCheckSum("12345");
	
		processor.refreshBundle(bundle).block();
	
		verify(repo, times(1)).save((TrustBundle)any());
	}		
	
	@Test
	public void testRefreshBundle_checkSumsMatch_assertUpdateNotCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepositories(repo, anchorRepo);
		
		final TrustBundle bundle = new TrustBundle();
		
		String bundleResourceName = "bundles/signedbundle.p7b";
		String bundleURL = getClass().getClassLoader().getResource(bundleResourceName).toString();
		
		byte[] rawBundleByte = IOUtils.resourceToByteArray(bundleResourceName, getClass().getClassLoader());
		
		bundle.setBundleName("Junit Bundle");
		bundle.setBundleURL(bundleURL);
		bundle.setCheckSum(BundleThumbprint.toThumbprint(rawBundleByte).toString());
		
		processor.refreshBundle(bundle).block();
		
		verify(repo, times(1)).save((TrustBundle)any());
	}	
	
	@Test
	public void testRefreshBundle_bundleNotFound_assertUpdateNotCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepositories(repo, anchorRepo);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Junit Bundle");
		String bundleURL = getClass().getClassLoader().getResource("bundles/signedbundle.p7b").toString();
		bundle.setBundleURL(bundleURL + "2122");
	
		processor.refreshBundle(bundle).block();
	
		verify(repo, times(1)).save((TrustBundle)any());
	}		
	
	@Test
	public void testRefreshBundle_invalidBundle_assertUpdateNotCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepositories(repo, anchorRepo);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Junit Bundle");
		String bundleURL = getClass().getClassLoader().getResource("bundles/invalidBundle.der").toString();
		bundle.setBundleURL(bundleURL);
	
		processor.refreshBundle(bundle).block();
	
		verify(repo, times(1)).save((TrustBundle)any());
	}	
	
	@Test
	public void testRefreshBundle_errorOnUpdate() throws Exception
	{
		Assertions.assertThrows(ConfigurationStoreException.class, () ->
		{
			DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
			processor.setRepositories(repo, anchorRepo);
			
			final TrustBundle bundle = new TrustBundle();
			bundle.setBundleName("Junit Bundle");
			String bundleURL = getClass().getClassLoader().getResource("bundles/signedbundle.p7b").toString();
			bundle.setBundleURL(bundleURL);
		
			doThrow(new ConfigurationStoreException("Just Passing Through")).when(repo).save((TrustBundle)any());
	
			processor.refreshBundle(bundle).block();
		});
	}
	
}
