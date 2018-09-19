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
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.nhindirect.config.store.dao.AddressDao;
import org.nhindirect.config.store.dao.DomainDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

/**
 * Resource for managing domain resources in the configuration service.
 * <p>
 * Although not required, this class is instantiated using the Jersey SpringServlet and dependencies are defined in the Sprint context XML file.
 * @author Greg Meyer
 * @since 2.0
 */
@RestController
@RequestMapping("domain")
public class DomainResource extends ProtectedResource
{	
    private static final Log log = LogFactory.getLog(DomainResource.class);

    /**
     * Address DAO is defined in the context XML file an injected by Spring
     */
    protected AddressDao addressDao;
    
    /**
     * Domain DAO is defined in the context XML file an injected by Spring
     */
    protected DomainDao domainDao;
    
    /**
     * Constructor
     */
    public DomainResource()
    {
		
	}
    
    /**
     * Sets the address Dao.  Auto populated by Spring
     * @param dao Address Dao
     */
    @Autowired
    public void setAddressDao(AddressDao addressDao) 
    {
        this.addressDao = addressDao;
    }
    
    /**
     * Sets the domain Dao.  Auto populate by Spring
     * @param domainDao The domain Dao.
     */
    @Autowired
    public void setDomainDao(DomainDao domainDao) 
    {
        this.domainDao = domainDao;
    }
    
    /**
     * Gets a domain by name.
     * @param domain The name of the domain to retrieve.
     * @return A JSON representation of the domain.  Returns a status of 404 if a domain with the given name does
     * not exist.
     */
    @GetMapping(value="{domain}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Domain> getDomain(@PathVariable("domain") String domain)
    {   	
    	try
    	{
    		org.nhindirect.config.store.Domain retDomain = domainDao.getDomainByName(domain);
    		if (retDomain == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(EntityModelConversion.toModelDomain(retDomain));
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }
    
    /**
     * Gets a list of domains that match a query.
     * @param domainName The name of the domain to to get.  Defaults to an empty string which means get all domains.
     * @param entityStatus The entity status that the returned domain must match.  Default to empty string which means to ignore the status filter.
     * @return A JSON representation of a collection of domains that match the search paremeters.  Returns a status of 204 if no
     * domains match the search parameters.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<Domain>> searchDomains(@RequestParam(name="domainName", defaultValue="") String domainName,
    		@RequestParam(name="entityStatus", defaultValue="")String entityStatus)
    {
    	
    	org.nhindirect.config.store.EntityStatus status = null;
    	// get the entity status requested
    	if (!entityStatus.isEmpty())
    	{
    		try
    		{
    			status = org.nhindirect.config.store.EntityStatus.valueOf(entityStatus);
    		}
    		catch (IllegalArgumentException e)
    		{
    			log.warn("EntityStatus enum value of " + entityStatus + " encountered.  Defaulting EntityStatus to null");
    		}
    	}
    	
    	// do the search
    	try
    	{
    		Collection<org.nhindirect.config.store.Domain> domains = domainDao.searchDomain(domainName.isEmpty() ? null : domainName, status);

    		if (domains.isEmpty())
    			return ResponseEntity.status(HttpStatus.NO_CONTENT).cacheControl(noCache).build();
    		
    		final Collection<Domain> retDomains = new ArrayList<Domain>();
    		domains.forEach(domain->
    			retDomains.add(EntityModelConversion.toModelDomain(domain)));
    			
    		return  ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(retDomains);   		
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up domains.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }
    
    /**
     * Adds a domain to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param domain The domain to add to the system.
     * @return Status of 201 if the domain was added or status of 409 if the domain already exists.
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addDomain(@RequestBody Domain domain, HttpServletRequest request) 
    {
    	
    	// check to see if it already exists
    	try
    	{
    		if (domainDao.getDomainByName(domain.getDomainName()) != null)
    			return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up existing domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	final org.nhindirect.config.store.Domain toDomain = EntityModelConversion.toEntityDomain(domain);
    	
    	try
    	{
    		domainDao.add(toDomain);

    		final String requestUrl = request.getRequestURL().toString();
    		final URI uri = new UriTemplate("{requestUrl}/{domain}").expand(requestUrl, "domain/" + domain.getDomainName());
    		
    		return ResponseEntity.created(uri).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error adding domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    }   
    
    /**
     * Updates a domain's attributes.  
     * @param domain The name of the domain to update.  
     * @return Status of 204 if the domain is updated or 404 if a domain with the given name does not exist.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)     
    public ResponseEntity<Void> updateDomain(@RequestBody Domain domain) 
    {
    	// make sure the domain exists
    	org.nhindirect.config.store.Domain existingDomain;
    	try
    	{
    		existingDomain = domainDao.getDomainByName(domain.getDomainName());
	    	if (existingDomain == null)
	    		return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up existing domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	final org.nhindirect.config.store.Domain toDomain = EntityModelConversion.toEntityDomain(domain);
    	toDomain.setId(existingDomain.getId());
    	/**
    	 * Make sure we don't jack up the addresses
    	 */
    	if(domain.getPostmasterAddress() != null)
    	{
	    	for (org.nhindirect.config.store.Address existingAddr : existingDomain.getAddresses())
	    	{
	    		for (org.nhindirect.config.store.Address newAddr : toDomain.getAddresses())
	    		{
	    			if (existingAddr.getEmailAddress().toLowerCase().equals(newAddr.getEmailAddress().toLowerCase()))
	    			{
	    				newAddr.setId(existingAddr.getId());
	    			}
	    		}
	    		
	    		if(domain.getPostmasterAddress() != null)
	    		{
		    		if (existingAddr.getEmailAddress().toLowerCase().equals(domain.getPostmasterAddress().getEmailAddress()))
		    		{
		    			toDomain.setPostmasterAddressId(existingAddr.getId());
		    		}
	    		}
	    	}
    	}
    	
    	try
    	{
    		domainDao.update(toDomain);
    		
    		return ResponseEntity.status(HttpStatus.NO_CONTENT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error updating domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    }     
    
    /**
     * Deletes a domain by name.
     * @param domain The name of the domain to delete.
     * @return Status of 200 if the domain was deleted of status of 404 if a domain with the given name does not exists.
     */
    @DeleteMapping("{domain}")
    public ResponseEntity<Void> removedDomain(@PathVariable("domain") String domain)   
    {
    	// make sure it exists
    	try
    	{
    		if (domainDao.getDomainByName(domain) == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up existing domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	try
    	{
    		domainDao.delete(domain);
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error deleting domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}    	
    }    
}
