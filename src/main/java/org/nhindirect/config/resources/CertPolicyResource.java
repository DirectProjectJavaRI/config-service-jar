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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.model.CertPolicyGroupDomainReltn;
import org.nhindirect.config.model.CertPolicyGroupUse;
import org.nhindirect.config.repository.CertPolicyGroupDomainReltnRepository;
import org.nhindirect.config.repository.CertPolicyGroupReltnRepository;
import org.nhindirect.config.repository.CertPolicyGroupRepository;
import org.nhindirect.config.repository.CertPolicyRepository;
import org.nhindirect.config.repository.DomainRepository;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.nhindirect.config.store.CertPolicyGroupReltn;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

import com.google.common.collect.Maps;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resource for managing certificate policy resources in the configuration service.
 * <p>
 * Although not required, this class is instantiated using the Jersey SpringServlet and dependencies are defined in the Sprint context XML file.
 * @author Greg Meyer
 * @since 2.0
 */
@RestController
@RequestMapping("certpolicy")
public class CertPolicyResource extends ProtectedResource
{
    private static final Log log = LogFactory.getLog(CertPolicyResource.class);
	
    protected CertPolicyRepository policyRepo;
  
    protected CertPolicyGroupRepository groupRepo;
    
    protected CertPolicyGroupDomainReltnRepository domainReltnRepo;
    
    protected CertPolicyGroupReltnRepository polGroupReltnRepo;
    
    protected DomainRepository domainRepo;
    
    protected CertPolicyResource transactionalThisProxy;
    
    /**
     * Constructor
     */
    public CertPolicyResource()
    {
		
	}
    
    /**
     * Sets the policy repository.  Auto populated by Spring
     * @param policyRepo CertPolicyDao repository
     */
    @Autowired
    public void setCertPolicyRepository(CertPolicyRepository policyRepo) 
    {
        this.policyRepo = policyRepo;
    }
    
    
    /**
     * Sets the domain repository.  Auto populated by Spring
     * @param domainRepo DomainDao repository
     */
    @Autowired
    public void setDomainRepository(DomainRepository domainRepo) 
    {
        this.domainRepo = domainRepo;
    }
    
    /**
     * Sets the policy group repository.  Auto populated by Spring
     * @param groupRepo CertPolicyGroup repository
     */
    @Autowired
    public void setCertPolicyGroupRepository(CertPolicyGroupRepository groupRepo) 
    {
        this.groupRepo = groupRepo;
    }
    
    /**
     * Sets the policy group to domain reltn repository.  Auto populated by Spring
     * @param reltnRepo CertPolicyGroupDomainReltn repository
     */
    @Autowired
    public void setCertPolicyGroupDomainReltnRepository(CertPolicyGroupDomainReltnRepository domainReltnRepo) 
    {
        this.domainReltnRepo = domainReltnRepo;
    }
    
    /**
     * Sets the policy group tp policy use reltn repository.  Auto populated by Spring
     * @param reltnRepo CertPolicyGroupDomainReltn repository
     */
    @Autowired
    public void setCertPolicyGroupReltnRepository(CertPolicyGroupReltnRepository polGroupReltnRepo) 
    {
        this.polGroupReltnRepo = polGroupReltnRepo;
    }
    
    @Autowired 
    public void setInternalThisProxy(CertPolicyResource internalProxy)
    {
    	transactionalThisProxy = internalProxy;
    }
    
