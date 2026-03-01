package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.BundleRefreshError;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class TrustBundleResource_refreshBundleTest extends SpringBaseTest
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
			
			protected abstract String getBundleNameToRefresh();
			
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

				final ResponseEntity<Void> resp = webClient.post()
					.uri(uriBuilder -> uriBuilder.path("/trustbundle/{bundle}/refreshBundle").build(getBundleNameToRefresh()))
					.retrieve().toBodilessEntity().block();
				if (resp.getStatusCode().value() != 204)
					throw new WebClientResponseException(resp.getStatusCode(), "", resp.getHeaders(), null, null, null);

				final ResponseEntity<TrustBundle> getBundle = webClient.get()
					.uri(uriBuilder -> uriBuilder.path("/trustbundle/{bundle}").build(getBundleNameToRefresh()))
					.retrieve().toEntity(TrustBundle.class).block();

				int statusCode = getBundle.getStatusCode().value();
				if (statusCode == 404)
					doAssertions(null);
				else if (statusCode == 200)
					doAssertions(getBundle.getBody());
				else
					throw new WebClientResponseException(getBundle.getStatusCode(), "", getBundle.getHeaders(), null, null, null);

			}
				
			protected void doAssertions(TrustBundle bundle) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testRefershBundle_assertBundleRefreshed()  throws Exception
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
				protected String getBundleNameToRefresh()
				{
					return "testBundle1";
				}
				
				protected void doAssertions(TrustBundle bundle) throws Exception
				{
					assertTrue(bundle.getLastRefreshAttempt() != null);
					assertEquals(BundleRefreshError.SUCCESS, bundle.getLastRefreshError());
				}
			}.perform();
		}	
		
		@Test
		public void testRefershBundle_bundleDoesNotExist_assertNotFound()  throws Exception
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
				protected String getBundleNameToRefresh()
				{
					return "testBundle2";
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
		public void testRefershBundle_errorInRefresh_assertServiceError()  throws Exception
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
				protected String getBundleNameToRefresh()
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
