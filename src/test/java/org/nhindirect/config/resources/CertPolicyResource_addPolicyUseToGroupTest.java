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
import java.util.List;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.model.CertPolicyGroupUse;
import org.nhindirect.config.model.CertPolicyUse;
import org.nhindirect.config.repository.CertPolicyGroupRepository;
import org.nhindirect.config.repository.CertPolicyRepository;
import org.nhindirect.policy.PolicyLexicon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import reactor.core.publisher.Mono;

public class CertPolicyResource_addPolicyUseToGroupTest extends SpringBaseTest
{
	@Autowired
	protected CertPolicyResource certService;
		
		abstract class TestPlan extends BaseTestPlan 
		{
			protected Collection<CertPolicyGroup> groups;
			
			protected List<CertPolicy> policies;
			
			
			@Override
			protected void tearDownMocks()
			{

			}

			protected Collection<CertPolicyGroup> getGroupsToAdd()
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
			
			protected Collection<CertPolicy> getPoliciesToAdd()
			{
				try
				{
					policies = new ArrayList<CertPolicy>();
					
					CertPolicy policy = new CertPolicy();
					policy.setPolicyName("Policy1");
					policy.setPolicyData(new byte[] {1,2,3});
					policy.setLexicon(PolicyLexicon.SIMPLE_TEXT_V1);
					policies.add(policy);
					
					policy = new CertPolicy();
					policy.setPolicyName("Policy2");
					policy.setPolicyData(new byte[] {1,2,5,6});
					policy.setLexicon(PolicyLexicon.JAVA_SER);
					policies.add(policy);
					
					return policies;
				}
				catch (Exception e)
				{
					throw new RuntimeException (e);
				}
			}
			
			protected abstract String getGroupNameToAssociate();
			
			protected abstract CertPolicyGroupUse getPolicyUseToAssociate();
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<CertPolicy> policiesToAdd = getPoliciesToAdd();
				
				if (policiesToAdd != null)
				{
					policiesToAdd.forEach(addPolicy->
					{
						final HttpEntity<CertPolicy> requestEntity = new HttpEntity<>(addPolicy);
						final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy", HttpMethod.PUT, requestEntity, Void.class);
						if (resp.getStatusCodeValue() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});	
					
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
				
				final HttpEntity<CertPolicyGroupUse> policyUseEntiity = new HttpEntity<>(getPolicyUseToAssociate());
				final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy/groups/uses/{groupName}", HttpMethod.POST, policyUseEntiity, Void.class, getGroupNameToAssociate());
				if (resp.getStatusCodeValue() != 204)
					throw new HttpClientErrorException(resp.getStatusCode());

				
				final ResponseEntity<CertPolicyGroup> getGroup = testRestTemplate.exchange("/certpolicy/groups/{groupName}", HttpMethod.GET, null, 
						CertPolicyGroup.class, getGroupNameToAssociate());

				doAssertions(getGroup.getBody());
				
				/*
				 * Delete the polices just so we can test the ability to delete policies
				 * that are part of a use
				 */
				if (policiesToAdd != null)
				{
					policiesToAdd.forEach(addPolicy->
					{
						final ResponseEntity<Void> delResp = testRestTemplate.exchange("/certpolicy/{name}", 
								HttpMethod.DELETE, null, Void.class,
								addPolicy.getPolicyName());

						if (delResp.getStatusCodeValue() != 200)
							throw new HttpClientErrorException(resp.getStatusCode());
					});	
					
				}
			}
				
			protected void doAssertions(CertPolicyGroup group) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testAddPolicyUseToGroup_assertPolicyAdded()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected CertPolicyGroupUse getPolicyUseToAssociate()
				{
					final CertPolicyGroupUse use = new CertPolicyGroupUse();
					
					use.setIncoming(true);
					use.setOutgoing(true);
					use.setPolicyUse(CertPolicyUse.TRUST);
					use.setPolicy(policies.get(0));
					
					return use;
				}
				
				@Override
				protected void doAssertions(CertPolicyGroup group) throws Exception
				{
					
					assertNotNull(group);
					
					assertEquals(getGroupNameToAssociate(), group.getPolicyGroupName());
					assertEquals(1, group.getPolicies().size());
					
					final CertPolicyGroupUse use = group.getPolicies().iterator().next();
					assertEquals(policies.get(0).getPolicyName(), use.getPolicy().getPolicyName());
					assertEquals(CertPolicyUse.TRUST, use.getPolicyUse());
					assertTrue(use.isIncoming());
					assertTrue(use.isOutgoing());
					
				}
			}.perform();
		}		
		
		@Test
		public void testAddPolicyUseToGroup_nonExistantGroup_assertNotFound()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getGroupNameToAssociate()
				{
					return "Group4";
				}
				
