package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.repository.CertPolicyRepository;
import org.nhindirect.policy.PolicyLexicon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;


public class CertPolicyResource_removePolicyByNameTest extends SpringBaseTest
{
	@Autowired
	protected CertPolicyResource certService;	
		abstract class TestPlan extends BaseTestPlan 
		{			
			@Override
			protected void tearDownMocks()
			{

			}

			protected abstract Collection<CertPolicy> getPoliciesToAdd();
			
			protected abstract String getPolicyNameToDelete();
			
			@Override
			protected void performInner() throws Exception
			{

				final Collection<CertPolicy> policiesToAdd = getPoliciesToAdd();

				if (policiesToAdd != null)
				{
					policiesToAdd.forEach(addPolicy->
					{
						final ResponseEntity<Void> resp = webClient.put()
							.uri(uriBuilder -> uriBuilder.path("/certpolicy").build())
							.bodyValue(addPolicy)
							.retrieve().toBodilessEntity().block();
						if (resp.getStatusCode().value() != 201)
							throw new WebClientResponseException(resp.getStatusCode(), "", resp.getHeaders(), null, null, null);
					});
				}

				final ResponseEntity<Void> resp = webClient.delete()
					.uri(uriBuilder -> uriBuilder.path("/certpolicy/{name}").build(getPolicyNameToDelete()))
					.retrieve().toBodilessEntity().block();

				if (resp.getStatusCode().value() != 200)
					throw new WebClientResponseException(resp.getStatusCode(), "", resp.getHeaders(), null, null, null);

				doAssertions();
			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}	
		
		@Test
		public void testremovePolicyByName_removeExistingPolicy_assertPolicyRemoved()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<CertPolicy> policies;
				
				@Override
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
						
						
						return policies;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}

				@Override
				protected String getPolicyNameToDelete()
				{
					return "Policy1";
				}
				
				@Override
				protected void doAssertions() throws Exception
				{
					assertNull(policyRepo.findByPolicyNameIgnoreCase(getPolicyNameToDelete()).block());
				}
			}.perform();
		}		
		
		@Test
		public void testremovePolicyByName_nonExistantPolicy_assertNotFound()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected Collection<CertPolicy> getPoliciesToAdd()
				{
					return null;
				}

				@Override
				protected String getPolicyNameToDelete()
				{
					return "Policy1";
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
		public void testremovePolicyByName_errorInLookup_assertServiceError()  throws Exception
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
						doThrow(new RuntimeException()).when(mockDAO).findByPolicyNameIgnoreCase((String)any());
						
						certService.setCertPolicyRepository(mockDAO);
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
					
					certService.setCertPolicyRepository(policyRepo);
				}	
				
				
				@Override
				protected Collection<CertPolicy> getPoliciesToAdd()
				{
					return null;
				}

				@Override
				protected String getPolicyNameToDelete()
				{
					return "Policy1";
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
		public void testremovePolicyByName_errorInDelete_assertServiceError()  throws Exception
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
						
						final org.nhindirect.config.store.CertPolicy pol = new org.nhindirect.config.store.CertPolicy();
						pol.setPolicyName("Test");
						when(mockDAO.findByPolicyNameIgnoreCase((String)any())).thenReturn(Mono.just(pol));
						doThrow(new RuntimeException()).when(mockDAO).deleteById((Long)any());
						
						certService.setCertPolicyRepository(mockDAO);
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
					
					certService.setCertPolicyRepository(policyRepo);
				}	
				
				
				@Override
				protected Collection<CertPolicy> getPoliciesToAdd()
				{
					return null;
				}

				@Override
				protected String getPolicyNameToDelete()
				{
					return "Policy1";
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
