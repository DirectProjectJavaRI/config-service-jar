package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class SettingResource_getAllSettingsTest extends SpringBaseTest
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

				final Collection<Setting> getSettings = webClient.get()
						.uri("setting")
						.retrieve().bodyToMono(new ParameterizedTypeReference<Collection<Setting>>() {}).block();
					
				doAssertions(getSettings);									

			}
				
			protected void doAssertions(Collection<Setting> setting) throws Exception
			{
				
			}
		}	
		
		@Test
		public void testGetSettings_assertSettingsRetrieved() throws Exception
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
				protected void doAssertions(Collection<Setting> setting) throws Exception
				{
					assertNotNull(setting);
					assertEquals(2, setting.size());
					
					final Iterator<Setting> addedSettingsIter = this.settings.iterator();
					
					for (Setting retrievedSetting : settings)
					{	
						final Setting addedSetting = addedSettingsIter.next(); 
						
						assertEquals(addedSetting.getName(), retrievedSetting.getName());
						assertEquals(addedSetting.getValue(), retrievedSetting.getValue());
					}
					
				}
			}.perform();
		}	
		
		
		@Test
		public void testGetSettings_noSettingsInStore_assertNoSettingsRetrieved() throws Exception
		{
			new TestPlan()
			{
				
				@Override
				protected Collection<Setting> getSettingsToAdd()
				{
					return null;
				}

				
				@Override
				protected void doAssertions(Collection<Setting> setting) throws Exception
				{
					assertNotNull(setting);
					assertEquals(0, setting.size());
					
					
				}
			}.perform();
		}		
		
		@Test
		public void testGetSettings_errorInLookup_assertServiceError() throws Exception
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
						doThrow(new RuntimeException()).when(mockDAO).findAll();
						
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
