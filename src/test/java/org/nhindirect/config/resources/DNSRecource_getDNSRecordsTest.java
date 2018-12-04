package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.DNSRecord;
import org.nhindirect.config.model.utils.DNSUtils;
import org.nhindirect.config.repository.DNSRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;
import org.xbill.DNS.Type;


public class DNSRecource_getDNSRecordsTest extends SpringBaseTest
{
	@Autowired
	protected DNSResource dnsService;
	
		abstract class TestPlan extends BaseTestPlan 
		{
			protected Collection<DNSRecord> records;

			@Override
			protected void tearDownMocks()
			{

			}

			protected Collection<DNSRecord> getDNSRecordsToAdd()
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

			protected abstract String getTypeToRetrieve();
			
			protected abstract String getNameToRetrieve();
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<DNSRecord> recsToAdd = getDNSRecordsToAdd();
				
				if (recsToAdd != null)
				{
					recsToAdd.forEach(addRec->		
					{
						final HttpEntity<DNSRecord> requestEntity = new HttpEntity<>(addRec);
						final ResponseEntity<Void> resp = testRestTemplate.exchange("/dns", HttpMethod.PUT, requestEntity, Void.class);
						if (resp.getStatusCodeValue() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});			
				}
				
				final UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/dns");

				if (getTypeToRetrieve() != null)
					builder.queryParam("type", getTypeToRetrieve());
				
				if (getNameToRetrieve() != null)
					builder.queryParam("name", getNameToRetrieve());

				final ResponseEntity<Collection<DNSRecord>> records = 
						testRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, null, new ParameterizedTypeReference<Collection<DNSRecord>>() {});

				if (records.getStatusCodeValue() == 404 || records.getStatusCodeValue() == 204)
					doAssertions(new ArrayList<DNSRecord>());
				else if (records.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(records.getStatusCode());
				else
					doAssertions(records.getBody());					
		
			}
				
			protected void doAssertions(Collection<DNSRecord> records) throws Exception
			{
				
			}
	  }

		@Test
		public void testGetDNSRecords_byTypeOnly_assertRecordsRetrieved() throws Exception
		{
			new TestPlan()
			{
					
				@Override
				protected String getTypeToRetrieve()
				{
					return Integer.toString(Type.A);
				}
				
				@Override
				protected String getNameToRetrieve()
				{
					return "";
				}
				
				@Override
				protected void doAssertions(Collection<DNSRecord> records) throws Exception
				{
					assertNotNull(records);
					assertEquals(3, records.size());
					
					final Iterator<DNSRecord> addedRecordsIter = this.records.iterator();
					
					for (DNSRecord retrievedRecord : records)
					{
						final DNSRecord addedRecord = addedRecordsIter.next(); 
						
						assertEquals(addedRecord.getDclass(), retrievedRecord.getDclass());
						assertEquals(Type.A, retrievedRecord.getType());						
						assertTrue(Arrays.equals(addedRecord.getData(), retrievedRecord.getData()));
						assertEquals(addedRecord.getTtl(), retrievedRecord.getTtl());
						assertEquals(addedRecord.getName(), retrievedRecord.getName());
					}
					
				}
			}.perform();
		}
		
		@Test
		public void testGetDNSRecords_byNameOnly_assertRecordsRetrieved() throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getTypeToRetrieve()
				{
					return null;
				}
				
				@Override
				protected String getNameToRetrieve()
				{
					return "myServer.com";
				}
				
