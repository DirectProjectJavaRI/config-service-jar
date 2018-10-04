package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Setting;
import org.nhindirect.config.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class SettingResource_removedSettingByNameTest extends SpringBaseTest
{
	@Autowired
	protected SettingResource settingService;
		
		abstract class TestPlan extends BaseTestPlan 
		{
			protected Setting addedSetting;
				
			@Override
			protected void tearDownMocks()
			{

			}

			protected Setting getSettingToAdd()
			{

				addedSetting = new Setting();
				addedSetting.setName("setting1");
				addedSetting.setValue("value1");
				return addedSetting;
			}
			
			protected abstract String getSettingNameToRemove();
			
			@Override
			protected void performInner() throws Exception
			{				
				final Setting addSetting = getSettingToAdd();
				
				if (addSetting != null)
				{
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/setting/{name}/{value}", HttpMethod.PUT, null, Void.class,
							addSetting.getName(), addSetting.getValue());
					if (resp.getStatusCodeValue() != 201)
						throw new HttpClientErrorException(resp.getStatusCode());
				}
				
				final ResponseEntity<?> resp = 
						testRestTemplate.exchange("/setting/{name}", HttpMethod.DELETE, null, Void.class, getSettingNameToRemove());
					
				if (resp.getStatusCodeValue() != 200)
					throw new HttpClientErrorException(resp.getStatusCode());	

				doAssertions();

				
			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}
		
		@Test
		public void testRemoveSetting_removeExistingSetting_assertSettingRemoved() throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getSettingNameToRemove()
				{
					return "setting1";
				}

				
				@Override
				protected void doAssertions() throws Exception
				{
					Collection<org.nhindirect.config.store.Setting> retrievedSettings = settingRepo.findAll();
					
					assertNotNull(retrievedSettings);
					assertEquals(0, retrievedSettings.size());
				}
			}.perform();
		}	
		
		@Test
		public void testRemoveSetting_settingNotFound_assertNotFound() throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getSettingNameToRemove()
				{
					return "setting2";
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
		public void testRemoveSetting_errorInLookup_assertServiceError() throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						SettingRepository mockDAO = mock(SettingRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).findByNameIgnoreCase(eq("setting1"));
						
						settingService.setSettingRepository(mockDAO);
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
					
					settingService.setSettingRepository(settingRepo);
				}	
				
				protected Setting getSettingToAdd()
				{
					return null;
				}
				
				
				@Override
				protected String getSettingNameToRemove()
				{
					return "setting1";
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
		public void testRemoveSetting_errorInDelete_assertServiceError() throws Exception
		{
			new TestPlan()
			{
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						SettingRepository mockDAO = mock(SettingRepository.class);
						org.nhindirect.config.store.Setting setting = new org.nhindirect.config.store.Setting();
						when(mockDAO.findByNameIgnoreCase(eq("setting1"))).thenReturn(setting);
						doThrow(new RuntimeException()).when(mockDAO).deleteByNameIgnoreCase(eq("setting1"));
						
						settingService.setSettingRepository(mockDAO);
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
					
					settingService.setSettingRepository(settingRepo);
				}	
				
				protected Setting getSettingToAdd()
				{
					return null;
				}
				
				
				@Override
				protected String getSettingNameToRemove()
				{
					return "setting1";
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
