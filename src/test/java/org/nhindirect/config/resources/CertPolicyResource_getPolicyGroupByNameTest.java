package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.repository.CertPolicyGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;


public class CertPolicyResource_getPolicyGroupByNameTest extends SpringBaseTest
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
			
			protected abstract String getGroupToRetrieve();
			
			@Override
			protected void performInner() throws Exception
			{

				final Collection<CertPolicyGroup> groupsToAdd = getGroupsToAdd();

				if (groupsToAdd != null)
				{
					groupsToAdd.forEach(addGroup->
					{
						ResponseEntity<Void> resp = webClient.put()
							.uri(uriBuilder -> uriBuilder.path("/certpolicy/groups").build())
							.bodyValue(addGroup)
							.retrieve().toBodilessEntity().block();
						if (resp.getStatusCode().value() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});
				}

				try {
					final ResponseEntity<CertPolicyGroup> getGroup = webClient.get()
						.uri(uriBuilder -> uriBuilder.path("/certpolicy/groups/{name}").build(getGroupToRetrieve()))
						.retrieve().toEntity(CertPolicyGroup.class).block();

					doAssertions(getGroup.getBody());
				} catch (WebClientResponseException e) {
					if (e.getStatusCode().value() == 404) {
						doAssertions(null);
					} else {
						throw e;
					}
				}

			}
				
			protected void doAssertions(CertPolicyGroup group) throws Exception
			{
				
			}
		}
		
		@Test
		public void testGetGroupByName_existingGroup_assertGroupRetrieved()  throws Exception
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
				protected String getGroupToRetrieve()
				{
					return "Group1";
				}
				
				@Override
				protected void doAssertions(CertPolicyGroup group) throws Exception
				{
					assertNotNull(group);
					
					final CertPolicyGroup addedGroup = this.groups.iterator().next();

					assertEquals(addedGroup.getPolicyGroupName(), group.getPolicyGroupName());
					assertTrue(group.getPolicies().isEmpty());	
					
				}
			}.perform();
		}		
		
		@Test
		public void testGetGroupByName_nonExistantGroup_assertGroupNotRetrieved()  throws Exception
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
				protected String getGroupToRetrieve()
				{
					return "Group144";
				}
				
				@Override
				protected void doAssertions(CertPolicyGroup group) throws Exception
				{
					assertNull(group);
					
				}
			}.perform();
		}		
		
		@Test
		public void testGetGroupByName_errorInLookup_assertServiceError()  throws Exception
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
				protected Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}

				@Override
				protected String getGroupToRetrieve()
				{
					return "Group1";
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
