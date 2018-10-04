package org.nhindirect.config.resources;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.repository.CertPolicyRepository;
import org.nhindirect.policy.PolicyLexicon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


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
						final HttpEntity<CertPolicy> requestEntity = new HttpEntity<>(addPolicy);
						final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy", HttpMethod.PUT, requestEntity, Void.class);
						if (resp.getStatusCodeValue() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});	
				}
				
				final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy/{name}", 
						HttpMethod.DELETE, null, Void.class,
						getPolicyNameToDelete());

				if (resp.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(resp.getStatusCode());

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
					assertNull(policyRepo.findByPolicyNameIgnoreCase(getPolicyNameToDelete()));
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
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(404, ex.getRawStatusCode());
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
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
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
						
						when(mockDAO.findByPolicyNameIgnoreCase((String)any())).thenReturn(new org.nhindirect.config.store.CertPolicy());
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
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}				
}
