package org.nhindirect.config.springconfig;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ClassUtils;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("org.nhindirect.config.repository")
public class EntityManagerConfig
{
	@Autowired
	protected DataSource datasource;
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory()
	{		   
		  try
		  {
			 final HibernateJpaVendorAdapter jpaAdaptor = new HibernateJpaVendorAdapter();
			 jpaAdaptor.setGenerateDdl(true);
			 
			 final LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
			 entityManagerFactory.setDataSource(datasource);
			 entityManagerFactory.setPersistenceUnitName("config-store");
			 entityManagerFactory.setJpaVendorAdapter(jpaAdaptor);
			 
			 entityManagerFactory.setPackagesToScan(ClassUtils.getPackageName(org.nhindirect.config.store.Anchor.class));

			 return entityManagerFactory;		
		  }
		  catch (Exception e)
		  {
			  throw new IllegalStateException("Failed to build entity factory manager.", e);
		  }
	}
}
