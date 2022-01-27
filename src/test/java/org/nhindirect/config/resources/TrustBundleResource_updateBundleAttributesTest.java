package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import reactor.core.publisher.Mono;


public class TrustBundleResource_updateBundleAttributesTest extends SpringBaseTest
{
	@Autowired
	protected TrustBundleResource bundleService;
		
		abstract class TestPlan extends BaseTestPlan 
		{
			@Override
			protected void tearDownMocks()
			{

			}

			protected abstract Collection<TrustBundle> getBundlesToAdd();

			protected abstract String getBundleToUpdate();
			
			protected abstract TrustBundle getBundleDataToUpdate() throws Exception;
			
			protected abstract String getBundleUpdatedName();
				
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<TrustBundle> bundlesToAdd = getBundlesToAdd();
				
				if (bundlesToAdd != null)
				{
					bundlesToAdd.forEach(addBundle->
					{
						final HttpEntity<TrustBundle> requestEntity = new HttpEntity<>(addBundle);
						final ResponseEntity<Void> resp = testRestTemplate.exchange("/trustbundle", HttpMethod.PUT, requestEntity, Void.class);
						if (resp.getStatusCodeValue() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});
				}
				
				
				final HttpEntity<TrustBundle> requestEntity = new HttpEntity<>(getBundleDataToUpdate());
				final ResponseEntity<Void> resp = testRestTemplate.exchange("/trustbundle/{bundle}/bundleAttributes", HttpMethod.POST, requestEntity, Void.class, 
						getBundleToUpdate());
				if (resp.getStatusCodeValue() != 204)
					throw new HttpClientErrorException(resp.getStatusCode());
				
				final ResponseEntity<TrustBundle> getBundle = testRestTemplate.getForEntity("/trustbundle/" + getBundleUpdatedName(), TrustBundle.class);
				
				int statusCode = getBundle.getStatusCodeValue();
				if (statusCode == 404)
					doAssertions(null);
				else if (statusCode == 200)
					doAssertions(getBundle.getBody());
				else
					throw new HttpClientErrorException(getBundle.getStatusCode());	

				
			}
				
			protected void doAssertions(TrustBundle bundle) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testUpdateBundleAttributes_changeName_assertNameChanged()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundle> bundles;
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					try
					{
						bundles = new ArrayList<TrustBundle>();
						
						TrustBundle bundle = new TrustBundle();
						bundle.setBundleName("testBundle1");
						String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
						bundle.setBundleURL(bundleURL);	
						bundle.setRefreshInterval(24);
						bundle.setSigningCertificateData(null);		
						bundles.add(bundle);
						
						return bundles;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected TrustBundle getBundleDataToUpdate() throws Exception
				{
					final TrustBundle bundleData = new TrustBundle();
					bundleData.setBundleName("testBundle2");
					
					return bundleData;
					
				}
				
				@Override
				protected String getBundleToUpdate()
				{
					return "testBundle1";
				}
				
				@Override 
				protected String getBundleUpdatedName()
				{
					return "testBundle2";
				}
				
				protected void doAssertions(TrustBundle bundle) throws Exception
				{
					final TrustBundle addedBundle = this.bundles.iterator().next();
					
					assertEquals(getBundleUpdatedName(), bundle.getBundleName());
					assertNull(bundle.getSigningCertificateAsX509Certificate());
					assertEquals(addedBundle.getBundleURL(), bundle.getBundleURL());
					assertEquals(0, bundle.getRefreshInterval());
				}
			}.perform();
		}	
		
