package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;


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
				final ResponseEntity<Void> resp = webClient.put()
					.uri(uriBuilder -> uriBuilder.path("/domain").build())
					.bodyValue(addDomain)
					.retrieve().toBodilessEntity().block();

				if (resp.getStatusCode().value() != 201)
					throw new WebClientResponseException(resp.getStatusCode().value(), resp.getStatusCode().toString(), null, null, null);
			}

			final ResponseEntity<Void> resp =
					webClient.delete()
					.uri(uriBuilder -> uriBuilder.path("/domain/{name}").build(getDomainNameToRemove()))
					.retrieve().toBodilessEntity().block();

			if (resp.getStatusCode().value() != 200)
				throw new WebClientResponseException(resp.getStatusCode().value(), resp.getStatusCode().toString(), null, null, null);

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
				
				final Address addr = new Address();
				addr.setEmailAddress("you@test.com");
				addr.setStatus(EntityStatus.ENABLED);
				Collection<Address> addrs = new ArrayList<Address>();
				addrs.add(addr);
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				domain.setAddresses(addrs);
				
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
				assertEquals(null,  domainRepo.findByDomainNameIgnoreCase("@test.com").block());
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
				assertTrue(exception instanceof WebClientResponseException);
				WebClientResponseException ex = (WebClientResponseException)exception;
				assertEquals(404, ex.getStatusCode().value());
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
			protected String getDomainNameToRemove()
			{
				return "test.com";
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

					DomainRepository mockDAO = mock(DomainRepository.class);
					
					final org.nhindirect.config.store.Domain dom = new org.nhindirect.config.store.Domain();
					dom.setDomainName("Test");
					when(mockDAO.findByDomainNameIgnoreCase((String)any())).thenReturn(Mono.just(dom));
					doThrow(new RuntimeException()).when(mockDAO).deleteByDomainNameIgnoreCase(eq("test.com"));
					
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
			protected String getDomainNameToRemove()
			{
				return "test.com";
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
