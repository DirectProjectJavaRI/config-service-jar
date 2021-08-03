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

import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nhindirect.common.cert.Thumbprint;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.repository.AnchorRepository;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * Resource for managing anchor resources in the configuration service.
 * <p>
 * Although not required, this class is instantiated using the Jersey SpringServlet and dependencies are defined in the Sprint context XML file.
 * @author Greg Meyer
 * @since 2.0
 */
@RestController
@RequestMapping("anchor")
@Slf4j
public class AnchorResource extends ProtectedResource
{
    
    protected AnchorRepository anchorRepo;
    
    /**
     * Constructor
     */
    public AnchorResource()
    {
		
	}
    
    /**
     * Sets the anchor repository.  Auto populated by Spring
     * @param anchorRepo Anchor repository
     */
    @Autowired
    public void setAnchorRepository(AnchorRepository anchorRepo) 
    {
        this.anchorRepo = anchorRepo;
    }
    
    
    /**
     * Gets a set of list of anchor for a given owner of the anchor.  Additional query parameters can further filter the return list.
     * @param incoming Returned anchors must be marked for use of incoming messages.  Defaults to false meaning that no filter is applied.
     * @param outgoing Returned anchors must be marked for use of outgoing messages.  Defaults to false meaning that no filter is applied.
     * @param thumbprint Returned anchors that match a specific thumbprint effectively limiting the number of returned anchors to 1.  
     * Defaults to an empty string meaning that no filter is applied.
     * @param owner The owner to retrieve anchors for.
     * @return A JSON representation of a collection of anchors that match the filters.  Returns a status of 204 if no anchors match the filters or no
     * anchors exist for the owner.
     */      
    @GetMapping(value="/{owner}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Anchor> getAnchorForOwner(@RequestParam(name="incoming", defaultValue="false") boolean incoming, 
    		@RequestParam(name="outgoing", defaultValue="false") boolean outgoing, 
    		@RequestParam(name="thumbprint", defaultValue="") String thumbprint, 
    		@PathVariable("owner") String owner)
    {
		return anchorRepo.findByOwnerIgnoreCase(owner)
				.filter(anchor -> !((incoming && !anchor.isIncoming()) || (outgoing && !anchor.isOutgoing()) ||
	    				(!thumbprint.isEmpty() && !thumbprint.equalsIgnoreCase(anchor.getThumbprint()))))
				.map(anchor -> EntityModelConversion.toModelAnchor(anchor))		
	   	     	.onErrorResume(e -> { 
	   	    		log.error("Error looking up anchors.", e);
	   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	   	    	});
    }
    
    /**
     * Gets all anchors in the system.
     * @return A JSON representation of a collection of all anchors in the system.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Anchor> getAnchors()
    {
		return anchorRepo.findAll()
				.map(anchor -> EntityModelConversion.toModelAnchor(anchor))
	   	     	.onErrorResume(e -> { 
	   	    		log.error("Error looking up anchors.", e);
	   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
	   	    	});	
    }
    
    /**
     * Adds an anchor to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param anchor The anchor to add to the system.
     * @return Returns a status of 201 if the anchor was added, or a status of 409 if the anchor already exists for 
     * a specific owner.
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)   
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addAnchor(@RequestBody Anchor anchor) 
    {

    	try
    	{
			final String thumbprint = (anchor.getThumbprint() == null || anchor.getThumbprint().isEmpty()) ?
					Thumbprint.toThumbprint(anchor.getAnchorAsX509Certificate()).toString() : anchor.getThumbprint();
					
			//final Collection<org.nhindirect.config.store.Anchor> existingAnchors = 
			return anchorRepo.findByOwnerIgnoreCase(anchor.getOwner())
					.filter(existingAnchor -> existingAnchor.getThumbprint().equalsIgnoreCase(thumbprint))
					.collectList()
					.switchIfEmpty(Mono.just(Collections.emptyList()))
					.flatMap(anchors -> 
					{
						if (!anchors.isEmpty())
							return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT));
						
			    		try
			    		{
							final org.nhindirect.config.store.Anchor addAnchor = EntityModelConversion.toEntityAnchor(anchor);
				    		addAnchor.setId(null);
				    		
				    		return anchorRepo.save(addAnchor)
				    		.then()
		    	   	     	.onErrorResume(e -> { 
		    	   	    		log.error("Error adding anchor.", e);
		    	   	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		    	   	    	});	
			    		}
			    		catch (Exception e)
			    		{
				    		log.error("Error converting anchor.", e);
				    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
			    		}
					});
    	} 
    	catch (CertificateException ex) 
    	{
	    		log.error("Error converting query thubmprint.", ex);
	    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
		}
    }
   
    /**
     * Deletes anchor from the system by system id.
     * @param ids List of ids to delete from the system.
     * @return Status of 200 if the anchors were deleted successfully.
     */
    @DeleteMapping(value="ids/{ids}")   
    public Mono<Void> removeAnchorsByIds(@PathVariable("ids")  String ids)
    {
    	final String[] idArray = ids.split(",");
    	final List<Long> idList = new ArrayList<>();

		for (String id : idArray)
			idList.add(Long.parseLong(id));
		
		return anchorRepo.deleteByIdIn(idList)
     	.onErrorResume(e -> { 
    		log.error("Error removing anchors by ids.", e);
    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
    	});	

    }
    
    /**
     * Delete all anchors for a specific owner.
     * @param owner The owner to delete anchor from.
     * @return Status of 200 if the anchors were deleted successfully.
     */
    @DeleteMapping(value="{owner}")  
    public Mono<Void> removeAnchorsByOwner(@PathVariable("owner") String owner)
    {
		return anchorRepo.deleteByOwnerIgnoreCase(owner)
     	.onErrorResume(e -> { 
    		log.error("Error removing anchors by owner.", e);
    		return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
    	});	

    }
}
