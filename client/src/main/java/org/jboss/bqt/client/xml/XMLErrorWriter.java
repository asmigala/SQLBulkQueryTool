/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.jboss.bqt.client.xml;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.jboss.bqt.client.ClientPlugin;
import org.jboss.bqt.client.TestProperties;
import org.jboss.bqt.client.api.ErrorWriter;
import org.jboss.bqt.client.api.ExpectedResults;
import org.jboss.bqt.client.api.QueryScenario;
import org.jboss.bqt.client.util.BQTUtil;
import org.jboss.bqt.core.exception.FrameworkException;
import org.jboss.bqt.core.exception.FrameworkRuntimeException;
import org.jboss.bqt.core.exception.QueryTestFailedException;
import org.jboss.bqt.core.util.ExceptionUtil;
import org.jboss.bqt.core.util.FileUtils;
import org.jboss.bqt.core.util.StringHelper;
import org.jboss.bqt.core.xml.JdomHelper;
import org.jboss.bqt.framework.AbstractQuery;
import org.jboss.bqt.framework.TestCase;
import org.jboss.bqt.framework.TestResult;
import org.jboss.bqt.framework.TransactionAPI;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.IllegalDataException;
import org.jdom2.JDOMException;
import org.jdom2.input.JDOMParseException;
import org.jdom2.output.XMLOutputter;

/**
 * @author vhalbert
 *
 */
public class XMLErrorWriter extends ErrorWriter {
	
	private String errorDirectory = null;
	
	
	
	public XMLErrorWriter(QueryScenario scenario, Properties props) {
		super(scenario, props);

		errorDirectory = props.getProperty(TestProperties.PROP_ERRORS_DIR);
		if (errorDirectory == null) {
			BQTUtil.throwInvalidProperty(TestProperties.PROP_ERRORS_DIR);
		}

		File d = new File(this.errorDirectory);		
		if (d.exists()) {
			FileUtils.removeDirectoryAndChildren(d);
		}
		if (!d.exists()) {
			d.mkdirs();
		}	
	}


	@Override
	public String getErrorDirectory() {
		return errorDirectory;
	}
	
	@Override
	public 	String generateErrorFile(final TestResult testResult, final List<Throwable> failures)
				throws FrameworkException {

		String errorFileName = null;
			// write actual results to error file
			errorFileName = this.getQueryScenario().getFileType().getErrorFileName(this.getQueryScenario(), testResult);
				//generateErrorFileName(queryID, querySetID);
			// configID, queryID, Integer.toString(clientID));
			//           CombinedTestClient.log("\t" + this.clientID + ": Writing error file with actual results: " + errorFileName); //$NON-NLS-1$ //$NON-NLS-2$
			File errorFile = new File(getErrorDirectory(), errorFileName);
			ClientPlugin.LOGGER.warn("**** Generate Error File: " + errorFile.getAbsolutePath());

			generateErrorResults(testResult,
					 (String) null, errorFile, (ResultSet) null, (File) null, failures);
		
		return errorFileName;
	}

	@Override
	public String generateErrorFile(TestCase testCase, ExpectedResults expectedResults, TransactionAPI transaction, List<Throwable> failures) throws QueryTestFailedException, FrameworkException {

		if (expectedResults == null) {
			return generateErrorFile(testCase.getTestResult(), failures);
		}

		
		ResultSet resultSet = ((AbstractQuery) transaction).getResultSet();
		
		String errorFileName = null;
		try {
			TestResult testResult = testCase.getTestResult();
			// write actual results to error file
			errorFileName = this.getQueryScenario().getFileType().getErrorFileName(this.getQueryScenario(), testResult);

			// configID, queryID, Integer.toString(clientID));
			//           CombinedTestClient.log("\t" + this.clientID + ": Writing error file with actual results: " + errorFileName); //$NON-NLS-1$ //$NON-NLS-2$
			File errorFile = new File(getErrorDirectory(), errorFileName);

			// rewind resultset
			if (resultSet != null) {
				resultSet.beforeFirst();
			}
			ClientPlugin.LOGGER.warn("**** E 2 Generate Error File");
		
			generateErrorResults(testResult, testResult.getQuery(), errorFile,
					resultSet, expectedResults.getExpectedResultsFile() , failures);

		} catch (SQLException sqle) {
			throw new QueryTestFailedException(sqle);
		} catch (FrameworkException fre) {
			throw fre;
		} catch (FrameworkRuntimeException e) {
			throw e;
		}
		return errorFileName;
	}

