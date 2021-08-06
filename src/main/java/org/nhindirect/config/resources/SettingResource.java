/* 
Copyright (c) 2010, NHIN Direct Project
All rights reserved.

Authors:
   Greg Meyer      gm2552@cerner.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.  Neither the name of the The NHIN Direct Project (nhindirect.org). 
nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.nhindirect.config.resources;

import org.nhindirect.config.model.Setting;
import org.nhindirect.config.repository.SettingRepository;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resource for managing settings resources in the configuration service.
 * <p>
 * Although not required, this class is instantiated using the Jersey SpringServlet and dependencies are defined in the Sprint context XML file.
 * @author Greg Meyer
 * @since 2.0
 */
@RestController
@RequestMapping("setting")
@Slf4j
public class SettingResource extends ProtectedResource
{	
    /**
     * Settings repository is injected by Spring
     */
    protected SettingRepository settingRepo;
    
    /**
     * Constructor
     */
    public SettingResource()
    {
		
	}
    
    /**
     * Sets the settings repository.  Auto populated by Spring
     * @param settingRepo Settings repository
     */
    @Autowired
    public void setSettingRepository(SettingRepository settingRepo) 
    {
        this.settingRepo = settingRepo;
    }
    
    /**
     * Gets all settings in the system.
     * @return A JSON representation of a collection of all settings in the system.  Returns a status of 204 if no settings exist.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Setting> getAllSettings()
    {
		return settingRepo.findAll()
		    .map(setting -> EntityModelConversion.toModelSetting(setting))
   	     	.onErrorResume(e -> { 
   	    		log.error("Error looking up settings.", e);
   	    		return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
   	    	});    	
    }
    
    /**
     * Gets a setting by name.
     * @param name The name of the setting to retrieve.
     * @return A JSON representation of the setting.  Returns a status of 404 if a setting with the given name does not exist.
     */
    @GetMapping(value="{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Setting> getSettingByName(@PathVariable("name") String name)
    {    	
    	return settingRepo.findByNameIgnoreCase(name.toUpperCase())
    		.switchIfEmpty(Mono.just(new org.nhindirect.config.store.Setting()))
    		.flatMap(setting -> 
    		{
    			if (setting.getName() == null)
    				return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    			
    			return Mono.just(EntityModelConversion.toModelSetting(setting))
		   	     	.onErrorResume(e -> { 
		   	    		log.error("Error looking up setting.", e);
		   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		   	    	}); 
    		});
 
    }  
    
    /**
     * Adds a setting to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param name The name of the setting to add.
     * @param value The value of the setting.
     * @return Status of 201 if the setting was created or a status of 409 if a setting with the same name
     * already exists.
     */
    @PutMapping("{name}/{value}")  
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addSetting(@PathVariable("name") String name, @PathVariable("value") String value)
    {    	
    	if (name == null || name.isEmpty())
    	{
    		log.error("Name cannot be null or empty");
    		return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
    	}
    	
       	if (value == null)
    	{
    		log.error("Value cannot be null");
    		return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
    	}
    	
       	return settingRepo.findByNameIgnoreCase(name)
       	   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Setting()))
       	   .flatMap(setting -> 
       	   {
       		   if (setting.getName() != null)
       			 return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT));
       		   
	       	   final org.nhindirect.config.store.Setting addSetting = new org.nhindirect.config.store.Setting();
	       	   addSetting.setName(name);
	       	   addSetting.setValue(value);
	       	   addSetting.setId(null);
	       		
	       	   return settingRepo.save(addSetting)
	       	   .then()
	   	       .onErrorResume(e -> { 
	   	    		log.error("Error looking up setting.", e);
	   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	   	       }); 
	       	   
       	   });
    }
    
    
    /**
     * Adds a setting to the system.  This is an alternative method for adding settings generally needed for
     * values that contain characters that would otherwise be determined unsafe as a part of a URL template.
     * @param Setting The setting argument that holds the name and value
     * @return Status of 201 if the setting was created or a status of 409 if a setting with the same name
     * already exists.
     */
    @PutMapping()  
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addSetting(@RequestBody Setting setting)
    { 
    	return addSetting(setting.getName(), setting.getValue());
    }
    
    /**
     * Updates the value of a setting.
     * @param name The name of the setting to update.
     * @param value The new value of the setting.
     * @return Status of 204 if the value of the setting was updated or a status of 404 if a setting with the given name
     * does not exist.
     */
    @PostMapping("{name}/{value}")  
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateSetting(@PathVariable("name") String name, @PathVariable("value") String value)
    {    	
    	
       	return settingRepo.findByNameIgnoreCase(name)
    	   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Setting()))
    	   .flatMap(setting -> 
    	   {
       		   if (setting.getName() == null)
       			 return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
       		   
       		   setting.setValue(value);
	       	   
       		   return settingRepo.save(setting)
	       	   .then()
	   	       .onErrorResume(e -> { 
	   	    		log.error("Error updating setting.", e);
	   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	   	       }); 
    	   });
    } 
    
    /**
     * Updates the value of a setting.  This is an alternative method for adding settings generally needed for
     * values that contain characters that would otherwise be determined unsafe as a part of a URL template.
     * @param Setting The setting argument that holds the name and value that is to be updated
     * @return Status of 204 if the value of the setting was updated or a status of 404 if a setting with the given name
     * does not exist.
     */
    @PostMapping()  
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateSetting(@RequestBody Setting setting)
    {
    	return updateSetting(setting.getName(), setting.getValue());
    }
    
    /**
     * Deletes a setting in the system by name.
     * @param name The name of the setting to delete.
     * @return Status of 200 if the setting was deleted or a status of 204 if a setting with the given name does not exist.
     */ 
    @DeleteMapping("{name}")
    public Mono<Void> removeSettingByName(@PathVariable("name") String name)
    {
       	return settingRepo.findByNameIgnoreCase(name)
    	   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Setting()))
    	   .flatMap(setting -> 
    	   {
       		   if (setting.getName() == null)
       			 return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
       		   
       		   return settingRepo.deleteByNameIgnoreCase(name)
	   	       .onErrorResume(e -> { 
	   	    	   log.error("Error removing setting by name.", e);
   	    			return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
   	           }); 
    	   });
    }       
}
