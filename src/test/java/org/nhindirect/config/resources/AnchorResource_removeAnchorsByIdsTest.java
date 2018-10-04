package org.nhindirect.config.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.SpringBaseTest;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.repository.AnchorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class AnchorResource_removeAnchorsByIdsTest extends SpringBaseTest
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
		
		protected abstract Collection<Long> getIdsToRemove();
		
		
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
		
			final Collection<Long> ids = getIdsToRemove();
			StringBuilder builder = new StringBuilder();
			int cnt = 0;
			for (Long id : ids)
			{
				builder.append(id);
				if (cnt < ids.size() - 1)
					builder.append(",");
				
				++cnt;
			}

			final ResponseEntity<?> resp = 
					testRestTemplate.exchange("/anchor/ids/" + builder.toString(), HttpMethod.DELETE, null, Void.class);
				
			if (resp.getStatusCodeValue() != 200)
				throw new HttpClientErrorException(resp.getStatusCode());
			
			doAssertions();
		}
		
		
		protected void doAssertions() throws Exception
		{
			
		}
	}	
	
	@Test
	public void testRemoveAnchorsByIds_removeExistingAnchors_assertAnchorRemoved() throws Exception
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
					anchor.setOwner("test.com");
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
			protected Collection<Long> getIdsToRemove()
			{
				final Collection<org.nhindirect.config.store.Anchor> anchors = anchorRepo.findAll();
				
				final Collection<Long> ids = new ArrayList<Long>();
				for (org.nhindirect.config.store.Anchor anchor : anchors)
					ids.add(anchor.getId());
				
				return ids;
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final Collection<org.nhindirect.config.store.Anchor> anchors = anchorRepo.findAll();
				assertTrue(anchors.isEmpty());
			}
		}.perform();
	}	
	
	@Test
	public void testRemoveAnchorsByIds_removeSingleAnchor_assertAnchorRemoved() throws Exception
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
					anchor.setOwner("test.com");
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
			protected Collection<Long> getIdsToRemove()
			{
				final Collection<org.nhindirect.config.store.Anchor> anchors = anchorRepo.findAll();
				
				final Collection<Long> ids = new ArrayList<Long>();

				ids.add(anchors.iterator().next().getId());
				
				return ids;
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final Collection<org.nhindirect.config.store.Anchor> anchors = anchorRepo.findAll();
				assertEquals(1, anchors.size());
			}
		}.perform();
	}	
	
	@Test
	public void testRemoveAnchorsByIds_errorInDelete_assertServerError() throws Exception
	{
		new TestPlan()
		{			
			@SuppressWarnings("unchecked")
			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();

					AnchorRepository mockDAO = mock(AnchorRepository.class);
					doThrow(new RuntimeException()).when(mockDAO).deleteByIdIn((List<Long>)any());
					
					anchorService.setAnchorRepository(mockDAO);
				}
				catch (Throwable t)
				{
					throw new RuntimeException(t);
				}
			}
			
			@Override
			protected Collection<Anchor> getAnchorsToAdd()
			{
				return null;
			}
			
			@Override
			protected void tearDownMocks()
			{
				super.tearDownMocks();
				
				anchorService.setAnchorRepository(anchorRepo);
			}
			
			@Override
			protected Collection<Long> getIdsToRemove()
			{
				
				final Collection<Long> ids = new ArrayList<Long>();
				ids.add(12345L);
				
				return ids;
			}
			
			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof HttpClientErrorException);
				HttpClientErrorException ex = (HttpClientErrorException)exception;
				assertEquals(500, ex.getRawStatusCode());
			}
		}.perform();
	}	
}
