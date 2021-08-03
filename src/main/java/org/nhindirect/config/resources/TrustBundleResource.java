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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.model.TrustBundleDomainReltn;
import org.nhindirect.config.model.exceptions.CertificateConversionException;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.processor.BundleRefreshProcessor;
import org.nhindirect.config.repository.AddressRepository;
import org.nhindirect.config.repository.DomainRepository;
import org.nhindirect.config.repository.TrustBundleAnchorRepository;
import org.nhindirect.config.repository.TrustBundleDomainReltnRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.nhindirect.config.store.Domain;
import org.nhindirect.config.store.TrustBundleAnchor;

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
@RequestMapping("trustbundle")
@Slf4j
public class TrustBundleResource extends ProtectedResource
{	
    /**
     * TrustBundle repository is injected by Spring
     */
    protected TrustBundleRepository bundleRepo;
  
    /**
     * TrustBundleAnchor repository is injected by Spring
     */
    protected TrustBundleAnchorRepository bundleAnchorRepo;
    
    /**
     * TrustBundleDomainReltn repository is injected by Spring
     */
    protected TrustBundleDomainReltnRepository reltnRepo;
    
    /**
     * Domain repository is injected by Spring
     */
    protected DomainRepository domainRepo;
    
    /**
     * Address repository is injected by Spring
     */
    protected AddressRepository addRepo;
    
    /**
     * Bundle refresh processor used to manually refresh a trust bundle;
     */
    protected BundleRefreshProcessor bundleRefreshProcessor;
    
    /**
     * Constructor
     */
    public TrustBundleResource()
    {
  
	}
   
    /**
     * Sets the trustBundle repository.  Auto populate by Spring
     * @param bundleRepo The trustBundle repository.
     */
    @Autowired
    public void setTrustBundleRepository(TrustBundleRepository bundleRepo) 
    {
        this.bundleRepo = bundleRepo;
    }
    
    /**
     * Sets the trustBundleAnchor repository.  Auto populate by Spring
     * @param bundleAnchorRepo The trustBundle anchor repository.
     */
    @Autowired
    public void setTrustBundleRepository(TrustBundleAnchorRepository bundleAnchorRepo) 
    {
        this.bundleAnchorRepo = bundleAnchorRepo;
    }
    