		/**
		 * GenerateExpectedResults an error file for a query that failed comparison. File should
		 * have the SQL, the actual results returned from the server and the results
		 * that were expected.
		 * @param testResult
		 * @param sql
		 * @param resultsFile
		 * @param actualResult
		 * @param expectedResultFile
		 * @param ex
		 * @throws FrameworkException
		 */
		private void generateErrorResults(TestResult testResult,
				String sql, File resultsFile, ResultSet actualResult,
				File expectedResultFile, List<Throwable> failures)
				throws FrameworkException {
			OutputStream outputStream;
			try {
				FileOutputStream fos = new FileOutputStream(resultsFile);
				outputStream = new BufferedOutputStream(fos);
			} catch (IOException e) {
				throw new FrameworkRuntimeException(
						"Failed to open error results file: " + resultsFile.getPath() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			}

			try {
				XMLQueryVisitationStrategy jstrat = new XMLQueryVisitationStrategy();

				// Create root JDOM element
				Element rootElement = new Element(TagNames.Elements.ROOT_ELEMENT);

				// create a JDOM element for the results
				Element resultElement = new Element(TagNames.Elements.QUERY_RESULTS);
				// set the queryIDAttr on the exception element
				resultElement.setAttribute(new Attribute(TagNames.Attributes.NAME,
						testResult.getQueryID()));
				// set the querySQLAttr on the exception element
				resultElement.setAttribute(new Attribute(TagNames.Attributes.VALUE,
						(sql != null ? sql : "NULL")));

				if (failures != null) {
					for (Throwable failure : failures) {
						Element failureEl = new Element(TagNames.Elements.FAILURE_MESSAGE);
						try {
							failureEl.setText(failure.getMessage());
						} catch(IllegalDataException e) {
							// unprintable characters, replace them
							failureEl.setText(StringHelper.replaceXmlUnprintable(failure.getMessage()));
						}
						resultElement.addContent(failureEl);
					}
				}

				// ---------------------
				// Actual Exception
				// ---------------------
				// create a JDOM element from the actual exception object
				// produce xml for the actualException and this to the
				// exceptionElement
				Throwable actualError = testResult.getException();
				if (actualError != null) {
					
					ClientPlugin.LOGGER.warn("**** E 3 Generate Error File");

					Element actualExceptionElement = new Element(
							TagNames.Elements.ACTUAL_EXCEPTION);

					actualExceptionElement = XMLQueryVisitationStrategy
							.jdomException(actualError, actualExceptionElement);
					resultElement.addContent(actualExceptionElement);
				} else if (actualResult != null) {
					ClientPlugin.LOGGER.warn("**** E 4 Generate Error File");

					// ------------------------------
					// Got a ResultSet from server
					// error was in comparing results
					// ------------------------------

					// --------------------------
					// Actual Result - ResultSet
					// --------------------------
					// produce a JDOM element from the actual results object
					Element actualResultsElement = new Element(
							TagNames.Elements.ACTUAL_QUERY_RESULTS);
					actualResultsElement = jstrat.produceMsg(actualResult,
							actualResultsElement);

					// add the results elements to the root element
					resultElement.addContent(actualResultsElement);

				} 
				
				if (expectedResultFile != null) {
				// ---------------------
				// Expected Results - ...
				// ---------------------
				// produce xml for the expected results
				// Get expected results
					Element expectedResult = new Element("bogus"); //$NON-NLS-1$
				
					try {

						expectedResult = jstrat.parseXMLResultsFile(expectedResultFile,
								expectedResult);
						ClientPlugin.LOGGER.warn("**** E 5 Expected: " + expectedResult);

						if (expectedResult.getChild(TagNames.Elements.CLASS) == null) { // exception-class element not found

							expectedResult
							.setName(TagNames.Elements.EXPECTED_QUERY_RESULTS);
							
						} else {
							
							expectedResult
							.setName(TagNames.Elements.EXPECTED_EXCEPTION);
							
						}
						
//						if (expectedResult.getChild(TagNames.Elements.SELECT) != null) {
//							// ----------------------------------------------------------
//							// -
//							// Expected result was a ResultSet set element name to
//							// reflect
//							// ----------------------------------------------------------
//							// -
//							expectedResult
//									.setName(TagNames.Elements.EXPECTED_QUERY_RESULTS);
//						} else {
//							// ----------------------------------------------------------
//							// --
//							// Expected result was an exception set element name to
//							// reflect
//							// ----------------------------------------------------------
//							// --
//							expectedResult
//									.setName(TagNames.Elements.EXPECTED_EXCEPTION);
//						}
						
						resultElement.addContent(expectedResult);
	
					} catch (Throwable jdomerror) {
						jstrat.produceMsg(jdomerror, resultElement);
					}
				}

				// ------------------------------
				// Got an exeption from the server
				// error was in comparing exceptions
				// ------------------------------

				// add the results elements to the root element
				rootElement.addContent(resultElement);
				ClientPlugin.LOGGER.warn("**** E 6 Generate Error File");

				// Output xml
				XMLOutputter outputter = new XMLOutputter(JdomHelper.getFormat(
						"  ", true)); //$NON-NLS-1$
				outputter.output(new Document(rootElement), outputStream);
			} catch (JDOMParseException jde) {
				ClientPlugin.LOGGER.warn("**** JDOMERROR Generate Error File");

				jde.printStackTrace();

			} catch (SQLException e) {
				throw new FrameworkException(
						"Failed to convert error results to JDOM: " + e.getMessage()); //$NON-NLS-1$
			} catch (JDOMException e) {
				throw new FrameworkException(
						"Failed to convert error results to JDOM: " + e.getMessage()); //$NON-NLS-1$
			} catch (IOException e) {
				throw new FrameworkException(
						"Failed to output error results to " + resultsFile.getPath() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (Throwable e) {
				throw new FrameworkException(
						"Failed to convert error results to JDOM: " + ExceptionUtil.getStackTrace(e)); //$NON-NLS-1$
			} finally {
				try {
					outputStream.close();
				} catch (IOException e) {
				}
			}
		}

	/**
	 * Creates file with all failure messages, for cases when there were more than one failure detected.
	 * @see org.jboss.bqt.client.api.ErrorWriter#generateErrorMessagesFile(org.jboss.bqt.framework.TestResult,
	 *      java.util.List)
	 */
	@Override
	public String generateErrorMessagesFile(TestResult testResult, List<Throwable> failures) throws FrameworkException {
		String messagesFileName = getQueryScenario().getFileType().getErrorMessagesFileName(getQueryScenario(),
				testResult);

		File messagesFile = new File(getErrorDirectory(), messagesFileName);
		ClientPlugin.LOGGER.warn("**** Generate Additional Error Messages File: " + messagesFile.getAbsolutePath());

		generateErrorMessages(messagesFile, failures);

		return messagesFileName;
	}

	/**
	 * Writes list of failure messages to specified file.
	 * @param messagesFile file with write permissions
	 * @param failures list of failures-exceptions
	 * @throws FrameworkException in case of any I/O error, possibly missing permissions or disk access failure
	 */
	private void generateErrorMessages(File messagesFile, List<Throwable> failures) throws FrameworkException {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(messagesFile));

			for (Throwable failure : failures) {
				bw.write(failure.getMessage() + '\n');
			}
		} catch (IOException e) {
			throw new FrameworkException("Failed to output error results to " + messagesFile.getPath() + ": "
					+ e.getMessage());
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					ClientPlugin.LOGGER.error("Cannot close the Additional Error Messages File: " + e.getMessage());
				}
			}
		}
	}

}
