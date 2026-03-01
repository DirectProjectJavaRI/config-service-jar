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

public class TrustBundleResource_disassociateTrustBundleFromDomainTest extends SpringBaseTest
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
					bundle.setBundleName("testBundle999");
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
				
				domain.setDomainName("testreltn.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				return domain;
			}
			
			protected String getBundleNameToAssociate()
			{
				return "testBundle999";
			}
			
			protected String getDomainNameToAssociate()
			{
				return "testreltn.com";
			}
			
			protected abstract String getBundleNameToDisassociate();
			
			protected abstract String getDomainNameToDisassociate();
			
			@Override
			protected void performInner() throws Exception
			{
				setUp();


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

				// make sure eveything is empty first
				if (addDomain != null)
				{
					final Collection<org.nhindirect.config.store.TrustBundleDomainReltn> bundleRelts =
						bundleDomainRepo.findByDomainId(domainRepo.findByDomainNameIgnoreCase(getDomainNameToAssociate()).block().getId()).collectList().block();

					assertTrue(bundleRelts.isEmpty());
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

				final ResponseEntity<Void> resp = webClient.delete()
					.uri(uriBuilder -> uriBuilder.path("/trustbundle/{bundle}/{domain}").build(getBundleNameToDisassociate(), getDomainNameToDisassociate()))
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
		public void testDisassociateBundleFromDomain_disassociateExistingDomainAndBundle_assertBundlesDisassociated()  throws Exception
		{
			new TestPlan()
			{			
				
				@Override
				protected String getBundleNameToDisassociate()
				{
					return "testBundle999";
				}
				
				@Override
				protected String getDomainNameToDisassociate()
				{
					return "testreltn.com";
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
		public void testDisassociateBundleFromDomain_unknownBundle_assertNotFound()  throws Exception
		{
			new TestPlan()
			{
			
				@Override
				protected void performInner() throws Exception
				{
					Thread.sleep(2000);
					
					super.performInner();
				}
				
				@Override
				protected String getBundleNameToDisassociate()
				{
					return "testBundle1333";
				}
				
				@Override
				protected String getDomainNameToDisassociate()
				{
					return "testreltn.com";
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
		public void testDisassociateBundleFromDomain_unknownDomain_assertNotFound()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getBundleNameToDisassociate()
				{
					return "testBundle999";
				}
				
				@Override
				protected String getDomainNameToDisassociate()
				{
					return "test.com123";
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
		public void testDisassociateBundleFromDomain_errorInBundleLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					return null;
				}
				
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
					return "testBundle999";
				}
				
				@Override
				protected String getDomainNameToDisassociate()
				{
					return "testreltn.com";
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
		public void testDisassociateBundleFromDomain_errorInDomainLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{

				protected Collection<TrustBundle> getBundlesToAdd()
				{
					return null;
				}
				
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
						
						when(mockBundleDAO.findByBundleNameIgnoreCase("testBundle1")).thenReturn(Mono.just(new org.nhindirect.config.store.TrustBundle()));
						doThrow(new RuntimeException()).when(mockDomainDAO).findByDomainNameIgnoreCase((String)any());
						
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
					return "testBundle999";
				}
				
				@Override
				protected String getDomainNameToDisassociate()
				{
					return "testreltn.com";
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
		public void testDisassociateBundleFromDomain_errorInDisassociate_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					return null;
				}
				
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
						TrustBundleDomainReltnRepository reltnDAO = mock(TrustBundleDomainReltnRepository.class);
						
						when(mockBundleDAO.findByBundleNameIgnoreCase("testBundle1")).thenReturn(Mono.just(new org.nhindirect.config.store.TrustBundle()));
						when(mockDomainDAO.findByDomainNameIgnoreCase("test.com")).thenReturn(Mono.just(new org.nhindirect.config.store.Domain()));
						doThrow(new RuntimeException()).when(reltnDAO).deleteByDomainIdAndTrustBundleId(any(), 
								any());
						
						
						bundleService.setTrustBundleRepository(mockBundleDAO);
						bundleService.setDomainRepository(mockDomainDAO);
						bundleService.setTrustBundleDomainReltnRepository(reltnDAO);
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
					bundleService.setTrustBundleDomainReltnRepository(bundleDomainRepo);
				}
				
				@Override
				protected String getBundleNameToDisassociate()
				{
					return "testBundle999";
				}
				
				@Override
				protected String getDomainNameToDisassociate()
				{
					return "testreltn.com";
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
