package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.repository.AddressRepository;
import org.nhindirect.config.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class AddressResource_getAddressesByDomainTest extends SpringBaseTest
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
		
		protected abstract String getDomainNameToGet();
		
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
			
			if (addAddress != null)
			{
				HttpEntity<Address> requestEntity = new HttpEntity<Address>(addAddress);
				ResponseEntity<Void> resp = testRestTemplate.exchange("/address", HttpMethod.PUT, requestEntity, Void.class);
				if (resp.getStatusCodeValue() != 201)
					throw new HttpClientErrorException(resp.getStatusCode());
			}
			
			final Collection<Address> addrs = webClient.get()
			.uri(uriBuilder ->  uriBuilder.path("address/domain/{dom}").build(getDomainNameToGet()))
			.retrieve().bodyToMono(new ParameterizedTypeReference<Collection<Address>>() {}).block();

			doAssertions(addrs);

			
		}
		
		
		protected void doAssertions(Collection<Address> addresses) throws Exception
		{
			
		}
	}	
	
	@Test
	public void testGetAddresseseByDomain_getExistingAddress_assertAddressRetrieved() throws Exception
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
			
			@Override
			protected String getDomainNameToGet()
			{
				return "test.com";
			}
			
			@Override
			protected void doAssertions(Collection<Address> addresses) throws Exception
			{
				
				
				assertNotNull(addresses);
				assertEquals(1, addresses.size());
				final Address address = addresses.iterator().next();
				
				assertEquals(this.address.getEmailAddress(), address.getEmailAddress());
				assertEquals(this.address.getType(), address.getType());
				assertEquals(this.address.getEndpoint(), address.getEndpoint());
				assertEquals(this.address.getDisplayName(), address.getDisplayName());
				assertEquals(this.address.getDomainName(), address.getDomainName());
			}
		}.perform();
	}		
	
	@Test
	public void testGetAddressesByDomain_nonExistentDomain_assertNull() throws Exception
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
	public void testGetAddressesByDomain_nonExistentAddress_assertNull() throws Exception
	{
		new TestPlan()
		{
			
			@Override
			protected  Address getAddressToAdd()
			{
				return null;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return "test2.com";
			}
			
			@Override
			protected String getDomainNameToGet()
			{
				return "test2.com";
			}
			
			@Override
			protected void doAssertions(Collection<Address> addresses) throws Exception
			{
				assertTrue(addresses.isEmpty());
			}
		}.perform();
	}	
	
	@Test
	public void testGetAddress_errorInDomainLookup_assertServerError() throws Exception
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
					doThrow(new RuntimeException()).when(mockDAO).findByDomainNameIgnoreCase(eq("blowup.com"));
					
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
			protected String getDomainNameToGet()
			{
				return "blowup.com";
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
	
	@Test
	public void testGetAddress_errorInAddressLookup_assertServerError() throws Exception
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
					doThrow(new RuntimeException()).when(mockDAO).findByDomainId(any());
					
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
			
			@Override
			protected String getDomainNameToGet()
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
