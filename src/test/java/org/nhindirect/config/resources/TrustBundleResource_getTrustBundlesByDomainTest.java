package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.model.TrustBundleDomainReltn;
import org.nhindirect.config.repository.DomainRepository;
import org.nhindirect.config.repository.TrustBundleDomainReltnRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class TrustBundleResource_getTrustBundlesByDomainTest extends SpringBaseTest
{
	@Autowired
	protected TrustBundleResource bundleService;
	
		abstract class TestPlan extends BaseTestPlan 
		{
			
			@Override
			protected void tearDownMocks()
			{

			}

			protected abstract Collection<TrustBundleDomainReltn> getBundlesToAdd();
			
			protected abstract Domain getDomainToAdd();
			
			protected abstract String getBundleNameToAssociate();
			
			protected abstract String getDomainNameToAssociate();
			
			protected abstract String getDomainNameToRetrieve();
			
			protected String getFetchAnchors()
			{
				return "true";
			}
			
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<TrustBundleDomainReltn> bundlesToAdd = getBundlesToAdd();
				
				if (bundlesToAdd != null)
				{
					bundlesToAdd.forEach(addBundle->
					{
						final HttpEntity<TrustBundle> requestEntity = new HttpEntity<>(addBundle.getTrustBundle());
						final ResponseEntity<Void> resp = testRestTemplate.exchange("/trustbundle", HttpMethod.PUT, requestEntity, Void.class);
						if (resp.getStatusCodeValue() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});
				}
				
				final Domain addDomain = getDomainToAdd();
				
				if (addDomain != null)
				{
					final HttpEntity<Domain> requestEntity = new HttpEntity<>(addDomain);
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/domain", HttpMethod.PUT, requestEntity, Void.class);
					if (resp.getStatusCodeValue() != 201)
						throw new HttpClientErrorException(resp.getStatusCode());
				}
				

				// associate bundle to domain
				if (addDomain != null && bundlesToAdd != null)
				{
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/trustbundle/{bundle}/{domain}", 
							HttpMethod.POST, null, Void.class, 
							getBundleNameToAssociate(), getDomainNameToAssociate());
					
					if (resp.getStatusCodeValue() != 204)
						throw new HttpClientErrorException(resp.getStatusCode());
				}

				final ResponseEntity<Collection<TrustBundleDomainReltn>> getBundles = testRestTemplate.exchange("/trustbundle/domains/{domain}?fetchAnchors={fetchAnchors}", 
						HttpMethod.GET, null, new ParameterizedTypeReference<Collection<TrustBundleDomainReltn>>() {}, 
						getDomainNameToRetrieve(), getFetchAnchors());
				
				if (getBundles.getStatusCodeValue() == 204)
					doAssertions(new ArrayList<TrustBundleDomainReltn>());
				else if (getBundles.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(getBundles.getStatusCode());
				else
					doAssertions(getBundles.getBody());	
	
			}
				
			protected void doAssertions(Collection<TrustBundleDomainReltn> bundles) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testGetBundlesByDomain_assertBundlesRetrieved()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundleDomainReltn> bundles;
				
				@Override
				protected Collection<TrustBundleDomainReltn> getBundlesToAdd()
				{
					try
					{
						bundles = new ArrayList<TrustBundleDomainReltn>();
						
						TrustBundle bundle = new TrustBundle();
						bundle.setBundleName("testBundle1");
						File fl = new File("src/test/resources/bundles/providerTestBundle.p7b");
						bundle.setBundleURL(filePrefix + fl.getAbsolutePath());	
						bundle.setRefreshInterval(24);
						bundle.setSigningCertificateData(null);		
						
						TrustBundleDomainReltn reltn = new TrustBundleDomainReltn();
						reltn.setDomain(new Domain());
						reltn.setTrustBundle(bundle);
						
						bundles.add(reltn);
			
						
						return bundles;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}

				@Override
				protected Domain getDomainToAdd()
				{
					final Address postmasterAddress = new Address();
					postmasterAddress.setEmailAddress("me@test.com");
					
					Domain domain = new Domain();
					
					domain.setDomainName("test.com");
					domain.setStatus(EntityStatus.ENABLED);
					domain.setPostmasterAddress(postmasterAddress);			
					
					return domain;
				}
				
				@Override
				protected String getBundleNameToAssociate()
				{
					return "testBundle1";
				}
				
				@Override
				protected String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override
				protected String getDomainNameToRetrieve()
				{
					return "test.com";
				}
				
				
				protected void doAssertions(Collection<TrustBundleDomainReltn> bundles) throws Exception
				{
					assertNotNull(bundles);
					assertEquals(1, bundles.size());
					
					final Iterator<TrustBundleDomainReltn> addedBundlesIter = this.bundles.iterator();
					
					for (TrustBundleDomainReltn retrievedBundle : bundles)
					{	
						final TrustBundleDomainReltn addedBundle = addedBundlesIter.next(); 
						
						assertEquals(addedBundle.getTrustBundle().getBundleName(), retrievedBundle.getTrustBundle().getBundleName());
						assertEquals(addedBundle.getTrustBundle().getBundleURL(), retrievedBundle.getTrustBundle().getBundleURL());
						assertEquals(addedBundle.getTrustBundle().getRefreshInterval(), retrievedBundle.getTrustBundle().getRefreshInterval());
						assertNull(retrievedBundle.getTrustBundle().getSigningCertificateData());
					}
				}
			}.perform();
		}		
		
		@Test
		public void testGetBundlesByDomain_suppressAnchors_assertBundlesRetrievedWithNoAnchors()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundleDomainReltn> bundles;
				
				@Override
				protected String getFetchAnchors()
				{
					return "false";
				}
				
				@Override
				protected Collection<TrustBundleDomainReltn> getBundlesToAdd()
				{
					try
					{
						bundles = new ArrayList<TrustBundleDomainReltn>();
						
						TrustBundle bundle = new TrustBundle();
						bundle.setBundleName("testBundle1");
						File fl = new File("src/test/resources/bundles/providerTestBundle.p7b");
						bundle.setBundleURL(filePrefix + fl.getAbsolutePath());	
						bundle.setRefreshInterval(24);
						bundle.setSigningCertificateData(null);		
						
						TrustBundleDomainReltn reltn = new TrustBundleDomainReltn();
						reltn.setTrustBundle(bundle);
						reltn.setDomain(new Domain());
						
						bundles.add(reltn);
			
						
						return bundles;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}

				@Override
				protected Domain getDomainToAdd()
				{
					final Address postmasterAddress = new Address();
					postmasterAddress.setEmailAddress("me@test.com");
					
					Domain domain = new Domain();
					
					domain.setDomainName("test.com");
					domain.setStatus(EntityStatus.ENABLED);
					domain.setPostmasterAddress(postmasterAddress);			
					
					return domain;
				}
				
				@Override
				protected String getBundleNameToAssociate()
				{
					return "testBundle1";
				}
				
				@Override
				protected String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override
				protected String getDomainNameToRetrieve()
				{
					return "test.com";
				}
				
				
				protected void doAssertions(Collection<TrustBundleDomainReltn> bundles) throws Exception
				{
					assertNotNull(bundles);
					assertEquals(1, bundles.size());
					
					final Iterator<TrustBundleDomainReltn> addedBundlesIter = this.bundles.iterator();
					
					for (TrustBundleDomainReltn retrievedBundle : bundles)
					{	
						final TrustBundleDomainReltn addedBundle = addedBundlesIter.next(); 
						
						assertEquals(addedBundle.getTrustBundle().getBundleName(), retrievedBundle.getTrustBundle().getBundleName());
						assertEquals(addedBundle.getTrustBundle().getBundleURL(), retrievedBundle.getTrustBundle().getBundleURL());
						assertEquals(addedBundle.getTrustBundle().getRefreshInterval(), retrievedBundle.getTrustBundle().getRefreshInterval());
						assertNull(retrievedBundle.getTrustBundle().getSigningCertificateData());
						assertTrue(retrievedBundle.getTrustBundle().getTrustBundleAnchors().isEmpty());
					}
				}
			}.perform();
		}	
		
		@Test
		public void testGetBundlesByDomain_noBundlesInDomain_assertNoBundlesRetrieved()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected Collection<TrustBundleDomainReltn> getBundlesToAdd()
				{
					return null;
				}

				@Override
				protected Domain getDomainToAdd()
				{
					final Address postmasterAddress = new Address();
					postmasterAddress.setEmailAddress("me@test.com");
					
					Domain domain = new Domain();
					
					domain.setDomainName("test.com");
					domain.setStatus(EntityStatus.ENABLED);
					domain.setPostmasterAddress(postmasterAddress);			
					
					return domain;
				}
				
				@Override
				protected String getBundleNameToAssociate()
				{
					return "testBundle1";
				}
				
				@Override
				protected String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override
				protected String getDomainNameToRetrieve()
				{
					return "test.com";
				}
				
				
				protected void doAssertions(Collection<TrustBundleDomainReltn> bundles) throws Exception
				{
					assertNotNull(bundles);
					assertEquals(0, bundles.size());

				}
			}.perform();
		}		
		
		
		@Test
		public void testGetBundlesByDomain_unknownDomain_assertNotFound()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected Collection<TrustBundleDomainReltn> getBundlesToAdd()
				{
					return null;
				}

				@Override
				protected Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected String getBundleNameToAssociate()
				{
					return "testBundle1";
				}
				
				@Override
				protected String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override
				protected String getDomainNameToRetrieve()
				{
					return "test.com";
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
		public void testGetBundlesByDomain_errorInDomainLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						TrustBundleRepository mockBundleDAO = mock(TrustBundleRepository.class);
						DomainRepository mockDomainDAO = mock(DomainRepository.class);
						
						doThrow(new RuntimeException()).when(mockDomainDAO).findByDomainNameIgnoreCase(eq("test.com"));
						
						bundleService.setTrustBundleRepository(mockBundleDAO);
						bundleService.setDomainRepository(mockDomainDAO);
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
					bundleService.setDomainRepository(domainRepo);
				}	
				
				@Override
				protected Collection<TrustBundleDomainReltn> getBundlesToAdd()
				{
					return null;
				}

				@Override
				protected Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected String getBundleNameToAssociate()
				{
					return "testBundle1";
				}
				
				@Override
				protected String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override
				protected String getDomainNameToRetrieve()
				{
					return "test.com";
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
		public void testGetBundlesByDomain_errorInGroupLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						DomainRepository mockDomainDAO = mock(DomainRepository.class);
						TrustBundleDomainReltnRepository mockReltnDAO = mock(TrustBundleDomainReltnRepository.class);
						
						when(mockDomainDAO.findByDomainNameIgnoreCase("test.com")).thenReturn(new org.nhindirect.config.store.Domain());
						doThrow(new RuntimeException()).when(mockReltnDAO).findByDomain((org.nhindirect.config.store.Domain)any());
						
						bundleService.setTrustBundleDomainReltnRepository(mockReltnDAO);
						bundleService.setDomainRepository(mockDomainDAO);
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
					
					bundleService.setTrustBundleDomainReltnRepository(bundleDomainRepo);
					bundleService.setDomainRepository(domainRepo);
				}	
				
				@Override
				protected Collection<TrustBundleDomainReltn> getBundlesToAdd()
				{
					return null;
				}

				@Override
				protected Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected String getBundleNameToAssociate()
				{
					return "testBundle1";
				}
				
				@Override
				protected String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override
				protected String getDomainNameToRetrieve()
				{
					return "test.com";
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
