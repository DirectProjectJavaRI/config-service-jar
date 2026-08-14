package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.repository.CertPolicyGroupRepository;
import org.nhindirect.config.SpringBaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;


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
						final ResponseEntity<Void> resp = webClient.put()
							.uri(uriBuilder -> uriBuilder.path("/certpolicy/groups").build())
							.bodyValue(addGroup)
							.retrieve().toBodilessEntity().block();
						if (resp.getStatusCode().value() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});
				}

				final ResponseEntity<Void> resp = webClient.post()
					.uri(uriBuilder -> uriBuilder.path("/certpolicy/groups/{group}/groupAttributes").build(getGroupToUpdate()))
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(getUpdateGroupAttributes())
					.retrieve().toBodilessEntity().block();

				if (resp.getStatusCode().value() != 204)
					throw new HttpClientErrorException(resp.getStatusCode());


				final ResponseEntity<CertPolicyGroup> getGroup = webClient.get()
					.uri(uriBuilder -> uriBuilder.path("/certpolicy/groups/{group}").build(getGroupUpdatedName()))
					.retrieve().toEntity(CertPolicyGroup.class).block();

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
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(404, ex.getStatusCode().value());
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
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(500, ex.getStatusCode().value());
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
						
						final org.nhindirect.config.store.CertPolicyGroup group = new org.nhindirect.config.store.CertPolicyGroup();
						group.setPolicyGroupName("Test");
						when(mockDAO.findByPolicyGroupNameIgnoreCase((String)any())).thenReturn(Mono.just(group));
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
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(500, ex.getStatusCode().value());
				}
			}.perform();
		}	
}
