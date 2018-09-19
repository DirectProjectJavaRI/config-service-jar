package org.nhindirect.config.resources;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;

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

public class TrustBundleResource_associateTrustBundleToDomainTest extends SpringBaseTest
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
			
			protected abstract Domain getDomainToAdd();
			
			protected abstract String getBundleNameToAssociate();
			
			protected abstract String getDomainNameToAssociate();
			
			protected String isIncoming()
			{
				return "true";
			}
			
			protected String isOutgoing()
			{
				return "true";
			}
			
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
				
				final ResponseEntity<Void> resp = testRestTemplate.exchange("/trustbundle/{bundle}/{domain}?incoming={incoming}&outgoing={outgoing}", 
						HttpMethod.POST, null, Void.class, 
						getBundleNameToAssociate(), getDomainNameToAssociate(), isIncoming(), isOutgoing());
				
				if (resp.getStatusCodeValue() != 204)
					throw new HttpClientErrorException(resp.getStatusCode());
				

				doAssertions();

			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}	
		
		@Test
		public void testAssociateBundleToDomain_associateExistingDomainAndBundle_assertBundlesAssociated()  throws Exception
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
				
				protected void doAssertions() throws Exception
				{
					
					final Collection<org.nhindirect.config.store.TrustBundleDomainReltn> bundleRelts =  
							bundleDao.getTrustBundlesByDomain(domainDao.getDomainByName(getDomainNameToAssociate()).getId());
					
					assertEquals(1, bundleRelts.size());
					
					final org.nhindirect.config.store.TrustBundleDomainReltn bundleReltn = bundleRelts.iterator().next();
					assertEquals(getDomainNameToAssociate(), bundleReltn.getDomain().getDomainName());
					assertEquals(getBundleNameToAssociate(), bundleReltn.getTrustBundle().getBundleName());
					assertTrue(bundleReltn.isIncoming());
					assertTrue(bundleReltn.isOutgoing());
				}
			}.perform();
		}	
		
		@Test
		public void testAssociateBundleToDomain_associateExistingDomainAndBundle_falseIncomingAndOutgoing_assertBundlesAssociated()  throws Exception
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
				
				protected String isIncoming()
				{
					return "false";
				}
				
				protected String isOutgoing()
				{
					return "false";
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
				
				protected void doAssertions() throws Exception
				{
					
					final Collection<org.nhindirect.config.store.TrustBundleDomainReltn> bundleRelts =  
							bundleDao.getTrustBundlesByDomain(domainDao.getDomainByName(getDomainNameToAssociate()).getId());
					
					assertEquals(1, bundleRelts.size());
					
					final org.nhindirect.config.store.TrustBundleDomainReltn bundleReltn = bundleRelts.iterator().next();
					assertEquals(getDomainNameToAssociate(), bundleReltn.getDomain().getDomainName());
					assertEquals(getBundleNameToAssociate(), bundleReltn.getTrustBundle().getBundleName());
					assertFalse(bundleReltn.isIncoming());
					assertFalse(bundleReltn.isOutgoing());
				}
			}.perform();
		}	
		
		@Test
		public void testAssociateBundleToDomain_unknownDomain_assertNotFound()  throws Exception
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
					return "test2.com";
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
		public void testAssociateBundleToDomain_unknownBundle_assertNotFound()  throws Exception
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
					return "testBundle2";
				}
				
				@Override
				protected String getDomainNameToAssociate()
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
		public void testAssociateBundleToDomain_errorInBundleLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						
						TrustBundleDao mockDAO = mock(TrustBundleDao.class);
						doThrow(new RuntimeException()).when(mockDAO).getTrustBundleByName(eq("testBundle1"));
						
						bundleService.setTrustBundleDao(mockDAO);
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
				}				
				
				
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
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}	
		
		
		@Test
		public void testAssociateBundleToDomain_errorInDomainLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						
						TrustBundleDao mockBundleDAO = mock(TrustBundleDao.class);
						DomainDao mockDomainDAO = mock(DomainDao.class);
						when(mockBundleDAO.getTrustBundleByName("testBundle1")).thenReturn(new org.nhindirect.config.store.TrustBundle());
						doThrow(new RuntimeException()).when(mockDomainDAO).getDomainByName(eq("test.com"));
						
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
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}	
		
		@Test
		public void testAssociateBundleToDomain_errorInAssociate_assertServiceError()  throws Exception
		{
			new TestPlan()
			{		
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						
						TrustBundleDao mockBundleDAO = mock(TrustBundleDao.class);
						DomainDao mockDomainDAO = mock(DomainDao.class);
						when(mockBundleDAO.getTrustBundleByName("testBundle1")).thenReturn(new org.nhindirect.config.store.TrustBundle());
						when(mockDomainDAO.getDomainByName("test.com")).thenReturn(new org.nhindirect.config.store.Domain());
						doThrow(new RuntimeException()).when(mockBundleDAO).associateTrustBundleToDomain(eq(0L), eq(0L), eq(true), eq(true));
						
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
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}		
		
		
}
