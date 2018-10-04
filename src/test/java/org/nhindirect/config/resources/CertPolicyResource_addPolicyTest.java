package org.nhindirect.config.resources;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

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


public class CertPolicyResource_addPolicyTest extends SpringBaseTest
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
				doAssertions();
			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}	
		
		@Test
		public void testAddPolicy_assertPoliciesAdded()  throws Exception
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

				
				protected void doAssertions() throws Exception
				{
					final Collection<org.nhindirect.config.store.CertPolicy> policies = policyRepo.findAll();
					
					assertNotNull(policies);
					assertEquals(2, policies.size());
					
					final Iterator<CertPolicy> addedPoliciesIter = this.policies.iterator();
					
					for (org.nhindirect.config.store.CertPolicy retrievedPolicy : policies)
					{	
						final CertPolicy addedPolicy = addedPoliciesIter.next(); 
						assertEquals(addedPolicy.getPolicyName(), retrievedPolicy.getPolicyName());
						assertTrue(Arrays.equals(addedPolicy.getPolicyData(), retrievedPolicy.getPolicyData()));
						assertEquals(addedPolicy.getLexicon(), retrievedPolicy.getLexicon());
					}
					
				}
			}.perform();
		}	

		@Test
		public void testAddPolicy_addDuplicatePolicy_assertConflict()  throws Exception
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
						
						policy = new CertPolicy();
						policy.setPolicyName("Policy1");
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

				
				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(409, ex.getRawStatusCode());
				}
			}.perform();
		}		
		
		@Test
		public void testAddPolicy_errorInAdd_assertServiceError()  throws Exception
		{
			new TestPlan()
			{				
				protected Collection<CertPolicy> policies;
				
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						CertPolicyRepository mockDAO = mock(CertPolicyRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).save((org.nhindirect.config.store.CertPolicy)any());
						
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
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}		
		
		@Test
		public void testAddPolicy_errorInLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				protected Collection<CertPolicy> policies;
				
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
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}				
}
