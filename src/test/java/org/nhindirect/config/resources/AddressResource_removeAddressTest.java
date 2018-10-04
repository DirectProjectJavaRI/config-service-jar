package org.nhindirect.config.resources;

import static org.mockito.Matchers.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import org.nhindirect.config.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class AddressResource_removeAddressTest  extends SpringBaseTest
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
		
		protected abstract String getAddressNameToRemove();
		
		@Override
		protected void performInner() throws Exception
		{				
			
			final Address addAddress = getAddressToAdd();
			final String domainName = getDomainToAdd();
			
			if (domainName != null && !domainName.isEmpty())
			{
				
				
				final Domain domain = new Domain();
				domain.setDomainName(domainName);
				domain.setStatus(EntityStatus.ENABLED);
				
				final HttpEntity<Domain> requestEntity = new HttpEntity<>(domain);
				testRestTemplate.exchange("/domain", HttpMethod.PUT, requestEntity, Void.class);
				
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
			
			final ResponseEntity<?> getAddresses = testRestTemplate.exchange("/address/" + getAddressNameToRemove(), HttpMethod.DELETE, null, 
					Void.class);
			
			
			if (getAddresses.getStatusCodeValue() != 200)
				throw new HttpClientErrorException(getAddresses.getStatusCode());

			doAssertions();
		}
		
		
		protected void doAssertions() throws Exception
		{
			
		}
	}	
	
	@Test
	public void testRemoveAddress_removeExistingAddress_assertAddressRemoved() throws Exception
	{
		new TestPlan()
		{
			protected Address address;
			
			@Override
			protected void setupMocks() 
			{
				assertTrue(addressRepo.findAll().isEmpty());
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
			protected String getAddressNameToRemove()
			{
				return "me@test.com";
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				assertNull(addressRepo.findByEmailAddressIgnoreCase("me@test.com"));
			}
		}.perform();
	}	
	
	@Test
	public void testRemoveAddress_nonExistentAddress_assertNotFound() throws Exception
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
			protected String getAddressNameToRemove()
			{
				return "me@test2.com";
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
	public void testRemoveAddress_nonErrorInDelete_assertServerError() throws Exception
	{
		new TestPlan()
		{			
			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();

					AddressRepository mockDAO = mock(AddressRepository.class);
					when(mockDAO.findByEmailAddressIgnoreCase((String)any())).thenReturn(new org.nhindirect.config.store.Address());
					doThrow(new RuntimeException()).when(mockDAO).delete((org.nhindirect.config.store.Address)any());
					
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
			protected String getAddressNameToRemove()
			{
				return "me@test.com";
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
	public void testRemoveAddress_nonErrorInLookup_assertServerError() throws Exception
	{
		new TestPlan()
		{
			
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
			protected String getAddressNameToRemove()
			{
				return "me@test.com";
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
