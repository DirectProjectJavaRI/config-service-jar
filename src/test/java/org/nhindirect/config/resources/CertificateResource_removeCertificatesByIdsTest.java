package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.repository.CertificateRepository;
import org.nhindirect.config.store.Certificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class CertificateResource_removeCertificatesByIdsTest extends SpringBaseTest
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
			
			protected abstract Collection<Long> getIdsToRemove();
			
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

				final Collection<Long> ids = getIdsToRemove();
				StringBuilder builder = new StringBuilder();
				int cnt = 0;
				for (Long id : ids)
				{
					builder.append(id);
					if (cnt < ids.size() - 1)
						builder.append(",");
					
					++cnt;
				}

				final ResponseEntity<Void> resp = 
						testRestTemplate.exchange("/certificate/ids/{ids}",
		                HttpMethod.DELETE, null, Void.class, builder.toString());
				
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
				protected Collection<Long> getIdsToRemove()
				{
					final Collection<org.nhindirect.config.store.Certificate> certs = certRepo.findAll();
					
					final Collection<Long> ids = new ArrayList<Long>();
					for (org.nhindirect.config.store.Certificate cert : certs)
						ids.add(cert.getId());
					
					return ids;
				}
				
				@Override
				protected void doAssertions() throws Exception
				{
					final Collection<org.nhindirect.config.store.Certificate> certs = certRepo.findAll();
					assertTrue(certs.isEmpty());
				}
			}.perform();
		}			
		
		@Test
		public void testRemoveCertificatesByIds_removeSingleCert_assertCertRemoved() throws Exception
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
				protected Collection<Long> getIdsToRemove()
				{
					final Collection<org.nhindirect.config.store.Certificate> certs = certRepo.findAll();
					
					final Collection<Long> ids = new ArrayList<Long>();

					ids.add(certs.iterator().next().getId());
					
					return ids;
				}
				
				@Override
				protected void doAssertions() throws Exception
				{
					final Collection<org.nhindirect.config.store.Certificate> certs = certRepo.findAll();
					assertEquals(1, certs.size());
				}
			}.perform();
		}			
		
		@Test
		public void testRemoveCertificatesByIds_wrappedKeys_removeSingleCert_assertCertRemoved() throws Exception
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
				protected Collection<Long> getIdsToRemove()
				{
					final Collection<org.nhindirect.config.store.Certificate> certs = certRepo.findAll();
					
					final Collection<Long> ids = new ArrayList<Long>();

					ids.add(certs.iterator().next().getId());
					
					return ids;
				}
				
				@Override
				protected void doAssertions() throws Exception
				{
					final Collection<org.nhindirect.config.store.Certificate> certs = certRepo.findAll();
					assertEquals(0, certs.size());
				}
			}.perform();
		}		
		
		@Test
		public void testRemoveCertificatesByIds_errorInDelete_assertServierError() throws Exception
		{
			new TestPlan()
			{
				
				@SuppressWarnings("unchecked")
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						CertificateRepository mockDAO = mock(CertificateRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).deleteByIdIn((List<Long>)any());
						
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
				protected Collection<Long> getIdsToRemove()
				{
					return Arrays.asList(new Long(1234L));
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
