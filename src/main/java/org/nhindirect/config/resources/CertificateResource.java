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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.common.cert.Thumbprint;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.model.utils.CertUtils.CertContainer;
import org.nhindirect.config.resources.util.EntityModelConversion;
import org.nhindirect.config.store.dao.CertificateDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

/**
 * Resource for managing certificate resources in the configuration service.
 * <p>
 * Although not required, this class is instantiated using the Jersey SpringServlet and dependencies are defined in the Sprint context XML file.
 * @author Greg Meyer
 * @since 2.0
 */
@RestController
@RequestMapping("certificate")
public class CertificateResource extends ProtectedResource
{	
    private static final Log log = LogFactory.getLog(CertificateResource.class);
    
    protected CertificateDao certDao;
    
    /**
     * Constructor
     */
    public CertificateResource()
    {
		
	}
    
    /**
     * Sets the certificate Dao.  Auto populate by Spring
     * @param certDao The certificate Dao
     */
    @Autowired
    public void setCertificateDao(CertificateDao certDao) 
    {
        this.certDao = certDao;
    }
    
    /**
     * Gets all certificates in the system.
     * @return A JSON representation of a collection of all certificates in the system.  Returns a status of 204 if no certificates
     * exist.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<Certificate>> getAllCertificates()
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
    public ResponseEntity<Collection<Certificate>> getCertificatesByOwner(@PathVariable("owner") String owner)
    {
    	List<org.nhindirect.config.store.Certificate> retCertificates;
    	
    	try
    	{
    		retCertificates = certDao.list(owner);
    		if (retCertificates.isEmpty())
    			return ResponseEntity.status(HttpStatus.NO_CONTENT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up certificates.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    	
    	final Collection<Certificate> modelCerts = new ArrayList<Certificate>();
    	retCertificates.forEach(cert-> modelCerts.add(EntityModelConversion.toModelCertificate(cert)));

		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).body(modelCerts);  	
    }  
    
    /**
     * Gets a certificate for a specific owner and thumbprint.
     * @param owner The owner or the certificate.
     * @param thumbprint The thubmprint of the certificates.
     * @return Returns a JSON representation of the certificate that matches the owner and thumbprint.  Returns a status of 404 
     * if no matching certificate is found.
     */
    @GetMapping(value="/{owner}/{thumbprint}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Certificate> getCertificatesByOwnerAndThumbprint(@PathVariable("owner") String owner, 
    		@PathVariable("thumbprint") String thumbprint)
    {
    	org.nhindirect.config.store.Certificate retCertificate;
    	
    	try
    	{
    		retCertificate = certDao.load(owner, thumbprint);
    		if (retCertificate == null)
    			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up certificate.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}

		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache)
				.body(EntityModelConversion.toModelCertificate(retCertificate)); 
    }  
    
    /**
     * Adds a certificate to the system.
     * @param uriInfo Injected URI context used for building the location URI.
     * @param cert The certificate to add.
     * @return Returns a status of 201 if the certificate was added or a status of 409 if the certificate already exists.
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)       
    public ResponseEntity<Void> addCertificate(@RequestBody Certificate cert, HttpServletRequest request)
    {
    	// check to see if it already exists
    	CertContainer cont = null;
    	try
    	{
    		cont = CertUtils.toCertContainer(cert.getData());
    		
    		if (certDao.load(cert.getOwner(), Thumbprint.toThumbprint(cont.getCert()).toString()) != null)
    			return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error looking up certificate.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    
    	try
    	{
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
    		
			final org.nhindirect.config.store.Certificate entCert = EntityModelConversion.toEntityCertificate(cert);
    		certDao.save(entCert);
    		
    		final String requestUrl = request.getRequestURL().toString();
    		final URI uri = new UriTemplate("{requestUrl}/{certificate}").expand(requestUrl, "certificate/" + entCert.getOwner());
    		
    		return ResponseEntity.created(uri).cacheControl(noCache).build();
    		
    	}
    	catch (Exception e)
    	{
    		log.error("Error adding certificate.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }
    
    /**
     * Deletes certificates by system id.
     * @param ids Comma delimited list of system ids to delete.
     * @return Status of 200 if the certificates were deleted.
     */
    @DeleteMapping(value="ids/{ids}")   
    public ResponseEntity<Void> removeCertificatesByIds(@PathVariable("ids") String ids)
    {
    	final String[] idArray = ids.split(",");
    	final List<Long> idList = new ArrayList<>();
    	
    	
    	try
    	{
    		for (String id : idArray)
    			idList.add(Long.parseLong(id));
    		
    		certDao.delete(idList);
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error removing certificates by ids.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }
    
    /**
     * Deletes all certificate for a specific owner.
     * @param owner The owner of the certificate.
     * @return Status of 200 if the certificates were deleted.
     */
    @DeleteMapping(value="{owner}")  
    public ResponseEntity<Void> removeCertificatesByOwner(@PathVariable("owner") String owner)
    {
    	try
    	{
    		certDao.delete(owner);
    		
    		return ResponseEntity.status(HttpStatus.OK).cacheControl(noCache).build();
    	}
    	catch (Exception e)
    	{
    		log.error("Error removing certificates by owner.", e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).cacheControl(noCache).build();
    	}
    }    
}
