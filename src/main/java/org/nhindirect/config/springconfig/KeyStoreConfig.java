package org.nhindirect.config.springconfig;

import org.nhindirect.common.crypto.KeyStoreProtectionManager;
import org.nhindirect.common.crypto.impl.BootstrappedKeyStoreProtectionManager;
import org.nhindirect.common.crypto.impl.BootstrappedPKCS11Credential;
import org.nhindirect.common.crypto.impl.StaticCachedPKCS11TokenKeyStoreProtectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeyStoreConfig
{
	  private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreConfig.class);	
	
	  @Value("${direct.config.keystore.keyStorePin:som3randomp!n}")	
	  private String keyStorePin;
	  
	  @Value("${direct.config.keystore.keyStoreType:Luna}")	
	  private String keyStoreType;
	  
	  @Value("${direct.config.keystore.keyStoreSourceAsString:slot:0}")	
	  private String keyStoreSourceAsString;
	  
	  @Value("${direct.config.keystore.keyStoreProviderName:com.safenetinc.luna.provider.LunaProvider}")	
	  private String keyStoreProviderName;
	  
	  @Value("${direct.config.keystore.keyStorePassPhraseAlias:keyStorePassPhrase}")	
	  private String keyStorePassPhraseAlias;
	  
	  @Value("${direct.config.keystore.privateKeyPassPhraseAlias:privateKeyPassPhrase}")	
	  private String privateKeyPassPhraseAlias;

	  @Value("${direct.config.keystore.initOnStart:true}")	
	  private String initOnStart;	  
	  
	  @Value("${direct.config.keystore.keyStorePassPhrase:H1TBr0s!}")	
	  private String keyStorePassPhrase;	  
	  
	  @Value("${direct.config.keystore.privateKeyPassPhrase:H1TCh1ckS!}")	
	  private String privateKeyPassPhrase;	
	  
	  @Bean	  
	  @ConditionalOnProperty(name="direct.config.keystore.hsmpresent", havingValue="true")
	  public KeyStoreProtectionManager hsmKeyStoreProtectionManager()
	  {
		  LOGGER.info("HSM configured.  Attempting to connect to device.");
		  
		  try
		  {
			  final BootstrappedPKCS11Credential cred = new BootstrappedPKCS11Credential(keyStorePin);
			  final StaticCachedPKCS11TokenKeyStoreProtectionManager mgr = new StaticCachedPKCS11TokenKeyStoreProtectionManager();
			  mgr.setCredential(cred);
			  mgr.setKeyStoreType(keyStoreType);
			  mgr.setKeyStoreSourceAsString(keyStoreSourceAsString);
			  mgr.setKeyStoreProviderName(keyStoreProviderName);
			  mgr.setKeyStorePassPhraseAlias(keyStorePassPhraseAlias);
			  mgr.setPrivateKeyPassPhraseAlias(privateKeyPassPhraseAlias);
			  
			  if (Boolean.parseBoolean(initOnStart))
				  mgr.initTokenStore();
			  
			  return mgr;
		  }
		  catch (Exception e)
		  {
			   throw new RuntimeException(e);
		  }
	  }
}
