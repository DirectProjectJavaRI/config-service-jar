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
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.repository.CertPolicyGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class CertPolicyResource_updateGroupAttributesTest extends SpringBaseTest
{
	@Autowired
	protected CertPolicyResource certService;	
		
		abstract class TestPlan extends BaseTestPlan 
		{
			
			protected Collection<CertPolicyGroup> groups;
			
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
					
					return groups;
				}
				catch (Exception e)
				{
					throw new RuntimeException (e);
				}
			}
			
			protected String getGroupToUpdate()
			{
				return "Group1";
			}

			
			protected abstract String getUpdateGroupAttributes();
			
			protected abstract String getGroupUpdatedName();
			
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
				
				final HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				final HttpEntity<String> requestEntity = new HttpEntity<>(getUpdateGroupAttributes(), headers);
				final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy/groups/{group}/groupAttributes", 
						HttpMethod.POST, requestEntity, Void.class,
						getGroupToUpdate());

				if (resp.getStatusCodeValue() != 204)
					throw new HttpClientErrorException(resp.getStatusCode());
				
				
				final ResponseEntity<CertPolicyGroup> getGroup = testRestTemplate.exchange("/certpolicy/groups/{group}", 
						HttpMethod.GET, null, CertPolicyGroup.class,
						getGroupUpdatedName());

				doAssertions(getGroup.getBody());
				
			}
				
			protected void doAssertions(CertPolicyGroup group) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testUpdateGroupAttributes_updateGroupName_assertNameUpdated()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected String getUpdateGroupAttributes()
				{
					return "Group2";
				}
				
				@Override
				protected String getGroupUpdatedName()
				{
					return "Group2";
				}
				
				@Override
				protected void doAssertions(CertPolicyGroup group) throws Exception
				{
					assertEquals(getUpdateGroupAttributes(), group.getPolicyGroupName());
				}
			}.perform();
		}
		
		@Test
		public void testUpdateGroupAttributes_nonExistantGroup_assertNotFound()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected String getGroupToUpdate()
				{
					return "Group2";
				}
				
				@Override
				protected String getUpdateGroupAttributes()
				{
					return "Group2";
				}
				
				@Override
				protected String getGroupUpdatedName()
				{
					return "Group2";
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
		public void testUpdateGroupAttributes_errorInLookup_assertServiceError()  throws Exception
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
				
				protected Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected String getUpdateGroupAttributes()
				{
					return "Group2";
				}
				
				@Override
				protected String getGroupUpdatedName()
				{
					return "Group2";
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
		public void testUpdateGroupAttributes_errorInUpdate_assertServiceError()  throws Exception
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
						when(mockDAO.findByPolicyGroupNameIgnoreCase((String)any())).thenReturn(new org.nhindirect.config.store.CertPolicyGroup());
						doThrow(new RuntimeException()).when(mockDAO).save((org.nhindirect.config.store.CertPolicyGroup)any());
						
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
				
				protected Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected String getUpdateGroupAttributes()
				{
					return "Group2";
				}
				
				@Override
				protected String getGroupUpdatedName()
				{
					return "Group2";
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
