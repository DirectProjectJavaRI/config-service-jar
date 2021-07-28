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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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

import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CertPolicyResource extends ProtectedResource
{	
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
    public Flux<CertPolicy> getPolicies()
    {
		return policyRepo.findAll()
				.map(pol -> {
					return EntityModelConversion.toModelCertPolicy(pol);				
				})
		     	.onErrorResume(e -> { 
		    		log.error("Error looking up cert policies.", e);
		    		return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		    	});	
    }
    
    /**
     * Gets a certificate policy by name.  
     * @param policyName The name of the certificate policy to retrieve.
     * @return A JSON representation of the certificate policy.  Returns a status of 404 if a certificate policy with the given name does not exist.
     */
    @GetMapping(value="/{policyName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<CertPolicy> getPolicyByName(@PathVariable("policyName") String policyName)
    {
		return policyRepo.findByPolicyNameIgnoreCase(policyName)
			.map(retPolicy -> {
				return EntityModelConversion.toModelCertPolicy(retPolicy);
			})
	     	.onErrorResume(e -> { 
	    		log.error("Error looking up cert policy.", e);
	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	    	});	
    }  
    
    /**
     * Adds a certificate policy to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param policy The certificate policy to add.
     * @return A status of 201 if the policy was added or a status of 409 if the policy already exists.
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)  
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addPolicy(@RequestBody CertPolicy policy)
    {
    	return policyRepo.findByPolicyNameIgnoreCase(policy.getPolicyName())
    	    .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicy()))
    	    .flatMap(pol -> 
    	    {
    	    	if (pol.getPolicyName() != null)
    	    		return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT));
    	    	
        		final org.nhindirect.config.store.CertPolicy entityPolicy = EntityModelConversion.toEntityCertPolicy(policy);
        		
        		return policyRepo.save(entityPolicy)
        		.then()
       	     	.onErrorResume(e -> { 
       	    		log.error("Error looking up cert policy.", e);
       	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
       	    	});
    		});
    }  
    
    /**
     * Deletes a certificate policy by name.
     * @param policyName The name of the certificate policy.
     * @return Status of 200 if the policy was delete or 404 if a certificate policy with the given name does not exist.
     */
    @DeleteMapping(value="{policyName}") 
    public Mono<Void> removePolicyByName(@PathVariable("policyName") String policyName)
    {
    	return policyRepo.findByPolicyNameIgnoreCase(policyName)
        	    .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicy()))
        	    .flatMap(pol -> 
        	    {
        	    	if (pol.getPolicyName() == null)
        	    		return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
        	    	
        	    	return polGroupReltnRepo.deleteByPolicyId(pol.getId())
        	    		.then(policyRepo.deleteById(pol.getId()))
               	     	.onErrorResume(e -> { 
               	    		log.error("Error deleting cert policy.", e);
               	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
               	    	});	
        	    });
    }    
    
    /**
     * Updates the information of a certificate policy.
     * @param policyName The name of the certificate policy to update.
     * @param policyData Data that should be update.  Any null or empty attributes will result in that attribute not being updated.
     * @return Status of 204 if the certificate policy was updated or 404 if a certificate policy with the given name does not exist.
     */
    @PostMapping(value="{policyName}/policyAttributes", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updatePolicyAttributes(@PathVariable("policyName") String policyName, @RequestBody CertPolicy policyData)
    { 
    	return policyRepo.findByPolicyNameIgnoreCase(policyName)
        	    .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicy()))
        	    .flatMap(pol -> 
        	    {
        	    	if (pol.getPolicyName() == null)
        	    		return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
        	    	
        			if (policyData.getPolicyData() != null && policyData.getPolicyData().length > 0)
        				pol.setPolicyData(policyData.getPolicyData());
        			
        			if (!StringUtils.isEmpty(policyData.getPolicyName()))
        				pol.setPolicyName(policyData.getPolicyName());
        			
        			if (policyData.getLexicon() != null)
        				pol.setLexicon(policyData.getLexicon().ordinal());
            		
        			return policyRepo.save(pol)
        			.then()
           	     	.onErrorResume(e -> { 
           	    		log.error("Error updating cert policy attributes.", e);
           	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
           	    	});	
        			
        	    });
    }
    
    /**
     * Gets all policy groups in the system.
     * @return A JSON representation of a collection of all policy groups in the system.  Returns a status of 204 if no policy
     * groups exist.
     */
    @GetMapping(value="groups", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<CertPolicyGroup> getPolicyGroups()
    {    	
		return groupRepo.findAll()
		.flatMap(group -> 
		{ 
			return polGroupReltnRepo.findByPolicyGroupId(group.getId())
				    .flatMap(reltn ->  
				    {
				       return policyRepo.findById(reltn.getPolicyId())
				       .map(pol -> Maps.immutableEntry(reltn, pol));
				    
				    })
				    .collect(Collectors.toMap(
							Map.Entry::getKey,
							Map.Entry::getValue))
				    .map(polUseMap -> EntityModelConversion.toModelCertPolicyGroup(group, polUseMap));
		})
     	.onErrorResume(e -> { 
    		log.error("Error looking up cert policy groups.", e);
    		return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
    	});	
   	
    }  
    
    /**
     * Gets a policy group name.
     * @param groupName The name of the policy group to retrieve.
     * @return A JSON representation of the policy group.  Returns a status of 404 if a policy group with the given name does
     * not exist.
     */
    @GetMapping(value="groups/{groupName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<CertPolicyGroup> getPolicyGroupByName(@PathVariable("groupName") String groupName)
    {

		return groupRepo.findByPolicyGroupNameIgnoreCase(groupName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicyGroup()))
		   .flatMap(group -> 
		   {
			   
			   if (group.getPolicyGroupName() == null)
				   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)); 
			   
			   return polGroupReltnRepo.findByPolicyGroupId(group.getId())
				    .flatMap(reltn ->  
				    {
				       return policyRepo.findById(reltn.getPolicyId())
				       .map(pol -> Maps.immutableEntry(reltn, pol));
				    
				    })
				    .collect(Collectors.toMap(
							Map.Entry::getKey,
							Map.Entry::getValue))
				    .map(polUseMap -> EntityModelConversion.toModelCertPolicyGroup(group, polUseMap))
			     	.onErrorResume(e -> { 
			    		log.error("Error looking up cert policy group.", e);
			    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
			    	});	
		   });
    		    	
    }
    
    /**
     * Adds a policy group to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param group The policy group to add.
     * @return Status of 201 if the policy group was added or a status of 409 if the policy group
     * already exists.
     */
    @PutMapping(value="groups", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addPolicyGroup(@RequestBody CertPolicyGroup group)
    {
		return groupRepo.findByPolicyGroupNameIgnoreCase(group.getPolicyGroupName())
			   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicyGroup()))
			   .flatMap(foundGroup -> 
			   {
				   if (foundGroup.getPolicyGroupName() != null)
					   return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT)); 
				   
				   final org.nhindirect.config.store.CertPolicyGroup entityGroup = EntityModelConversion.toEntityCertPolicyGroup(group);
				   
				   return groupRepo.save(entityGroup)
				      .then()
				      .onErrorResume(e -> { 
				    		log.error("Error adding trust cert policy group.", e);
				    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
				    	});	
			   });
    }  
    
    /**
     * Deletes a policy group from the system.
     * @param groupName The name of the policy group to delete.
     * @return A status of 200 if the policy group was deleted or a status of 404 if a policy group with the given
     * name does not exist.
     */
    @DeleteMapping(value="groups/{groupName}")  
    public Mono<Void> removePolicyGroupByName(@PathVariable("groupName") String groupName)
    {
		return groupRepo.findByPolicyGroupNameIgnoreCase(groupName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicyGroup()))
		   .flatMap(foundGroup -> 
		   {
			   if (foundGroup.getPolicyGroupName() == null)
				   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			   
	    		return domainReltnRepo.deleteByPolicyGroupId(foundGroup.getId())
	    		   .then(polGroupReltnRepo.deleteByPolicyGroupId(foundGroup.getId()))
	    		   .then(groupRepo.deleteById(foundGroup.getId()))
			       .onErrorResume(e -> { 
			    		log.error("Error looking up existing cert policy group.", e);
			    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
			    	});						   
		   });

    }       
    
    /**
     * Updates the attributes of a policy group.  This method only updates the policy group name.
     * @param groupName The name of the policy group to update.
     * @param newGroupName The new name of the policy group.
     * @return Status of 204 if the policy group was updated or a status of 404 if a policy group with the given name
     * does not exist.
     */ 
    @PostMapping(value="groups/{groupName}/groupAttributes", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateGroupAttributes(@PathVariable("groupName") String groupName, @RequestBody String newGroupName)
    { 
		return groupRepo.findByPolicyGroupNameIgnoreCase(groupName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicyGroup()))
		   .flatMap(foundGroup -> 
		   {
			   if (foundGroup.getPolicyGroupName() == null)
				   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			   
	    	   if (!StringUtils.isEmpty(newGroupName))
	    		   foundGroup.setPolicyGroupName(newGroupName);
	    	   
	    	   return groupRepo.save(foundGroup)
	    	      .then()
			      .onErrorResume(e -> { 
			    		log.error("Error updating cert policy group attributes.", e);
			    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
			      });	
		   });
    }  
    
    /**
     * Adds a certificate policy usage to a policy group.
     * @param groupName The name of the policy group to add the usage to.
     * @param use The certificate policy usage to add to the policy group.
     * @return Status of 204 if the usage was added to the policy group or a status of 404 if either the certificate
     * policy or policy group does not exist.
     */ 
    @PostMapping(value="groups/uses/{group}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> addPolicyUseToGroup(@PathVariable("group") String groupName, @RequestBody CertPolicyGroupUse use)
    {
		return groupRepo.findByPolicyGroupNameIgnoreCase(groupName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicyGroup()))
		   .flatMap(foundGroup -> 
		   {
			   if (foundGroup.getPolicyGroupName() == null)
				   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			   
			   return policyRepo.findByPolicyNameIgnoreCase(use.getPolicy().getPolicyName())
			      .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicy()))
			      .flatMap(entityPolicy -> 
			      {
					   if (entityPolicy.getPolicyName() == null)
						   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
					   
						final CertPolicyGroupReltn reltn = new CertPolicyGroupReltn();
						reltn.setPolicyId(entityPolicy.getId());
						reltn.setPolicyGroupId(foundGroup.getId());
						reltn.setPolicyUse(org.nhindirect.config.store.CertPolicyUse.valueOf(use.getPolicyUse().toString()).ordinal());
						reltn.setIncoming(use.isIncoming());
						reltn.setOutgoing(use.isOutgoing());

						return polGroupReltnRepo.save(reltn)
						  .then()
					      .onErrorResume(e -> { 
					    		log.error("Error adding cert policy to group.", e);
					    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
					      }); 
			      });
		   });
    }
    
    /**
     * Removes a certificate policy usage from a policy group.
     * @param groupName  The name of the policy group to remove the usage from.
     * @param use The certificate policy usage to removed from the policy group.
     * @return A status of 200 if the usage is removed from the policy group or a status of 404 if the certificate policy, policy group,
     * or existing relationship is not found.
     */
    @PostMapping(value="groups/uses/{group}/removePolicy", consumes = MediaType.APPLICATION_JSON_VALUE)    
    public Mono<Void> removedPolicyUseFromGroup(@PathVariable("group") String groupName, @RequestBody CertPolicyGroupUse use)
    {
		return groupRepo.findByPolicyGroupNameIgnoreCase(groupName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicyGroup()))
		   .flatMap(foundGroup -> 
		   {
			   if (foundGroup.getPolicyGroupName() == null)
				   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			   
		    	final org.nhindirect.config.store.CertPolicyUse entityUse = 
		    			org.nhindirect.config.store.CertPolicyUse.valueOf(use.getPolicyUse().toString());

		    	return polGroupReltnRepo.findByPolicyGroupId(foundGroup.getId())
		    		.next()
		    		.flatMap(reltn -> 
		    		{
		    			return policyRepo.findById(reltn.getPolicyId())
		    				.flatMap(certPol -> 
		    				{
		    					
		    					if (certPol.getPolicyName().equals(use.getPolicy().getPolicyName()) &&
		    							reltn.isIncoming() == use.isIncoming() && reltn.isOutgoing() == use.isOutgoing() &&
		    									reltn.getPolicyUse() == entityUse.ordinal())
		    					{
		    						return polGroupReltnRepo.deleteById(reltn.getId())
		    						  .then(groupRepo.save(foundGroup))
		    						  .then();
		    					}
		    					
		    					return Mono.empty();
		    				});

		    		});	
		   });
		
    }
    
    /**
     * Gets all policy group to domain relationships.
     * @return A JSON representation of a collection of domain to policy group relationships.  Returns a status of 204 if no
     * relationships exist.
     */
    @GetMapping(value="/groups/domain", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<CertPolicyGroupDomainReltn> getPolicyGroupDomainReltns()
    {    	
    		return domainReltnRepo.findAll()
 	    	.flatMap(reltn -> 
 	    	{
 	    		
  	    	   final Mono<CertPolicyGroup> polGroup = groupRepo.findById(reltn.getPolicyGroupId())
   	        		.flatMap(group -> 
   	        		{
   	        			
   	        			return polGroupReltnRepo.findByPolicyGroupId(group.getId())
   	        				    .flatMap(groupReltn ->  
   	        				    {
   	        				       return policyRepo.findById(groupReltn.getPolicyId())
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
  	    	   
  	    	   			   
 	    	})
		    .onErrorResume(e -> { 
		    		log.error("Error looking up policy group/domain relations.", e);
		    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		     }); 
    }
    
    /**
     * Gets all policy groups associated with a domain.
     * @param domainName The domain name to retrieve associate policy groups from.
     * @return A JSON representation of a collection of policy groups that are associated to the given domain.  Returns
     * a status of 404 if the a domain with the given name does not exist or a status of 204 or no policy groups are associated
     * to the given domain.
     */
    @GetMapping(value="groups/domain/{domain}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<CertPolicyGroup> getPolicyGroupsByDomain(@PathVariable("domain") String domainName)
    {
		return domainRepo.findByDomainNameIgnoreCase(domainName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
		   .flatMapMany(domain -> 
		   {
			   if (domain.getDomainName() == null)
				   return Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND));

			   return domainReltnRepo.findByDomainId(domain.getId())
		    	 	.flatMap(reltn -> 
		    	 	{
		    	 		return groupRepo.findById(reltn.getPolicyGroupId())
		        		.flatMap(group -> 
		        		{
		        			
		        			return polGroupReltnRepo.findByPolicyGroupId(group.getId())
		        				    .flatMap(groupReltn ->  
		        				    {
		        				       return policyRepo.findById(groupReltn.getPolicyId())
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
		    	 	})
				    .onErrorResume(e -> { 
			    		log.error("Error looking up cert policy groups.", e);
			    		return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
			        });
		   });
    }
    
    /**
     * Associates a policy group to a domain.
     * @param groupName The policy group name to associate to the domain.
     * @param domainName The domain name to associate to the policy group.
     * @return Status of 204 if the policy group and domain are associated or a status of 404 if either the policy group
     * or domain with the given respective names do not exist.
     */
    @PostMapping("groups/domain/{group}/{domain}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> associatePolicyGroupToDomain(@PathVariable("group") String groupName, @PathVariable("domain") String domainName)
    {
		return groupRepo.findByPolicyGroupNameIgnoreCase(groupName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicyGroup()))
		   .flatMap(group -> 
		   {
			   if (group.getPolicyGroupName() == null)
				   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			   
			   return domainRepo.findByDomainNameIgnoreCase(domainName)
				   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
				   .flatMap(domain -> 
				   {
					   if (domain.getDomainName() == null)
						   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
					   
					   final org.nhindirect.config.store.CertPolicyGroupDomainReltn policyGroupDomainAssoc = 
								new org.nhindirect.config.store.CertPolicyGroupDomainReltn();
					   policyGroupDomainAssoc.setDomainId(domain.getId());
					   policyGroupDomainAssoc.setPolicyGroupId(group.getId());
			    		
					   return domainReltnRepo.save(policyGroupDomainAssoc)
					   .then()
					   .onErrorResume(e -> { 
				    		log.error("Error associating policy group to domain.", e);
				    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
				       }); 
				   });
		   });
    }
    
    /**
     * Removed a policy group from a domain.
     * @param groupName The policy group name to remove from the domain.
     * @param domainName The domain name to remove from the policy group.
     * @return A status of 200 if the policy group is removed from the domain or a status of 404 if either the policy group
     * or domain with the given respective names do not exist.
     */
    @DeleteMapping("groups/domain/{group}/{domain}")
    public Mono<Void> disassociatePolicyGroupFromDomain(@PathVariable("group") String groupName, @PathVariable("domain") String domainName)
    {
		return groupRepo.findByPolicyGroupNameIgnoreCase(groupName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicyGroup()))
		   .flatMap(group -> 
		   {
			   if (group.getPolicyGroupName() == null)
				   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			   
			   return domainRepo.findByDomainNameIgnoreCase(domainName)
				   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
				   .flatMap(domain -> 
				   {
					   if (domain.getDomainName() == null)
						   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
					   
					   return domainReltnRepo.deleteByDomainIdAndPolicyGroupId(domain.getId(), group.getId())
						   .onErrorResume(e -> { 
					    		log.error("Error disassociating policy group from domain.", e);
						    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
						       }); 
					   });
			   });
}
    
    /**
     * Removes all policy groups for a given domain.
     * @param domainName The domain to remove all policy groups from.
     * @return Status of 204 if all policy groups are removed from the domain or a status or 404 if a domain with the given name does
     * not exist.
     */
    @DeleteMapping(value="groups/domain/{domain}/deleteFromDomain")
    public Mono<Void> disassociatePolicyGroupsFromDomain(@PathVariable("domain") String domainName)
    {
	   return domainRepo.findByDomainNameIgnoreCase(domainName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.Domain()))
		   .flatMap(domain -> 
		   {
			   if (domain.getDomainName() == null)
				   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			   
			   return domainReltnRepo.deleteByDomainId(domain.getId())
			       .onErrorResume(e -> { 
		    		log.error("Error disassociating policy groups from domain.", e);
			    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
			       }); 
		   });
    }
    
    /**
     * Removes a given policy group from all domains.
     * @param groupName The policy group to remove from all domains.
     * @return Status of 200 if the policy group is removed from all domains or a status of 404 if the a policy group with the given
     * name does not exist.
     */
    @DeleteMapping("groups/domain/{group}/deleteFromGroup")
    public Mono<Void> disassociatePolicyGroupFromDomains(@PathVariable("group") String groupName)
    {
		return groupRepo.findByPolicyGroupNameIgnoreCase(groupName)
		   .switchIfEmpty(Mono.just(new org.nhindirect.config.store.CertPolicyGroup()))
		   .flatMap(group -> 
		   {
			   if (group.getPolicyGroupName() == null)
				   return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			   
			   return domainReltnRepo.deleteByPolicyGroupId(group.getId())
		       .onErrorResume(e -> { 
	    		log.error("Error disassociating policy groups from domain.", e);
		    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		       }); 
		   });
    }    
}
