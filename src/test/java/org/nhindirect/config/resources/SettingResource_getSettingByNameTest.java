package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Setting;
import org.nhindirect.config.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;


public class SettingResource_getSettingByNameTest extends SpringBaseTest
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
			
			protected abstract String getSettingToRetrieve();
			
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

				final Setting getSetting = webClient.get()
						.uri(uriBuilder ->  uriBuilder.path("setting/{set}").build(getSettingToRetrieve()))
						.retrieve().bodyToMono(Setting.class).block();
						
				doAssertions(getSetting);		
				
			}
				
			protected void doAssertions(Setting setting) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testGetSettingByName_assertSettingRetrieved() throws Exception
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

				protected String getSettingToRetrieve()
				{
					return "setting1";
				}
				
				@Override
				protected void doAssertions(Setting setting) throws Exception
				{
					assertNotNull(setting);
					
					final Setting addedSetting = this.settings.iterator().next();
				
					assertEquals(addedSetting.getName(), setting.getName());
					assertEquals(addedSetting.getValue(), setting.getValue());
					
				}
			}.perform();
		}	
		
		@Test
		public void testGetSettingByName_settingNotInStore_assertNoSettingRetrieved() throws Exception
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

				protected String getSettingToRetrieve()
				{
					return "settin51";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(404, ex.getRawStatusCode());
				}
			}.perform();
		}		
		
		@Test
		public void testGetSettingByName_errorInLookup_assertServiceError() throws Exception
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
						SettingRepository mockDAO = mock(SettingRepository.class);
						doThrow(new RuntimeException()).when(mockDAO).findByNameIgnoreCaseIn((List<String>)any());
						
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

					return null;

				}

				protected String getSettingToRetrieve()
				{
					return "settin51";
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
