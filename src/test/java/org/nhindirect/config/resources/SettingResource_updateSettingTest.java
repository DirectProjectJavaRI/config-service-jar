package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.model.Setting;
import org.nhindirect.config.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

public class SettingResource_updateSettingTest extends SpringBaseTest
{
	@Autowired
	protected SettingResource settingService;
		
		abstract class TestPlan extends BaseTestPlan 
		{
			protected boolean useEntityRequestObject()
			{
				return false;
			}
			
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
			
			protected abstract String getSettingNameToUpdate();
			
			protected abstract String getSettingValueToUpdate();
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Setting addSetting = getSettingToAdd();
				
				if (addSetting != null)
				{
					ResponseEntity<Void> resp = null;
					if (!useEntityRequestObject())
					{
						resp = webClient.put()
							.uri(uriBuilder -> uriBuilder.path("/setting/{name}/{value}").build(addSetting.getName(), addSetting.getValue()))
							.retrieve().toBodilessEntity().block();
					}
					else
					{
						resp = webClient.put()
							.uri(uriBuilder -> uriBuilder.path("/setting").build())
							.bodyValue(addSetting)
							.retrieve().toBodilessEntity().block();
					}

					if (resp.getStatusCode().value() != 201)
						throw new WebClientResponseException(resp.getStatusCode().value(), resp.getStatusCode().toString(), null, null, null);

				}

				final ResponseEntity<Void> resp =
						webClient.post()
						.uri(uriBuilder -> uriBuilder.path("/setting/{name}/{value}").build(getSettingNameToUpdate(), getSettingValueToUpdate()))
						.retrieve().toBodilessEntity().block();

				if (resp.getStatusCode().value() != 204)
					throw new WebClientResponseException(resp.getStatusCode().value(), resp.getStatusCode().toString(), null, null, null);


				final ResponseEntity<Setting> getSetting = webClient.get()
					.uri(uriBuilder -> uriBuilder.path("/setting/{name}").build(getSettingNameToUpdate()))
					.retrieve().toEntity(Setting.class).block();

				int statusCode = getSetting.getStatusCode().value();
				if (statusCode == 404)
					doAssertions(null);
				else if (statusCode == 200)
					doAssertions(getSetting.getBody());
				else
					throw new WebClientResponseException(statusCode, getSetting.getStatusCode().toString(), null, null, null);
			}
				
			protected void doAssertions(Setting setting) throws Exception
			{
				
			}
		}
		
		@Test
		public void testUpdateSetting_updateExistingSetting_assertSettingUpdated() throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getSettingNameToUpdate()
				{
					return "setting1";
				}
				
				protected String getSettingValueToUpdate()
				{
					return "value2";
				}
				
				@Override
				protected void doAssertions(Setting setting) throws Exception
				{
					assertEquals("setting1", setting.getName());
					assertEquals("value2", setting.getValue());
				}
			}.perform();
		}	
		
		@Test
		public void testUpdateSetting_updateExistingSetting_entityParam_assertSettingUpdated() throws Exception
		{
			new TestPlan()
			{
				@Override
				protected boolean useEntityRequestObject()
				{
					return true;
				}
				
				@Override
				protected String getSettingNameToUpdate()
				{
					return "setting1";
				}
				
				protected String getSettingValueToUpdate()
				{
					return "value2//value2";
				}
				
				@Override
				protected void doAssertions(Setting setting) throws Exception
				{
					assertEquals("setting1", setting.getName());
					assertEquals("value2//value2", setting.getValue());
				}
			}.perform();
		}			
		
		@Test
		public void testUpdateSetting_settingNotFound_assertNotFound() throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getSettingNameToUpdate()
				{
					return "setting2";
				}
				
				protected String getSettingValueToUpdate()
				{
					return "value2";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception
				{
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(404, ex.getStatusCode().value());
				}
			}.perform();
		}		
		
		@Test
		public void testUpdateSetting_errorInLookup_assertServiceError() throws Exception
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
				protected String getSettingNameToUpdate()
				{
					return "setting1";
				}
				
				protected String getSettingValueToUpdate()
				{
					return "value2";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception
				{
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(500, ex.getStatusCode().value());
				}
			}.perform();
		}
		
		@Test
		public void testUpdateSetting_errorInUpdate_assertServiceError() throws Exception
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
						setting.setName("setting1");
						when(mockDAO.findByNameIgnoreCase(eq("setting1"))).thenReturn(Mono.just(setting));
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
				
				protected Setting getSettingToAdd()
				{
					return null;
				}
				
				@Override
				protected String getSettingNameToUpdate()
				{
					return "setting1";
				}
				
				protected String getSettingValueToUpdate()
				{
					return "value2";
				}
				
				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException)exception;
					assertEquals(500, ex.getStatusCode().value());
				}
			}.perform();
		}		
}
