package org.nhindirect.config.processor.impl;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;

import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;


import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.nhindirect.config.store.BundleThumbprint;
import org.nhindirect.config.store.ConfigurationStoreException;
import org.nhindirect.config.store.TrustBundle;


public class DefaultBundleRefreshProcessorImpl_refreshBundleTest
{
	protected TrustBundleRepository repo;
	protected String filePrefix;
	
	@Before
	public void setUp()
	{
		repo = mock(TrustBundleRepository.class);
		
		// check for Windows... it doens't like file://<drive>... turns it into FTP
		File file = new File("./src/test/resources/bundles/signedbundle.p7b");
		if (file.getAbsolutePath().contains(":/"))
			filePrefix = "file:///";
		else
			filePrefix = "file:///";
	}
	
	@Test
	public void testRefreshBundle_validBundle_noCheckSum_needsRefreshed_assertUpdateCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepository(repo);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Junit Bundle");
		File fl = new File("src/test/resources/bundles/signedbundle.p7b");
		bundle.setBundleURL(filePrefix + fl.getAbsolutePath());
	
		processor.refreshBundle(bundle);
	
		verify(repo, times(1)).save((TrustBundle)any());
	}	
	
	@Test
	public void testRefreshBundle_validBundle_unmatchedChecksum_needsRefreshed_assertUpdateCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepository(repo);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Junit Bundle");
		File fl = new File("src/test/resources/bundles/signedbundle.p7b");
		bundle.setBundleURL(filePrefix + fl.getAbsolutePath());
		bundle.setCheckSum("12345");
	
		processor.refreshBundle(bundle);
	
		verify(repo, times(1)).save((TrustBundle)any());
	}		
	
	@Test
	public void testRefreshBundle_checkSumsMatch_assertUpdateNotCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepository(repo);
		
		final TrustBundle bundle = new TrustBundle();
		
		File fl = new File("src/test/resources/bundles/signedbundle.p7b");
		
		byte[] rawBundleByte = FileUtils.readFileToByteArray(fl);
		
		bundle.setBundleName("Junit Bundle");
		bundle.setBundleURL(filePrefix + fl.getAbsolutePath());
		bundle.setCheckSum(BundleThumbprint.toThumbprint(rawBundleByte).toString());
		
		processor.refreshBundle(bundle);
	
		verify(repo, times(1)).save((TrustBundle)any());
	}	
	
	@Test
	public void testRefreshBundle_bundleNotFound_assertUpdateNotCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepository(repo);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Junit Bundle");
		File fl = new File("src/test/resources/bundles/signedbundle.p7b2122");
		bundle.setBundleURL(filePrefix + fl.getAbsolutePath());
	
		processor.refreshBundle(bundle);
	
		verify(repo, times(1)).save((TrustBundle)any());
	}		
	
	@Test
	public void testRefreshBundle_invalidBundle_assertUpdateNotCalled() throws Exception
	{
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepository(repo);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Junit Bundle");
		File fl = new File("src/test/resources/bundles/invalidBundle.der");
		bundle.setBundleURL(filePrefix + fl.getAbsolutePath());
	
		processor.refreshBundle(bundle);
	
		verify(repo, times(1)).save((TrustBundle)any());
	}	
	
	@Test(expected=ConfigurationStoreException.class)
	public void testRefreshBundle_errorOnUpdate() throws Exception
	{
		
		DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepository(repo);
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("Junit Bundle");
		File fl = new File("src/test/resources/bundles/signedbundle.p7b");
		bundle.setBundleURL(filePrefix + fl.getAbsolutePath());
	
		doThrow(new ConfigurationStoreException("Just Passing Through")).when(repo).save((TrustBundle)any());

		processor.refreshBundle(bundle);

	}
	
}
