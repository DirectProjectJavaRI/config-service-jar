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

package org.nhindirect.config.processor.impl;

import java.time.LocalDateTime;

import org.nhindirect.config.processor.BundleCacheUpdateProcessor;
import org.nhindirect.config.processor.BundleRefreshProcessor;
import org.nhindirect.config.repository.TrustBundleRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Default implementation of the {@linkplain BundleCacheUpdateProcessor} interface.
 * <p>
 * This implementation can be triggered on a regular interval to check if a bundle needs to be refreshed.
 * The implementation iterates through the entire list of configured trust bundles in the system checking
 * each bundle's last refresh time.
 * If a bundles refresh interval has not been exceeded since its last update, then it will not checked
 * for updates.
 * @author Greg Meyer
 * @since 1.3
 */
@Slf4j
public class DefaultBundleCacheUpdateProcessorImpl implements BundleCacheUpdateProcessor
{
	
    /**
     * Trust bundle repo
     */
	protected TrustBundleRepository bundleRepo;

	/**
	 * The bundle refresh processor.
	 */
	protected BundleRefreshProcessor refreshProcessor;
	
	/**
	 * Default constructor
	 */
	public DefaultBundleCacheUpdateProcessorImpl()
	{
		
	}
	
	/**
	 * Sets the trust bundle repository used to get the last refresh date/time.
	 * @param bundleRepo
	 */
	public void setRepository(TrustBundleRepository bundleRepo)
	{
		this.bundleRepo = bundleRepo;
	}
	
	/**
	 * Sets the {@link BundleRefreshProcessor} used to refresh a bundle the bundle's refresh interval
	 * has been exceeded.
	 * @param refreshProcessor The {@link BundleRefreshProcessor}.
	 */
	public void setRefreshProcessor(BundleRefreshProcessor refreshProcessor)
	{
		this.refreshProcessor = refreshProcessor;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Mono<Void> updateBundleCache()
	{
		try
		{
			return bundleRepo.findAll()
				.flatMap(bundle -> 
				{
					
					// if the refresh interval is 0 or less, then we won't ever auto refresh the bundle
					if (bundle.getRefreshInterval() <= 0)
						return Mono.empty(); 
					
					boolean refresh = false;
					
					
					// see if this bundle needs to be checked for updating
					final LocalDateTime lastAttempt = bundle.getLastSuccessfulRefresh();
					
					if (lastAttempt == null)
						// never been attempted successfully... better go get it
						refresh = true;
					else
					{
						// check the the last attempt date against now and see if we need to refresh
						LocalDateTime now = LocalDateTime.now();
						LocalDateTime lastAttemptCheck = LocalDateTime.from(lastAttempt);
						lastAttemptCheck = lastAttemptCheck.plusSeconds(bundle.getRefreshInterval());
						
						if (lastAttemptCheck.isBefore(now))
							refresh = true;
					}
					
					final Mono<?> retVal = (refresh) ? refreshProcessor.refreshBundle(bundle) : Mono.empty();
	
					return retVal
				   	     	.onErrorResume(e -> { 
				   	    		log.error("Error refreshing trust bundles", e);
				   	    		return Mono.empty();
				   	    	});	
				})
				.onErrorResume(e -> { 
	   	    		log.error("Error refreshing trust bundles", e);
	   	    		return Mono.empty();
	   	    	})
				.then();
		}
		catch (Exception e)
		{
    		log.error("Error retriving trust bundles", e);
    		return Mono.empty();
		}

	}
}
