package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Setting;
import org.nhindirect.config.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class SettingResource_addSettingTest extends SpringBaseTest
{
	@Autowired
	protected SettingResource settingService;
		
		abstract class TestPlan extends BaseTestPlan 
		{			
			@Override
			protected void tearDownMocks()
			{

			}

			protected boolean useEntityRequestObject()
			{
				return false;
			}
			
			protected abstract Collection<Setting> getSettingsToAdd();
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<Setting> settingsToAdd = getSettingsToAdd();
				
				if (settingsToAdd != null)
				{
					settingsToAdd.forEach(addSetting->
					{
						ResponseEntity<Void> resp = null;
						
						if (!useEntityRequestObject())
						{
							resp = testRestTemplate.exchange("/setting/{name}/{value}", HttpMethod.PUT, null, Void.class,
								addSetting.getName(), addSetting.getValue());
						}
						else
						{
							HttpEntity<Setting> requestEntity = new HttpEntity<Setting>(addSetting);
							resp = testRestTemplate.exchange("/setting", HttpMethod.PUT, requestEntity, Void.class);
						}
						
						if (resp.getStatusCodeValue() != 201)
							throw new HttpClientErrorException(resp.getStatusCode());
					});
				}
				
				doAssertions();

			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}
		
		@Test
		public void testAddSetting_assertSettingAdded() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Setting> settings;
				
				@Override
				protected Collection<Setting> getSettingsToAdd()
				{

					settings = new ArrayList<Setting>();
					
					Setting setting = new Setting();					
					setting.setName("setting1");
					setting.setValue("value1");
					settings.add(setting);
					
					setting = new Setting();					
					setting.setName("setting2");
					setting.setValue("value2");
					settings.add(setting);
					
					return settings;

				}

				
				@Override
				protected void doAssertions() throws Exception
				{
					Collection<org.nhindirect.config.store.Setting> retrievedSettings = settingRepo.findAll().collectList().block();
					
					assertNotNull(retrievedSettings);
					assertEquals(this.settings.size(), retrievedSettings.size());
					
					final Iterator<Setting> addedSettingsIter = this.settings.iterator();
					
					for (org.nhindirect.config.store.Setting retrievedSetting : retrievedSettings)
					{
						final Setting addedSetting = addedSettingsIter.next(); 
						
						assertEquals(addedSetting.getName(), retrievedSetting.getName());
						assertEquals(addedSetting.getValue(), retrievedSetting.getValue());
					}
					
				}
			}.perform();
		}		
		
		@Test
		public void testAddSettingWithSlashName_useTemplateParameters_assertSettingAdded() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Setting> settings;
				
				@Override
				protected Collection<Setting> getSettingsToAdd()
				{

					settings = new ArrayList<Setting>();
					
					Setting setting = new Setting();					
					setting.setName("setting1/setting1");
					setting.setValue("value1");
					settings.add(setting);
					
					return settings;

				}

				
				@Override
				protected void doAssertions() throws Exception
				{
					Collection<org.nhindirect.config.store.Setting> retrievedSettings = settingRepo.findAll().collectList().block();
					
					assertNotNull(retrievedSettings);
					assertEquals(this.settings.size(), retrievedSettings.size());
					
					final Iterator<Setting> addedSettingsIter = this.settings.iterator();
					
					for (org.nhindirect.config.store.Setting retrievedSetting : retrievedSettings)
					{
						final Setting addedSetting = addedSettingsIter.next(); 
						
						assertEquals(addedSetting.getName(), retrievedSetting.getName());
						assertEquals(addedSetting.getValue(), retrievedSetting.getValue());
					}
					
				}
			}.perform();
		}		
		
		@Test
		public void testAddSettingWithSlashValue_useEntityParameter_assertSettingAdded() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Setting> settings;
				
				@Override
				protected boolean useEntityRequestObject()
				{
					return true;
				}
				
				@Override
				protected Collection<Setting> getSettingsToAdd()
				{

					settings = new ArrayList<Setting>();
					
					Setting setting = new Setting();					
					setting.setName("setting1");
					setting.setValue("value1//value1");
					settings.add(setting);
					
					return settings;

				}

				
				@Override
				protected void doAssertions() throws Exception
				{
					Collection<org.nhindirect.config.store.Setting> retrievedSettings = settingRepo.findAll().collectList().block();
					
					assertNotNull(retrievedSettings);
					assertEquals(this.settings.size(), retrievedSettings.size());
					
					final Iterator<Setting> addedSettingsIter = this.settings.iterator();
					
					for (org.nhindirect.config.store.Setting retrievedSetting : retrievedSettings)
					{
						final Setting addedSetting = addedSettingsIter.next(); 
						
						assertEquals(addedSetting.getName(), retrievedSetting.getName());
						assertEquals(addedSetting.getValue(), retrievedSetting.getValue());
					}
					
				}
			}.perform();
		}			
		
		@Test
		public void testAddSetting_addDuplicate_assertConflict() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Setting> settings;
				
				@Override
				protected Collection<Setting> getSettingsToAdd()
				{

					settings = new ArrayList<Setting>();
					
					Setting setting = new Setting();					
					setting.setName("setting1");
					setting.setValue("value1");
					settings.add(setting);
					
					setting = new Setting();					
					setting.setName("setting1");
					setting.setValue("value2");
					settings.add(setting);
					
					return settings;

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
		public void testAddSetting_errorInLookup_assertServiceError() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Setting> settings;
				
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
				
				@Override
				protected Collection<Setting> getSettingsToAdd()
				{

					settings = new ArrayList<Setting>();
					
					Setting setting = new Setting();					
					setting.setName("setting1");
					setting.setValue("value1");
					settings.add(setting);
					
					
					return settings;

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
		public void testAddSetting_errorInAdd_assertServiceError() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Setting> settings;

				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();

						SettingRepository mockDAO = mock(SettingRepository.class);

						doThrow(new RuntimeException()).when(mockDAO).save((org.nhindirect.config.store.Setting)any());
						
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
				
				@Override
				protected Collection<Setting> getSettingsToAdd()
				{

					settings = new ArrayList<Setting>();
					
					Setting setting = new Setting();					
					setting.setName("setting1");
					setting.setValue("value1");
					settings.add(setting);
					
					
					return settings;

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
