package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

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
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class DomainResource_getDomainTest extends SpringBaseTest
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
		
		protected abstract String getDomainNameToGet();
		
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
			
			final Domain domain = webClient.get()
			.uri(uriBuilder ->  uriBuilder.path("/domain/{domainName}").build(getDomainNameToGet()))
			.retrieve().bodyToMono(Domain.class).block();
			
			doAssertions(domain);

		}
		
		
		protected void doAssertions(Domain domain) throws Exception
		{
			
		}
	}
	
	@Test
	public void testGetDomain_getExistingDomain_assertDomainRetrieved() throws Exception
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
			protected String getDomainNameToGet()
			{
				return "test.com";
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
	public void testGetDomain_nonExistentDomain_assertNull() throws Exception
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
			protected String getDomainNameToGet()
			{
				return "test2.com";
			}
			
			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof WebClientResponseException);
				WebClientResponseException ex = (WebClientResponseException)exception;
				assertEquals(404, ex.getRawStatusCode());
			}
		}.perform();
	}	
	
	@Test
	public void testGetDomain_errorInLookup_assertServerError() throws Exception
	{
		new TestPlan()
		{
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
			
			@Override
			protected String getDomainNameToGet()
			{
				return "test.com";
			}
			
			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof WebClientResponseException);
				WebClientResponseException ex = (WebClientResponseException)exception;
				assertEquals(500, ex.getRawStatusCode());
			}
		}.perform();
	}	
}
