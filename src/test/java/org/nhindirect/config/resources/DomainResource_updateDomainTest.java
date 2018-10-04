package org.nhindirect.config.resources;

import static org.mockito.Matchers.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class DomainResource_updateDomainTest extends SpringBaseTest
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
		
		protected abstract Domain getDomainToUpdate();
		
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
			

			final HttpEntity<Domain> requestEntity = new HttpEntity<>(getDomainToUpdate());
			final ResponseEntity<Void> resp = testRestTemplate.exchange("/domain", HttpMethod.POST, requestEntity, Void.class);
			if (resp.getStatusCodeValue() != 204)
				throw new HttpClientErrorException(resp.getStatusCode());

			final ResponseEntity<Domain> getDomain = testRestTemplate.getForEntity("/domain/" + getDomainToUpdate().getDomainName(), Domain.class);
			
			int statusCode = getDomain.getStatusCodeValue();
			if (statusCode == 404)
				doAssertions(null);
			else if (statusCode == 200)
				doAssertions(getDomain.getBody());
			else
				throw new HttpClientErrorException(getDomain.getStatusCode());						
		}
		
		
		protected void doAssertions(Domain domain) throws Exception
		{
			
		}
	}		
	
	@Test
	public void testUpdateDomain_updateExistingDomain_assertDomainUpdated() throws Exception
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
			
			protected Domain getDomainToUpdate()
			{				
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.NEW);	
				domain.setPostmasterAddress(postmasterAddress);			
				
				return domain;
			}
			
			@Override
			protected void doAssertions(Domain domain) throws Exception
			{
				assertNotNull(domain);
				assertEquals(this.domain.getDomainName(), domain.getDomainName());
				assertEquals(this.domain.getStatus(), domain.getStatus());
				assertEquals(this.domain.getPostmasterAddress().getEmailAddress(), domain.getPostmasterAddress().getEmailAddress());
			}
		}.perform();
	}	
	
	@Test
	public void testUpdateDomain_nonExistentDomain_assertNonFound() throws Exception
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
			
			protected Domain getDomainToUpdate()
			{				
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test2.com");
				
				domain = new Domain();
				
				domain.setDomainName("test2.com");
				domain.setStatus(EntityStatus.NEW);	
				domain.setPostmasterAddress(postmasterAddress);			
				
				return domain;
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
	public void testUpdateDomain_errorInDomain_assertServerError() throws Exception
	{
		new TestPlan()
		{
			protected Domain domain;

			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();

					DomainRepository mockDAO = mock(DomainRepository.class);
					doThrow(new RuntimeException()).when(mockDAO).findByDomainNameIgnoreCase(eq("test.com"));
					
					domainService.setDomainRepository(mockDAO);
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
				
				domainService.setDomainRepository(domainRepo);
			}
			
			@Override
			protected Domain getDomainToAdd()
			{
				return null;
			}
			
			protected Domain getDomainToUpdate()
			{				
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.NEW);	
				domain.setPostmasterAddress(postmasterAddress);			
				
				return domain;
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
	public void testUpdateDomain_errorInUpdate_assertServerError() throws Exception
	{
		new TestPlan()
		{
			protected Domain domain;

			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();

					DomainRepository mockDAO = mock(DomainRepository.class);
					when(mockDAO.findByDomainNameIgnoreCase((String)any())).thenReturn(new org.nhindirect.config.store.Domain());
					doThrow(new RuntimeException()).when(mockDAO).save((org.nhindirect.config.store.Domain)any());
					
					domainService.setDomainRepository(mockDAO);
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
				
				domainService.setDomainRepository(domainRepo);
			}
			
			@Override
			protected Domain getDomainToAdd()
			{
				return null;
			}
			
			protected Domain getDomainToUpdate()
			{				
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.NEW);	
				domain.setPostmasterAddress(postmasterAddress);			
				
				return domain;
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
