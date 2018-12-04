package org.nhindirect.config.resources;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Arrays;
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

public class CertPolicyResource_updatePolicyAttributesTest extends SpringBaseTest
{
	@Autowired
	protected CertPolicyResource certService;	
		
		abstract class TestPlan extends BaseTestPlan 
		{
			protected Collection<CertPolicy> policies;
			
			@Override
			protected void tearDownMocks()
			{

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
					
					return policies;
				}
				catch (Exception e)
				{
					throw new RuntimeException (e);
				}
			}
			
			protected String getPolicyToUpdate()
			{
				return "Policy1";
			}

			
			protected abstract CertPolicy getUpdatePolicyAttributes();
			
			protected abstract String getPolicyUpdatedName();
			
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
				
				final HttpEntity<CertPolicy> requestEntity = new HttpEntity<>(getUpdatePolicyAttributes());
				final ResponseEntity<Void> resp = testRestTemplate.exchange("/certpolicy/{policy}/policyAttributes", 
						HttpMethod.POST, requestEntity, Void.class,
						getPolicyToUpdate());

				if (resp.getStatusCodeValue() != 204)
					throw new HttpClientErrorException(resp.getStatusCode());

				
				final ResponseEntity<CertPolicy> getPolicy = testRestTemplate.exchange("/certpolicy/{policy}", 
						HttpMethod.GET, null, CertPolicy.class,
						getPolicyUpdatedName());

				doAssertions(getPolicy.getBody());
			}
				
			protected void doAssertions(CertPolicy policy) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testUpdatePolicyAttributes_assertAttributesChanged()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected CertPolicy getUpdatePolicyAttributes()
				{
					final CertPolicy policy = new CertPolicy();
					policy.setPolicyName("Policy 2");
					policy.setLexicon(PolicyLexicon.XML);
					policy.setPolicyData(new byte[] {1,3,9,8});
					
					return policy;
				}
				
				@Override
				protected String getPolicyUpdatedName()
				{
					return "Policy 2";
				}

				@Override
				protected void doAssertions(CertPolicy policy) throws Exception
				{
					assertNotNull(policies);

						
					assertEquals("Policy 2", policy.getPolicyName());
					assertTrue(Arrays.equals(new byte[] {1,3,9,8}, policy.getPolicyData()));
					assertEquals(PolicyLexicon.XML, policy.getLexicon());
				}
			}.perform();
		}	
		
		@Test
		public void testUpdatePolicyAttributes_nullNameAndLexiconChange_assertAttributesUpdated()  throws Exception
		{
			new TestPlan()
			{
				@Override
				protected CertPolicy getUpdatePolicyAttributes()
				{
					final CertPolicy policy = new CertPolicy();
					policy.setPolicyData(new byte[] {1,3,9,8});
					
					return policy;
				}
				
				@Override
				protected String getPolicyUpdatedName()
				{
					return "Policy1";
				}

				@Override
				protected void doAssertions(CertPolicy policy) throws Exception
				{
					assertNotNull(policies);

						
					assertEquals("Policy1", policy.getPolicyName());
					assertTrue(Arrays.equals(new byte[] {1,3,9,8}, policy.getPolicyData()));
					assertEquals(PolicyLexicon.SIMPLE_TEXT_V1, policy.getLexicon());
				}
			}.perform();
		}	
		
		@Test
		public void testUpdatePolicyAttributes_nonExistantPolicy_assertNotFound()  throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected Collection<CertPolicy> getPoliciesToAdd()
				{
					return null;
				}
				
				@Override
				protected CertPolicy getUpdatePolicyAttributes()
				{
					final CertPolicy policy = new CertPolicy();
					policy.setPolicyData(new byte[] {1,3,9,8});
					
					return policy;
				}
				
				@Override
				protected String getPolicyUpdatedName()
				{
					return "Policy4";
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
		public void testUpdatePolicyAttributes_errorInLookup_assertServiceError()  throws Exception
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
				protected CertPolicy getUpdatePolicyAttributes()
				{
					final CertPolicy policy = new CertPolicy();
					policy.setPolicyData(new byte[] {1,3,9,8});
					
					return policy;
				}
				
				@Override
				protected String getPolicyUpdatedName()
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
		public void testUpdatePolicyAttributes_errorInUpdate_assertServiceError()  throws Exception
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
					return null;
				}
				
				@Override
				protected CertPolicy getUpdatePolicyAttributes()
				{
					final CertPolicy policy = new CertPolicy();
					policy.setPolicyData(new byte[] {1,3,9,8});
					
					return policy;
				}
				
				@Override
				protected String getPolicyUpdatedName()
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
