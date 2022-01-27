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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.nhindirect.common.crypto.CryptoExtensions;
import org.nhindirect.common.options.OptionsManager;
import org.nhindirect.common.options.OptionsParameter;
import org.nhindirect.config.processor.BundleRefreshProcessor;
import org.nhindirect.config.repository.TrustBundleAnchorRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.nhindirect.config.store.BundleRefreshError;
import org.nhindirect.config.store.BundleThumbprint;
import org.nhindirect.config.store.TrustBundle;
import org.nhindirect.config.store.TrustBundleAnchor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Default implementation of the {@linkplain BundleRefreshProcessor} interface.
 * <p>
 * The implementation allows for bundles to be downloaded from SSL protected sites that may not
 * chain back to a trust CA.  This is useful in development environments and is not recommended in
 * a production environment.  By default, this feature is disable, but can be enabled using the 
 * {@link DefaultBundleRefreshProcessorImpl#BUNDLE_REFRESH_PROCESSOR_ALLOW_DOWNLOAD_FROM_UNTRUSTED} options parameter.
 * @author Greg Meyer
 * @since 1.3
 */
@Slf4j
public class DefaultBundleRefreshProcessorImpl implements BundleRefreshProcessor 
{
	
	/**
	 * Boolean value that specifies if bundles can be downloaded from non verified or untrusted SSL URLs.  The default value is false.
	 * <p><b>JVM Parameter/Options Name:</b> org.nhindirect.config.processor.impl.bundlerefresh.AllowNonVerifiedSSL
	 */
    public final static String BUNDLE_REFRESH_PROCESSOR_ALLOW_DOWNLOAD_FROM_UNTRUSTED = "BUNDLE_REFRESH_PROCESSOR_ALLOW_DOWNLOAD_FROM_UNTRUSTED";   
	
	protected static final int DEFAULT_URL_CONNECTION_TIMEOUT = 10000; // 10 seconds	
	protected static final int DEFAULT_URL_READ_TIMEOUT = 10000; // 10 seconds
	protected static final int POOL_MAX_IDLE_TIME = 20; // 20 seconds
	
    /**
     * Trust bundle repo
     */
	protected TrustBundleRepository bundleRepo;
	
	protected TrustBundleAnchorRepository bundleAnchorRepo;
	
	protected SslContext sslContext;
	
    static
    {
    	initJVMParams();
    	
    	CryptoExtensions.registerJCEProviders();
    }
	
    /**
     * Initializes system preferences using the Direct {@link OptionsManager} pattern.
     */
	public synchronized static void initJVMParams()
	{
		
		final Map<String, String> JVM_PARAMS = new HashMap<String, String>();
		JVM_PARAMS.put(BUNDLE_REFRESH_PROCESSOR_ALLOW_DOWNLOAD_FROM_UNTRUSTED, "org.nhindirect.config.processor.impl.bundlerefresh.AllowNonVerifiedSSL");	
		
		OptionsManager.addInitParameters(JVM_PARAMS);
	}    
    
	/**
	 * Default constructor.
	 */
	@SneakyThrows
	public DefaultBundleRefreshProcessorImpl()
	{
		OptionsParameter allowNonVerSSLParam = OptionsManager.getInstance().getParameter(BUNDLE_REFRESH_PROCESSOR_ALLOW_DOWNLOAD_FROM_UNTRUSTED);
		///CLOVER:OFF
		final SslContextBuilder sslBuilder  = SslContextBuilder
        .forClient();

		if (OptionsParameter.getParamValueAsBoolean(allowNonVerSSLParam, false))
			sslBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
			
		sslContext = sslBuilder.build();

		///CLOVER:ON
	}
	
	/**
	 * Sets the trust bundle repository for updating the bundle storage medium.
	 * @param bundleRepo The trust bundle repository
	 * @deprecated
	 */
	public void setRepository(TrustBundleRepository bundleRepo)
	{
		this.bundleRepo = bundleRepo;
	}
	
	/**
	 * Sets the trust bundle repositories for updating the bundle storage medium.
	 * @param bundleRepo The trust bundle repository
	 * @param bundleAnchorRepo The trust bundle anchor repository
	 */
	public void setRepositories(TrustBundleRepository bundleRepo, TrustBundleAnchorRepository bundleAnchorRepo)
	{
		this.bundleRepo = bundleRepo;
		this.bundleAnchorRepo = bundleAnchorRepo;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Mono<?> refreshBundle(TrustBundle bundle)
	{
		// track when the process started
		final LocalDateTime processAttempStart = LocalDateTime.now();

		// get the bundle from the URL
		return downloadBundleToByteArray(bundle, processAttempStart)
			.flatMap(rawBundle -> 
			{
				if (rawBundle == null || rawBundle.length == 0)
					return Mono.empty();
				
				// check to see if there is a difference in the anchor sets
				// use a checksum 
				boolean update = false;
				String checkSum = "";
				if (StringUtils.isBlank(bundle.getCheckSum()))
					// never got a check sum... 
					update = true;
				else
				{
					try
					{
						checkSum = BundleThumbprint.toThumbprint(rawBundle).toString();
						update = !bundle.getCheckSum().equals(BundleThumbprint.toThumbprint(rawBundle).toString());
						if (update) {
							log.info("Detected a change in bundle [{}] (old checksum: {}; new checkSum: {})!", bundle.getBundleName(), bundle.getCheckSum(), checkSum);
						}
					}
					///CLOVER:OFF
					catch (NoSuchAlgorithmException ex)
					{
						bundle.setLastRefreshAttempt(processAttempStart);
						bundle.setLastRefreshError(BundleRefreshError.INVALID_BUNDLE_FORMAT.ordinal());
						
						log.error("Failed to generate downloaded bundle thumbprint", ex);
						
						return bundleRepo.save(bundle);
						
					}	
					///CLOVER:ON
				}
				
				final String finalCheckSum = checkSum;
				
				if (!update)
				{
					// bundle was not updated, but mark it as a successful refresh
					bundle.setLastRefreshAttempt(processAttempStart);
					bundle.setLastSuccessfulRefresh(LocalDateTime.now());
					bundle.setLastRefreshError(BundleRefreshError.SUCCESS.ordinal());
					return bundleRepo.save(bundle);
				}
				
				return convertRawBundleToAnchorCollection(rawBundle, bundle, processAttempStart)
					.flatMap(bundleCerts ->
				{
					if (bundleCerts == null || bundleCerts.isEmpty())
						return Mono.empty();
					
					final HashSet<X509Certificate> downloadedSet = new HashSet<X509Certificate>((Collection<X509Certificate>)bundleCerts);	
	
	
					final Collection<TrustBundleAnchor> newAnchors = new ArrayList<TrustBundleAnchor>();
					for (X509Certificate downloadedAnchor : downloadedSet)
					{
						try
						{
							final TrustBundleAnchor anchorToAdd = new TrustBundleAnchor();
							anchorToAdd.setData(downloadedAnchor.getEncoded());
							anchorToAdd.setTrustBundleId(bundle.getId());
							
							newAnchors.add(anchorToAdd);
						}
						///CLOVER:OFF
						catch (Exception e) 
						{ 
							log.warn("Failed to convert downloaded anchor to byte array.", e);
							return Mono.empty();
						}
						///CLOVER:ON
					}
	
		    		return bundleAnchorRepo.deleteByTrustBundleId(bundle.getId())
		    		.then(bundleAnchorRepo.saveAll(newAnchors).collectList())
		    		.flatMap(res -> 
		    		{
						bundle.setLastRefreshAttempt(processAttempStart);
						bundle.setLastRefreshError(BundleRefreshError.SUCCESS.ordinal());
						bundle.setCheckSum(finalCheckSum);
						bundle.setLastSuccessfulRefresh(LocalDateTime.now());
						
						return bundleRepo.save(bundle)
							.doOnSuccess(savedBundle -> {
								log.info("successfully refreshed bundle {}", bundle.getBundleName());
							})
							.onErrorResume(ex -> 
							{
								log.error("Failed to write updated bundle anchors to data store", ex);
								
							
								bundle.setLastRefreshAttempt(processAttempStart);
								bundle.setLastRefreshError(BundleRefreshError.INVALID_BUNDLE_FORMAT.ordinal());
								return bundleRepo.save(bundle);
								
							});	
		    		});
				});

			});
    }
	
	/**
	 * Converts a trust raw trust bundle byte array into a collection of {@link X509Certificate} objects.
	 * @param rawBundle The raw representation of the bundle.  This generally the raw byte string downloaded from the bundle's URL.
	 * @param existingBundle The configured bundle object in the DAO.  This object may contain the signing certificate
	 * used for bundle authenticity checking.
	 * @param processAttempStart The time that the update process started.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	protected Mono<Collection<X509Certificate>> convertRawBundleToAnchorCollection(byte[] rawBundle, final TrustBundle existingBundle,
			final LocalDateTime processAttempStart)
	{
		Collection<? extends Certificate> bundleCerts = null;
		InputStream inStream = null;
		// check to see if its an unsigned PKCS7 container
		try
		{
			inStream = new ByteArrayInputStream(rawBundle);
			bundleCerts = CertificateFactory.getInstance("X.509").generateCertificates(inStream);
			
			// in Java 7, an invalid bundle may be returned as a null instead of throw an exception
			// if its null and has no anchors, then try again as a signed bundle
			if (bundleCerts != null && bundleCerts.size() == 0)
				bundleCerts = null;
			
		}
		catch (Exception e)
		{
			/* no-op for now.... this may not be a p7b, so try it as a signed message*/
		}
		finally
		{
			IOUtils.closeQuietly(inStream);
		}
		
		// didnt work... try again as a CMS signed message
		if (bundleCerts == null)
		{
			try
			{
				final CMSSignedData signed = new CMSSignedData(rawBundle);
				
				// if there is a signing certificate assigned to the bundle,
				// then verify the signature
				if (existingBundle.getSigningCertificateData() != null)
				{
					boolean sigVerified = false;
					
					
					final X509Certificate signingCert = existingBundle.toSigningCertificate();
		    		for (SignerInformation sigInfo : (Collection<SignerInformation>)signed.getSignerInfos().getSigners())	
		    		{
		    			
		    			try
		    			{
				    		if (sigInfo.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(CryptoExtensions.getJCEProviderName()).build(signingCert)))
				    		{
				    			sigVerified = true;
				    			break;
				    		}
		    			}
		    			catch (Exception e) {/* no-op... can't verify */}
		    		}
		    		
		    		if (!sigVerified)
		    		{
		    			existingBundle.setLastRefreshAttempt(processAttempStart);
		    			existingBundle.setLastRefreshError(BundleRefreshError.UNMATCHED_SIGNATURE.ordinal());
		    			log.warn("Downloaded bundle signature did not match configured signing certificate.");
		    			
		    			return bundleRepo.save(existingBundle).thenReturn(Collections.emptyList());
						
		    		}
				}
				
				final CMSProcessableByteArray signedContent = (CMSProcessableByteArray)signed.getSignedContent();
				
				inStream = new ByteArrayInputStream((byte[])signedContent.getContent());
				
				bundleCerts = CertificateFactory.getInstance("X.509").generateCertificates(inStream);
			}
			catch (Exception e)
			{
    			existingBundle.setLastRefreshAttempt(processAttempStart);
    			existingBundle.setLastRefreshError(BundleRefreshError.INVALID_BUNDLE_FORMAT.ordinal());
    			
    			log.warn("Failed to extract anchors from downloaded bundle at URL " + existingBundle.getBundleURL());
    			
    			return bundleRepo.save(existingBundle).thenReturn(Collections.emptyList());
				
			}
			finally
			{
				IOUtils.closeQuietly(inStream);
			}
		}
		
		return Mono.just((Collection<X509Certificate>)bundleCerts);
	}
	
	/**
	 * Downloads a bundle from the bundle's URL and returns the result as a byte array.
	 * @param bundle The bundle that will be downloaded.
	 * @param processAttempStart The time that the update process started. 
	 * @return A byte array representing the raw data of the bundle.
	 */
	protected Mono<byte[]> downloadBundleToByteArray(TrustBundle bundle, LocalDateTime processAttempStart)
	{
		try
		{
			final URI uri = new URI(bundle.getBundleURL());
			
			if (uri.getScheme().compareToIgnoreCase("file") == 0)
			{
				// file scheme URIs are used by unit tests
				final ByteArrayOutputStream ouStream = new ByteArrayOutputStream();

				
				final URL certURL = new URL(bundle.getBundleURL());
				
				final URLConnection connection = certURL.openConnection();
				
				// open the URL as in input stream
				InputStream inputStream = connection.getInputStream();
				
				int BUF_SIZE = 2048;		
				int count = 0;

				final byte buf[] = new byte[BUF_SIZE];
				
				while ((count = inputStream.read(buf)) > -1)
				{
					ouStream.write(buf, 0, count);
				}
				
				return Mono.just(ouStream.toByteArray());

			}
			else
			{
				// this custom connection provider with a configured pool max idle time prevents
				// "Connection reset by peer" errors
				ConnectionProvider provider = ConnectionProvider.builder("custom")
						.maxIdleTime(Duration.ofSeconds(POOL_MAX_IDLE_TIME)).build();
				final HttpClient httpClient = HttpClient.create(provider)
						  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, DEFAULT_URL_CONNECTION_TIMEOUT)
						  .secure(t -> t.sslContext(sslContext))
						  .responseTimeout(Duration.ofMillis(DEFAULT_URL_CONNECTION_TIMEOUT))
						  .doOnConnected(conn -> 
						    conn.addHandlerLast(new ReadTimeoutHandler(DEFAULT_URL_READ_TIMEOUT, TimeUnit.MILLISECONDS))
						      .addHandlerLast(new WriteTimeoutHandler(DEFAULT_URL_READ_TIMEOUT, TimeUnit.MILLISECONDS)));
				
				return WebClient.builder().baseUrl(bundle.getBundleURL())
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build()
				.get()
		        .exchangeToMono(response -> response.bodyToMono(ByteArrayResource.class))
		        .map(ByteArrayResource::getByteArray)
		        .onErrorResume(ex -> 
		        {
		        	log.warn("Failed to download bundle from URL {}", bundle.getBundleURL(), ex);
		        	
					bundle.setLastRefreshAttempt(processAttempStart);
					bundle.setLastRefreshError(BundleRefreshError.DOWNLOAD_TIMEOUT.ordinal());
					return bundleRepo.save(bundle)
							.then(Mono.empty());
		
		        });
			}
		}
		catch (Exception e )
		{
			log.warn("Failed to download bundle from URL {}", bundle.getBundleURL(), e);
			
			bundle.setLastRefreshAttempt(processAttempStart);
			bundle.setLastRefreshError(BundleRefreshError.NOT_FOUND.ordinal());
			return bundleRepo.save(bundle)
					.then(Mono.empty());
			
		}


	}
}
