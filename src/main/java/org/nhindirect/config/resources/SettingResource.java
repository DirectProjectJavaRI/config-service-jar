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

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.config.model.Setting;
import org.nhindirect.config.repository.SettingRepository;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

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
public class SettingResource extends ProtectedResource
{	
    private static final Log log = LogFactory.getLog(SettingResource.class);
   
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
    public ResponseEntity<Flux<Setting>> getAllSettings()
    {
    	try
    	{
    		final Flux<Setting> retVal = Flux.fromStream(settingRepo.findAll().stream().
    		    	map(setting -> {	    		
    		    		return EntityModelConversion.toModelSetting(setting);
    		    	}));   
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(retVal);
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up settings.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }
    
    /**
     * Gets a setting by name.
     * @param name The name of the setting to retrieve.
     * @return A JSON representation of the setting.  Returns a status of 404 if a setting with the given name does not exist.
     */
    @GetMapping(value="{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<Setting>> getSettingByName(@PathVariable("name") String name)
    {    	
    	try
    	{
    		final Collection<org.nhindirect.config.store.Setting> retSettings = settingRepo.findByNameIgnoreCaseIn(Arrays.asList(name.toUpperCase()));
    		if (retSettings.isEmpty())
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    		
    		final Setting modelSetting = EntityModelConversion.toModelSetting(retSettings.iterator().next());
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(Mono.just(modelSetting)); 	
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up setting.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
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
    public ResponseEntity<Mono<Void>> addSetting(@PathVariable("name") String name, @PathVariable("value") String value)
    {    	
    	if (name == null || name.isEmpty())
    	{
    		log.error("Name cannot be null or empty");
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).cacheControl(noCache).build();	
    	}
    	
       	if (value == null)
    	{
    		log.error("Value cannot be null");
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).cacheControl(noCache).build();	
    	}
    	
    	// check to see if it already exists
    	try
    	{
    		if (settingRepo.findByNameIgnoreCase(name) != null)
    			return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up setting.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	try
    	{
    		
    		
    		final org.nhindirect.config.store.Setting addSetting = new org.nhindirect.config.store.Setting();
    		addSetting.setName(name);
    		addSetting.setValue(value);
    		settingRepo.save(addSetting);
    		

    		final URI uri = new UriTemplate("/{name}").expand("setting/" + name);
    		
    		return ResponseEntity.created(uri).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error adding setting.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }
    
    /**
     * Updates the value of a setting.
     * @param name The name of the setting to update.
     * @param value The new value of the setting.
     * @return Status of 204 if the value of the setting was updated or a status of 404 if a setting with the given name
     * does not exist.
     */
    @PostMapping("{name}/{value}")  
    public ResponseEntity<Mono<Void>> updateSetting(@PathVariable("name") String name, @PathVariable("value") String value)
    {    	
    	
    	org.nhindirect.config.store.Setting retSetting = null;
    	// make sure it exists
    	try
    	{
    		retSetting = settingRepo.findByNameIgnoreCase(name);
    		if (settingRepo.findByNameIgnoreCase(name) == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up setting.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	try
    	{
    		retSetting.setValue(value);
    		settingRepo.save(retSetting);
    		
    		return ResponseEntity.status(HttpStatus.NO_CONTENT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error updating setting.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    } 
    
    
    /**
     * Deletes a setting in the system by name.
     * @param name The name of the setting to delete.
     * @return Status of 200 if the setting was deleted or a status of 204 if a setting with the given name does not exist.
     */ 
    @DeleteMapping("{name}")
    public ResponseEntity<Mono<Void>> removeSettingByName(@PathVariable("name") String name)
    {
    	// check to see if it already exists
    	try
    	{
    		if (settingRepo.findByNameIgnoreCase(name) == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up setting.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	try
    	{
    		settingRepo.deleteByNameIgnoreCase(name);
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error removing setting by name.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }       
}
