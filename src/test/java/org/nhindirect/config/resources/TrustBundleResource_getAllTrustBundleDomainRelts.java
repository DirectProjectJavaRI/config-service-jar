package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.model.TrustBundleDomainReltn;
import org.nhindirect.config.repository.DomainRepository;
import org.nhindirect.config.repository.TrustBundleDomainReltnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

public class TrustBundleResource_getAllTrustBundleDomainRelts extends SpringBaseTest
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
						final ResponseEntity<Void> resp = webClient.put()
							.uri(uriBuilder -> uriBuilder.path("/trustbundle").build())
							.bodyValue(addBundle.getTrustBundle())
							.retrieve().toBodilessEntity().block();
						if (resp.getStatusCode().value() != 201)
							throw new WebClientResponseException(resp.getStatusCode(), "", resp.getHeaders(), null, null, null);
					});
				}

				final Domain addDomain = getDomainToAdd();

				if (addDomain != null)
				{
					final ResponseEntity<Void> resp = webClient.put()
						.uri(uriBuilder -> uriBuilder.path("/domain").build())
						.bodyValue(addDomain)
						.retrieve().toBodilessEntity().block();
					if (resp.getStatusCode().value() != 201)
						throw new WebClientResponseException(resp.getStatusCode(), "", resp.getHeaders(), null, null, null);
				}


				// associate bundle to domain
				if (addDomain != null && bundlesToAdd != null)
				{
					final ResponseEntity<Void> resp = webClient.post()
						.uri(uriBuilder -> uriBuilder.path("/trustbundle/{bundle}/{domain}").build(getBundleNameToAssociate(), getDomainNameToAssociate()))
						.retrieve().toBodilessEntity().block();

					if (resp.getStatusCode().value() != 204)
						throw new WebClientResponseException(resp.getStatusCode(), "", resp.getHeaders(), null, null, null);
				}

				final Collection<TrustBundleDomainReltn> reltn = webClient.get()
				        .uri(uriBuilder -> uriBuilder.path("/trustbundle/domains/bundles/reltns")
				        	.queryParam("fetchAnchors", getFetchAnchors())
				             .build())
				        .retrieve()
				        .bodyToMono(new ParameterizedTypeReference<Collection<TrustBundleDomainReltn>>() {})
				        .defaultIfEmpty(new ArrayList<TrustBundleDomainReltn>() ).block();


				doAssertions(reltn);

			}
				
			protected void doAssertions(Collection<TrustBundleDomainReltn> bundles) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testGetAllTrustBundleDomainRelts_assertBundlesRetrieved()  throws Exception
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
						String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
						bundle.setBundleURL(bundleURL);	
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
		public void testGetAllTrustBundleDomainRelts_suppressAnchors_assertBundlesRetrievedWithNoAnchors()  throws Exception
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
						String bundleURL = getClass().getClassLoader().getResource("bundles/providerTestBundle.p7b").toString();
						bundle.setBundleURL(bundleURL);	
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
		public void testGetAllTrustBundleDomainRelts_noBundlesInDomain_assertNoBundlesRetrieved()  throws Exception
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
				
				protected void doAssertions(Collection<TrustBundleDomainReltn> bundles) throws Exception
				{
					assertNotNull(bundles);
					assertEquals(0, bundles.size());

				}
			}.perform();
		}			
				
		@Test
		public void testGetAllTrustBundleDomainRelts_errorInGroupLookup_assertServiceError()  throws Exception
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
						
						org.nhindirect.config.store.Domain dom = new org.nhindirect.config.store.Domain();
						dom.setDomainName("Test");
						when(mockDomainDAO.findByDomainNameIgnoreCase("test.com")).thenReturn(Mono.just(dom));
						doThrow(new RuntimeException()).when(mockReltnDAO).findAll();
						
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
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(500, ex.getStatusCode().value());
				}
			}.perform();
		}		
}
