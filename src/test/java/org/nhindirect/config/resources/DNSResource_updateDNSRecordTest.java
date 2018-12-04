package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.DNSRecord;

import org.nhindirect.config.model.utils.DNSUtils;
import org.nhindirect.config.repository.DNSRepository;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class DNSResource_updateDNSRecordTest extends SpringBaseTest
{
	@Autowired
	protected DNSResource dnsService;
		
		abstract class TestPlan extends BaseTestPlan 
		{
			protected DNSRecord addedRecord;
			
			@Override
			protected void tearDownMocks()
			{

			}
			
			protected DNSRecord getDNSRecordToAdd()
			{

				addedRecord = DNSUtils.createARecord("myserver.com", 3600, "10.232.12.43");			
				return addedRecord;
			}
			
			protected abstract DNSRecord getRecordToUpdate();
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final DNSRecord addRecord = getDNSRecordToAdd();
				
				if (addRecord != null)
				{
					final HttpEntity<DNSRecord> requestEntity = new HttpEntity<>(addRecord);
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/dns", HttpMethod.PUT, requestEntity, Void.class);
					if (resp.getStatusCodeValue() != 201)
						throw new HttpClientErrorException(resp.getStatusCode());
				}
				
				final DNSRecord recordToUpdate = getRecordToUpdate();

				final HttpEntity<DNSRecord> requestEntity = new HttpEntity<>(recordToUpdate);
				final ResponseEntity<Void> records = 
						testRestTemplate.exchange("/dns", HttpMethod.POST, requestEntity, Void.class);
				
				if (records.getStatusCodeValue() != 204)
					throw new HttpClientErrorException(records.getStatusCode());

				final ResponseEntity<Collection<DNSRecord>> getRecords = 
						testRestTemplate.exchange("/dns?type={type}&name={name}", HttpMethod.GET, null, new ParameterizedTypeReference<Collection<DNSRecord>>() {}, 
						recordToUpdate.getType(), recordToUpdate.getName());

				if (getRecords.getStatusCodeValue() == 404 || getRecords.getStatusCodeValue() == 204)
					doAssertions(new ArrayList<DNSRecord>());
				else if (getRecords.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(records.getStatusCode());
				else
					doAssertions(getRecords.getBody());					
			}
			
			
			protected void doAssertions(Collection<DNSRecord> records) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testUpdateDNSRecord_updateExistingRecord_assertRecordUpdated() throws Exception
		{
			new TestPlan()
			{
				protected DNSRecord updatedRecord;
				
				@Override
				protected DNSRecord getRecordToUpdate()
				{				
					Collection<org.nhindirect.config.store.DNSRecord> records = dnsRepo.findByNameIgnoreCaseAndType(addedRecord.getName(), addedRecord.getType());
					
					// should be one record
					assertEquals(1, records.size());
					
					org.nhindirect.config.store.DNSRecord record = records.iterator().next();
					record.setName("server2.com.");
					
					updatedRecord = EntityModelConversion.toModelDNSRecord(record);
					
					return updatedRecord;
				}
				
				@Override
				protected void doAssertions(Collection<DNSRecord> records) throws Exception
				{
					assertEquals(1, records.size());
					
					DNSRecord record = records.iterator().next();
					
					assertEquals("server2.com.", record.getName());
					assertTrue(Arrays.equals(updatedRecord.getData(), record.getData()));
				}
			}.perform();
		}	
		
		@Test
		public void testUpdateDNSRecord_updateExistingRecord_noDottedSuffix_assertRecordUpdated() throws Exception
		{
			new TestPlan()
			{
				protected DNSRecord updatedRecord;
				
				@Override
				protected DNSRecord getRecordToUpdate()
				{				
					Collection<org.nhindirect.config.store.DNSRecord> records = dnsRepo.findByNameIgnoreCaseAndType(addedRecord.getName(), addedRecord.getType());
					
					// should be one record
					assertEquals(1, records.size());
					
					org.nhindirect.config.store.DNSRecord record = records.iterator().next();
					record.setName("server2.com");
					
					updatedRecord = EntityModelConversion.toModelDNSRecord(record);
					
					return updatedRecord;
				}
				
				@Override
				protected void doAssertions(Collection<DNSRecord> records) throws Exception
				{
					assertEquals(1, records.size());
					
					DNSRecord record = records.iterator().next();
					
					assertEquals("server2.com.", record.getName());
					assertTrue(Arrays.equals(updatedRecord.getData(), record.getData()));
				}
			}.perform();
		}	
		
		@Test
		public void testUpdateDNSRecord_recordDoesntExist_assertNotFound() throws Exception
		{
			new TestPlan()
			{
				protected DNSRecord updatedRecord;
				
				@Override
				protected DNSRecord getRecordToUpdate()
				{				
					updatedRecord = DNSUtils.createARecord("myserver.com", 3600, "10.232.12.43");		
					updatedRecord.setId(1233);
					return updatedRecord;
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
		public void testUpdateDNSRecord_errorInLookup_assertServerError() throws Exception
		{
			new TestPlan()
			{
				protected DNSRecord updatedRecord;
				
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						
						DNSRepository mockDAO = mock(DNSRepository.class);
						
						doThrow(new RuntimeException()).when(mockDAO).findById(eq(1233L));
						
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
				protected DNSRecord getDNSRecordToAdd()
				{
					return null;
				}
				
				@Override
				protected DNSRecord getRecordToUpdate()
				{				
					updatedRecord = DNSUtils.createARecord("myserver.com", 3600, "10.232.12.43");		
					updatedRecord.setId(1233);
					return updatedRecord;
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
		public void testUpdateDNSRecord_errorInUpdate_assertServerError() throws Exception
		{
			new TestPlan()
			{
				protected DNSRecord updatedRecord;

				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						DNSRepository mockDAO = mock(DNSRepository.class);
						final Optional<org.nhindirect.config.store.DNSRecord> op = Optional.of(new org.nhindirect.config.store.DNSRecord());
						when(mockDAO.findById(1233L)).thenReturn(op);
						doThrow(new RuntimeException()).when(mockDAO).save((org.nhindirect.config.store.DNSRecord)any());
						
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
				protected DNSRecord getDNSRecordToAdd()
				{
					return null;
				}
				
				@Override
				protected DNSRecord getRecordToUpdate()
				{				
					updatedRecord = DNSUtils.createARecord("myserver.com", 3600, "10.232.12.43");		
					updatedRecord.setId(1233);
					return updatedRecord;
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
