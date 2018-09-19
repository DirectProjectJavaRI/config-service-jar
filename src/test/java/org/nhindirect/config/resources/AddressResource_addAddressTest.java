package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.store.dao.AddressDao;
import org.nhindirect.config.store.dao.DomainDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

public class AddressResource_addAddressTest extends SpringBaseTest
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
		
		@Override
		protected void performInner() throws Exception
		{				
			
			final Address addAddress = getAddressToAdd();
			final String domainName = getDomainToAdd();
			
			if (domainName != null && !domainName.isEmpty())
			{
				final org.nhindirect.config.store.Domain domain = new org.nhindirect.config.store.Domain();
				domain.setDomainName(domainName);
				domain.setStatus(org.nhindirect.config.store.EntityStatus.ENABLED);
				domainDao.add(domain);
				
				addAddress.setDomainName(domainName);
			}
			

				HttpEntity<Address> requestEntity = new HttpEntity<Address>(addAddress);
				ResponseEntity<Void> resp = testRestTemplate.exchange("/address", HttpMethod.PUT, requestEntity, Void.class);
				if (resp.getStatusCodeValue() != 201)
					throw new HttpClientErrorException(resp.getStatusCode());

			
			try
			{
				final ResponseEntity<Address> getAddress = testRestTemplate.getForEntity("/address/" + addAddress.getEmailAddress(), Address.class);
				doAssertions(getAddress.getBody());
			}
			catch (RestClientException e)
			{
				throw e;
			}
			
		}
		
		
		protected void doAssertions(Address address) throws Exception
		{
			
		}
	}		
	
	@Test
	public void testAddAddress_addNewAddress_assertAddressCreated() throws Exception
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
	public void testAddAddress_addNewAddress_nullDomainName_assertBadRequest() throws Exception
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
				return null;
			}
			
			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof HttpClientErrorException);
				HttpClientErrorException ex = (HttpClientErrorException)exception;
				assertEquals(400, ex.getRawStatusCode());
			}
		}.perform();
	}	
	
	@Test
	public void testAddAddress_addNewAddress_emptyDomainName_assertBadRequest() throws Exception
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
				address.setDomainName("");
				
				return address;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return null;
			}
			
			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof HttpClientErrorException);
				HttpClientErrorException ex = (HttpClientErrorException)exception;
				assertEquals(400, ex.getRawStatusCode());
			}
		}.perform();
	}	
	
	@Test
	public void testAddAddress_addNewAddress_unknownDomainName_assertNotFound() throws Exception
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
				address.setDomainName("hello.com");
				
				return address;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return null;
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
	public void testAddAddress_addNewAddress_alreadyExists_assertConflict() throws Exception
	{
		new TestPlan()
		{
			protected Address address;
			
			
			@Override
			protected void setupMocks()
			{
				super.setupMocks();
				
				org.nhindirect.config.store.Domain domain = new org.nhindirect.config.store.Domain();
				domain.setDomainName("test.com");
				domain.setStatus(org.nhindirect.config.store.EntityStatus.ENABLED);
				domainDao.add(domain);
				
				org.nhindirect.config.store.Address address = new org.nhindirect.config.store.Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("email");
				address.setEndpoint("none");
				address.setDisplayName("me");
				address.setStatus(org.nhindirect.config.store.EntityStatus.ENABLED);
				address.setDomain(domain);
				
				addressDao.add(address);
			}
			
			@Override
			protected  Address getAddressToAdd()
			{
				address = new Address();
				
				address.setEmailAddress("me@test.com");
				address.setType("email");
				address.setEndpoint("none");
				address.setDisplayName("me");
				address.setDomainName("test.com");
				
				return address;
			}
			
			@Override
			protected String getDomainToAdd()
			{
				return null;
			}
			
			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof HttpClientErrorException);
				HttpClientErrorException ex = (HttpClientErrorException)exception;
				assertEquals(409, ex.getRawStatusCode());
			}
		}.perform();
	}
	
	@Test
	public void testAddAddress_addNewAddress_errorInDomainLookup_assertServerError() throws Exception
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

					DomainDao mockDAO = mock(DomainDao.class);
					doThrow(new RuntimeException()).when(mockDAO).getDomainByName((String)any());
					
					addressService.setDomainDao(mockDAO);
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
				
				addressService.setDomainDao(domainDao);
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
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof HttpClientErrorException);
				HttpClientErrorException ex = (HttpClientErrorException)exception;
				assertEquals(500, ex.getRawStatusCode());
			}
		}.perform();
	}		
	
	@Test
	public void testAddAddress_addNewAddress_errorInAddressLookup_assertServerError() throws Exception
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

					AddressDao mockDAO = mock(AddressDao.class);
					doThrow(new RuntimeException()).when(mockDAO).get((String)any());
					
					addressService.setAddressDao(mockDAO);
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
				
				addressService.setAddressDao(addressDao);
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
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof HttpClientErrorException);
				HttpClientErrorException ex = (HttpClientErrorException)exception;
				assertEquals(500, ex.getRawStatusCode());
			}
		}.perform();
	}		
	
	@Test
	public void testAddAddress_addNewAddress_errorInAddressAdd_assertServerError() throws Exception
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

					AddressDao mockDAO = mock(AddressDao.class);
					doThrow(new RuntimeException()).when(mockDAO).add((org.nhindirect.config.store.Address)any());
					
					addressService.setAddressDao(mockDAO);
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
				
				addressService.setAddressDao(addressDao);
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
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof HttpClientErrorException);
				HttpClientErrorException ex = (HttpClientErrorException)exception;
				assertEquals(500, ex.getRawStatusCode());
			}
		}.perform();
	}			
}
