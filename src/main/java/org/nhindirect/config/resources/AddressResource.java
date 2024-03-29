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

import org.nhindirect.config.model.Address;
import org.nhindirect.config.repository.AddressRepository;
import org.nhindirect.config.repository.DomainRepository;
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
 * Resource for managing address resources in the configuration service.
 * <p>
 * Although not required, this class is instantiated using the Jersey SpringServlet and dependencies are defined in the Sprint context XML file.
 * @author Greg Meyer
 * @since 2.0
 */
@RestController
@RequestMapping("address")
@Slf4j
public class AddressResource extends ProtectedResource
{    
    /**
     * Address repository is injected by Spring
     */
    protected AddressRepository addRepo;
  
    /**
     * Domain DAO is njected by Spring
     */
    protected DomainRepository domainRepo;
    
    /**
     * Constructor
     */
    public AddressResource()
    {
		
	}
    
    /**
     * Sets the address repository.  Auto populated by Spring
     * @param addRepo Address repository
     */
    @Autowired
    public void setAddressRepository(AddressRepository addRepo) 
    {
        this.addRepo = addRepo;
    }
    
    /**
     * Sets the domain repository.  Auto populate by Spring
     * @param domainRepo
     */
    @Autowired
    public void setDomainRepository(DomainRepository domainRepo) 
    {
        this.domainRepo = domainRepo;
    }
    
    /**
     * Gets an address by name.
     * @return A JSON representation of an Address.  Returns 404 if the address doesn't exists.
     * @param address The address to retrieve.
     */ 
    @GetMapping(value="/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Address> getAddress(@PathVariable String address)
    {   
		return addRepo.findByEmailAddressIgnoreCase(address)
			    .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Address()))
				.flatMap(addr ->
				{
					if (addr.getDomainId() == null)
						return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
				
					return domainRepo.findById(addr.getDomainId())
							.map(domain -> EntityModelConversion.toModelAddress(addr, domain.getDomainName()))
				   	     	.onErrorResume(e -> { 
				   	    		log.error("Error looking up address", e);
				   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
				   	    	});	
				});
    }
    
    /**
     * Gets all addresses configured for a given domain.
     * @param domainName The domain name to retrieve addresses for.
     * @return  A JSON representation of a list of addresses.  Returns a 404 status if the domain does not exists
     * or a 204 status if no addresses are configured for the domain.
     */
    @GetMapping(value="domain/{domainName}", produces = MediaType.APPLICATION_JSON_VALUE)     
    public Flux<Address> getAddressesByDomain(@PathVariable String domainName)
    {   	
		return domainRepo.findByDomainNameIgnoreCase(domainName)
			    .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
				.flatMapMany(domain ->
				{
					if (domain.getDomainName() == null)
						return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
					
					return addRepo.findByDomainId(domain.getId())
		    				.map(address -> {
		    					return EntityModelConversion.toModelAddress(address, domainName);
		    				})
		    	   	     	.onErrorResume(e -> { 
		    	   	    		log.error("Error looking up addresses", e);
		    	   	    		return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		    	   	    	});	
				});
    }
    
    /**
     * Adds an address to the system and associates it with a domain.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param address The address to add.
     * @return Returns status 201 if added successfully, 404 if the domain does not exist, or 409 if
     * the address already exists.
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)   
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addAddress(@RequestBody Address address) 
    {
    	if (address.getDomainName() == null || address.getDomainName().isEmpty())
    		return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
    	
		return domainRepo.findByDomainNameIgnoreCase(address.getDomainName())
			    .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
				.flatMap(domain ->
				{
					if (domain.getDomainName() == null)
						return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
					
					return addRepo.findByEmailAddressIgnoreCase(address.getEmailAddress())
					 .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Address()))
					 .flatMap(addr -> 
					 {
						 if (addr.getDomainId() != null)
							 return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT)); 
						 
						 
					     final org.nhindirect.config.store.Address toAdd = EntityModelConversion.toEntityAddress(address, domain);
					     toAdd.setId(null);
						 
					    return  addRepo.save(toAdd)
					    	.then()
		    	   	     	.onErrorResume(e -> { 
		    	   	    		log.error("Error adding address.", e);
		    	   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		    	   	    	});	
					 });			
				});
    }
    
    /**
     * Updates the attributes of an existing address.
     * @param address The address to update along with new attributes.
     * @return Returns 204 if the address is updated successfully, 400 if the domain name is empty, 404 if the
     * domain or address does not exist.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)    
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateAddress(@RequestBody Address address) 
    {
    	if (address.getDomainName() == null || address.getDomainName().isEmpty())
    		return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
    	
    	
		return domainRepo.findByDomainNameIgnoreCase(address.getDomainName())
			    .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
				.flatMap(domain ->
				{
					if (domain.getDomainName() == null)
						return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
					
					return addRepo.findByEmailAddressIgnoreCase(address.getEmailAddress())
					 .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Address()))
					 .flatMap(addr -> 
					 {
						 if (addr.getDomainId() == null)
							 return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)); 
						 
				    	final org.nhindirect.config.store.Address toAdd = EntityModelConversion.toEntityAddress(address, domain);
				    	toAdd.setId(addr.getId());
						 
					    return addRepo.save(toAdd)
					    	.then()
		    	   	     	.onErrorResume(e -> { 
		    	   	    		log.error("Error updating address.", e);
		    	   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		    	   	    	});	
					 });			
				});
    }    
    
    /**
     * Removes an address from the system.
     * @param address The address to removed.
     * @return Returns a status of 200 if the address was removed or 404 if the address does not exists.
     */
    @DeleteMapping(value="{address}")  
    public Mono<Void> removedAddress(@PathVariable("address") String address)   
    {
		return addRepo.findByEmailAddressIgnoreCase(address)
				 .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Address()))
				 .flatMap(addr -> 
				 {
					 if (addr.getDomainId() == null)
						 return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)); 
    	
					 return addRepo.deleteById(addr.getId())
						.then()
	    	   	     	.onErrorResume(e -> { 
	    	   	    		log.error("Error deleting address.", e);
	    	   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	    	   	    	});			 
				 }); 	
    }
}
