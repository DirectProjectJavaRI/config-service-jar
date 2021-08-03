package org.nhindirect.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.repository.AnchorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class AnchorResource_getAnchorsTest extends SpringBaseTest
{
	@Autowired
	protected AnchorResource anchorService;
	
	abstract class TestPlan extends BaseTestPlan 
	{		
		@Override
		protected void tearDownMocks()
		{

		}

		protected abstract Collection<Anchor> getAnchorsToAdd();
		
		@Override
		protected void performInner() throws Exception
		{				
			
			final Collection<Anchor> anchorsToAdd = getAnchorsToAdd();
			
			if (anchorsToAdd != null)
			{
				anchorsToAdd.forEach(addAnchor->		
				{
					final HttpEntity<Anchor> requestEntity = new HttpEntity<>(addAnchor);
					final ResponseEntity<Void> resp = testRestTemplate.exchange("/anchor", HttpMethod.PUT, requestEntity, Void.class);
					if (resp.getStatusCodeValue() != 201)
						throw new HttpClientErrorException(resp.getStatusCode());
				});
			}
			
			final Collection<Anchor> getAnchors = webClient.get()
			.uri(uriBuilder ->  uriBuilder.path("/anchor/").build())
			.retrieve().bodyToMono(new ParameterizedTypeReference<Collection<Anchor>>() {}).block();

			doAssertions(getAnchors);
			
		}
			
		protected void doAssertions(Collection<Anchor> anchors) throws Exception
		{
			
		}
	}	
	
	
	@Test
	public void testGetAnchors_assertAnchorsRetrieved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Anchor> anchors;
			
			@Override
			protected Collection<Anchor> getAnchorsToAdd()
			{
				try
				{
					anchors = new ArrayList<Anchor>();
					
					Anchor anchor = new Anchor();
					anchor.setOwner("test.com");
					anchor.setIncoming(true);
					anchor.setOutgoing(true);
					anchor.setStatus(EntityStatus.ENABLED);
					anchor.setCertificateData(TestUtils.loadSigner("bundleSigner.der").getEncoded());
								
					anchors.add(anchor);
					
					
				    anchor = new Anchor();
					anchor.setOwner("test2.com");
					anchor.setIncoming(true);
					anchor.setOutgoing(true);
					anchor.setStatus(EntityStatus.ENABLED);
					anchor.setCertificateData(TestUtils.loadSigner("sm1.direct.com Root CA.der").getEncoded());	
					
					anchors.add(anchor);
					
					return anchors;
				}
				catch (Exception e)
				{
					throw new RuntimeException (e);
				}
			}

			
			@Override
			protected void doAssertions(Collection<Anchor> anchors)
			{
				
				assertNotNull(anchors);
				assertEquals(2, anchors.size());
				
				final Iterator<Anchor> addedAnchorsIter = this.anchors.iterator();
				
				for (Anchor retrievedAnchor : anchors)
				{
					final Anchor addedAnchor = addedAnchorsIter.next(); 
					assertEquals(addedAnchor.getOwner(), retrievedAnchor.getOwner());
					assertEquals(addedAnchor.getAnchorAsX509Certificate(), retrievedAnchor.getAnchorAsX509Certificate());
					assertEquals(addedAnchor.isIncoming(), retrievedAnchor.isIncoming());
					assertEquals(addedAnchor.isOutgoing(), retrievedAnchor.isOutgoing());
					assertEquals(addedAnchor.getStatus(), retrievedAnchor.getStatus());
					assertFalse(retrievedAnchor.getThumbprint().isEmpty());
				}
				
			}
		}.perform();
	}	
	
	@Test
	public void testGetAnchors_noAnchorsInStore_assertNoAnchorsRetrieved() throws Exception
	{
		new TestPlan()
		{
			
			@Override
			protected Collection<Anchor> getAnchorsToAdd()
			{
				return null;
			}

			@Override
			protected void doAssertions(Collection<Anchor> anchors)
			{
				assertNotNull(anchors);
				assertTrue(anchors.isEmpty());
				
			}
		}.perform();
	}	
	
	@Test
	public void testGetAnchors_errorInLookup_assertServerError() throws Exception
	{
		new TestPlan()
		{
			
			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();

					AnchorRepository mockDAO = mock(AnchorRepository.class);
					doThrow(new RuntimeException()).when(mockDAO).findAll();
					
					anchorService.setAnchorRepository(mockDAO);
				}
				catch (Throwable t)
				{
					throw new RuntimeException(t);
				}
			}
			
			@Override
			protected void tearDownMocks()
			{
				super.tearDownMocks();
				
				anchorService.setAnchorRepository(anchorRepo);
			}
			
			@Override
			protected Collection<Anchor> getAnchorsToAdd()
			{
				return null;
			}

			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof WebClientResponseException);
				WebClientResponseException ex = (WebClientResponseException)exception;
				assertEquals(500, ex.getRawStatusCode());
			}
		}.perform();
	}		
}
