package org.nhindirect.config.processor.impl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.model.TrustBundleAnchor;
import org.nhindirect.config.processor.BundleCacheUpdateProcessor;
import org.nhindirect.config.processor.BundleRefreshProcessor;
import org.nhindirect.config.resources.TrustBundleResource;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;

public class DefaultBundleCacheUpdateProcessorImpl_springInitTest extends SpringBaseTest
{
	@Autowired
	protected TrustBundleResource trustService;

	@Autowired
	protected BundleRefreshProcessor refreshProcessor;
	
	@Autowired
	protected BundleCacheUpdateProcessor updateProcessor;
	
	@Mock
	protected HttpServletRequest servletRequest;
	
	@Before
	public void setup() throws Exception
	{
		Mockito.when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://mock.com"));
	}
	
	@Test
	public void testLoadConfigService_validSpringConfig_assertComponentsLoaded() throws Exception
	{

		assertNotNull(trustService);
		assertNotNull(updateProcessor);
		assertNotNull(refreshProcessor);	
			
	}
	
	@Test
	public void testLoadConfigService_addTrustBundle_bundleAnchorsAdded() throws Exception
	{
		File bundleLocation = new File("./src/test/resources/bundles/signedbundle.p7b");
		
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Test Bundle");
		bundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		
		trustService.addTrustBundle(bundle, servletRequest);
		
		final ResponseEntity<TrustBundle> addedBundle = trustService.getTrustBundleByName("Test Bundle");
		assertTrue(addedBundle.getBody().getTrustBundleAnchors().size() > 0);		
		
		for (TrustBundleAnchor anchor : addedBundle.getBody().getTrustBundleAnchors())
			assertNotNull(anchor.getAnchorData());
	}
	
	@Test
	public void testLoadConfigService_refreshBundle_assertBundleRefreshed() throws Exception
	{
		File bundleLocation = new File("./src/test/resources/bundles/signedbundle.p7b");
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Test Bundle");
		bundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		
		trustService.addTrustBundle(bundle, servletRequest);
		
		final ResponseEntity<TrustBundle> addedBundle = trustService.getTrustBundleByName("Test Bundle");
		assertTrue(addedBundle.getBody().getTrustBundleAnchors().size() > 0);
		final Calendar lastRefreshAttemp = addedBundle.getBody().getLastRefreshAttempt();
		final Calendar lastSuccessfulRefresh = addedBundle.getBody().getLastSuccessfulRefresh();
		
		// now refresh
		trustService.refreshTrustBundle(addedBundle.getBody().getBundleName());
		
		final ResponseEntity<TrustBundle> refreshedBundle = trustService.getTrustBundleByName("Test Bundle");
		assertEquals(lastSuccessfulRefresh.getTimeInMillis(), refreshedBundle.getBody().getLastSuccessfulRefresh().getTimeInMillis());
		assertTrue(refreshedBundle.getBody().getLastRefreshAttempt().getTimeInMillis() > lastRefreshAttemp.getTimeInMillis());
	}
	
	@Test
	public void testLoadConfigService_refreshBundle_newBundleData_assertBundleRefreshed() throws Exception
	{
		final File originalBundleLocation = new File("./src/test/resources/bundles/signedbundle.p7b");
		final File updatedBundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final File targetTempFileLocation = new File("./target/tempFiles/bundle.p7b");
		
		// copy the original bundle to the target location
		FileUtils.copyFile(originalBundleLocation, targetTempFileLocation);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Test Bundle");
		bundle.setBundleURL(filePrefix + targetTempFileLocation.getAbsolutePath());
		
		trustService.addTrustBundle(bundle, servletRequest);
		
		final ResponseEntity<TrustBundle> addedBundle = trustService.getTrustBundleByName("Test Bundle");
		assertTrue(addedBundle.getBody().getTrustBundleAnchors().size() > 0);
		
		// validate the contents of the bundle
		final ResponseEntity<TrustBundle> firstBundleInsert = trustService.getTrustBundleByName("Test Bundle");
		assertEquals(1, firstBundleInsert.getBody().getTrustBundleAnchors().size());
		
		// copy in the new bundle
		FileUtils.copyFile(updatedBundleLocation, targetTempFileLocation);
		
		// now refresh
		trustService.refreshTrustBundle(addedBundle.getBody().getBundleName());
		
		final ResponseEntity<TrustBundle> refreshedBundle = trustService.getTrustBundleByName("Test Bundle");
		assertEquals(6, refreshedBundle.getBody().getTrustBundleAnchors().size());
	}
}
