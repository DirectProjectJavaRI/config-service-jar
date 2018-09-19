package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.store.dao.DomainDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class DomainResource_removeDomainTest extends SpringBaseTest
{   
	@Autowired
	protected DomainResource domainService;
	
	abstract class TestPlan extends BaseTestPlan 
	{	
		@Override
		protected void tearDownMocks()
		{

		}
		
		protected abstract Domain getDomainToAdd();
		
		protected abstract String getDomainNameToRemove();
		
		@Override
		protected void performInner() throws Exception
		{				
			
			final Domain addDomain = getDomainToAdd();

			
			if (addDomain != null)
			{
				final HttpEntity<Domain> requestEntity = new HttpEntity<>(addDomain);
				final ResponseEntity<Void> resp = testRestTemplate.exchange("/domain", HttpMethod.PUT, requestEntity, Void.class);
				if (resp.getStatusCodeValue() != 201)
					throw new HttpClientErrorException(resp.getStatusCode());
			}

			final ResponseEntity<?> resp = 
					testRestTemplate.exchange("/domain/{name}", HttpMethod.DELETE, null, Void.class, getDomainNameToRemove());
				
			if (resp.getStatusCodeValue() != 200)
				throw new HttpClientErrorException(resp.getStatusCode());				

			doAssertions();
		}
		
		
		protected void doAssertions() throws Exception
		{
			
		}
	}	
	
	@Test
	public void testRemoveDomain_removeExistingDomain_assertDomainRemoved() throws Exception
	{
		new TestPlan()
		{
			protected Domain domain;
			
			@Override
			protected Domain getDomainToAdd()
			{
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				return domain;
			}
			
			@Override
			protected String getDomainNameToRemove()
			{
				return "test.com";
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				assertNull(domainDao.getDomainByName("@test.com"));
			}
		}.perform();
	}
	
	@Test
	public void testRemoveDomain_nonExxistentDomain_assertNotFound() throws Exception
	{
		new TestPlan()
		{
			protected Domain domain;
			
			@Override
			protected Domain getDomainToAdd()
			{
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				return domain;
			}
			
			@Override
			protected String getDomainNameToRemove()
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
	public void testRemoveDomain_errorInLookup_assertServerError() throws Exception
	{
		new TestPlan()
		{
			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();

					DomainDao mockDAO = mock(DomainDao.class);
					doThrow(new RuntimeException()).when(mockDAO).getDomainByName(eq("test.com"));
					
					domainService.setDomainDao(mockDAO);
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
				
				domainService.setDomainDao(domainDao);
			}
			
			
			@Override
			protected Domain getDomainToAdd()
			{
				return null;
			}
			
			@Override
			protected String getDomainNameToRemove()
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
	public void testRemoveDomain_errorInDelete_assertServerError() throws Exception
	{
		new TestPlan()
		{
	
			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();

					DomainDao mockDAO = mock(DomainDao.class);
					when(mockDAO.getDomainByName((String)any())).thenReturn(new org.nhindirect.config.store.Domain());
					doThrow(new RuntimeException()).when(mockDAO).delete(eq("test.com"));
					
					domainService.setDomainDao(mockDAO);
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
				
				domainService.setDomainDao(domainDao);
			}
			
			
			@Override
			protected Domain getDomainToAdd()
			{
				return null;
			}
			
			@Override
			protected String getDomainNameToRemove()
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
