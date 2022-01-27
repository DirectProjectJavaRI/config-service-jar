package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.repository.CertificateRepository;
import org.nhindirect.config.model.Certificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class CertificateResource_removeCertificatesByOwnerTest extends SpringBaseTest
{
	@Autowired
	protected CertificateResource certService;
		
		abstract class TestPlan extends BaseTestPlan 
		{			
			@Override
			protected void tearDownMocks()
			{

			}

			protected abstract Collection<Certificate> getCertsToAdd() throws Exception;
			
			protected abstract String getOwnerToRemove();
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<Certificate> certsToAdd = getCertsToAdd();

				if (certsToAdd != null)
				{
					certsToAdd.forEach(addCert->
					{
						final HttpEntity<Certificate> requestEntity = new HttpEntity<>(addCert);
						final ResponseEntity<Void> resp = testRestTemplate.exchange("/certificate", HttpMethod.PUT, requestEntity, Void.class);
						if (resp.getStatusCodeValue() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});			
				}

				final ResponseEntity<Void> resp = 
						testRestTemplate.exchange("/certificate/{ownder}",
		                HttpMethod.DELETE, null, Void.class, getOwnerToRemove());
				
				if (resp.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(resp.getStatusCode());
				
				doAssertions();
			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}	
		
		@Test
		public void testRemoveCertificatesByIds_removeExistingCerts_assertCertRemoved() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected Collection<Certificate> getCertsToAdd() throws Exception
				{
					try
					{
						certs = new ArrayList<Certificate>();
						
						Certificate cert = new Certificate();					
						cert.setData(TestUtils.loadCert("gm2552.der").getEncoded());
						
						certs.add(cert);
			
						cert = new Certificate();					
						cert.setData(TestUtils.loadCert("umesh.der").getEncoded());
						
						certs.add(cert);
						
						return certs;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected String getOwnerToRemove()
				{
					return "gm2552@securehealthemail.com";
				}
				
				@Override
				protected void doAssertions() throws Exception
				{
					final Collection<org.nhindirect.config.store.Certificate> certs = certRepo.findAll().collectList().block();
					assertEquals(1, certs.size());
				}
			}.perform();
		}
		
		@Test
		public void testRemoveCertificatesByIds_wrappedKeys_removeExistingCerts_assertCertRemoved() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected Collection<Certificate> getCertsToAdd() throws Exception
				{
					try
					{
						certs = new ArrayList<Certificate>();
						
						Certificate cert = new Certificate();	
						byte[] keyData = IOUtils.resourceToByteArray("certs/gm2552Key.der", getClass().getClassLoader());
						
						cert.setData(CertUtils.certAndWrappedKeyToRawByteFormat(keyData, TestUtils.loadCert("gm2552.der")));
						
						certs.add(cert);
			
						
						return certs;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected String getOwnerToRemove()
				{
					return "gm2552@securehealthemail.com";
				}
				
				@Override
				protected void doAssertions() throws Exception
				{
					final Collection<org.nhindirect.config.store.Certificate> certs = certRepo.findAll().collectList().block();
					assertEquals(0, certs.size());
				}
			}.perform();
		}
		
		@Test
		public void testRemoveCertificatesByIds_errorInDelete_assertServierError() throws Exception
		{
			new TestPlan()
			{

				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						CertificateRepository mockDAO = mock(CertificateRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).deleteByOwnerIgnoreCase((String)any());
						
						certService.setCertificateRepository(mockDAO);
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
					
					certService.setCertificateRepository(certRepo);
				}			
				
				@Override
				protected Collection<Certificate> getCertsToAdd()
				{
					return null;
				}

				
				@Override
				protected String getOwnerToRemove()
				{
					return "gm2552@securehealthemail.com";
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
