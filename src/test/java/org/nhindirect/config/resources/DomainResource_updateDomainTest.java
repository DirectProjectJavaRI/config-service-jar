package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

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
				final ResponseEntity<Void> resp = webClient.put()
					.uri(uriBuilder -> uriBuilder.path("/domain").build())
					.bodyValue(addDomain)
					.retrieve().toBodilessEntity().block();

				if (resp.getStatusCode().value() != 201)
					throw new WebClientResponseException(resp.getStatusCode().value(), resp.getStatusCode().toString(), null, null, null);
			}


			final ResponseEntity<Void> resp = webClient.post()
				.uri(uriBuilder -> uriBuilder.path("/domain").build())
				.bodyValue(getDomainToUpdate())
				.retrieve().toBodilessEntity().block();

			if (resp.getStatusCode().value() != 204)
				throw new WebClientResponseException(resp.getStatusCode().value(), resp.getStatusCode().toString(), null, null, null);

			final ResponseEntity<Domain> getDomain = webClient.get()
				.uri(uriBuilder -> uriBuilder.path("/domain/{domainName}").build(getDomainToUpdate().getDomainName()))
				.retrieve().toEntity(Domain.class).block();

			int statusCode = getDomain.getStatusCode().value();
			if (statusCode == 404)
				doAssertions(null);
			else if (statusCode == 200)
				doAssertions(getDomain.getBody());
			else
				throw new WebClientResponseException(statusCode, getDomain.getStatusCode().toString(), null, null, null);
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
				assertTrue(exception instanceof WebClientResponseException);
				WebClientResponseException ex = (WebClientResponseException)exception;
				assertEquals(404, ex.getStatusCode().value());
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
				assertTrue(exception instanceof WebClientResponseException);
				WebClientResponseException ex = (WebClientResponseException)exception;
				assertEquals(500, ex.getStatusCode().value());
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
					
					final org.nhindirect.config.store.Domain dom = new org.nhindirect.config.store.Domain();
					dom.setDomainName("Test");
					when(mockDAO.findByDomainNameIgnoreCase((String)any())).thenReturn(Mono.just(dom));
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
				assertTrue(exception instanceof WebClientResponseException);
				WebClientResponseException ex = (WebClientResponseException)exception;
				assertEquals(500, ex.getStatusCode().value());
			}
		}.perform();
	}		
}
