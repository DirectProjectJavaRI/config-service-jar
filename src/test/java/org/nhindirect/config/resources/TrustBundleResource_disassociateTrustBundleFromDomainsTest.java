package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.repository.DomainRepository;
import org.nhindirect.config.repository.TrustBundleDomainReltnRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

public class TrustBundleResource_disassociateTrustBundleFromDomainsTest extends SpringBaseTest
{
	@Autowired
	protected TrustBundleResource bundleService;
	
		
		abstract class TestPlan extends BaseTestPlan 
		{
			protected Collection<TrustBundle> bundles;
			
			@Override
			protected void tearDownMocks()
			{

			}
			
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
			
			protected String getBundleNameToAssociate()
			{
				return "testBundle1";
			}
			
			protected String getDomainNameToAssociate()
			{
				return "test.com";
			}

			protected abstract String getBundleNameToDisassociate();
			
			@Override
			protected void performInner() throws Exception
			{

				final Collection<TrustBundle> bundlesToAdd = getBundlesToAdd();

				if (bundlesToAdd != null)
				{
					bundlesToAdd.forEach(addBundle->
					{
						final ResponseEntity<Void> resp = webClient.put()
							.uri(uriBuilder -> uriBuilder.path("/trustbundle").build())
							.bodyValue(addBundle)
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

				// associate the bundle and domain
				if (bundlesToAdd != null && addDomain != null)
				{
					final ResponseEntity<Void> resp = webClient.post()
						.uri(uriBuilder -> uriBuilder.path("/trustbundle/{bundle}/{domain}").build(getBundleNameToAssociate(), getDomainNameToAssociate()))
						.retrieve().toBodilessEntity().block();

					if (resp.getStatusCode().value() != 204)
						throw new WebClientResponseException(resp.getStatusCode(), "", resp.getHeaders(), null, null, null);
				}

				// disassociate the domain from all bundles
				final ResponseEntity<Void> resp = webClient.delete()
					.uri(uriBuilder -> uriBuilder.path("/trustbundle/{bundle}/deleteFromBundle").build(getBundleNameToDisassociate()))
					.retrieve().toBodilessEntity().block();

				if (resp.getStatusCode().value() != 200)
					throw new WebClientResponseException(resp.getStatusCode(), "", resp.getHeaders(), null, null, null);

				doAssertions();

			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}	
		
		@Test
		public void testDisassociateBundleFromDomains_disassociateExistingDomainAndBundle_assertBundlesDisassociated()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getBundleNameToDisassociate()
				{
					return "testBundle1";
				}
				
				protected void doAssertions() throws Exception
				{
					final Collection<org.nhindirect.config.store.TrustBundleDomainReltn> bundleRelts =  
							bundleDomainRepo.findByDomainId(domainRepo.findByDomainNameIgnoreCase(getDomainNameToAssociate()).block().getId()).collectList().block();
					
					assertTrue(bundleRelts.isEmpty());
					
				}
			}.perform();
		}
		
		@Test
		public void testDisassociateBundleFromDomains_nonexistantBundle_assertNotFound()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getBundleNameToDisassociate()
				{
					return "testBundle12";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception
				{
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(404, ex.getStatusCode().value());
				}
			}.perform();
		}
		
		
		@Test
		public void testDisassociateBundleFromDomains_errorInBundleLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					return null;
				}
				
				@Override
				protected Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						TrustBundleRepository mockBundleDAO = mock(TrustBundleRepository.class);
						DomainRepository mockDomainDAO = mock(DomainRepository.class);
						
						doThrow(new RuntimeException()).when(mockBundleDAO).findByBundleNameIgnoreCase((String)any());
						
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
				protected String getBundleNameToDisassociate()
				{
					return "testBundle1";
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
		
		@Test
		public void testDisassociateBundleFromDomains_errorInDisassociate_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					return null;
				}
				
				@Override
				protected Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						TrustBundleRepository mockBundleDAO = mock(TrustBundleRepository.class);
						TrustBundleDomainReltnRepository mockReltnDAO = mock(TrustBundleDomainReltnRepository.class);
						
						org.nhindirect.config.store.TrustBundle bundle = new org.nhindirect.config.store.TrustBundle();
						bundle.setBundleName("Test");
						when(mockBundleDAO.findByBundleNameIgnoreCase(getBundleNameToDisassociate())).thenReturn(Mono.just(bundle));
						doThrow(new RuntimeException()).when(mockReltnDAO).deleteByTrustBundleId(any());
						
						bundleService.setTrustBundleRepository(mockBundleDAO);
						bundleService.setTrustBundleDomainReltnRepository(mockReltnDAO);
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
					bundleService.setTrustBundleDomainReltnRepository(bundleDomainRepo);
				}
				
				@Override
				protected String getBundleNameToDisassociate()
				{
					return "testBundle1";
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
