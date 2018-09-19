package org.nhindirect.config.resources;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Setting;
import org.nhindirect.config.store.dao.SettingDao;
import org.springframework.beans.factory.annotation.Autowired;
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

			protected abstract Collection<Setting> getSettingsToAdd();
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<Setting> settingsToAdd = getSettingsToAdd();
				
				if (settingsToAdd != null)
				{
					settingsToAdd.forEach(addSetting->
					{
						final ResponseEntity<Void> resp = testRestTemplate.exchange("/setting/{name}/{value}", HttpMethod.PUT, null, Void.class,
								addSetting.getName(), addSetting.getValue());
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
					Collection<org.nhindirect.config.store.Setting> retrievedSettings = settingDao.getAll();
					
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
						
						SettingDao mockDAO = mock(SettingDao.class);
						doThrow(new RuntimeException()).when(mockDAO).getByNames(Arrays.asList("setting1"));
						
						settingService.setSettingDao(mockDAO);
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
					
					settingService.setSettingDao(settingDao);
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

						SettingDao mockDAO = mock(SettingDao.class);
						when(mockDAO.getByNames(Arrays.asList("setting1"))).thenReturn(new ArrayList<org.nhindirect.config.store.Setting>());
						doThrow(new RuntimeException()).when(mockDAO).add(eq("setting1"), eq("value1"));
						
						settingService.setSettingDao(mockDAO);
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
					
					settingService.setSettingDao(settingDao);
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
