package org.nhindirect.config.processor.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.repository.TrustBundleAnchorRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.nhindirect.config.store.TrustBundle;

import reactor.core.publisher.Mono;

public class DefaultBundleRefreshProcessorImpl_convertRawBundleToAnchorCollectionTest
{
	@Test
	public void testConvertRawBundleToAnchorCollection_getFromP7B_assertAnchors() throws Exception
	{
		final byte[] rawBundle = TestUtils.loadBundle("signedbundle.p7b");
		
		final DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		
		final TrustBundle existingBundle = new TrustBundle();
		
		final Calendar processAttempStart = Calendar.getInstance(Locale.getDefault());
		
		Collection<X509Certificate> anchors = processor.convertRawBundleToAnchorCollection(rawBundle, existingBundle, processAttempStart);
		
		assertNotNull(anchors);
		
		assertEquals(1, anchors.size());
	}	
	
	@Test
	public void testConvertRawBundleToAnchorCollection_getFromSignedBundle_noVerification_assertAnchors() throws Exception
	{
		final byte[] rawBundle = TestUtils.loadBundle("signedbundle.p7m");
		
		final DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		
		final TrustBundle existingBundle = new TrustBundle();
		
		final Calendar processAttempStart = Calendar.getInstance(Locale.getDefault());
		
		Collection<X509Certificate> anchors = processor.convertRawBundleToAnchorCollection(rawBundle, existingBundle, processAttempStart);
		
		assertNotNull(anchors);
		
		assertEquals(1, anchors.size());
	}
	
	@Test
	public void testConvertRawBundleToAnchorCollection_getFromSignedBundle_verifySigner_assertAnchors() throws Exception
	{
		final X509Certificate signer = TestUtils.loadSigner("bundleSigner.der");
		
		final byte[] rawBundle = TestUtils.loadBundle("signedbundle.p7m");
		
		final DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		
		final TrustBundle existingBundle = new TrustBundle();
		existingBundle.setSigningCertificateData(signer.getEncoded());
		
		final Calendar processAttempStart = Calendar.getInstance(Locale.getDefault());
		
		Collection<X509Certificate> anchors = processor.convertRawBundleToAnchorCollection(rawBundle, existingBundle, processAttempStart);
		
		assertNotNull(anchors);
		
		assertEquals(1, anchors.size());
	}	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testConvertRawBundleToAnchorCollection_getFromSignedBundle_invalidSigner_assertNoAnchors() throws Exception
	{
		
		TrustBundleRepository repo = mock(TrustBundleRepository.class);
		TrustBundleAnchorRepository anchorRepo = mock(TrustBundleAnchorRepository.class);
		when(repo.save(any())).thenReturn(mock(Mono.class));
		
		
		final X509Certificate signer = TestUtils.loadSigner("sm1.direct.com Root CA.der");
		
		final byte[] rawBundle = TestUtils.loadBundle("signedbundle.p7m");
		
		final DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepositories(repo, anchorRepo);
		
		final TrustBundle existingBundle = new TrustBundle();
		existingBundle.setSigningCertificateData(signer.getEncoded());
		
		final Calendar processAttempStart = Calendar.getInstance(Locale.getDefault());
		
		Collection<X509Certificate> anchors = processor.convertRawBundleToAnchorCollection(rawBundle, existingBundle, processAttempStart);
		
		assertNull(anchors);

	}	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testConvertRawBundleToAnchorCollection_invalidBundle_assertNoAnchors() throws Exception
	{
		TrustBundleRepository repo = mock(TrustBundleRepository.class);
		TrustBundleAnchorRepository anchorRepo = mock(TrustBundleAnchorRepository.class);
		when(repo.save(any())).thenReturn(mock(Mono.class));
		
		final byte[] rawBundle = TestUtils.loadBundle("invalidBundle.der");
		
		final DefaultBundleRefreshProcessorImpl processor = new DefaultBundleRefreshProcessorImpl();
		processor.setRepositories(repo, anchorRepo);
		
		final TrustBundle existingBundle = new TrustBundle();
		
		final Calendar processAttempStart = Calendar.getInstance(Locale.getDefault());
		
		Collection<X509Certificate> anchors = processor.convertRawBundleToAnchorCollection(rawBundle, existingBundle, processAttempStart);
		
		assertNull(anchors);

	}		
}