				@Override
				protected void doAssertions(Collection<DNSRecord> records) throws Exception
				{
					assertNotNull(records);
					assertEquals(3, records.size());
					
					for (DNSRecord retrievedRecord : records)
					{						
						assertTrue(retrievedRecord.getName().equalsIgnoreCase("myServer.com."));
					}
					
				}
			}.perform();
		}	
		
		@Test
		public void testGetDNSRecords_byNameOnly_dottedSuffix_assertRecordsRetrieved() throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getTypeToRetrieve()
				{
					return null;
				}
				
				@Override
				protected String getNameToRetrieve()
				{
					return "myServer.com.";
				}
				
				@Override
				protected void doAssertions(Collection<DNSRecord> records) throws Exception
				{
					assertNotNull(records);
					assertEquals(3, records.size());
					
					for (DNSRecord retrievedRecord : records)
					{						
						assertTrue(retrievedRecord.getName().equalsIgnoreCase("myServer.com."));
					}
					
				}
			}.perform();
		}	
		
		@Test
		public void testGetDNSRecords_byNameAndType_assertRecordsRetrieved() throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getTypeToRetrieve()
				{
					return Integer.toString(Type.A);
				}
				
				@Override
				protected String getNameToRetrieve()
				{
					return "myServer.com";
				}
				
				@Override
				protected void doAssertions(Collection<DNSRecord> records) throws Exception
				{
					assertNotNull(records);
					assertEquals(2, records.size());
					
					for (DNSRecord retrievedRecord : records)
					{					
						assertEquals(Type.A, retrievedRecord.getType());	
						assertTrue(retrievedRecord.getName().equalsIgnoreCase("myServer.com."));
					}
					
				}
			}.perform();
		}	
		
		@Test
		public void testGetDNSRecords_byNameAndType_dottedSuffix_assertRecordsRetrieved() throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getTypeToRetrieve()
				{
					return Integer.toString(Type.A);
				}
				
				@Override
				protected String getNameToRetrieve()
				{
					return "myServer.com.";
				}
				
				@Override
				protected void doAssertions(Collection<DNSRecord> records) throws Exception
				{
					assertNotNull(records);
					assertEquals(2, records.size());
					
					for (DNSRecord retrievedRecord : records)
					{					
						assertEquals(Type.A, retrievedRecord.getType());	
						assertTrue(retrievedRecord.getName().equalsIgnoreCase("myServer.com."));
					}
					
				}
			}.perform();
		}	
		
		@Test
		public void testGetDNSRecords_getAllWithAnyType_assertRecordsRetrieved() throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getTypeToRetrieve()
				{
					return Integer.toString(Type.ANY);
				}
				
				@Override
				protected String getNameToRetrieve()
				{
					return "";
				}
				
				@Override
				protected void doAssertions(Collection<DNSRecord> records) throws Exception
				{
					assertNotNull(records);
					assertEquals(this.records.size(), records.size());
					
					final Iterator<DNSRecord> addedRecordsIter = this.records.iterator();
					
					for (DNSRecord retrievedRecord : records)
					{
						final DNSRecord addedRecord = addedRecordsIter.next(); 
						
						assertEquals(addedRecord.getDclass(), retrievedRecord.getDclass());
						assertEquals(addedRecord.getType(), retrievedRecord.getType());						
						assertTrue(Arrays.equals(addedRecord.getData(), retrievedRecord.getData()));
						assertEquals(addedRecord.getTtl(), retrievedRecord.getTtl());
						assertEquals(addedRecord.getName(), retrievedRecord.getName());
					}
					
				}
			}.perform();
		}	
		
		@Test
		public void testGetDNSRecords_typeNotInStore_assertNoRecordsRetrieved() throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getTypeToRetrieve()
				{
					return Integer.toString(Type.A6);
				}
				
				@Override
				protected String getNameToRetrieve()
				{
					return "";
				}
				
				@Override
				protected void doAssertions(Collection<DNSRecord> records) throws Exception
				{
					assertNotNull(records);
					assertTrue(records.isEmpty());
					
				}
			}.perform();
		}	
		
		@Test
		public void testGetDNSRecords_noTypeOrName_assertBadRequest() throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected String getTypeToRetrieve()
				{
					return "-1";
				}
				
				@Override
				protected String getNameToRetrieve()
				{
					return "";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof HttpClientErrorException);
					HttpClientErrorException ex = (HttpClientErrorException)exception;
					assertEquals(400, ex.getRawStatusCode());
				}
			}.perform();
		}	
		
		@Test
		public void testGetDNSRecords_errorInLookup_assertServiceError() throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						DNSRepository mockDAO = mock(DNSRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).findByNameIgnoreCase((String)any());
						
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
				protected String getTypeToRetrieve()
				{
					return "-1";
				}
				
				@Override
				protected String getNameToRetrieve()
				{
					return "myserver.com";
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
