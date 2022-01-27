package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.nhindirect.common.cert.Thumbprint;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.model.utils.CertUtils.CertContainer;
import org.nhindirect.config.repository.CertificateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class CertificateResource_getCertificatesByOwnerAndThumbprintTest extends SpringBaseTest
{
	@Autowired
	protected CertificateResource certService;
		
		abstract class TestPlan extends BaseTestPlan 
		{
			
			@Override
			protected void tearDownMocks()
			{

			}

			protected abstract Collection<Certificate> getCertsToAdd();

			protected abstract String getOwnerToRetrieve();
			
			protected abstract String getTPToRetrieve() throws Exception;
			
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
			

				final Certificate cert = webClient.get()
				        .uri(uriBuilder -> 
				        {	
								try {
									return uriBuilder.path("/certificate/{owner}/{thumbprint}")
									 .build(getOwnerToRetrieve(), getTPToRetrieve());
								} catch (Exception e) {
									return mock(URI.class);
								}

				        })
				        .retrieve()
				        .bodyToMono(Certificate.class).block();
				
					doAssertions(cert);

			}
				
			protected void doAssertions(Certificate cert) throws Exception
			{
				
			}
	  }

		@Test
		public void testGetCertificatesByOwnerAndThumbrint_assertCertRetrieved() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected Collection<Certificate> getCertsToAdd()
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
				protected String getOwnerToRetrieve()
				{
					return "gm2552@securehealthemail.com";
				}
				
				protected String getTPToRetrieve() throws Exception
				{
					return Thumbprint.toThumbprint(TestUtils.loadCert("gm2552.der")).toString();
				}
				
				@Override
				protected void doAssertions(Certificate retrievedCert) throws Exception
				{
					assertNotNull(retrievedCert);
					
					Certificate addedCert = new Certificate();					
					addedCert.setData(TestUtils.loadCert("gm2552.der").getEncoded());

					final X509Certificate retrievedX509Cert = CertUtils.toX509Certificate(retrievedCert.getData());
					final X509Certificate addedX509Cert = CertUtils.toX509Certificate(addedCert.getData());
					
					assertEquals(CertUtils.getOwner(addedX509Cert), retrievedCert.getOwner());
					assertEquals(Thumbprint.toThumbprint(addedX509Cert).toString(), retrievedCert.getThumbprint());
					assertEquals(retrievedX509Cert, addedX509Cert);
					assertEquals(EntityStatus.NEW, retrievedCert.getStatus());
					assertEquals(addedX509Cert.getNotAfter(), retrievedCert.getValidEndDate().getTime());
					assertEquals(addedX509Cert.getNotBefore(), retrievedCert.getValidStartDate().getTime());

				}
			}.perform();
		}				
		
		@Test
		public void testGetCertificatesByOwnerAndThumbrint_wrappedKey_assertCertRetrieved() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected Collection<Certificate> getCertsToAdd()
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
				protected String getOwnerToRetrieve()
				{
					return "gm2552@securehealthemail.com";
				}
				
				protected String getTPToRetrieve() throws Exception
				{
					return Thumbprint.toThumbprint(TestUtils.loadCert("gm2552.der")).toString();
				}
				
				@Override
				protected void doAssertions(Certificate retrievedCert) throws Exception
				{
					assertNotNull(retrievedCert);
					
					Certificate addedCert = new Certificate();					
					addedCert.setData(TestUtils.loadCert("gm2552.der").getEncoded());

					final X509Certificate retrievedX509Cert = CertUtils.toX509Certificate(retrievedCert.getData());
					final X509Certificate addedX509Cert = CertUtils.toX509Certificate(addedCert.getData());
					final CertContainer cont = CertUtils.toCertContainer(retrievedCert.getData());
					assertNotNull(cont.getWrappedKeyData());
					
					assertEquals(CertUtils.getOwner(addedX509Cert), retrievedCert.getOwner());
					assertEquals(Thumbprint.toThumbprint(addedX509Cert).toString(), retrievedCert.getThumbprint());
					assertEquals(retrievedX509Cert, addedX509Cert);
					assertEquals(EntityStatus.NEW, retrievedCert.getStatus());
					assertEquals(addedX509Cert.getNotAfter(), retrievedCert.getValidEndDate().getTime());
					assertEquals(addedX509Cert.getNotBefore(), retrievedCert.getValidStartDate().getTime());

				}
			}.perform();
		}	
		
		@Test
		public void testGetCertificatesByOwnerAndThumbrint_TPNotFound_assertCertNotRetrieved() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected Collection<Certificate> getCertsToAdd()
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
				protected String getOwnerToRetrieve()
				{
					return "gm2552@securehealthemail.com";
				}
				
				protected String getTPToRetrieve() throws Exception
				{
					return "12345";
				}
				
				@Override
				protected void doAssertions(Certificate retrievedCert) throws Exception
				{
					assertNull(retrievedCert);				

				}
			}.perform();
		}		
	
		@Test
		public void testGetCertificatesByOwnerAndThumbprint_errorInLookup_assertServerError() throws Exception
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
						doThrow(new RuntimeException()).when(mockDAO).findByThumbprint((String)any());
						
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
				protected String getOwnerToRetrieve()
				{
					return "gm2554345432@securehealthemail.com";
				}
				
				protected String getTPToRetrieve() throws Exception
				{
					return "12345";
				}

				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}	
}
