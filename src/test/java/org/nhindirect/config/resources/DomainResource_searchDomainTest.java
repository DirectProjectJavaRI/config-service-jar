package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class DomainResource_searchDomainTest extends SpringBaseTest
{
	@Autowired
	protected DomainResource domainService;
	
	abstract class TestPlan extends BaseTestPlan 
	{
		
		@Override
		protected void tearDownMocks()
		{

		}
		
		protected abstract Collection<Domain> getDomainsToAdd();
		
		protected abstract String getDomainNameToSearch();
		
		protected abstract String getEntityStatusToSearch();
		
		@Override
		protected void performInner() throws Exception
		{				
			
			final Collection<Domain> addDomains = getDomainsToAdd();
			
			if (addDomains != null)
			{
				addDomains.forEach(addDomain->
				{
					final HttpEntity<Domain> requestEntity = new HttpEntity<>(addDomain);
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/domain", HttpMethod.PUT, requestEntity, Void.class);
					if (resp.getStatusCodeValue() != 201)
						throw new HttpClientErrorException(resp.getStatusCode());
				});
				
			}

			final String entityStatus = (getEntityStatusToSearch() != null) ? getEntityStatusToSearch() : "";
			final ResponseEntity<Collection<Domain>> getDomains = 
					testRestTemplate.exchange("/domain?domainName={name}&entityStatus={status}", 
					HttpMethod.GET, null, new ParameterizedTypeReference<Collection<Domain>>() {},
					getDomainNameToSearch(), entityStatus);

			if (getDomains.getStatusCodeValue() == 404 || getDomains.getStatusCodeValue() == 204)
				doAssertions(new ArrayList<>());
			else if (getDomains.getStatusCodeValue() != 200)
				throw new HttpClientErrorException(getDomains.getStatusCode());
			else
				doAssertions(getDomains.getBody());				

			
		}
		
		
		protected void doAssertions(Collection<Domain> domains) throws Exception
		{
			
		}
	}	
	
	@Test
	public void testSearchDomains_getExistingDomain_nullEntityStatus_assertDomainRetrieved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Domain> domains;
			
			@Override
			protected Collection<Domain> getDomainsToAdd()
			{
				domains = new ArrayList<Domain>();
				
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				Domain domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				domains.add(domain);
				
				return domains;
			}
			
			@Override
			protected String getDomainNameToSearch()
			{
				return "test.com";
			}
			
			protected String getEntityStatusToSearch()
			{
				return null;
			}
			
			@Override
			protected void doAssertions(Collection<Domain> domains) throws Exception
			{
				assertNotNull(domains);
				assertEquals(1, domains.size());
				
				Domain retrievedDomain = domains.iterator().next();
				Domain addedDomain = this.domains.iterator().next();
				
				assertEquals(addedDomain.getDomainName(), retrievedDomain.getDomainName());
				assertEquals(addedDomain.getStatus(), retrievedDomain.getStatus());
				assertEquals(addedDomain.getPostmasterAddress().getEmailAddress(), retrievedDomain.getPostmasterAddress().getEmailAddress());
			}
		}.perform();
	}	
	
	@Test
	public void testSearchDomains_getExistingDomain_invalidEntityStatus_assertDomainRetrieved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Domain> domains;
			
			@Override
			protected Collection<Domain> getDomainsToAdd()
			{
				domains = new ArrayList<Domain>();
				
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				Domain domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				domains.add(domain);
				
				return domains;
			}
			
			@Override
			protected String getDomainNameToSearch()
			{
				return "test.com";
			}
			
			protected String getEntityStatusToSearch()
			{
				return "invalid";
			}
			
			@Override
			protected void doAssertions(Collection<Domain> domains) throws Exception
			{
				assertNotNull(domains);
				assertEquals(1, domains.size());
				
				Domain retrievedDomain = domains.iterator().next();
				Domain addedDomain = this.domains.iterator().next();
				
				assertEquals(addedDomain.getDomainName(), retrievedDomain.getDomainName());
				assertEquals(addedDomain.getStatus(), retrievedDomain.getStatus());
				assertEquals(addedDomain.getPostmasterAddress().getEmailAddress(), retrievedDomain.getPostmasterAddress().getEmailAddress());
			}
		}.perform();
	}		
	
	@Test
	public void testSearchDomains_getExistingDomain_newEntityStatus_assertNoDomainRetrieved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Domain> domains;
			
			@Override
			protected Collection<Domain> getDomainsToAdd()
			{
				domains = new ArrayList<Domain>();
				
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				Domain domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				domains.add(domain);
				
				return domains;
			}
			
			@Override
			protected String getDomainNameToSearch()
			{
				return "test.com";
			}
			
			protected String getEntityStatusToSearch()
			{
				return "NEW";
			}
			
			@Override
			protected void doAssertions(Collection<Domain> domains) throws Exception
			{
				assertNotNull(domains);
				assertEquals(0, domains.size());
				
			}
		}.perform();
	}	
	
	@Test
	public void testSearchDomains_getNonExistantDomain_assertNoDomainRetrieved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Domain> domains;
			
			@Override
			protected Collection<Domain> getDomainsToAdd()
			{
				domains = new ArrayList<Domain>();
				
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				Domain domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				domains.add(domain);
				
				return domains;
			}
			
			@Override
			protected String getDomainNameToSearch()
			{
				return "test2.com";
			}
			
			protected String getEntityStatusToSearch()
			{
				return "";
			}
			
			@Override
			protected void doAssertions(Collection<Domain> domains) throws Exception
			{
				assertNotNull(domains);
				assertEquals(0, domains.size());
				
			}
		}.perform();
	}		
	
	@Test
	public void testSearchDomains_getExistingDomain_emptySearchString_assertDomainRetrieved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Domain> domains;
			
			@Override
			protected Collection<Domain> getDomainsToAdd()
			{
				domains = new ArrayList<Domain>();
				
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				Domain domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				domains.add(domain);
				
				return domains;
			}
			
			@Override
			protected String getDomainNameToSearch()
			{
				return "";
			}
			
			protected String getEntityStatusToSearch()
			{
				return "ENABLED";
			}
			
			@Override
			protected void doAssertions(Collection<Domain> domains) throws Exception
			{
				assertNotNull(domains);
				assertEquals(1, domains.size());
				
				Domain retrievedDomain = domains.iterator().next();
				Domain addedDomain = this.domains.iterator().next();
				
				assertEquals(addedDomain.getDomainName(), retrievedDomain.getDomainName());
				assertEquals(addedDomain.getStatus(), retrievedDomain.getStatus());
				assertEquals(addedDomain.getPostmasterAddress().getEmailAddress(), retrievedDomain.getPostmasterAddress().getEmailAddress());
				
			}
		}.perform();
	}		
	
	@Test
	public void testSearchDomains_getExistingDomains_emptySearchString_assertDomainsRetrieved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Domain> domains;
			
			@Override
			protected Collection<Domain> getDomainsToAdd()
			{
				domains = new ArrayList<Domain>();
				
				// domain 1
				Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				Domain domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				domains.add(domain);
				
				// domain 2
				postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test2.com");
				
				domain = new Domain();
				
				domain.setDomainName("test2.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				domains.add(domain);				
				
				return domains;
			}
			
			@Override
			protected String getDomainNameToSearch()
			{
				return "";
			}
			
			protected String getEntityStatusToSearch()
			{
				return null;
			}
			
			@Override
			protected void doAssertions(Collection<Domain> domains) throws Exception
			{
				assertNotNull(domains);
				assertEquals(2, domains.size());
				
			}
		}.perform();
	}	
	
	@Test
	public void testSearchDomains_getExistingDomains_emptySearchString_enabledOnly_assertDomainRetrieved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Domain> domains;
			
			@Override
			protected Collection<Domain> getDomainsToAdd()
			{
				domains = new ArrayList<Domain>();
				
				// domain 1
				Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				Domain domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.NEW);
				domain.setPostmasterAddress(postmasterAddress);			
				
				domains.add(domain);
				
				// domain 2
				postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test2.com");
				
				domain = new Domain();
				
				domain.setDomainName("test2.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				domains.add(domain);				
				
				return domains;
			}
			
			@Override
			protected String getDomainNameToSearch()
			{
				return "";
			}
			
			protected String getEntityStatusToSearch()
			{
				return "ENABLED";
			}
			
			@Override
			protected void doAssertions(Collection<Domain> domains) throws Exception
			{
				assertNotNull(domains);
				assertEquals(1, domains.size());
				
				assertNotNull(domains);
				assertEquals(1, domains.size());
				
				Domain retrievedDomain = domains.iterator().next();
				
				assertEquals("test2.com", retrievedDomain.getDomainName());

			}
		}.perform();
	}
	
	@Test
	public void testSearchDomains_errorInSearch_assertServerError() throws Exception
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
					doThrow(new RuntimeException()).when(mockDAO).findByDomainNameContainingIgnoreCase(eq("test.com"));
					
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
			protected Collection<Domain> getDomainsToAdd()
			{
				return null;
			}
			
			@Override
			protected String getDomainNameToSearch()
			{
				return "test.com";
			}
			
			protected String getEntityStatusToSearch()
			{
				return null;
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
