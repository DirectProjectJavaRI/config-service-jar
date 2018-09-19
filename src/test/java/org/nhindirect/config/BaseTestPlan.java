package org.nhindirect.config;

import java.io.File;

public abstract class BaseTestPlan
{	
	static protected String filePrefix;
	
    static
    {

		// check for Windows... it doens't like file://<drive>... turns it into FTP
		File file = new File("./src/test/resources/bundles/signedbundle.p7b");
		if (file.getAbsolutePath().contains(":/"))
			filePrefix = "file:///";
		else
			filePrefix = "file:///";
    }
    
	
	public void perform() throws Exception 
	{
		try 
		{
			setupMocks();
			Exception exception = null;
			try 
			{
				performInner();
			} 
			catch (Exception e) 
			{
				exception = e;
			}
			assertException(exception);
		} 
		finally 
		{
			tearDownMocks();
		}
	}

	protected abstract void performInner() throws Exception;

	protected void setupMocks() {
	}

	protected void tearDownMocks() {
	}

	protected void assertException(Exception exception) throws Exception {
		// default case should not throw an exception
		if (exception != null) {
			throw exception;
		}
	}
}
