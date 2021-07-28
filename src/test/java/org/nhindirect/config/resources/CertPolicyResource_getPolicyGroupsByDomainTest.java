package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.repository.CertPolicyGroupDomainReltnRepository;
import org.nhindirect.config.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;


public class CertPolicyResource_getPolicyGroupsByDomainTest extends SpringBaseTest
{
	@Autowired
	protected CertPolicyResource certService;	
		
		abstract class TestPlan extends BaseTestPlan 
		{
			protected Collection<CertPolicyGroup> groups;
			
			protected Collection<CertPolicy> policies;
			
			@Override
			protected void tearDownMocks()
			{

			}

			protected  Collection<CertPolicyGroup> getGroupsToAdd()
			{
				try
				{
					groups = new ArrayList<CertPolicyGroup>();
					
					CertPolicyGroup group = new CertPolicyGroup();
					group.setPolicyGroupName("Group1");
					groups.add(group);
					
					group = new CertPolicyGroup();
					group.setPolicyGroupName("Group2");
					groups.add(group);
					
					return groups;
				}
				catch (Exception e)
				{
					throw new RuntimeException (e);
				}
			}
			
			protected  Domain getDomainToAdd()
			{
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				Domain domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				return domain;
			}
			
			protected abstract String getGroupNameToAssociate();
			
			protected abstract String getDomainNameToAssociate();
			
			protected abstract String getDomainNameToLookup();
			
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
				
				final Collection<CertPolicyGroup> groupsToAdd = getGroupsToAdd();
				
				if (groupsToAdd != null)
				{
					groupsToAdd.forEach(addGroup->
					{
						final HttpEntity<CertPolicyGroup> requestEntity = new HttpEntity<>(addGroup);
						final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy/groups", HttpMethod.PUT, requestEntity, Void.class);
						if (resp.getStatusCodeValue() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});	
				}
				
				if (groupsToAdd != null && addDomain != null)
				{					
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy/groups/domain/{groupName}/{domainName}", HttpMethod.POST, null, Void.class,
							getGroupNameToAssociate(), getDomainNameToAssociate());
					if (resp.getStatusCodeValue() != 204)
						throw new HttpClientErrorException(resp.getStatusCode());
				}
				
				
				final Collection<CertPolicyGroup> getGroups = webClient.get()
						.uri(uriBuilder ->  uriBuilder.path("/certpolicy/groups/domain/{domain}").build(getDomainNameToLookup()))
						.retrieve().bodyToMono(new ParameterizedTypeReference<Collection<CertPolicyGroup>>() {}).block();

				doAssertions(getGroups);	
				
				
			}
				
			protected void doAssertions(Collection<CertPolicyGroup> groups) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testGetPolicyGroupsByDomain_assertGroupsRetrieved()  throws Exception
		{
			new TestPlan()
			{

				
				@Override
				protected  String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected  String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override 
				protected String getDomainNameToLookup()
				{
					return "test.com";
							
				}
				
				@Override
				protected void doAssertions(Collection<CertPolicyGroup> groups) throws Exception
				{
					
					assertNotNull(groups);
					
					assertEquals(1, groups.size());
					final CertPolicyGroup group = groups.iterator().next();
					
					assertEquals("Group1", group.getPolicyGroupName());
					
				}
			}.perform();
		}		
		
		@Test
		public void testGetPolicyGroupsByDomain_noGroupsInDomain_assertNoGroupsRetrieved()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected  Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected  String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected  String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override 
				protected String getDomainNameToLookup()
				{
					return "test.com";
							
				}
				
				@Override
				protected void doAssertions(Collection<CertPolicyGroup> groups) throws Exception
				{
					
					assertNotNull(groups);
					
					assertEquals(0, groups.size());
					
				}
			}.perform();
		}	
		
		@Test
		public void testGetPolicyGroupsByDomain_domainNotFound_assertNotFound()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected  Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected  String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected  String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override 
				protected String getDomainNameToLookup()
				{
					return "test.com1";
							
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
		public void testGetPolicyGroupsByDomain_errorInDomainLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();


						DomainRepository mockDomainDAO = mock(DomainRepository.class);
						
						doThrow(new RuntimeException()).when(mockDomainDAO).findByDomainNameIgnoreCase((String)any());
						
						certService.setDomainRepository(mockDomainDAO);
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

					certService.setDomainRepository(domainRepo);
				}
				
				@Override
				protected  Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected  Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected  String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected  String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override 
				protected String getDomainNameToLookup()
				{
					return "test.com1";
							
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
		public void testGetPolicyGroupsByDomain_errorInPolicyGroupLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						
						CertPolicyGroupDomainReltnRepository mockPolicyDAO = mock(CertPolicyGroupDomainReltnRepository.class);
						DomainRepository mockDomainDAO = mock(DomainRepository.class);
						
						final org.nhindirect.config.store.Domain domain = new org.nhindirect.config.store.Domain();
						domain.setDomainName("Test");
						when(mockDomainDAO.findByDomainNameIgnoreCase((String)any())).thenReturn(Mono.just(domain));
						doThrow(new RuntimeException()).when(mockPolicyDAO).findByDomainId(any());
						
						certService.setCertPolicyGroupDomainReltnRepository(mockPolicyDAO);
						certService.setDomainRepository(mockDomainDAO);
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
					
					certService.setCertPolicyGroupDomainReltnRepository(groupDomainReltnRepo);
					certService.setDomainRepository(domainRepo);
				}
				
				@Override
				protected  Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected  Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected  String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected  String getDomainNameToAssociate()
				{
					return "test.com";
				}
				
				@Override 
				protected String getDomainNameToLookup()
				{
					return "test.com1";
							
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
