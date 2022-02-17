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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.nhindirect.config.model.DNSRecord;
import org.nhindirect.config.repository.DNSRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.xbill.DNS.Type;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * JAX-RS resource for managing DNS resources in the configuration service.
 * <p>
 * Although not required, this class is instantiated using the Jersey SpringServlet and dependencies are defined in the Sprint context XML file.
 * @author Greg Meyer
 * @since 2.0
 */
@RestController
@RequestMapping("dns")
@Slf4j
public class DNSResource extends ProtectedResource
{	
    /**
     * DNS repository is injected by Spring
     */
    protected DNSRepository dnsRepo;
    
    /**
     * Constructor
     */
    public DNSResource()
    {
		
	}
    
    /**
     * Sets the DNS repository.  Auto populated by Spring
     * @param dnsRepo DNS repository
     */
    @Autowired
    public void setDNSRepository(DNSRepository dnsRepo) 
    {
        this.dnsRepo = dnsRepo;
    }
     
    /**
     * Gets DNS records that match a given name, type, or combination of both.
     * @param type DNS record type filter.  Defaults to -1 which means no filter will be applied.
     * @param name DNS record name filter.  Defaults to an empty string which means no filter will be applied.
     * @return A JSON representation of a collection of all DNS records that match the given search criteria.  Returns
     * a status of 204 if no records match the search criteria.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<DNSRecord> getDNSRecords(@RequestParam(name="type", defaultValue = "-1")int type, 
    		@RequestParam(name="name", defaultValue="") String name)
    {
    	Flux<org.nhindirect.config.store.DNSRecord> retRecords;

    	if (type > -1 && !name.isEmpty())
    	{
    		if (type == Type.ANY)
    			retRecords = dnsRepo.findByNameIgnoreCase(name.endsWith(".") ? name : (name + "."));
    		else
    			retRecords = dnsRepo.findByNameIgnoreCaseAndType(name.endsWith(".") ? name : (name + "."), type);
    	}
    	else if (type > -1)
    	{
    		if (type == Type.ANY)
    			retRecords = dnsRepo.findAll();
    		else
    			retRecords = dnsRepo.findByType(type);
    	}
    	else if (!name.isEmpty())
    		retRecords = dnsRepo.findByNameIgnoreCase(name.endsWith(".") ? name : (name + "."));
    	else
    	{
    		log.error("Either a DNS query name or type (or both) must be specified.");
    		return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
    	}
    		
		return retRecords
			.map(record -> {
				return EntityModelConversion.toModelDNSRecord(record);
			})
	     	.onErrorResume(e -> { 
	    		log.error("Error looking up DNS records.", e);
	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	    	});  	
    }
    
    /**
     * Adds a DNS record.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param record The DNS record to add.
     * @return Status fo 201 if the DNS record was added to the system or a status of 409 if the record already exists.
     */   
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addDNSRecord(@RequestBody DNSRecord record)
    {
    	if (record.getType() == Type.ANY)
    	{
    		log.error("Cannot add record with type ANY");
    		return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
    	}
    		
    	if (!record.getName().endsWith("."))
    		record.setName(record.getName() + ".");
    	

		return dnsRepo.findByNameIgnoreCaseAndType(record.getName(), record.getType())
		 .filter(compareRecord -> Arrays.equals(record.getData(), compareRecord.getData()))
		 .collectList()
		 .switchIfEmpty(Mono.just(Collections.emptyList()))
		 .flatMap(records -> 
		 {
			 if (!records.isEmpty())
				 return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT));
			 
    		final org.nhindirect.config.store.DNSRecord addRec = EntityModelConversion.toEntityDNSRecord(record);
    		addRec.setId(null);
    		addRec.setCreateTime(LocalDateTime.now());
    		
    		return dnsRepo.save(addRec)
    		.then()
	     	.onErrorResume(e -> { 
	    		log.error("Error adding DNS record.", e);
	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	    	}); 
    		
		 });
    }
    
    /**
     * Updates the attributes of an existing DNS record.
     * @param updateRecord The DNS record to update.
     * @return Status of 204 if the DNS record was updated or status of 404 if the record could not be found. 
     */    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateDNSRecord(@RequestBody DNSRecord updateRecord)
    {       	
    	return dnsRepo.findById(updateRecord.getId())
    		.switchIfEmpty(Mono.just(new org.nhindirect.config.store.DNSRecord()))
    		.flatMap(record -> 
    		{
    			if (record.getName() == null)
    				return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    			
    	    	if (!updateRecord.getName().endsWith("."))
    	    		updateRecord.setName(updateRecord.getName() + ".");
    	    	
    	    	return dnsRepo.save(EntityModelConversion.toEntityDNSRecord(updateRecord))
    	    		.then()
    		     	.onErrorResume(e -> { 
    		    		log.error("Error updating DNS record.", e);
    		    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
    		    	}); 
    		});
    }
    
    /**
     * Delete DNS records by system id. 
     * @param ids Comma delimited list of ids to delete.
     * @return Status of 200 if the DNS records were deleted.
     */
    @DeleteMapping("{ids}")
    public Mono<Void> removeDNSRecordsByIds(@PathVariable("ids") String ids)
    {
    	final String[] idArray = ids.split(",");
    	
		return Flux.fromStream(Arrays.stream(idArray))
		.flatMap(id ->
		{
			return dnsRepo.deleteById(Long.parseLong(id));
		})
		.collectList()
		.flatMap(deletes -> Mono.empty()); 

    }
}
