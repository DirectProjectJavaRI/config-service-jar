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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.repository.AddressRepository;
import org.nhindirect.config.repository.CertPolicyGroupDomainReltnRepository;
import org.nhindirect.config.repository.DomainRepository;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
     * Trust bundle resource injected by Spring
     */
    protected TrustBundleResource bundleResource;
    
    /**
     * Cert policy repository injected by Spring
     */
    protected CertPolicyGroupDomainReltnRepository domainReltnRepo;
    
    /**
     * Address repository is injected by Spring
     */
    protected AddressRepository addRepo;
    
    /**
     * Domain repository is injected by Spring
     */
    protected DomainRepository domainRepo;
    
    /**
     * Constructor
     */
    public DomainResource()
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
     * @param domainRepo The domain repository.
     */
    @Autowired
    public void setDomainRepository(DomainRepository domainRepo) 
    {
        this.domainRepo = domainRepo;
    }
    
    /**
     * Sets the trust bundle resource.  Auto populate by Spring
     * @param bundleResource The trust bundle resource.
     */
    @Autowired
    public void setAddressResource(TrustBundleResource bundleResource) 
    {
        this.bundleResource = bundleResource;
    } 
    
    /**
     * Sets the cert policy/domain relation repository.  Auto populate by Spring
     * @param domainReltnRepo The cert policy/domain relation repository.
     */
    @Autowired
    public void setDomainReltnRepository(CertPolicyGroupDomainReltnRepository domainReltnRepo) 
    {
        this.domainReltnRepo = domainReltnRepo;
    }
    
    /**
     * Gets a domain by name.
     * @param domain The name of the domain to retrieve.
     * @return A JSON representation of the domain.  Returns a status of 404 if a domain with the given name does
     * not exist.
     */
    @GetMapping(value="{domain}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<Domain>> getDomain(@PathVariable("domain") String domain)
    {   	
    	try
    	{
    		org.nhindirect.config.store.Domain retDomain = domainRepo.findByDomainNameIgnoreCase(domain).block();
    		if (retDomain == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    		
    		final List<org.nhindirect.config.store.Address> addrs =  addRepo.findByDomainId(retDomain.getId()).collectList().block();
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(Mono.just(EntityModelConversion.toModelDomain(retDomain, addrs)));
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
    public ResponseEntity<Flux<Domain>> searchDomains(@RequestParam(name="domainName", defaultValue="") String domainName,
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
    	
    	domainName = StringUtils.remove(domainName, '*');
    	
    	// do the search
    	try
    	{

    		Flux<org.nhindirect.config.store.Domain> domains = null;
    		if (status == null && domainName.isEmpty())
    			domains = domainRepo.findAll();
    		else if (status == null)
    			domains = domainRepo.findByDomainNameContainingIgnoreCase("%" + domainName.toUpperCase() + "%");
    		else if (domainName.isEmpty())
    			domains = domainRepo.findByStatus(status.ordinal());
    		else
    			domains = domainRepo.findByDomainNameContainingIgnoreCaseAndStatus("%" + domainName.toUpperCase() + "%", status.ordinal());

    		final Flux<Domain> retVal = domains
    		.flatMap(domain -> 
    		{
    			return addRepo.findByDomainId(domain.getId())
    			.map(addr -> new DomainAddrsTuple(domain, addr))
    			.switchIfEmpty(Mono.just(new DomainAddrsTuple(domain, null)))
    			.collectList();
    			
    		})
    		.map(tupls ->
    		{
    			final List<org.nhindirect.config.store.Address> addrs = new ArrayList<>();
    			
    			for (DomainAddrsTuple tupl : tupls)
    				if (tupl.getAddr() != null)
    					addrs.add(tupl.getAddr());
    				
    			return EntityModelConversion.toModelDomain(tupls.get(0).getDomain(), addrs);
    		});
    		
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(retVal);
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
    public ResponseEntity<Mono<Void>> addDomain(@RequestBody Domain domain) 
    {
    	
    	// check to see if it already exists
    	try
    	{
    		if (domainRepo.findByDomainNameIgnoreCase(domain.getDomainName()).block() != null)
    			return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up existing domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	Map.Entry<org.nhindirect.config.store.Domain, Collection<org.nhindirect.config.store.Address>> toEntry = EntityModelConversion.toEntityDomain(domain);
    	
    	
    	try
    	{
    		org.nhindirect.config.store.Domain savedDomain = toEntry.getKey();
    		
    		domainRepo.save(savedDomain).block();
    		
    		Collection<org.nhindirect.config.store.Address> saveAddrs = toEntry.getValue();
    			
    		for(org.nhindirect.config.store.Address addr : saveAddrs)
    		{
    			addr.setId(null);
    			addr.setDomainId(savedDomain.getId());
    		}
    		
    		if (!saveAddrs.isEmpty())
    			addRepo.saveAll(saveAddrs).collectList().block();
    		
    		// find the postmaster if it exists
       		for(org.nhindirect.config.store.Address addr : saveAddrs)
    		{
       			if (addr.getEmailAddress().compareToIgnoreCase(domain.getPostmasterAddress().getEmailAddress()) == 0)
       			{
       				savedDomain.setPostmasterAddressId(addr.getId());
       				domainRepo.save(savedDomain).block();
       				break;
       			}
    		}
    		
    		final URI uri = new UriTemplate("/{domain}").expand("domain/" + domain.getDomainName());
    		
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
    public ResponseEntity<Mono<Void>> updateDomain(@RequestBody Domain domain) 
    {
    	// make sure the domain exists
    	org.nhindirect.config.store.Domain existingDomain;
    	try
    	{
    		existingDomain = domainRepo.findByDomainNameIgnoreCase(domain.getDomainName()).block();
	    	if (existingDomain == null)
	    		return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up existing domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	final org.nhindirect.config.store.Domain toDomain = EntityModelConversion.toEntityDomain(domain).getKey();
    	toDomain.setId(existingDomain.getId());
    	
    	List<org.nhindirect.config.store.Address> addrs = addRepo.findByDomainId(existingDomain.getId()).collectList().block();
    	
    	
    	toDomain.setPostmasterAddressId(0L);
    	if(domain.getPostmasterAddress() != null && !addrs.isEmpty())
    	{
	    	for (org.nhindirect.config.store.Address existingAddr : addrs)
	    	{
	    		if (existingAddr.getEmailAddress().toLowerCase().equals(domain.getPostmasterAddress().getEmailAddress()))
	    		{
	    			toDomain.setPostmasterAddressId(existingAddr.getId());
	    		}
	    	}
    	}
    	
    	try
    	{
    		domainRepo.save(toDomain).block();
    		
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
    public ResponseEntity<Mono<Void>> removedDomain(@PathVariable("domain") String domain)   
    {
    	final org.nhindirect.config.store.Domain foundDomain = domainRepo.findByDomainNameIgnoreCase(domain).block();
    	
    	try
    	{
    		if (foundDomain == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up existing domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	try
    	{
    		// remove any associations to bundles
    		bundleResource.disassociateTrustBundlesFromDomain(domain);
    		
    		// remove any association to policies
    		domainReltnRepo.deleteByDomainId(foundDomain.getId()).block();
    		
    		addRepo.deleteByDomainId(foundDomain.getId()).block();
    		
    		domainRepo.deleteById(foundDomain.getId()).block();
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error deleting domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}    	
    }    
    
    static class DomainAddrsTuple
    {
    	final org.nhindirect.config.store.Domain domain;
    	final org.nhindirect.config.store.Address addr;

		public DomainAddrsTuple(org.nhindirect.config.store.Domain domain, org.nhindirect.config.store.Address addr)
    	{
    		this.domain = domain;
    		this.addr = addr;
    	}
    	
    	public org.nhindirect.config.store.Domain getDomain()
		{
			return domain;
		}

		public org.nhindirect.config.store.Address getAddr()
		{
			return addr;
		}
    }
}
