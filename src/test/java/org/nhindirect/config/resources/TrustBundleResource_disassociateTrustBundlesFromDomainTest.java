package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.store.dao.DomainDao;
import org.nhindirect.config.store.dao.TrustBundleDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class TrustBundleResource_disassociateTrustBundlesFromDomainTest extends SpringBaseTest
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
					File fl = new File("src/test/resources/bundles/providerTestBundle.p7b");
					bundle.setBundleURL(filePrefix + fl.getAbsolutePath());	
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

			protected abstract String getDomainNameToDisassociate();
			
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
				
				final Domain addDomain = getDomainToAdd();
				
				if (addDomain != null)
				{
					final HttpEntity<Domain> requestEntity = new HttpEntity<>(addDomain);
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/domain", HttpMethod.PUT, requestEntity, Void.class);
					if (resp.getStatusCodeValue() != 201)
						throw new HttpClientErrorException(resp.getStatusCode());
				}
				
				// associate the bundle and domain
				if (bundlesToAdd != null && addDomain != null)
				{					
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/trustbundle/{bundle}/{domain}", 
						HttpMethod.POST, null, Void.class, 
						getBundleNameToAssociate(), getDomainNameToAssociate());
					
					if (resp.getStatusCodeValue() != 204)
						throw new HttpClientErrorException(resp.getStatusCode());
				}

				// disassociate the domain from all bundles
				final ResponseEntity<Void> resp = testRestTemplate.exchange("/trustbundle/{domain}/deleteFromDomain", 
						HttpMethod.DELETE, null, Void.class, 
						getDomainNameToDisassociate());
				
				if (resp.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(resp.getStatusCode());

				doAssertions();

			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}	
		
		@Test
		public void testDisassociateBundlesFromDomain_disassociateExistingDomainAndBundle_assertBundlesDisassociated()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getDomainNameToDisassociate()
				{
					return "test.com";
				}
				
				protected void doAssertions() throws Exception
				{
					final Collection<org.nhindirect.config.store.TrustBundleDomainReltn> bundleRelts =  
							bundleDao.getTrustBundlesByDomain(domainDao.getDomainByName(getDomainNameToDisassociate()).getId());
					
					assertTrue(bundleRelts.isEmpty());
					
				}
			}.perform();
		}	
		
		@Test
		public void testDisassociateBundlesFromDomain_unknownDomain_assertNotFound()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getDomainNameToDisassociate()
				{
					return "test.com1";
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
		public void testDisassociateBundlesFromDomain_errorInDomainLookup_assertServiceErorr()  throws Exception
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
						TrustBundleDao mockBundleDAO = mock(TrustBundleDao.class);
						DomainDao mockDomainDAO = mock(DomainDao.class);
						
						doThrow(new RuntimeException()).when(mockDomainDAO).getDomainByName((String)any());
						
						bundleService.setTrustBundleDao(mockBundleDAO);
						bundleService.setDomainDao(mockDomainDAO);
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
					
					bundleService.setTrustBundleDao(bundleDao);
					bundleService.setDomainDao(domainDao);
				}
				
				@Override
				protected String getDomainNameToDisassociate()
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
		public void testDisassociateBundlesFromDomain_errorInDisassociate_assertServiceErorr()  throws Exception
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
						TrustBundleDao mockBundleDAO = mock(TrustBundleDao.class);
						DomainDao mockDomainDAO = mock(DomainDao.class);
						
						when(mockDomainDAO.getDomainByName("test.com")).thenReturn(new org.nhindirect.config.store.Domain());
						doThrow(new RuntimeException()).when(mockBundleDAO).disassociateTrustBundlesFromDomain(eq(0L));
						
						bundleService.setTrustBundleDao(mockBundleDAO);
						bundleService.setDomainDao(mockDomainDAO);
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
					
					bundleService.setTrustBundleDao(bundleDao);
					bundleService.setDomainDao(domainDao);
				}
				
				@Override
				protected String getDomainNameToDisassociate()
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