    /**
     * Gets all certificate policies in the system.
     * @return A JSON representation of a collection of all certificate policies in the system.  Returns a status of 204 if
     * no certificate policies exists.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Flux<CertPolicy>> getPolicies()
    {
    	try
    	{
    		final Flux<CertPolicy> retVal = policyRepo.findAll()
    				.map(pol -> {
    					return EntityModelConversion.toModelCertPolicy(pol);				
    				});
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(retVal);
    		
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up cert policies.", e);
       		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	} 	
    }
    
    /**
     * Gets a certificate policy by name.  
     * @param policyName The name of the certificate policy to retrieve.
     * @return A JSON representation of the certificate policy.  Returns a status of 404 if a certificate policy with the given name does not exist.
     */
    @GetMapping(value="/{policyName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<CertPolicy>> getPolicyByName(@PathVariable("policyName") String policyName)
    {
    	try
    	{
    		final org.nhindirect.config.store.CertPolicy retPolicy = policyRepo.findByPolicyNameIgnoreCase(policyName).block();
    		
    		if (retPolicy == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();

    		final CertPolicy modelPolicy = EntityModelConversion.toModelCertPolicy(retPolicy);
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(Mono.just(modelPolicy));   
    		
    	}
    	catch (Throwable e)
    	{
    		log.error("Error looking up cert policy", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}    	
    }  
    
    /**
     * Adds a certificate policy to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param policy The certificate policy to add.
     * @return A status of 201 if the policy was added or a status of 409 if the policy already exists.
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)  
    public ResponseEntity<Mono<Void>> addPolicy(@RequestBody CertPolicy policy)
    {
    	// make sure it doesn't exist
    	try
    	{
    		if (policyRepo.findByPolicyNameIgnoreCase(policy.getPolicyName()).block() != null)
    			return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up cert policy.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	try
    	{    		
    		final org.nhindirect.config.store.CertPolicy entityPolicy = EntityModelConversion.toEntityCertPolicy(policy);
    		
    		policyRepo.save(entityPolicy).block();
 
    		final URI uri = new UriTemplate("/{certpolicy}").expand("certpolicy/" + policy.getPolicyName());
    		
    		return ResponseEntity.created(uri).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error adding trust cert policy.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }  
    
    /**
     * Deletes a certificate policy by name.
     * @param policyName The name of the certificate policy.
     * @return Status of 200 if the policy was delete or 404 if a certificate policy with the given name does not exist.
     */
    @DeleteMapping(value="{policyName}") 
    public ResponseEntity<Mono<Void>> removePolicyByName(@PathVariable("policyName") String policyName)
    {
    	// make sure it exists
    	org.nhindirect.config.store.CertPolicy enitityPolicy = null;
    	try
    	{
    		enitityPolicy = policyRepo.findByPolicyNameIgnoreCase(policyName).block();
    		if (enitityPolicy == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up existing cert policy.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	/*
    	 * Need to remove this policy from any possible group that it belongs to.
    	 * 
    	 */
    	polGroupReltnRepo.deleteByPolicyId(enitityPolicy.getId()).block();
    	
    	try
    	{
    		policyRepo.deleteById(enitityPolicy.getId()).block();
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error deleting cert policy.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}  
    }    
    
    /**
     * Updates the information of a certificate policy.
     * @param policyName The name of the certificate policy to update.
     * @param policyData Data that should be update.  Any null or empty attributes will result in that attribute not being updated.
     * @return Status of 204 if the certificate policy was updated or 404 if a certificate policy with the given name does not exist.
     */
    @PostMapping(value="{policyName}/policyAttributes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<Void>> updatePolicyAttributes(@PathVariable("policyName") String policyName, @RequestBody CertPolicy policyData)
    { 
       	// make sure the policy exists
    	org.nhindirect.config.store.CertPolicy entityPolicy;
    	try
    	{
    		entityPolicy = policyRepo.findByPolicyNameIgnoreCase(policyName).block();
    		if (entityPolicy == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up cert policy.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}

    	// update the policy
    	try
    	{
			if (policyData.getPolicyData() != null && policyData.getPolicyData().length > 0)
				entityPolicy.setPolicyData(policyData.getPolicyData());
			
			if (!StringUtils.isEmpty(policyData.getPolicyName()))
				entityPolicy.setPolicyName(policyData.getPolicyName());
			
			if (policyData.getLexicon() != null)
				entityPolicy.setLexicon(policyData.getLexicon().ordinal());
    		
			policyRepo.save(entityPolicy).block();

    		return ResponseEntity.status(HttpStatus.NO_CONTENT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error updating cert policy attributes.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }
    
    /**
     * Gets all policy groups in the system.
     * @return A JSON representation of a collection of all policy groups in the system.  Returns a status of 204 if no policy
     * groups exist.
     */
    @GetMapping(value="groups", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Flux<CertPolicyGroup>> getPolicyGroups()
    {    	
    	try
    	{
    		final Flux<CertPolicyGroup> retVal = groupRepo.findAll()
    		.flatMap(group -> 
    		{
    			
    		    
    			return polGroupReltnRepo.findByGroupId(group.getId())
    				    .flatMap(reltn ->  
    				    {
    				       return policyRepo.findById(reltn.getCertPolicyId())
    				       .map(pol -> Maps.immutableEntry(reltn, pol));
    				    
    				    })
    				    .collect(Collectors.toMap(
    							Map.Entry::getKey,
    							Map.Entry::getValue))
    				    .map(polUseMap -> 
    				    {
    				    	return EntityModelConversion.toModelCertPolicyGroup(group, polUseMap);
    				    });
    		});
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(retVal);
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up cert policy groups.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}   	
    }  
    
    /**
     * Gets a policy group name.
     * @param groupName The name of the policy group to retrieve.
     * @return A JSON representation of the policy group.  Returns a status of 404 if a policy group with the given name does
     * not exist.
     */
    @GetMapping(value="groups/{groupName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<CertPolicyGroup>> getPolicyGroupByName(@PathVariable("groupName") String groupName)
    {
    	try
    	{
    		final org.nhindirect.config.store.CertPolicyGroup retGroup = groupRepo.findByPolicyGroupNameIgnoreCase(groupName).block();
    		
    		if (retGroup == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();

		    Map<CertPolicyGroupReltn, org.nhindirect.config.store.CertPolicy> polUseMap =  
		    polGroupReltnRepo.findByGroupId(retGroup.getId())
		    .flatMap(reltn ->  
		    {
		       return policyRepo.findById(reltn.getCertPolicyId())
		       .map(pol -> Maps.immutableEntry(reltn, pol));
		    
		    })
		    .collect(Collectors.toMap(
					Map.Entry::getKey,
					Map.Entry::getValue)).block();
		   

    		
		    final CertPolicyGroup modelGroup = EntityModelConversion.toModelCertPolicyGroup(retGroup, polUseMap);
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(Mono.just(modelGroup));    
    		
    	}
    	catch (Throwable e)
    	{
    		log.error("Error looking up cert policy group", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}     	
    }
    
    /**
     * Adds a policy group to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param group The policy group to add.
     * @return Status of 201 if the policy group was added or a status of 409 if the policy group
     * already exists.
     */
    @PutMapping(value="groups", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<Void>> addPolicyGroup(@RequestBody CertPolicyGroup group)
    {
    	// make sure it doesn't exist
    	try
    	{
    		if (groupRepo.findByPolicyGroupNameIgnoreCase(group.getPolicyGroupName()).block() != null)
    			return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(noCache).build();	
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up cert policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	try
    	{    		
    		final org.nhindirect.config.store.CertPolicyGroup entityGroup = EntityModelConversion.toEntityCertPolicyGroup(group);
    		
    		groupRepo.save(entityGroup).block();
    		
    		final URI uri = new UriTemplate("/{certpolicy}").expand("certpolicy/group+/" + group.getPolicyGroupName());
    		
    		return ResponseEntity.created(uri).cacheControl(noCache).build();
    		
    	}
    	catch (Exception e)
    	{
    		log.error("Error adding trust cert policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }  
    
    /**
     * Deletes a policy group from the system.
     * @param groupName The name of the policy group to delete.
     * @return A status of 200 if the policy group was deleted or a status of 404 if a policy group with the given
     * name does not exist.
     */
    @DeleteMapping(value="groups/{groupName}")  
    public ResponseEntity<Mono<Void>> removePolicyGroupByName(@PathVariable("groupName") String groupName)
    {
    	// make sure it exists
    	org.nhindirect.config.store.CertPolicyGroup enitityGroup = null;
    	try
    	{
    		enitityGroup = groupRepo.findByPolicyGroupNameIgnoreCase(groupName).block();
    		if (enitityGroup == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up existing cert policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	try
    	{
    		domainReltnRepo.deleteByCertPolicyGroupId(enitityGroup.getId()).block();
    		
    		polGroupReltnRepo.deleteByGroupId(enitityGroup.getId()).block();
    		
    		groupRepo.deleteById(enitityGroup.getId()).block();
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error deleting cert policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}  
    }       
    
    /**
     * Updates the attributes of a policy group.  This method only updates the policy group name.
     * @param groupName The name of the policy group to update.
     * @param newGroupName The new name of the policy group.
     * @return Status of 204 if the policy group was updated or a status of 404 if a policy group with the given name
     * does not exist.
     */ 
    @PostMapping(value="groups/{groupName}/groupAttributes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<Void>> updateGroupAttributes(@PathVariable("groupName") String groupName, @RequestBody String newGroupName)
    { 
       	// make sure the policy exists
    	org.nhindirect.config.store.CertPolicyGroup entityGroup;
    	try
    	{
    		entityGroup = groupRepo.findByPolicyGroupNameIgnoreCase(groupName).block();
    		if (entityGroup == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();	
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up cert policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}

    	// update the group
    	try
    	{
    		if (!StringUtils.isEmpty(newGroupName))
    			entityGroup.setPolicyGroupName(newGroupName);
    			
    		groupRepo.save(entityGroup).block();
    		return ResponseEntity.status(HttpStatus.NO_CONTENT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error updating cert policy group attributes.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }  
    
    /**
     * Adds a certificate policy usage to a policy group.
     * @param groupName The name of the policy group to add the usage to.
     * @param use The certificate policy usage to add to the policy group.
     * @return Status of 204 if the usage was added to the policy group or a status of 404 if either the certificate
     * policy or policy group does not exist.
     */ 
    @PostMapping(value="groups/uses/{group}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<Void>> addPolicyUseToGroup(@PathVariable("group") String groupName, @RequestBody CertPolicyGroupUse use)
    {
    	// make sure the group exists
    	org.nhindirect.config.store.CertPolicyGroup entityGroup;
    	try
    	{
    		entityGroup = groupRepo.findByPolicyGroupNameIgnoreCase(groupName).block();
    		if (entityGroup == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();	
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	// make sure the policy exists
    	org.nhindirect.config.store.CertPolicy entityPolicy;
    	try
    	{
    		entityPolicy = policyRepo.findByPolicyNameIgnoreCase(use.getPolicy().getPolicyName()).block();
    		if (entityPolicy == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up policy.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
       	// associate the group and policy
    	try
    	{
			final CertPolicyGroupReltn reltn = new CertPolicyGroupReltn();
			reltn.setCertPolicyId(entityPolicy.getId());
			reltn.setCertPolicyGroupId(entityGroup.getId());
			reltn.setPolicyUse(org.nhindirect.config.store.CertPolicyUse.valueOf(use.getPolicyUse().toString()).ordinal());
			reltn.setIncoming(use.isIncoming());
			reltn.setOutgoing(use.isOutgoing());

			polGroupReltnRepo.save(reltn).block();
			
    		return ResponseEntity.status(HttpStatus.NO_CONTENT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error adding cert policy to group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }
    
    /**
     * Removes a certificate policy usage from a policy group.
     * @param groupName  The name of the policy group to remove the usage from.
     * @param use The certificate policy usage to removed from the policy group.
     * @return A status of 200 if the usage is removed from the policy group or a status of 404 if the certificate policy, policy group,
     * or existing relationship is not found.
     */
    @PostMapping(value="groups/uses/{group}/removePolicy", consumes = MediaType.APPLICATION_JSON_VALUE)    
    public ResponseEntity<Mono<Void>> removedPolicyUseFromGroup(@PathVariable("group") String groupName, @RequestBody CertPolicyGroupUse use)
    {
    	// make sure the group exists
    	org.nhindirect.config.store.CertPolicyGroup entityGroup;
    	try
    	{
    		entityGroup = groupRepo.findByPolicyGroupNameIgnoreCase(groupName).block();
    		if (entityGroup == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}    
    	
    	final org.nhindirect.config.store.CertPolicyUse entityUse = org.nhindirect.config.store.CertPolicyUse.valueOf(use.getPolicyUse().toString());
    	
    	org.nhindirect.config.store.CertPolicyGroupReltn foundReltn = null;
    	
    	final Collection<org.nhindirect.config.store.CertPolicyGroupReltn> reltns = polGroupReltnRepo.findByGroupId(entityGroup.getId()).collectList().block();  
    	
		if (reltns != null)
		{
			
			for (org.nhindirect.config.store.CertPolicyGroupReltn groupReltn : reltns)
			{
				final org.nhindirect.config.store.CertPolicy certPol = policyRepo.findById(groupReltn.getCertPolicyId()).block();
				
				if (certPol.getPolicyName().equals(use.getPolicy().getPolicyName()) &&
						groupReltn.isIncoming() == use.isIncoming() && groupReltn.isOutgoing() == use.isOutgoing() &&
						groupReltn.getPolicyUse() == entityUse.ordinal())
				{
					foundReltn = groupReltn;
					break;
				}					
			}
		}
		
		if (foundReltn == null)
			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
		try
		{
			polGroupReltnRepo.deleteById(foundReltn.getId()).block();
			groupRepo.save(entityGroup).block();
		}
    	catch (Exception e)
    	{
    		log.error("Error removing cert policy from group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
		
		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    }
    
    /**
     * Gets all policy group to domain relationships.
     * @return A JSON representation of a collection of domain to policy group relationships.  Returns a status of 204 if no
     * relationships exist.
     */
    @GetMapping(value="/groups/domain", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Flux<CertPolicyGroupDomainReltn>> getPolicyGroupDomainReltns()
    {    	
    	try
    	{
    		final Flux<CertPolicyGroupDomainReltn> retVal = domainReltnRepo.findAll()
 	    	.flatMap(reltn -> 
 	    	{
 	    		
  	    	   final Mono<CertPolicyGroup> polGroup = groupRepo.findById(reltn.getCertPolicyGroupId())
   	        		.flatMap(group -> 
   	        		{
   	        			
   	        			return polGroupReltnRepo.findByGroupId(group.getId())
   	        				    .flatMap(groupReltn ->  
   	        				    {
   	        				       return policyRepo.findById(groupReltn.getCertPolicyId())
   	        				       .map(pol -> Maps.immutableEntry(groupReltn, pol));
   	        				    
   	        				    })
   	        				    .collect(Collectors.toMap(
   	        							Map.Entry::getKey,
   	        							Map.Entry::getValue))
   	        				    .map(polUseMap -> 
   	        				    {
   	        				    	return EntityModelConversion.toModelCertPolicyGroup(group, polUseMap);
   	        				    });
   	        		});
 	    	   
  	    	   final Mono<org.nhindirect.config.store.Domain> domain = this.domainRepo.findById(reltn.getDomainId());
  	    	   
  	    	   return domain.zipWith(polGroup, (d, p) ->
  	    	   {
 	    	    	final CertPolicyGroupDomainReltn cpgdReltn = new CertPolicyGroupDomainReltn();
  	    	    	
 	    	    	cpgdReltn.setId(reltn.getId());
 	    	    	cpgdReltn.setPolicyGroup(p);
 	    	    	cpgdReltn.setDomain(EntityModelConversion.toModelDomain(d, Collections.emptyList()));
 	    	    	
 	    	    	return cpgdReltn;
  	    	   });
  	    	   
  	    	   			   
 	    	});   		
    		
 	       return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(retVal);
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up policy group/domain relations.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}  
    }
    
    /**
     * Gets all policy groups associated with a domain.
     * @param domainName The domain name to retrieve associate policy groups from.
     * @return A JSON representation of a collection of policy groups that are associated to the given domain.  Returns
     * a status of 404 if the a domain with the given name does not exist or a status of 204 or no policy groups are associated
     * to the given domain.
     */
    @GetMapping(value="groups/domain/{domain}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Flux<CertPolicyGroup>> getPolicyGroupsByDomain(@PathVariable("domain") String domainName)
    {
    	// make sure the domain exists
    	org.nhindirect.config.store.Domain entityDomain;
    	try
    	{
    		entityDomain = domainRepo.findByDomainNameIgnoreCase(domainName).block();
    		if (entityDomain == null)
    		{
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    		}
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	} 
    	
    	try
    	{

    		final Flux<CertPolicyGroup> retVal = domainReltnRepo.findByDomainId(entityDomain.getId())
    	 	.flatMap(reltn -> 
    	 	{
    	 		return groupRepo.findById(reltn.getCertPolicyGroupId())
        		.flatMap(group -> 
        		{
        			
        			return polGroupReltnRepo.findByGroupId(group.getId())
        				    .flatMap(groupReltn ->  
        				    {
        				       return policyRepo.findById(groupReltn.getCertPolicyId())
        				       .map(pol -> Maps.immutableEntry(groupReltn, pol));
        				    
        				    })
        				    .collect(Collectors.toMap(
        							Map.Entry::getKey,
        							Map.Entry::getValue))
        				    .map(polUseMap -> 
        				    {
        				    	return EntityModelConversion.toModelCertPolicyGroup(group, polUseMap);
        				    });
        		});
    	 	});   
    		
    	    return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(retVal);
    	    
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up cert policy groups.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	       	
    }
    
    /**
     * Associates a policy group to a domain.
     * @param groupName The policy group name to associate to the domain.
     * @param domainName The domain name to associate to the policy group.
     * @return Status of 204 if the policy group and domain are associated or a status of 404 if either the policy group
     * or domain with the given respective names do not exist.
     */
    @PostMapping("groups/domain/{group}/{domain}")
    public ResponseEntity<Mono<Void>> associatePolicyGroupToDomain(@PathVariable("group") String groupName, @PathVariable("domain") String domainName)
    {
    	// make sure the group exists
    	org.nhindirect.config.store.CertPolicyGroup entityGroup;
    	try
    	{
    		entityGroup = groupRepo.findByPolicyGroupNameIgnoreCase(groupName).block();
    		if (entityGroup == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}  
    	
    	// make sure the domain exists
    	org.nhindirect.config.store.Domain entityDomain;
    	try
    	{
    		entityDomain = domainRepo.findByDomainNameIgnoreCase(domainName).block();
    		if (entityDomain == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}  
    	
       	// associate the domain and group
    	try
    	{
			final org.nhindirect.config.store.CertPolicyGroupDomainReltn policyGroupDomainAssoc = 
					new org.nhindirect.config.store.CertPolicyGroupDomainReltn();
			policyGroupDomainAssoc.setDomainId(entityDomain.getId());
			policyGroupDomainAssoc.setCertPolicyGroupId(entityGroup.getId());
    		
			domainReltnRepo.save(policyGroupDomainAssoc).block();

    		return ResponseEntity.status(HttpStatus.NO_CONTENT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error associating policy group to domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }
    
    /**
     * Removed a policy group from a domain.
     * @param groupName The policy group name to remove from the domain.
     * @param domainName The domain name to remove from the policy group.
     * @return A status of 200 if the policy group is removed from the domain or a status of 404 if either the policy group
     * or domain with the given respective names do not exist.
     */
    @DeleteMapping("groups/domain/{group}/{domain}")
    public ResponseEntity<Mono<Void>> disassociatePolicyGroupFromDomain(@PathVariable("group") String groupName, @PathVariable("domain") String domainName)
    {
    	// make sure the group exists
    	org.nhindirect.config.store.CertPolicyGroup entityGroup;
    	try
    	{
    		entityGroup = groupRepo.findByPolicyGroupNameIgnoreCase(groupName).block();
    		if (entityGroup == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}  
    	
    	// make sure the domain exists
    	org.nhindirect.config.store.Domain entityDomain;
    	try
    	{
    		entityDomain = domainRepo.findByDomainNameIgnoreCase(domainName).block();
    		if (entityDomain == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	} 
    	
    	// now make the disassociation
    	try
    	{
    		domainReltnRepo.deleteByDomainIdAndCertPolicyGroupId(entityDomain.getId(), entityGroup.getId()).block();
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error disassociating policy group from domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	} 
    }
    
    /**
     * Removes all policy groups for a given domain.
     * @param domainName The domain to remove all policy groups from.
     * @return Status of 204 if all policy groups are removed from the domain or a status or 404 if a domain with the given name does
     * not exist.
     */
    @DeleteMapping(value="groups/domain/{domain}/deleteFromDomain")
    public ResponseEntity<Mono<Void>> disassociatePolicyGroupsFromDomain(@PathVariable("domain") String domainName)
    {
    	// make sure the domain exists
    	org.nhindirect.config.store.Domain entityDomain;
    	try
    	{
    		entityDomain = domainRepo.findByDomainNameIgnoreCase(domainName).block();
    		if (entityDomain == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	} 
    	
    	// now make the disassociation
    	try
    	{
    		domainReltnRepo.deleteByDomainId(entityDomain.getId()).block();
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error disassociating policy groups from domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	} 
    }
    
    /**
     * Removes a given policy group from all domains.
     * @param groupName The policy group to remove from all domains.
     * @return Status of 200 if the policy group is removed from all domains or a status of 404 if the a policy group with the given
     * name does not exist.
     */
    @DeleteMapping("groups/domain/{group}/deleteFromGroup")
    public ResponseEntity<Mono<Void>> disassociatePolicyGroupFromDomains(@PathVariable("group") String groupName)
    {
    	// make sure the group exists
    	org.nhindirect.config.store.CertPolicyGroup entityGroup;
    	try
    	{
    		entityGroup = groupRepo.findByPolicyGroupNameIgnoreCase(groupName).block();
    		if (entityGroup == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up policy group.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}  
    	
    	// now make the disassociation
    	try
    	{
    		domainReltnRepo.deleteByCertPolicyGroupId(entityGroup.getId()).block();
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error disassociating policy groups from domain.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	} 
    }    
}
