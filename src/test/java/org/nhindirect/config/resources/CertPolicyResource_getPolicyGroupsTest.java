package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.repository.CertPolicyGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class CertPolicyResource_getPolicyGroupsTest extends SpringBaseTest
{
	@Autowired
	protected CertPolicyResource certService;
		
		abstract class TestPlan extends BaseTestPlan 
		{

			
			@Override
			protected void tearDownMocks()
			{

			}

			protected abstract Collection<CertPolicyGroup> getGroupsToAdd();
			
			
			@Override
			protected void performInner() throws Exception
			{				
				
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
				
				final ResponseEntity<Collection<CertPolicyGroup>> getGroups = testRestTemplate.exchange("/certpolicy/groups", HttpMethod.GET, null, 
						new ParameterizedTypeReference<Collection<CertPolicyGroup>>() {});

				if (getGroups.getStatusCodeValue() == 404 || getGroups.getStatusCodeValue() == 204)
					doAssertions(new ArrayList<>());
				else if (getGroups.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(getGroups.getStatusCode());
				else
					doAssertions(getGroups.getBody());			
			}
				
			protected void doAssertions(Collection<CertPolicyGroup> groups) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testGetAllPolicyGroups_assertPolicyGroupsRetrieved()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<CertPolicyGroup> groups;
				
				@Override
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

				@Override
				protected void doAssertions(Collection<CertPolicyGroup> groups) throws Exception
				{
					assertNotNull(groups);
					assertEquals(2, groups.size());
					
					final Iterator<CertPolicyGroup> addedGroupsIter = this.groups.iterator();
					
					for (CertPolicyGroup retrievedGroup : groups)
					{	
						final CertPolicyGroup addedGroup = addedGroupsIter.next(); 
						
						assertEquals(addedGroup.getPolicyGroupName(), retrievedGroup.getPolicyGroupName());
						assertTrue(retrievedGroup.getPolicies().isEmpty());

					}
					
				}
			}.perform();
		}		
		
		@Test
		public void testGetAllPolicyGroups_noGroupsInStore_assertNoPoliciesRetrieved()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
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
		public void testGetAllPolicyGroups_errorInLookup_assertServiceError()  throws Exception
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
						doThrow(new RuntimeException()).when(mockDAO).findAll();
						
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
				protected Collection<CertPolicyGroup> getGroupsToAdd()
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
