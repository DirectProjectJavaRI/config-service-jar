package org.nhindirect.config.resources;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class CertificateResource_getAllCertificatesTest extends SpringBaseTest
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
				

				final ResponseEntity<Collection<Certificate>> getCertificates = 
						testRestTemplate.exchange("/certificate", HttpMethod.GET, null, new ParameterizedTypeReference<Collection<Certificate>>() {});

				if (getCertificates.getStatusCodeValue() == 404 || getCertificates.getStatusCodeValue() == 204)
					doAssertions(new ArrayList<>());
				else if (getCertificates.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(getCertificates.getStatusCode());
				else
					doAssertions(getCertificates.getBody());

				
			}
				
			protected void doAssertions(Collection<Certificate> certs) throws Exception
			{
				
			}
		}	
	
		
		@Test
		public void testGetAllCertificates_assertCertsRetrieved() throws Exception
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
				protected void doAssertions(Collection<Certificate> certs) throws Exception
				{
					assertNotNull(certs);
					assertEquals(2, certs.size());
					
					final Iterator<Certificate> addedCertsIter = this.certs.iterator();
					
					for (Certificate retrievedCert : certs)
					{	
						final Certificate addedCert = addedCertsIter.next(); 
						
						final X509Certificate retrievedX509Cert = CertUtils.toX509Certificate(retrievedCert.getData());
						final X509Certificate addedX509Cert = CertUtils.toX509Certificate(addedCert.getData());
						
						assertEquals(CertUtils.getOwner(addedX509Cert), retrievedCert.getOwner());
						assertEquals(Thumbprint.toThumbprint(addedX509Cert).toString(), retrievedCert.getThumbprint());
						assertEquals(retrievedX509Cert, addedX509Cert);
						assertEquals(EntityStatus.NEW, retrievedCert.getStatus());
						assertEquals(addedX509Cert.getNotAfter(), retrievedCert.getValidEndDate().getTime());
						assertEquals(addedX509Cert.getNotBefore(), retrievedCert.getValidStartDate().getTime());
					}
					
				}
			}.perform();
		}			
		
		@Test
		public void testGetAllCertificates_wrappedKeys_assertCertsRetrieved() throws Exception
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
						byte[] keyData = FileUtils.readFileToByteArray(new File("./src/test/resources/certs/gm2552Key.der"));
						
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
				protected void doAssertions(Collection<Certificate> certs) throws Exception
				{
					assertNotNull(certs);
					assertEquals(1, certs.size());
					
					final Iterator<Certificate> addedCertsIter = this.certs.iterator();
					
					for (Certificate retrievedCert : certs)
					{	
						final Certificate addedCert = addedCertsIter.next(); 
						
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
					
				}
			}.perform();
		}	
		
		@Test
		public void testGetAllCertificates_noCertsInStore_assertNoCertsRetrieved() throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected Collection<Certificate> getCertsToAdd()
				{
					return null;
				}

				
				@Override
				protected void doAssertions(Collection<Certificate> certs) throws Exception
				{
					assertNotNull(certs);
					assertEquals(0, certs.size());
				}
			}.perform();
		}	
		
		@Test
		public void testGetAllCertificates_errorInLookup_assertServerError() throws Exception
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
						doThrow(new RuntimeException()).when(mockDAO).findAll();
						
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
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(500, ex.getRawStatusCode());
				}
			}.perform();
		}		
}
