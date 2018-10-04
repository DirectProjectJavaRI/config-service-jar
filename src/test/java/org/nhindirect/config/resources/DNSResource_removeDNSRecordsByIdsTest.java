package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.DNSRecord;
import org.nhindirect.config.model.utils.DNSUtils;
import org.nhindirect.config.repository.DNSRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class DNSResource_removeDNSRecordsByIdsTest extends SpringBaseTest
{
	@Autowired
	protected DNSResource dnsService;
		
		abstract class TestPlan extends BaseTestPlan 
		{			
			@Override
			protected void tearDownMocks()
			{

			}

			protected abstract Collection<DNSRecord> getRecordsToAdd() throws Exception;
			
			protected abstract Collection<Long> getIdsToRemove();
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<DNSRecord> recordsToAdd = getRecordsToAdd();

				if (recordsToAdd != null)
				{
					recordsToAdd.forEach(addRec->		
					{
						final HttpEntity<DNSRecord> requestEntity = new HttpEntity<>(addRec);
						final ResponseEntity<Void> resp = testRestTemplate.exchange("/dns", HttpMethod.PUT, requestEntity, Void.class);
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

				final ResponseEntity<?> resp = 
						testRestTemplate.exchange("/dns/{ids}", HttpMethod.DELETE, null, Void.class, builder.toString());
					
				if (resp.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(resp.getStatusCode());
				
				doAssertions();
			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}
		
		@Test
		public void testRemoveDNSRecordsByIds_removeExistingRecords_assertRecordsRemoved() throws Exception
		{
			new TestPlan()
			{
				protected Collection<DNSRecord> records;
				
				@Override
				protected Collection<DNSRecord> getRecordsToAdd() throws Exception
				{
					try
					{
						records = new ArrayList<DNSRecord>();
						
						DNSRecord record = DNSUtils.createARecord("myserver.com", 3600, "10.232.12.43");			
						records.add(record);
						
						
						record = DNSUtils.createARecord("myserver.com", 3600, "10.232.12.44");						
						records.add(record);
						
						record = DNSUtils.createARecord("myserver2.com", 3600, "10.232.12.99");						
						records.add(record);
						
						record = DNSUtils.createX509CERTRecord("gm2552@securehealthemail.com", 3600, TestUtils.loadCert("gm2552.der"));					
						records.add(record);
						
						record = DNSUtils.createMXRecord("myserver.com", "10.232.12.77", 3600, 2);
						records.add(record);
						
						return records;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}
				
				@Override
				protected Collection<Long> getIdsToRemove()
				{
					final Collection<org.nhindirect.config.store.DNSRecord> recs = dnsRepo.findAll();
					
					final Collection<Long> ids = new ArrayList<Long>();
					for (org.nhindirect.config.store.DNSRecord rec : recs)
						ids.add(rec.getId());
					
					return ids;
				}
				
				@Override
				protected void doAssertions() throws Exception
				{
					final Collection<org.nhindirect.config.store.DNSRecord> recs = dnsRepo.findAll();
					assertTrue(recs.isEmpty());
				}
			}.perform();
		}		
		
		@Test
		public void testRemoveDNSRecordsByIds_errorInDelete_assertServiceError() throws Exception
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

						DNSRepository mockDAO = mock(DNSRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).deleteByIdIn((List<Long>)any());
						
						dnsService.setDNSRepository(mockDAO);
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
					
					dnsService.setDNSRepository(dnsRepo);
				}
				
				@Override
				protected Collection<DNSRecord> getRecordsToAdd() throws Exception
				{
					return null;
				}
				
				@Override
				protected Collection<Long> getIdsToRemove()
				{	
					
					return Arrays.asList(1234L);
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
