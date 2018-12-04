package org.nhindirect.config;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nhindirect.config.repository.AddressRepository;
import org.nhindirect.config.repository.AnchorRepository;
import org.nhindirect.config.repository.CertPolicyGroupDomainReltnRepository;
import org.nhindirect.config.repository.CertPolicyGroupRepository;
import org.nhindirect.config.repository.CertPolicyRepository;
import org.nhindirect.config.repository.CertificateRepository;
import org.nhindirect.config.repository.DNSRepository;
import org.nhindirect.config.repository.DomainRepository;
import org.nhindirect.config.repository.SettingRepository;
import org.nhindirect.config.repository.TrustBundleDomainReltnRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource("classpath:bootstrap.properties")
public abstract class SpringBaseTest
{
	protected String filePrefix;
	
	@Autowired
	protected TestRestTemplate testRestTemplate;
	
	@Autowired
	protected AddressRepository addressRepo;
	
	@Autowired
	protected TrustBundleRepository bundleRepo;	
	
	@Autowired
	protected TrustBundleDomainReltnRepository bundleDomainRepo;
	
	@Autowired
	protected DomainRepository domainRepo;
	
	@Autowired
	protected AnchorRepository anchorRepo;
	
	@Autowired
	protected CertificateRepository certRepo;
	
	@Autowired 
	protected DNSRepository dnsRepo;
	
	@Autowired
	protected SettingRepository settingRepo;
	
	@Autowired
	protected CertPolicyRepository policyRepo;
	
	@Autowired
	protected CertPolicyGroupRepository policyGroupRepo;
	
	@Autowired
	protected CertPolicyGroupDomainReltnRepository groupReltnRepo;
	
	@Before
	public void setUp()
	{
		
		// check for Windows... it doens't like file://<drive>... turns it into FTP
		File file = new File("./src/test/resources/bundles/signedbundle.p7b");
		if (file.getAbsolutePath().contains(":/"))
			filePrefix = "file:///";
		else
			filePrefix = "file:///";
		
		try
		{
			cleanDataStore();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		// clean up the file system
		File dir = new File("./target/tempFiles");
		if (dir.exists())
		try
		{
			FileUtils.cleanDirectory(dir);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected void cleanDataStore() throws Exception
	{		
		anchorRepo.deleteAll();
		
		groupReltnRepo.deleteAll();
		
		addressRepo.deleteAll();
		
		bundleDomainRepo.deleteAll();
		
		bundleRepo.deleteAll();
		
		domainRepo.deleteAll();
		
		policyGroupRepo.deleteAll();
		
		policyRepo.deleteAll();
		
		certRepo.deleteAll();
		
		dnsRepo.deleteAll();
		
		settingRepo.deleteAll();
	
	}
}
