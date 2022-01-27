package org.nhindirect.config;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;

public class TestUtils 
{
	private static final String certsBasePath = "certs/"; 
	private static final String signerBasePath = "signers/"; 
	private static final String bundleBasePath = "bundles/"; 
	
	public static byte[] loadBundle(String bundleFileName) throws Exception
	{
		return IOUtils.resourceToByteArray(bundleBasePath + bundleFileName, TestUtils.class.getClassLoader());
	}
	
	public static X509Certificate loadCert(String certFileName) throws Exception
	{
		return fromFile(certsBasePath, certFileName);
	}	
	
	public static X509Certificate loadSigner(String authorityFileName) throws Exception
	{
		return fromFile(signerBasePath, authorityFileName);
	}		
	
	protected static final X509Certificate fromFile(String base, String file) throws Exception
	{
		try (final InputStream data = TestUtils.class.getClassLoader().getResourceAsStream(base + file))
		{
			X509Certificate retVal = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(data);
			return retVal;
		}
		finally
		{
		}
	}
}
