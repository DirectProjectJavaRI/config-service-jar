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
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

public class TrustBundleResource_deleteBundleTest extends SpringBaseTest
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
			
			protected abstract String getBundleNameToDelete();
			
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
							throw new HttpClientErrorException(resp.getStatusCode());
					});
				}

				final ResponseEntity<Void> resp = webClient.delete()
					.uri(uriBuilder -> uriBuilder.path("/trustbundle/{bundle}").build(getBundleNameToDelete()))
					.retrieve().toBodilessEntity().block();

				if (resp.getStatusCode().value() != 200)
					throw new HttpClientErrorException(resp.getStatusCode());

				doAssertions();


			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}	
		
		@Test
		public void testDeleteBundle_removeExistingBundle_assertBundleRemoved() throws Exception
		{
			new TestPlan()
			{
				
				protected Collection<TrustBundle> bundles;
				
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
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
				
				@Override
				protected String getBundleNameToDelete()
				{
					return "testBundle1";
				}
				
				@Override
				protected void doAssertions() throws Exception
				{
					assertNull(bundleRepo.findByBundleNameIgnoreCase("testBundle1").block());
				}
			}.perform();
		}
		
		@Test
		public void testDeleteBundle_nonExistentBundle_assertNotFound() throws Exception
		{
			new TestPlan()
			{
				@Override
				protected Collection<TrustBundle> getBundlesToAdd()
				{
					return null;
	
				}
				
				@Override
				protected String getBundleNameToDelete()
				{
					return "testBundle1";
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
		public void testDeleteBundle_errorInLookup_assertServiceError() throws Exception
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
				protected String getBundleNameToDelete()
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
		public void testDeleteBundle_errorDelete_assertServiceError() throws Exception
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

						org.nhindirect.config.store.TrustBundle bundle = new org.nhindirect.config.store.TrustBundle();
						bundle.setBundleName("Test");
						when(mockDAO.findByBundleNameIgnoreCase((String)any())).thenReturn(Mono.just(bundle));
						doThrow(new RuntimeException()).when(mockDAO).deleteById((Long)any());

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
				protected String getBundleNameToDelete()
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
