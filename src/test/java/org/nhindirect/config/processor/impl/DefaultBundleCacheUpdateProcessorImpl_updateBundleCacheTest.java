package org.nhindirect.config.processor.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.nhindirect.config.processor.BundleRefreshProcessor;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.nhindirect.config.store.ConfigurationStoreException;
import org.nhindirect.config.store.TrustBundle;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DefaultBundleCacheUpdateProcessorImpl_updateBundleCacheTest
{
	protected BundleRefreshProcessor processor;
	
	protected TrustBundleRepository repo;
	
	@BeforeEach
	public void setUp()
	{
		processor = mock(BundleRefreshProcessor.class);
		repo = mock(TrustBundleRepository.class);
		when(processor.refreshBundle(any())).thenReturn(Mono.empty());
	}
	
	@Test
	public void testUpdateBundleCache_updateCache_bundleNeverUpdated_assertBundleRefreshCalled() throws Exception
	{
		final DefaultBundleCacheUpdateProcessorImpl cacheUpdate = new DefaultBundleCacheUpdateProcessorImpl();
		cacheUpdate.setRepository(repo);
		cacheUpdate.setRefreshProcessor(processor);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setRefreshInterval(1);
		
		final List<TrustBundle> bundles = Arrays.asList(bundle);
		
		when(repo.findAll()).thenReturn(Flux.fromIterable(bundles));
		
		cacheUpdate.updateBundleCache().block();
		
		verify(repo, times(1)).findAll();
		verify(processor, times(1)).refreshBundle(bundle);
	}
	
	@Test
	public void testUpdateBundleCache_updateCache_zeroRefreshInterval_assertBundleRefreshNotCalled() throws Exception
	{
		final DefaultBundleCacheUpdateProcessorImpl cacheUpdate = new DefaultBundleCacheUpdateProcessorImpl();
		cacheUpdate.setRepository(repo);
		cacheUpdate.setRefreshProcessor(processor);
		
		final TrustBundle bundle = new TrustBundle();
		
		final List<TrustBundle> bundles = Arrays.asList(bundle);
		
		when(repo.findAll()).thenReturn(Flux.fromIterable(bundles));
		
		cacheUpdate.updateBundleCache().block();
		
		verify(repo, times(1)).findAll();
		verify(processor, never()).refreshBundle(bundle);
	}
	
	@Test
	public void testUpdateBundleCache_updateCache_refreshIntervalNotExpired_assertBundleRefreshNotCalled() throws Exception
	{		
		final DefaultBundleCacheUpdateProcessorImpl cacheUpdate = new DefaultBundleCacheUpdateProcessorImpl();
		cacheUpdate.setRepository(repo);
		cacheUpdate.setRefreshProcessor(processor);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setRefreshInterval(5000);
		bundle.setLastSuccessfulRefresh(LocalDateTime.now());
		
		final List<TrustBundle> bundles = Arrays.asList(bundle);
		
		when(repo.findAll()).thenReturn(Flux.fromIterable(bundles));
		
		cacheUpdate.updateBundleCache().block();
		
		verify(repo, times(1)).findAll();
		verify(processor, never()).refreshBundle(bundle);
	}
	
	@Test
	public void testUpdateBundleCache_updateCache_refreshIntervalExpired_assertBundleRefreshCalled() throws Exception
	{
		final DefaultBundleCacheUpdateProcessorImpl cacheUpdate = new DefaultBundleCacheUpdateProcessorImpl();
		cacheUpdate.setRepository(repo);
		cacheUpdate.setRefreshProcessor(processor);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setRefreshInterval(1000);
		final Calendar lastSuccessRefresh = Calendar.getInstance(Locale.getDefault());
		lastSuccessRefresh.add(Calendar.SECOND, -1200);
		bundle.setLastSuccessfulRefresh(EntityModelConversion.localDateTimeFromCalendar(lastSuccessRefresh));
		
		final List<TrustBundle> bundles = Arrays.asList(bundle);
		
		when(repo.findAll()).thenReturn(Flux.fromIterable(bundles));
		
		cacheUpdate.updateBundleCache().block();
		
		verify(repo, times(1)).findAll();
		verify(processor, times(1)).refreshBundle(bundle);
	}
	
	@Test
	public void testUpdateBundleCache_updateCache_errorInRetreavingBundles_assertBundleRefreshNotCalled() throws Exception
	{
		final DefaultBundleCacheUpdateProcessorImpl cacheUpdate = new DefaultBundleCacheUpdateProcessorImpl();
		cacheUpdate.setRepository(repo);
		cacheUpdate.setRefreshProcessor(processor);
		

		doThrow(new ConfigurationStoreException("Just Passing Through")).when(repo).findAll();

		cacheUpdate.updateBundleCache().block();
		
		verify(repo, times(1)).findAll();
		verify(processor, never()).refreshBundle((TrustBundle)any());
	}	
	
	@Test
	public void testUpdateBundleCache_updateCache_errorInUpdate_assertBundleRefreshCalled() throws Exception
	{
		final DefaultBundleCacheUpdateProcessorImpl cacheUpdate = new DefaultBundleCacheUpdateProcessorImpl();
		cacheUpdate.setRepository(repo);
		cacheUpdate.setRefreshProcessor(processor);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setRefreshInterval(1);
		
		final List<TrustBundle> bundles = Arrays.asList(bundle);
		
		when(repo.findAll()).thenReturn(Flux.fromIterable(bundles));
		
		doThrow(new RuntimeException("Just Passing Through")).when(processor).refreshBundle(bundle);
		
		cacheUpdate.updateBundleCache().block();
	
		verify(repo, times(1)).findAll();
		verify(processor, times(1)).refreshBundle(bundle);
	}	
}
