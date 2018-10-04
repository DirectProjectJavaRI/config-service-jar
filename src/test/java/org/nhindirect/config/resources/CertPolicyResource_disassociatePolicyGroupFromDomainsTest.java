package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.repository.CertPolicyGroupDomainReltnRepository;
import org.nhindirect.config.repository.CertPolicyGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class CertPolicyResource_disassociatePolicyGroupFromDomainsTest extends SpringBaseTest
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
			
			
			
			protected  String getGroupNameToAssociate()
			{
				return "Group1";
			}
			
			protected  String getDomainNameToAssociate()
			{
				return "test.com";
			}
			
			protected abstract String getGroupNameToDisassociate();
			
			
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
				

				// associate the bundle and domain
				if (groupsToAdd != null && addDomain != null)
				{					
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy/groups/domain/{groupName}/{domainName}", HttpMethod.POST, null, Void.class,
							getGroupNameToAssociate(), getDomainNameToAssociate());
					if (resp.getStatusCodeValue() != 204)
						throw new HttpClientErrorException(resp.getStatusCode());
				}
				
				// disassociate
				final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy/groups/domain/{groupName}/deleteFromGroup", HttpMethod.DELETE, null, Void.class,
						getGroupNameToDisassociate());
				if (resp.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(resp.getStatusCode());

				doAssertions();
				
			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}
		
		@Test
		public void testDisassociatePolicyGroupFromDomains_assertGroupDomainDisassociated()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getGroupNameToDisassociate()
				{
					return "Group1";
				}
				
				
				@Override
				protected void doAssertions() throws Exception
				{
					final org.nhindirect.config.store.Domain domain = domainRepo.findByDomainNameIgnoreCase(getDomainNameToAssociate());
					
					final Collection<org.nhindirect.config.store.CertPolicyGroupDomainReltn> reltns = groupReltnRepo.findByDomain(domain);
					
					assertEquals(0, reltns.size());
				}
			}.perform();
		}	
		
		@Test
		public void testDisassociatePolicyGroupFromDomains_unknownGroup_assertNotFound()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getGroupNameToDisassociate()
				{
					return "Group4";
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
		public void testDisassociatePolicyGroupFromDomains_errorInGroupLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						CertPolicyGroupRepository mockDAO = mock(CertPolicyGroupRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).findByPolicyGroupNameIgnoreCase((String)any());
						
						certService.setCertPolicyGroupRepository(mockDAO);
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
					
					certService.setCertPolicyGroupRepository(policyGroupRepo);
				}
				
				@Override
				protected  Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected  Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected String getGroupNameToDisassociate()
				{
					return "Group1";
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
		public void testDisassociatePolicyGroupFromDomains_errorInDisassociate_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						CertPolicyGroupRepository mockPolicyDAO = mock(CertPolicyGroupRepository.class);
						CertPolicyGroupDomainReltnRepository mockDomainDAO = mock(CertPolicyGroupDomainReltnRepository.class);
						
						when(mockPolicyDAO.findByPolicyGroupNameIgnoreCase("Group1")).thenReturn(new org.nhindirect.config.store.CertPolicyGroup());
						doThrow(new RuntimeException()).when(mockDomainDAO).deleteByCertPolicyGroup((org.nhindirect.config.store.CertPolicyGroup)any());
						
						certService.setCertPolicyGroupRepository(mockPolicyDAO);
						certService.setCertPolicyGroupDomainReltnRepository(mockDomainDAO);
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
					
					certService.setCertPolicyGroupRepository(policyGroupRepo);
					certService.setCertPolicyGroupDomainReltnRepository(groupReltnRepo);
				}
				
				@Override
				protected  Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected  Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected String getGroupNameToDisassociate()
				{
					return "Group1";
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
