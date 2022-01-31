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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nhindirect.common.cert.Thumbprint;
import org.nhindirect.common.crypto.KeyStoreProtectionManager;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.model.utils.CertUtils.CertContainer;
import org.nhindirect.config.repository.CertificateRepository;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.nhindirect.config.store.util.CertificateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * Resource for managing certificate resources in the configuration service.
 * <p>
 * Although not required, this class is instantiated using the Jersey SpringServlet and dependencies are defined in the Sprint context XML file.
 * @author Greg Meyer
 * @since 2.0
 */
@RestController
@RequestMapping("certificate")
@Slf4j
public class CertificateResource extends ProtectedResource
{   
    protected CertificateRepository certRepo;
    
    private KeyStoreProtectionManager kspMgr;
    
    /**
     * Constructor
     */
    public CertificateResource()
    {
		
	}
    
    /**
     * Sets the certificate repository.  Auto populate by Spring
     * @param certRepo The certificate repository
     */
    @Autowired
    public void setCertificateRepository(CertificateRepository certRepo) 
    {
        this.certRepo = certRepo;
    }
    
    @Autowired(required = false)
    public void setKeyStoreProtectionMgr(KeyStoreProtectionManager kspMgr) 
    {
        this.kspMgr = kspMgr;
    }
    
    /**
     * Gets all certificates in the system.
     * @return A JSON representation of a collection of all certificates in the system.  Returns a status of 204 if no certificates
     * exist.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Certificate> getAllCertificates()
    {
		
		return getCertificatesByOwner(null);
    }
    
    /**
     * Gets all certificates for a specific owner.
     * @param owner The owner to retrieive certificates for.
     * @return A JSON representation of a collection of all certificates in the system.  Returns a status of 204 if no certificates
     * exist for the owner.
     */
    @GetMapping(value="/{owner}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Certificate> getCertificatesByOwner(@PathVariable("owner") String owner)
    {  	    	
		final Flux<org.nhindirect.config.store.Certificate> lookupFlux = 
				(StringUtils.isEmpty(owner)) ? certRepo.findAll() : certRepo.findByOwnerIgnoreCase(owner);	
		
		return lookupFlux
			.map(cert -> 
			{
				CertificateUtils.stripP12Protection(cert, this.kspMgr);
				return EntityModelConversion.toModelCertificate(cert);	
			})
   	     	.onErrorResume(e -> { 
   	    		log.error("Error looking up certificates.", e);
   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
   	    	});
    }  
    
    /**
     * Gets a certificate for a specific owner and thumbprint.
     * @param owner The owner or the certificate.
     * @param thumbprint The thubmprint of the certificates.
     * @return Returns a JSON representation of the certificate that matches the owner and thumbprint.  Returns a status of 404 
     * if no matching certificate is found.
     */
    @GetMapping(value="/{owner}/{thumbprint}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<?> getCertificatesByOwnerAndThumbprint(@PathVariable("owner") String owner, 
    		@PathVariable("thumbprint") String thumbprint)
    {
		Mono<List<org.nhindirect.config.store.Certificate>> retCertificates = null;
		
		if (StringUtils.isEmpty(owner) && StringUtils.isEmpty(thumbprint))
			retCertificates = certRepo.findAll().collectList();
        else if (!StringUtils.isEmpty(owner) && StringUtils.isEmpty(thumbprint))
        	retCertificates = certRepo.findByOwnerIgnoreCase(owner).collectList();
        else if (StringUtils.isEmpty(owner) && !StringUtils.isEmpty(thumbprint))
        	retCertificates = certRepo.findByThumbprint(thumbprint).collectList();		
        else
        {

        	retCertificates = certRepo.findByThumbprint(thumbprint)
        	.filter(cert -> cert.getOwner().equalsIgnoreCase(owner))
           	.collectList();
        }
        	
		return retCertificates
		.switchIfEmpty(Mono.just(Collections.emptyList()))
       	.flatMap(certs ->
       	{
       		if (certs.isEmpty())
       			return Mono.empty();
       		
            for (org.nhindirect.config.store.Certificate cert : certs)
            	CertificateUtils.stripP12Protection(cert, this.kspMgr);
            
            return Mono.just(EntityModelConversion.toModelCertificate(certs.iterator().next()));
            
       	})
     	.onErrorResume(e -> { 
    		log.error("Error looking up certificates.", e);
    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
    	});
    }
    
    /**
     * Adds a certificate to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param cert The certificate to add.
     * @return Returns a status of 201 if the certificate was added or a status of 409 if the certificate already exists.
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)  
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addCertificate(@RequestBody Certificate cert)
    {
    	CertContainer cont = null;
		cont = CertUtils.toCertContainer(cert.getData());
		// get the owner if it doesn't alreay exists
		if ((cert.getOwner() == null || cert.getOwner().isEmpty()))
		{
			if (cont != null && cont.getCert() != null)
			{
				
				// now get the owner info from the cert
				final String theOwner = CertUtils.getOwner(cont.getCert());

				if (theOwner != null && !theOwner.isEmpty())
					cert.setOwner(theOwner);
			}
		}
    	
		
		try
		{
		
		return certRepo.findByOwnerIgnoreCaseAndThumbprint(cert.getOwner(), Thumbprint.toThumbprint(cont.getCert()).toString())
				.switchIfEmpty(Mono.just(new org.nhindirect.config.store.Certificate()))
				.flatMap(lookupCert -> 
				{
					if (lookupCert.getData() != null)
					{
			    		log.error("Certificate already exists");
			    		return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT));
					}
					try
					{
						org.nhindirect.config.store.Certificate entCert = EntityModelConversion.toEntityCertificate(cert);
						entCert = CertificateUtils.applyCertRepositoryAttributes(entCert, kspMgr);
						entCert.setId(null);
						entCert.setCreateTime(LocalDateTime.now());
						
						return certRepo.save(entCert)
							.then()
					     	.onErrorResume(e -> { 
					    		log.error("Error adding certificate.", e);
					    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
					    	});
					}
					catch (Exception e)
					{
			    		log.error("Error adding certificate.", e);
			    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
					}					
					
				});
		}
		catch (Exception e)
		{
    		log.error("Error adding certificate.", e);
    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		}

    }
    
    /**
     * Deletes certificates by system id.
     * @param ids Comma delimited list of system ids to delete.
     * @return Status of 200 if the certificates were deleted.
     */
    @DeleteMapping(value="ids/{ids}")   
    public Mono<Void> removeCertificatesByIds(@PathVariable("ids") String ids)
    {
    	final String[] idArray = ids.split(",");
    	final List<Long> idList = new ArrayList<>();
    	
		for (String id : idArray)
			idList.add(Long.parseLong(id));
    	
		return certRepo.deleteByIdIn(idList)
		     	.onErrorResume(e -> { 
		    		log.error("Error removing anchors by ids.", e);
		    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		    	});	
    	
    }
    
    /**
     * Deletes all certificate for a specific owner.
     * @param owner The owner of the certificate.
     * @return Status of 200 if the certificates were deleted.
     */
    @DeleteMapping(value="{owner}")  
    public Mono<Void> removeCertificatesByOwner(@PathVariable("owner") String owner)
    {
		return certRepo.deleteByOwnerIgnoreCase(owner)
		     	.onErrorResume(e -> { 
		    		log.error("Error removing certificates by owner.", e);
		    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		    	});	
    }   

}
