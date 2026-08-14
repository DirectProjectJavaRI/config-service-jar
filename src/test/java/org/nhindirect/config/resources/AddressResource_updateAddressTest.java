package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.repository.AddressRepository;
import org.nhindirect.config.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

public class AddressResource_updateAddressTest  extends SpringBaseTest
{
	@Autowired
	protected AddressResource addressService;	
	
	abstract class TestPlan extends BaseTestPlan 
	{

		@Override
		protected void tearDownMocks()
		{

		}
		
		protected abstract Address getAddressToAdd();
		
		protected abstract String getDomainToAdd();
		
		protected abstract Address getAddressToUpdate();
		
		@Override
		protected void performInner() throws Exception
		{				
			
			final Address addAddress = getAddressToAdd();
			final String domainName = getDomainToAdd();
			
			if (domainName != null && !domainName.isEmpty())
			{
				final org.nhindirect.config.store.Domain domain = new org.nhindirect.config.store.Domain();
				domain.setDomainName(domainName);
				domain.setStatus(org.nhindirect.config.store.EntityStatus.ENABLED.ordinal());
				domainRepo.save(domain).block();
				
				if (addAddress != null)
					addAddress.setDomainName(domainName);
			}
			
			/*
			 * Add address
			 */
			if (addAddress != null)
			{
				final ResponseEntity<Void> resp = webClient.put()
					.uri(uriBuilder -> uriBuilder.path("/address").build())
					.bodyValue(addAddress)
					.retrieve().toBodilessEntity().block();

				if (resp.getStatusCode().value() != 201)
					throw new WebClientResponseException(resp.getStatusCode().value(), resp.getStatusCode().toString(), null, null, null);
			}

			/*
			 * Update address
			 */
			final ResponseEntity<Void> resp = webClient.post()
				.uri(uriBuilder -> uriBuilder.path("/address").build())
				.bodyValue(getAddressToUpdate())
				.retrieve().toBodilessEntity().block();

			if (resp.getStatusCode().value() != 204)
				throw new WebClientResponseException(resp.getStatusCode().value(), resp.getStatusCode().toString(), null, null, null);

			/*
			 * Get and validated
			 */
			try
			{
				final ResponseEntity<Address> getAddress = webClient.get()
					.uri(uriBuilder -> uriBuilder.path("/address/{emailAddress}").build(getAddressToUpdate().getEmailAddress()))
					.retrieve().toEntity(Address.class).block();
				doAssertions(getAddress.getBody());
			}
			catch (Exception e)
			{
				throw e;
			}
		}
		
		
		protected void doAssertions(Address address) throws Exception
		{
			
		}
	}		
	
	@Test
	public void testUpdateAddress_updateExistingAddress_assertAddressUpdated() throws Exception
	{
		new TestPlan()
		{
			protected Address address;
			
			@Override
			protected  Address getAddressToAdd()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("email");
				address.setEndpoint("none");
				address.setDisplayName("me");
				
				return address;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return "test.com";
			}
			
			protected Address getAddressToUpdate()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("XD");
				address.setEndpoint("http://you.me.com");
				address.setDisplayName("me");
				address.setDomainName(getDomainToAdd());
				
				return address;
			}
			
