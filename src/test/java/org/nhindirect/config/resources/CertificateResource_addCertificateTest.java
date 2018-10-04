package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.nhindirect.common.cert.Thumbprint;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.model.utils.CertUtils.CertContainer;
import org.nhindirect.config.repository.CertificateRepository;
import org.nhindirect.config.store.Certificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class CertificateResource_addCertificateTest extends SpringBaseTest
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
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<Certificate> certsToAdd = getCertsToAdd();

				certsToAdd.forEach(addCert->
				{
					final HttpEntity<Certificate> requestEntity = new HttpEntity<>(addCert);
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/certificate", HttpMethod.PUT, requestEntity, Void.class);
					if (resp.getStatusCodeValue() != 201)
						throw new HttpClientErrorException(resp.getStatusCode());
				});		

				doAssertions();
			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}	
		
		@Test
		public void testAddCertificates_assertCertsAdded() throws Exception
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
				protected void doAssertions() throws Exception
				{
					List<org.nhindirect.config.store.Certificate> retrievedCerts = certRepo.findAll();
					
					assertNotNull(retrievedCerts);
					assertEquals(2, retrievedCerts.size());
					
					final Iterator<Certificate> addedCertsIter = this.certs.iterator();
					
					for (org.nhindirect.config.store.Certificate retrievedCert : retrievedCerts)
					{	
						final Certificate addedCert = addedCertsIter.next(); 
						
						final X509Certificate retrievedX509Cert = CertUtils.toX509Certificate(retrievedCert.getData());
						final X509Certificate addedX509Cert = CertUtils.toX509Certificate(addedCert.getData());
						
						assertEquals(CertUtils.getOwner(addedX509Cert), retrievedCert.getOwner());
						assertEquals(Thumbprint.toThumbprint(addedX509Cert).toString(), retrievedCert.getThumbprint());
						assertEquals(retrievedX509Cert, addedX509Cert);
						assertEquals(org.nhindirect.config.store.EntityStatus.NEW, retrievedCert.getStatus());
						assertEquals(addedX509Cert.getNotAfter(), retrievedCert.getValidEndDate().getTime());
						assertEquals(addedX509Cert.getNotBefore(), retrievedCert.getValidStartDate().getTime());
					}
										
				}
			}.perform();
		}		
		
		@Test
		public void testAddCertificates_setSubmittedOwners_assertCertsAdded() throws Exception
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
						cert.setOwner("gm2552@securehealthemail.com");
						
						certs.add(cert);
			
						cert = new Certificate();					
						cert.setData(TestUtils.loadCert("umesh.der").getEncoded());
						cert.setOwner("umesh@securehealthemail.com");
						
						certs.add(cert);
						
						return certs;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}

				
				@Override
				protected void doAssertions() throws Exception
				{
					List<org.nhindirect.config.store.Certificate> retrievedCerts = certRepo.findAll();
					
					assertNotNull(retrievedCerts);
					assertEquals(2, retrievedCerts.size());
					
					final Iterator<Certificate> addedCertsIter = this.certs.iterator();
					
					for (org.nhindirect.config.store.Certificate retrievedCert : retrievedCerts)
					{	
						final Certificate addedCert = addedCertsIter.next(); 
						
						final X509Certificate retrievedX509Cert = CertUtils.toX509Certificate(retrievedCert.getData());
						final X509Certificate addedX509Cert = CertUtils.toX509Certificate(addedCert.getData());
						
						assertEquals(CertUtils.getOwner(addedX509Cert), retrievedCert.getOwner());
						assertEquals(Thumbprint.toThumbprint(addedX509Cert).toString(), retrievedCert.getThumbprint());
						assertEquals(retrievedX509Cert, addedX509Cert);
						assertEquals(org.nhindirect.config.store.EntityStatus.NEW, retrievedCert.getStatus());
						assertEquals(addedX509Cert.getNotAfter(), retrievedCert.getValidEndDate().getTime());
						assertEquals(addedX509Cert.getNotBefore(), retrievedCert.getValidStartDate().getTime());
					}
										
				}
			}.perform();
		}		
		
		@Test
		public void testAddCertificates_emptySubmittedOwners_assertCertsAdded() throws Exception
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
						cert.setOwner("");
						
						certs.add(cert);
			
						cert = new Certificate();					
						cert.setData(TestUtils.loadCert("umesh.der").getEncoded());
						cert.setOwner("");
						
						certs.add(cert);
						
						return certs;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}

				
				@Override
				protected void doAssertions() throws Exception
				{
					List<org.nhindirect.config.store.Certificate> retrievedCerts = certRepo.findAll();
					
					assertNotNull(retrievedCerts);
					assertEquals(2, retrievedCerts.size());
					
					final Iterator<Certificate> addedCertsIter = this.certs.iterator();
					
					for (org.nhindirect.config.store.Certificate retrievedCert : retrievedCerts)
					{	
						final Certificate addedCert = addedCertsIter.next(); 
						
						final X509Certificate retrievedX509Cert = CertUtils.toX509Certificate(retrievedCert.getData());
						final X509Certificate addedX509Cert = CertUtils.toX509Certificate(addedCert.getData());
						
						assertEquals(CertUtils.getOwner(addedX509Cert), retrievedCert.getOwner());
						assertEquals(Thumbprint.toThumbprint(addedX509Cert).toString(), retrievedCert.getThumbprint());
						assertEquals(retrievedX509Cert, addedX509Cert);
						assertEquals(org.nhindirect.config.store.EntityStatus.NEW, retrievedCert.getStatus());
						assertEquals(addedX509Cert.getNotAfter(), retrievedCert.getValidEndDate().getTime());
						assertEquals(addedX509Cert.getNotBefore(), retrievedCert.getValidStartDate().getTime());
					}
										
				}
			}.perform();
		}			
		
		@Test
		public void testAddCertificate_wrappedKeyData_assertCertsAdded() throws Exception
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
				protected void doAssertions() throws Exception
				{
					List<org.nhindirect.config.store.Certificate> retrievedCerts = certRepo.findAll();
					
					assertNotNull(retrievedCerts);
					assertEquals(1, retrievedCerts.size());
					
					final Iterator<Certificate> addedCertsIter = this.certs.iterator();
					
					for (org.nhindirect.config.store.Certificate retrievedCert : retrievedCerts)
					{	
						final Certificate addedCert = addedCertsIter.next(); 
						
						final X509Certificate retrievedX509Cert = CertUtils.toX509Certificate(retrievedCert.getData());
						final X509Certificate addedX509Cert = CertUtils.toX509Certificate(addedCert.getData());
						final CertContainer cont = CertUtils.toCertContainer(retrievedCert.getData());
						assertNotNull(cont.getWrappedKeyData());
						
						assertEquals(CertUtils.getOwner(addedX509Cert), retrievedCert.getOwner());
						assertEquals(Thumbprint.toThumbprint(addedX509Cert).toString(), retrievedCert.getThumbprint());
						assertEquals(retrievedX509Cert, addedX509Cert);
						assertEquals(org.nhindirect.config.store.EntityStatus.NEW, retrievedCert.getStatus());
						assertEquals(addedX509Cert.getNotAfter(), retrievedCert.getValidEndDate().getTime());
						assertEquals(addedX509Cert.getNotBefore(), retrievedCert.getValidStartDate().getTime());
						assertTrue(retrievedCert.isPrivateKey());
					}
										
				}
			}.perform();
		}		
		
		@Test
		public void testAddCertificates_submittedTwice_assertConflict() throws Exception
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
						cert.setOwner("gm2552@cerner.com");
						
						certs.add(cert);
			
						cert = new Certificate();	
						cert.setOwner("gm2552@cerner.com");
						cert.setData(TestUtils.loadCert("gm2552.der").getEncoded());
						
						certs.add(cert);
						
						return certs;
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
		public void testAddCertificates_errorInLookup_assertServierError() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						CertificateRepository mockDAO = mock(CertificateRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).findByOwnerIgnoreCaseAndThumbprint((String)any(),(String)any());
						
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
					try
					{
						certs = new ArrayList<Certificate>();
						
						Certificate cert = new Certificate();					
						cert.setData(TestUtils.loadCert("gm2552.der").getEncoded());
						
						certs.add(cert);
						
						return certs;
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
		public void testAddCertificates_errorInAdd_assertServierError() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						CertificateRepository mockDAO = mock(CertificateRepository.class);
						when(mockDAO.findByOwnerIgnoreCaseAndThumbprint((String)any(),(String)any())).thenReturn(null);
						doThrow(new RuntimeException()).when(mockDAO).save((org.nhindirect.config.store.Certificate)any());
						
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
					try
					{
						certs = new ArrayList<Certificate>();
						
						Certificate cert = new Certificate();					
						cert.setData(TestUtils.loadCert("gm2552.der").getEncoded());
						
						certs.add(cert);
						
						return certs;
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