    /**
     * Sets the trustBundleDomainReltn repository.  Auto populate by Spring
     * @param reltnRepo The trustBundleDomainReltn repository.
     */
    @Autowired
    public void setTrustBundleDomainReltnRepository(TrustBundleDomainReltnRepository reltnRepo) 
    {
        this.reltnRepo = reltnRepo;
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
     * Sets the address repository.  Auto populated by Spring
     * @param addRepo Address repository
     */
    @Autowired
    public void setAddressRepository(AddressRepository addRepo) 
    {
        this.addRepo = addRepo;
    }
    
    /**
     * Sets the bundleRefreshProcessor.  Auto populate by Spring
     * @param bundleRefreshProcessor The bundleRefreshProcessor.
     */
    @Autowired
    public void setBunldeRefreshProcessor(BundleRefreshProcessor bundleRefreshProcessor) 
    {
        this.bundleRefreshProcessor = bundleRefreshProcessor;
    }
    
    /**
     * Gets all trust bundles in the system.
     * @param fetchAnchors Indicates if the retrieval should also include the trust anchors in the bundle.  When only needing bundle names,
     * this parameter should be set to false for better performance. 
     * @return A JSON representation of a collection of all trust bundles in the system.  Returns a status of 204 if no trust bundles exist.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<TrustBundle> getTrustBundles(@RequestParam(name="fetchAnchors", defaultValue="true") boolean fetchAnchors)
    {
    	return bundleRepo.findAll()
    		.flatMap(bundle -> 
    		{
    			
    			final Flux<TrustBundleAnchor> anchorFlux = (!fetchAnchors) ? Flux.fromIterable(new ArrayList<TrustBundleAnchor>()) :
    							bundleAnchorRepo.findByTrustBundleId(bundle.getId());

    			return anchorFlux
    			.collectList()
    			.map(anchors ->
    			{
    				return EntityModelConversion.toModelTrustBundle(bundle , anchors);
    			});
    		})
   	     	.onErrorResume(e -> { 
   	    		log.error("Error looking up trust bundles", e);
   	    		return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
   	    	});	
    		

    }
    
    @GetMapping(value="domains/bundles/reltns", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<TrustBundleDomainReltn> getAllTrustBundleDomainRelts(@RequestParam(name="fetchAnchors", defaultValue="true") boolean fetchAnchors)
    {
    		return reltnRepo.findAll()
    	    		.flatMap(bundleReltn -> 
    	    		{
    	    			return bundleRepo.findById(bundleReltn.getTrustBundleId())
    		    		.flatMap(bundle -> 
    		    		{
    		    			
    		    			final Flux<TrustBundleAnchor> anchorFlux = (!fetchAnchors) ? Flux.fromIterable(new ArrayList<TrustBundleAnchor>()) :
    		    							bundleAnchorRepo.findByTrustBundleId(bundle.getId());

    		    			return anchorFlux
    		    			.collectList()
    		    			.map(anchors ->
    		    			{
    		    				return EntityModelConversion.toModelTrustBundle(bundle , anchors);
    		    			});
    		    		})
    	  	    		.flatMap(bundle ->
        	    		{
        	    			return domainRepo.findById(bundleReltn.getDomainId())
        	    			.map(domain ->
        	    			{
            	    			final TrustBundleDomainReltn newReltn = new TrustBundleDomainReltn();
            	    			
            		    		newReltn.setIncoming(bundleReltn.isIncoming());
            		    		newReltn.setOutgoing(bundleReltn.isOutgoing());	
            		    		newReltn.setDomain(EntityModelConversion.toModelDomain(domain, Collections.emptyList()));
            		    		newReltn.setTrustBundle(bundle);
            		    		
            	    			return newReltn;
        	    			});
        	    		});   
    	    		})
	       	     	.onErrorResume(e -> { 
	       	    		log.error("Error looking up trust bundles", e);
	       	    		return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	       	    	});	
    }
    
    /**
     * Gets all trust bundles associated to a domain.
     * @param domainName The name of the domain to fetch trust bundles for.
     * @param fetchAnchors  Indicates if the retrieval should also include the trust anchors in the bundle.  When only needing bundle names,
     * this parameter should be set to false for better performance. 
     * @return  A JSON representation of a collection of trust bundle that are associated to the given domain.  Returns a status of
     * 404 if a domain with the given name does not exist or a status of 404 if no trust bundles are associated with the given name.
     */
    @GetMapping(value="domains/{domainName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<TrustBundleDomainReltn> getTrustBundlesByDomain(@PathVariable("domainName") String domainName, 
    		@RequestParam(name="fetchAnchors", defaultValue="true") boolean fetchAnchors)
    {
    	return domainRepo.findByDomainNameIgnoreCase(domainName)
    	.switchIfEmpty(Mono.just(new Domain()))	
    	.flatMapMany(domain -> 
    	{
    		if (domain.getDomainName() == null)
    			return Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    		
    		return reltnRepo.findByDomainId(domain.getId())
       	    		.flatMap(bundleReltn -> 
    	    		{
    	    			return bundleRepo.findById(bundleReltn.getTrustBundleId())
    		    		.flatMap(bundle -> 
    		    		{
    		    			
    		    			final Flux<TrustBundleAnchor> anchorFlux = (!fetchAnchors) ? Flux.fromIterable(new ArrayList<TrustBundleAnchor>()) :
    		    							bundleAnchorRepo.findByTrustBundleId(bundle.getId());

    		    			return anchorFlux
    		    			.collectList()
    		    			.map(anchors ->
    		    			{
    		    				return EntityModelConversion.toModelTrustBundle(bundle , anchors);
    		    			});
    		    		})
    	  	    		.map(bundle ->
        	    		{
            	    			final TrustBundleDomainReltn newReltn = new TrustBundleDomainReltn();
            	    			
            		    		newReltn.setIncoming(bundleReltn.isIncoming());
            		    		newReltn.setOutgoing(bundleReltn.isOutgoing());	
            		    		newReltn.setDomain(EntityModelConversion.toModelDomain(domain, Collections.emptyList()));
            		    		newReltn.setTrustBundle(bundle);
            		    		
            	    			return newReltn;
        	    		});   
    	    		})
	       	     	.onErrorResume(e -> { 
	       	    		log.error("Error looking up trust bundles", e);
	       	    		return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	       	    	});
    	});
    }
    
    /**
     * Gets a trust bundle by name.
     * @param bundleName The name of the trust bundle to retrieve.
     * @return A JSON representation of a the trust bundle.  Returns a status of 404 if a trust bundle with the given name
     * does not exist.
     */
    @GetMapping(value="{bundleName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TrustBundle> getTrustBundleByName(@PathVariable("bundleName") String bundleName)
    {
		return bundleRepo.findByBundleNameIgnoreCase(bundleName)
	    .switchIfEmpty(Mono.just(new org.nhindirect.config.store.TrustBundle()))
		.flatMap(bundle ->
		{
			if (bundle.getBundleName() == null)
				return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			
			final Flux<TrustBundleAnchor> anchorFlux = 
				bundleAnchorRepo.findByTrustBundleId(bundle.getId());

			return anchorFlux
			.collectList()
			.map(anchors ->
			{
				return EntityModelConversion.toModelTrustBundle(bundle , anchors);
			})
   	     	.onErrorResume(e -> { 
   	    		log.error("Error looking up trust bundles", e);
   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
   	    	});
		});
    }  
    
    /**
     * Adds a trust bundle to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param bundle The bundle to add to the system.
     * @return Status of 201 if the bundle was added or a status of 409 if a bundle with the same name already exists.
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addTrustBundle(@RequestBody TrustBundle bundle)
    {
    	return bundleRepo.findByBundleNameIgnoreCase(bundle.getBundleName())
    	.switchIfEmpty(Mono.just(new org.nhindirect.config.store.TrustBundle()))
    	.flatMap(foundBundle ->
    	{
    		if (foundBundle.getBundleName() != null)
    			return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT));
    		
    		Map.Entry<org.nhindirect.config.store.TrustBundle, Collection<org.nhindirect.config.store.TrustBundleAnchor>> entry = EntityModelConversion.toEntityTrustBundle(bundle);
    		
    		org.nhindirect.config.store.TrustBundle addBundle = entry.getKey();
    		addBundle.setId(null);
    		
    		return bundleRepo.save(addBundle)
    		  .flatMap(trustBundle -> bundleRefreshProcessor.refreshBundle(trustBundle).then())
     	      .onErrorResume(e -> { 
       	    		log.error("Error adding trust bundle", e);
       	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
       	    	});
    		  
    	});
    }   
    
    /**
     * Forces the refresh of a trust bundle.
     * @param bundleName  The name of the trust bundle to refresh.
     * @return Status of 204 if the bundle was refreshed or a status of 404 if a trust bundle with the given name does not exist.
     */
    @PostMapping("{bundle}/refreshBundle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> refreshTrustBundle(@PathVariable("bundle") String bundleName)    
    {
    	return bundleRepo.findByBundleNameIgnoreCase(bundleName)
    	.switchIfEmpty(Mono.just(new org.nhindirect.config.store.TrustBundle()))
    	.flatMap(foundBundle ->
    	{
    		if (foundBundle.getBundleName() == null)
    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    			
    	    return bundleRefreshProcessor.refreshBundle(foundBundle).then();
    		  
    	});
    }
    
    /**
     * Deletes a trust bundle.
     * @param bundleName  The name of the bundle to delete.
     * @return Status of 200 if the trust bundle was deleted or a status of 404 if a trust bundle with the given name
     * does not exist.
     */
    @DeleteMapping("{bundle}")
    public Mono<Void> deleteBundle(@PathVariable("bundle") String bundleName)
    {
    	return bundleRepo.findByBundleNameIgnoreCase(bundleName)
    	.switchIfEmpty(Mono.just(new org.nhindirect.config.store.TrustBundle()))
    	.flatMap(foundBundle ->
    	{
    		if (foundBundle.getBundleName() == null)
    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    		
    		return bundleAnchorRepo.deleteByTrustBundleId(foundBundle.getId())
    		.then(reltnRepo.deleteByTrustBundleId(foundBundle.getId())
    				.then(bundleRepo.deleteById(foundBundle.getId())))
   	      .onErrorResume(e -> { 
 	    		log.error("Error deleting bundle", e);
 	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
 	    	});
    		  
    	});
    }
    
    /**
     * Updates the signing certificate of a trust bundle.
     * @param bundleName The name of the trust bundle to update.
     * @param certData A DER encoded representation of the new signing certificate.
     * @return Status of 204 if the trust bundle's signing certificate was updated, status of 400 if the signing certificate is
     * invalid, or a status 404 if a trust bundle with the given name does not exist.
     */
    @PostMapping(value="{bundle}/signingCert", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateSigningCert(@PathVariable("bundle") String bundleName, @RequestBody(required=false) byte[] certData)
    {   
    	
    	return bundleRepo.findByBundleNameIgnoreCase(bundleName)
    	.switchIfEmpty(Mono.just(new org.nhindirect.config.store.TrustBundle()))
    	.flatMap(foundBundle ->
    	{
    		if (foundBundle.getBundleName() == null)
    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    		try
    		{
    			foundBundle.setSigningCertificateData(null);
    	    	if (certData != null && certData.length > 0)
    	    	{
    		    	try
    		    	{
    		    		X509Certificate signingCert = CertUtils.toX509Certificate(certData);	
    		    		foundBundle.setSigningCertificateData(signingCert.getEncoded());
    		    	}
    		    	catch (CertificateConversionException ex)
    		    	{
    		    		log.error("Signing certificate is not in a valid format " + bundleName, ex);
    		    		return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
    		    	}
    	    	}    			
    			
    		}
    		catch (Exception e)
    		{
    			log.error("Error geting encoded signing certificate ", e);
    			return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
    		}
    		
    		return bundleRepo.save(foundBundle)
    				.then()
    	     	    .onErrorResume(e -> { 
    	       	    		log.error("Error updating trust bundle signing certificate.", e);
    	       	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
    	       	    	});
    		  
    	});
    	
    }
    
    /**
     * Updates multiple bundle attributes.  If the URL of the bundle changes, then the bundle is automatically refreshed.
     * @param bundleName The name of the bundle to update.
     * @param bundleData The data of the trust bundle to update.  Empty or null attributes indicate that the attribute should not be changed.
     * @return Status of 204 if the bundle attributes were updated, status of 400 if the signing certificate is
     * invalid, or a status 404 if a trust bundle with the given name does not exist.
     */
    @PostMapping(value="{bundle}/bundleAttributes", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateBundleAttributes(@PathVariable("bundle") String bundleName, @RequestBody TrustBundle bundleData)
    {  
    	return bundleRepo.findByBundleNameIgnoreCase(bundleName)
    	.switchIfEmpty(Mono.just(new org.nhindirect.config.store.TrustBundle()))
    	.flatMap(foundBundle ->
    	{
    		if (foundBundle.getBundleName() == null)
    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));

        	// check to see if the bundle info is the same... if so, then exit
        	if (foundBundle.getBundleName().equals(bundleData.getBundleName()) &&
        		foundBundle.getBundleURL().equals(bundleData.getBundleURL()) &&
        		foundBundle.getRefreshInterval() == bundleData.getRefreshInterval())
        	{
        		if (bundleData.getSigningCertificateData() == null && foundBundle.getSigningCertificateData() == null)
        			return Mono.empty();
        		
        		else if (bundleData.getSigningCertificateData() != null && foundBundle.getSigningCertificateData() != null
        				&& Arrays.equals(bundleData.getSigningCertificateData(), foundBundle.getSigningCertificateData()))
        				return Mono.empty();
        	}
        
        	final String oldBundleURL = foundBundle.getBundleURL();
        	
        	// if there is a signing certificate in the request, make sure it's valid
        	X509Certificate newSigningCert = null;
        	if (bundleData.getSigningCertificateData() != null)
        	{
            	
            	try
            	{
            		newSigningCert = CertUtils.toX509Certificate(bundleData.getSigningCertificateData());		
            	}
            	catch (CertificateConversionException ex)
            	{
            		log.error("Signing certificate is not in a valid format " + bundleName, ex);
            		return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
            	}
        	}

        	// update the bundle
        	try
        	{
				if (newSigningCert == null)
					foundBundle.setSigningCertificateData(null);
				else
					foundBundle.setSigningCertificateData(newSigningCert.getEncoded());
        	}
        	catch (Exception e)
        	{
 	    		log.error("Setting signing certificate encoded data", e);
 	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        	}
    		
			if (!StringUtils.isEmpty(bundleData.getBundleName()))
				foundBundle.setBundleName(bundleData.getBundleName());
			
			foundBundle.setRefreshInterval(bundleData.getRefreshInterval());
			
			if (!StringUtils.isEmpty(bundleData.getBundleURL()))
				foundBundle.setBundleURL(bundleData.getBundleURL());				
			
			return bundleRepo.save(foundBundle)
  		    .doOnSuccess(trustBundle -> 
  		    {
    			// if the URL changed, the bundle needs to be refreshed
    			if (bundleData.getBundleURL() != null && !bundleData.getBundleURL().isEmpty() && !oldBundleURL.equals(bundleData.getBundleURL()))
    			{
    				bundleRefreshProcessor.refreshBundle(foundBundle);
    			}      			  
      		})
      		.then()
       	    .onErrorResume(e -> { 
         	    		log.error("Error updating bundle attributes", e);
         	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
         	    	});    		  
    	});    	
    }
    
    /**
     * Associates a trust bundle to a domain along with directional trust.
     * @param bundleName The name of the bundle to associate to a domain.
     * @param domainName The name of the domain to associate to a bundle.
     * @param incoming Indicates if trust should be allowed for incoming messages.
     * @param outgoing Indicates if trust should be allowed for outgoing messages.
     * @return Status of 204 if the association was made or a status of 404 if either a domain or trust bundle with its given name
     * does not exist.
     */
    @PostMapping("{bundle}/{domain}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> associateTrustBundleToDomain(@PathVariable("bundle") String bundleName, @PathVariable("domain") String domainName,
    		@RequestParam(name="incoming", defaultValue="true") boolean incoming, @RequestParam(name="outgoing", defaultValue="true") boolean outgoing)
    {
    	return  bundleRepo.findByBundleNameIgnoreCase(bundleName)
    	.switchIfEmpty(Mono.just(new org.nhindirect.config.store.TrustBundle()))
    	.flatMap(foundBundle ->
    	{
    		if (foundBundle.getBundleName() == null)
    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    		

    		return domainRepo.findByDomainNameIgnoreCase(domainName)
    			.switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
    			.flatMap(foundDomain ->
    			{
    	    		if (foundDomain.getDomainName() == null)
    	    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));    				
    				
    	    		final org.nhindirect.config.store.TrustBundleDomainReltn reltn = 
    	    				new org.nhindirect.config.store.TrustBundleDomainReltn();
    	    		
    	    		reltn.setDomainId(foundDomain.getId());
    	    		reltn.setTrustBundleId(foundBundle.getId());
    	    		reltn.setIncoming(incoming);
    	    		reltn.setOutgoing(outgoing);

    	    		return reltnRepo.save(reltn)
    	                .then() 
    	          	    .onErrorResume(e -> { 
             	    		log.error("Error associating trust bundle to domain.", e);
             	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
             	    	});    

    			});    		
    	});
    }
    
    /**
     * Removes the association of a trust bundle from a domain.
     * @param bundleName The name of the trust bundle to remove from the domain.
     * @param domainName The name of the domain to remove from the trust bundle.
     * @return Status of 200 if the association was removed or a status of 404 if either a domain or trust bundle with its given name
     * does not exist.
     */
    @DeleteMapping("{bundle}/{domain}")
    public Mono<Void> disassociateTrustBundleFromDomain(@PathVariable("bundle") String bundleName, @PathVariable("domain") String domainName)
    {    
    	
    	return  bundleRepo.findByBundleNameIgnoreCase(bundleName)
    	.switchIfEmpty(Mono.just(new org.nhindirect.config.store.TrustBundle()))
    	.flatMap(foundBundle ->
    	{
    		if (foundBundle.getBundleName() == null)
    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    		

    		return domainRepo.findByDomainNameIgnoreCase(domainName)
    			.switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
    			.flatMap(foundDomain ->
    			{
    	    		if (foundDomain.getDomainName() == null)
    	    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));    				
    				

    	    		return reltnRepo.deleteByDomainIdAndTrustBundleId(foundDomain.getId(), foundBundle.getId())
    	                .then() 
    	          	    .onErrorResume(e -> { 
             	    		log.error("Error disassociating trust bundle from domain.", e);
             	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
             	    	});    
    			});    		
    	});    	 	
    }
    
    /**
     * Removes all trust bundle from a domain.
     * @param domainName The name of the domain to remove trust bundle from.
     * @return Status of 200 if trust bundles were removed from the domain or a status of 404 if a domain with the given name
     * does not exist.
     */
    @DeleteMapping("{domain}/deleteFromDomain")
    public Mono<Void> disassociateTrustBundlesFromDomain(@PathVariable("domain") String domainName)
    {   
		return domainRepo.findByDomainNameIgnoreCase(domainName)
    			.switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
    			.flatMap(foundDomain ->
    			{
    	    		if (foundDomain.getDomainName() == null)
    	    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));    
    	    		
    	    		
    	    		return reltnRepo.deleteByDomainId(foundDomain.getId())
    	                .then()
    	          	    .onErrorResume(e -> { 
    	     	    		log.error("Error disassociating trust bundle from domain.", e);
    	     	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
    	     	    	}); 
    			});
    }
    
    /**
     * Removes a trust bundle from all domains.
     * @param bundleName The name of the trust bundle to remove from all domains.
     * @return Status of 200 if the trust bundle was removed from all domains or a status of 404 if a trust bundle with the given
     * name does not exist.
     */
    @DeleteMapping("{bundle}/deleteFromBundle")
    public Mono<Void> disassociateTrustBundleFromDomains(@PathVariable("bundle") String bundleName)
    {   
    	return  bundleRepo.findByBundleNameIgnoreCase(bundleName)
    	.switchIfEmpty(Mono.just(new org.nhindirect.config.store.TrustBundle()))
    	.flatMap(foundBundle ->
    	{
    		if (foundBundle.getBundleName() == null)
    			return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    	
    		return reltnRepo.deleteByTrustBundleId(foundBundle.getId())
	                .then()
	          	    .onErrorResume(e -> { 
	     	    		log.error("Error disassociating trust bundle from domains.", e);
	     	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	     	    	}); 
    	});
    }  
    
}