			@Override
			protected void doAssertions(Address address) throws Exception
			{
				assertNotNull(address);
				assertEquals(this.address.getEmailAddress(), address.getEmailAddress());
				assertEquals(this.address.getType(), address.getType());
				assertEquals(this.address.getEndpoint(), address.getEndpoint());
				assertEquals(this.address.getDisplayName(), address.getDisplayName());
				assertEquals(this.address.getDomainName(), address.getDomainName());
			}
		}.perform();
	}	
	
	@Test
	public void testUpdateAddress_nonExistentDomain_assertNotFound() throws Exception
	{
		new TestPlan()
		{
			protected Address address;
			
			@Override
			protected  Address getAddressToAdd()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("email");
				address.setEndpoint("none");
				address.setDisplayName("me");
				
				return address;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return "test.com";
			}
			
			protected Address getAddressToUpdate()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("XD");
				address.setEndpoint("http://you.me.com");
				address.setDisplayName("me");
				address.setDomainName("test2.com");
				
				return address;
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
	public void testUpdateAddress_nonExistentAddress_assertNotFound() throws Exception
	{
		new TestPlan()
		{
			protected Address address;
			
			@Override
			protected  Address getAddressToAdd()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("email");
				address.setEndpoint("none");
				address.setDisplayName("me");
				
				return address;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return "test.com";
			}
			
			protected Address getAddressToUpdate()
			{
				address = new Address();
				
				address.setEmailAddress("me2@test.com");
				address.setType("XD");
				address.setEndpoint("http://you.me.com");
				address.setDisplayName("me");
				address.setDomainName(getDomainToAdd());
				
				return address;
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
	public void testUpdateAddress_emptyDomainName_assertBadRequest() throws Exception
	{
		new TestPlan()
		{
			protected Address address;
			
			@Override
			protected  Address getAddressToAdd()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("email");
				address.setEndpoint("none");
				address.setDisplayName("me");
				
				return address;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return "test.com";
			}
			
			protected Address getAddressToUpdate()
			{
				address = new Address();
				
				address.setEmailAddress("me2@test.com");
				address.setType("XD");
				address.setEndpoint("http://you.me.com");
				address.setDisplayName("me");
				address.setDomainName("");
				
				return address;
			}
			
			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof WebClientResponseException);
				WebClientResponseException ex = (WebClientResponseException)exception;
				assertEquals(400, ex.getStatusCode().value());
			}
		}.perform();
	}	
	
	@Test
	public void testUpdateAddress_nullDomainName_assertBadRequest() throws Exception
	{
		new TestPlan()
		{
			protected Address address;
			
			@Override
			protected  Address getAddressToAdd()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("email");
				address.setEndpoint("none");
				address.setDisplayName("me");
				
				return address;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return "test.com";
			}
			
			protected Address getAddressToUpdate()
			{
				address = new Address();
				
				address.setEmailAddress("me2@test.com");
				address.setType("XD");
				address.setEndpoint("http://you.me.com");
				address.setDisplayName("me");
				address.setDomainName(null);
				
				return address;
			}
			
			@Override
			protected void assertException(Exception exception) throws Exception
			{
				assertTrue(exception instanceof WebClientResponseException);
				WebClientResponseException ex = (WebClientResponseException)exception;
				assertEquals(400, ex.getStatusCode().value());
			}
		}.perform();
	}
	
	@Test
	public void testRemoveAddress_nonErrorInDomainLookup_assertServerError() throws Exception
	{
		new TestPlan()
		{	
			protected Address address;
			
			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();
					

					DomainRepository mockDAO = mock(DomainRepository.class);
					doThrow(new RuntimeException()).when(mockDAO).findByDomainNameIgnoreCase(eq("test.com"));
					
					addressService.setDomainRepository(mockDAO);
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
				
				addressService.setDomainRepository(domainRepo);
			}
			
			@Override
			protected  Address getAddressToAdd()
			{
				
				return null;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return null;
			}
			
			@Override
			protected Address getAddressToUpdate()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("XD");
				address.setEndpoint("http://you.me.com");
				address.setDisplayName("me");
				address.setDomainName("test.com");
				
				return address;
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
	public void testRemoveAddress_nonErrorInAddressLookup_assertServerError() throws Exception
	{
		new TestPlan()
		{			
			protected Address address;
			
			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();

					AddressRepository mockDAO = mock(AddressRepository.class);
					doThrow(new RuntimeException()).when(mockDAO).findByEmailAddressIgnoreCase(eq("me@test.com"));
					
					addressService.setAddressRepository(mockDAO);
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
				
				addressService.setAddressRepository(addressRepo);
			}
			
			@Override
			protected  Address getAddressToAdd()
			{
				
				return null;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return "test.com";
			}
			
			@Override
			protected Address getAddressToUpdate()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("XD");
				address.setEndpoint("http://you.me.com");
				address.setDisplayName("me");
				address.setDomainName("test.com");
				
				return address;
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
	public void testRemoveAddress_nonErrorInUpdate_assertServerError() throws Exception
	{
		new TestPlan()
		{
			protected Address address;
			
			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();

					AddressRepository mockDAO = mock(AddressRepository.class);
					
					org.nhindirect.config.store.Address addr = new org.nhindirect.config.store.Address();
					addr.setDomainId(1234L);
					when(mockDAO.findByEmailAddressIgnoreCase(any())).thenReturn(Mono.justOrEmpty(addr));
					doThrow(new RuntimeException()).when(mockDAO).save((org.nhindirect.config.store.Address)any());
					
					addressService.setAddressRepository(mockDAO);
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
				
				addressService.setAddressRepository(addressRepo);
			}
			
			@Override
			protected  Address getAddressToAdd()
			{
				return null;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return "test.com";
			}
			
			protected Address getAddressToUpdate()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("XD");
				address.setEndpoint("http://you.me.com");
				address.setDisplayName("me");
				address.setDomainName(getDomainToAdd());
				
				return address;
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