				@Override
				protected CertPolicyGroupUse getPolicyUseToAssociate()
				{
					final CertPolicyGroupUse use = new CertPolicyGroupUse();
					
					use.setIncoming(true);
					use.setOutgoing(true);
					use.setPolicyUse(CertPolicyUse.TRUST);
					use.setPolicy(policies.iterator().next());
					
					return use;
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
		public void testAddPolicyUseToGroup_nonExistantPolicy_assertNotFound()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected CertPolicyGroupUse getPolicyUseToAssociate()
				{
					final CertPolicyGroupUse use = new CertPolicyGroupUse();
					
					final CertPolicy policy = new CertPolicy();
					policy.setPolicyName("bogus");
					
					use.setIncoming(true);
					use.setOutgoing(true);
					use.setPolicyUse(CertPolicyUse.TRUST);
					use.setPolicy(policy);
					
					return use;
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
		public void testAddPolicyUseToGroup_errorInGroupLookup_assertServiceError()  throws Exception
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
				protected Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected Collection<CertPolicy> getPoliciesToAdd()
				{
					return null;
				}
				
				@Override
				protected void tearDownMocks()
				{
					super.tearDownMocks();
					
					certService.setCertPolicyRepository(policyRepo);
					certService.setCertPolicyGroupRepository(policyGroupRepo);
				}
				
				@Override
				protected String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected CertPolicyGroupUse getPolicyUseToAssociate()
				{
					final CertPolicyGroupUse use = new CertPolicyGroupUse();
					
					final CertPolicy policy = new CertPolicy();
					policy.setPolicyName("bogus");
					
					use.setIncoming(true);
					use.setOutgoing(true);
					use.setPolicyUse(CertPolicyUse.TRUST);
					use.setPolicy(policy);
					
					return use;
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
		public void testAddPolicyUseToGroup_errorInPolicyLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						CertPolicyRepository mockDAO = mock(CertPolicyRepository.class);
						CertPolicyGroupRepository mockGroupDAO = mock(CertPolicyGroupRepository.class);
						
						final org.nhindirect.config.store.CertPolicyGroup group = new org.nhindirect.config.store.CertPolicyGroup();
						group.setPolicyGroupName("Test");
						when(mockGroupDAO.findByPolicyGroupNameIgnoreCase(any())).thenReturn(Mono.just(group));
						doThrow(new RuntimeException()).when(mockDAO).findByPolicyNameIgnoreCase(any());
						
						certService.setCertPolicyRepository(mockDAO);
						certService.setCertPolicyGroupRepository(mockGroupDAO);
					}
					catch (Throwable t)
					{
						throw new RuntimeException(t);
					}
				}
				
				@Override
				protected Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected Collection<CertPolicy> getPoliciesToAdd()
				{
					return null;
				}
				
				@Override
				protected void tearDownMocks()
				{
					super.tearDownMocks();
					
					certService.setCertPolicyRepository(policyRepo);
					certService.setCertPolicyGroupRepository(policyGroupRepo);
				}
				
				@Override
				protected String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected CertPolicyGroupUse getPolicyUseToAssociate()
				{
					final CertPolicyGroupUse use = new CertPolicyGroupUse();
					
					final CertPolicy policy = new CertPolicy();
					policy.setPolicyName("bogus");
					
					use.setIncoming(true);
					use.setOutgoing(true);
					use.setPolicyUse(CertPolicyUse.TRUST);
					use.setPolicy(policy);
					
					return use;
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
		public void testAddPolicyUseToGroup_errorInAssociate_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						CertPolicyRepository mockDAO = mock(CertPolicyRepository.class);
						CertPolicyGroupRepository mockGroupDAO = mock(CertPolicyGroupRepository.class);
						
						final org.nhindirect.config.store.CertPolicyGroup group = new org.nhindirect.config.store.CertPolicyGroup();
						group.setPolicyGroupName("Test");
						
						final org.nhindirect.config.store.CertPolicy policy = new org.nhindirect.config.store.CertPolicy();
						policy.setPolicyName("Test");
						
						when(mockGroupDAO.findByPolicyGroupNameIgnoreCase(any())).thenReturn(Mono.just(group));
						when(mockDAO.findByPolicyNameIgnoreCase(any())).thenReturn(Mono.just(policy));
						doThrow(new RuntimeException()).when(mockGroupDAO).save(any());
						
						certService.setCertPolicyRepository(mockDAO);
						certService.setCertPolicyGroupRepository(mockGroupDAO);
					}
					catch (Throwable t)
					{
						throw new RuntimeException(t);
					}
				}
				
				@Override
				protected Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected Collection<CertPolicy> getPoliciesToAdd()
				{
					return null;
				}
				
				@Override
				protected void tearDownMocks()
				{
					super.tearDownMocks();
					
					certService.setCertPolicyRepository(policyRepo);
					certService.setCertPolicyGroupRepository(policyGroupRepo);
				}
				
				@Override
				protected String getGroupNameToAssociate()
				{
					return "Group1";
				}
				
				@Override
				protected CertPolicyGroupUse getPolicyUseToAssociate()
				{
					final CertPolicyGroupUse use = new CertPolicyGroupUse();
					
					final CertPolicy policy = new CertPolicy();
					policy.setPolicyName("Policy1");
					
					use.setIncoming(true);
					use.setOutgoing(true);
					use.setPolicyUse(CertPolicyUse.TRUST);
					use.setPolicy(policy);
					
					return use;
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