		@Test
		public void testUpdateBundleAttributes_noChange_assertNameChanged()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundle> bundles;
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					try
					{
						bundles = new ArrayList<TrustBundle>();
						
						TrustBundle bundle = new TrustBundle();
						bundle.setBundleName("testBundle1");
						String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
						bundle.setBundleURL(bundleURL);	
						bundle.setRefreshInterval(24);
						bundle.setSigningCertificateData(null);		
						bundles.add(bundle);
						
						return bundles;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected TrustBundle getBundleDataToUpdate() throws Exception
				{
					final TrustBundle bundleData = new TrustBundle();
					bundleData.setBundleName("testBundle1");
					String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
					bundleData.setBundleURL(bundleURL);	
					bundleData.setRefreshInterval(24);
					bundleData.setSigningCertificateData(null);	
					
					return bundleData;
					
				}
				
				@Override
				protected String getBundleToUpdate()
				{
					return "testBundle1";
				}
				
				@Override 
				protected String getBundleUpdatedName()
				{
					return "testBundle1";
				}
				
				protected void doAssertions(TrustBundle bundle) throws Exception
				{
					final TrustBundle addedBundle = this.bundles.iterator().next();
					
					assertEquals(getBundleUpdatedName(), bundle.getBundleName());
					assertNull(bundle.getSigningCertificateAsX509Certificate());
					assertEquals(addedBundle.getBundleURL(), bundle.getBundleURL());
					assertEquals(24, bundle.getRefreshInterval());
				}
			}.perform();
		}	
		
		@Test
		public void testUpdateBundleAttributes_newSigningCert_assertSigningCertChanged()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundle> bundles;
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					try
					{
						bundles = new ArrayList<TrustBundle>();
						
						TrustBundle bundle = new TrustBundle();
						bundle.setBundleName("testBundle1");
						String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
						bundle.setBundleURL(bundleURL);	
						bundle.setRefreshInterval(24);
						bundle.setSigningCertificateData(null);		
						bundles.add(bundle);
						
						return bundles;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected TrustBundle getBundleDataToUpdate() throws Exception
				{
					final TrustBundle bundleData = new TrustBundle();
					bundleData.setBundleName("testBundle2");
					bundleData.setSigningCertificateData(TestUtils.loadSigner("bundleSigner.der").getEncoded());
					
					return bundleData;
					
				}
				
				@Override
				protected String getBundleToUpdate()
				{
					return "testBundle1";
				}
				
				@Override 
				protected String getBundleUpdatedName()
				{
					return "testBundle2";
				}
				
				protected void doAssertions(TrustBundle bundle) throws Exception
				{
					final TrustBundle addedBundle = this.bundles.iterator().next();
					
					assertEquals(getBundleUpdatedName(), bundle.getBundleName());
					assertEquals(TestUtils.loadSigner("bundleSigner.der"), bundle.getSigningCertificateAsX509Certificate());
					assertEquals(addedBundle.getBundleURL(), bundle.getBundleURL());
					assertEquals(0, bundle.getRefreshInterval());
				}
			}.perform();
		}			
		
		@Test
		public void testUpdateBundleAttributes_removeSigningCert_assertSigningCertNull()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundle> bundles;
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					try
					{
						bundles = new ArrayList<TrustBundle>();
						
						TrustBundle bundle = new TrustBundle();
						bundle.setBundleName("testBundle1");
						String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
						bundle.setBundleURL(bundleURL);	
						bundle.setRefreshInterval(24);
						bundle.setSigningCertificateData(TestUtils.loadSigner("bundleSigner.der").getEncoded());		
						bundles.add(bundle);
						
						return bundles;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected TrustBundle getBundleDataToUpdate() throws Exception
				{
					final TrustBundle bundleData = new TrustBundle();
					bundleData.setSigningCertificateData(null);
					bundleData.setBundleURL("");
					
					return bundleData;
					
				}
				
				@Override
				protected String getBundleToUpdate()
				{
					return "testBundle1";
				}
				
				@Override 
				protected String getBundleUpdatedName()
				{
					return "testBundle1";
				}
				
				protected void doAssertions(TrustBundle bundle) throws Exception
				{
					final TrustBundle addedBundle = this.bundles.iterator().next();
					
					assertEquals(getBundleUpdatedName(), bundle.getBundleName());
					assertNull(bundle.getSigningCertificateAsX509Certificate());
					assertEquals(addedBundle.getBundleURL(), bundle.getBundleURL());
					assertEquals(0, bundle.getRefreshInterval());
				}
			}.perform();
		}	
		
		@Test
		public void testUpdateBundleAttributes_newURL_assertURLChanged()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundle> bundles;
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					try
					{
						bundles = new ArrayList<TrustBundle>();
						
						TrustBundle bundle = new TrustBundle();
						bundle.setBundleName("testBundle1");
						String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
						bundle.setBundleURL(bundleURL);	
						bundle.setRefreshInterval(24);
						bundle.setSigningCertificateData(TestUtils.loadSigner("bundleSigner.der").getEncoded());		
						bundles.add(bundle);
						
						return bundles;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected TrustBundle getBundleDataToUpdate() throws Exception
				{
					final TrustBundle bundleData = new TrustBundle();
					String bundleURL = getClass().getClassLoader().getResource("bundles/invalidBundle.der").toString();
					bundleData.setBundleURL(bundleURL);	
					
					return bundleData;
					
				}
				
				@Override
				protected String getBundleToUpdate()
				{
					return "testBundle1";
				}
				
				@Override 
				protected String getBundleUpdatedName()
				{
					return "testBundle1";
				}
				
				protected void doAssertions(TrustBundle bundle) throws Exception
				{
					
					assertEquals(getBundleUpdatedName(), bundle.getBundleName());
					assertNull(bundle.getSigningCertificateAsX509Certificate());
					assertTrue(bundle.getBundleURL().contains("invalidBundle.der"));
					assertEquals(0, bundle.getRefreshInterval());
				}
			}.perform();
		}			
		
		@Test
		public void testUpdateBundleAttributes_invalidNewCert_assertBadRequest()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundle> bundles;
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					try
					{
						bundles = new ArrayList<TrustBundle>();
						
						TrustBundle bundle = new TrustBundle();
						bundle.setBundleName("testBundle1");
						String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
						bundle.setBundleURL(bundleURL);	
						bundle.setRefreshInterval(24);
						bundle.setSigningCertificateData(null);		
						bundles.add(bundle);
						
						return bundles;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected TrustBundle getBundleDataToUpdate() throws Exception
				{
					final TrustBundle bundleData = new TrustBundle();
					bundleData.setSigningCertificateData(new byte[]{1, 3,2});
					
					return bundleData;
					
				}
				
				@Override
				protected String getBundleToUpdate()
				{
					return "testBundle1";
				}
				
				@Override 
				protected String getBundleUpdatedName()
				{
					return "testBundle1";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(400, ex.getRawStatusCode());
				}
			}.perform();
		}		
		
		@Test
		public void testUpdateBundleAttributes_nonExistentBundle_assertNotFound()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundle> bundles;
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					try
					{
						bundles = new ArrayList<TrustBundle>();
						
						TrustBundle bundle = new TrustBundle();
						bundle.setBundleName("testBundle1");
						String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
						bundle.setBundleURL(bundleURL);	
						bundle.setRefreshInterval(24);
						bundle.setSigningCertificateData(null);		
						bundles.add(bundle);
						
						return bundles;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected TrustBundle getBundleDataToUpdate() throws Exception
				{
					return new TrustBundle();
				}
				
				@Override
				protected String getBundleToUpdate()
				{
					return "testBundle2";
				}
				
				@Override 
				protected String getBundleUpdatedName()
				{
					return "testBundle2";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(404, ex.getRawStatusCode());
				}
			}.perform();
		}	
		
		@Test
		public void testUpdateBundleAttributes_errorInLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						TrustBundleRepository mockDAO = mock(TrustBundleRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).findByBundleNameIgnoreCase(eq("testBundle1"));
						
						bundleService.setTrustBundleRepository(mockDAO);
					}
					catch (Throwable t)
					{
						throw new RuntimeException(t);
					}
				}
				
				@Override
				protected void tearDownMocks()
				{
					super.tearDownMocks();
					
					bundleService.setTrustBundleRepository(bundleRepo);
				}
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					return null;
				}
				
				@Override
				protected TrustBundle getBundleDataToUpdate() throws Exception
				{
					return new TrustBundle();
				}
				
				@Override
				protected String getBundleToUpdate()
				{
					return "testBundle1";
				}
				
				@Override 
				protected String getBundleUpdatedName()
				{
					return "testBundle1";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}

		@Test
		public void testUpdateBundleAttributes_errorInUpdate_assertServiceError()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						org.nhindirect.config.store.TrustBundle bundle = new org.nhindirect.config.store.TrustBundle();
						bundle.setBundleName("Test");
						TrustBundleRepository mockDAO = mock(TrustBundleRepository.class);
						when(mockDAO.findByBundleNameIgnoreCase("testBundle1")).thenReturn(Mono.just(bundle));
						doThrow(new RuntimeException()).when(mockDAO).save(any());
						
						bundleService.setTrustBundleRepository(mockDAO);
					}
					catch (Throwable t)
					{
						throw new RuntimeException(t);
					}
				}
				
				@Override
				protected void tearDownMocks()
				{
					super.tearDownMocks();
					
					bundleService.setTrustBundleRepository(bundleRepo);
				}
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					return null;
				}
				
				@Override
				protected TrustBundle getBundleDataToUpdate() throws Exception
				{
					return new TrustBundle();
				}
				
				@Override
				protected String getBundleToUpdate()
				{
					return "testBundle1";
				}
				
				@Override 
				protected String getBundleUpdatedName()
				{
					return "testBundle1";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}
}
