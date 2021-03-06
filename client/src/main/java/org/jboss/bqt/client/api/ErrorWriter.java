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

package org.jboss.bqt.client.api;

import java.util.List;
import java.util.Properties;

import org.jboss.bqt.client.TestProperties;
import org.jboss.bqt.core.exception.FrameworkException;
import org.jboss.bqt.core.exception.QueryTestFailedException;
import org.jboss.bqt.framework.TestCase;
import org.jboss.bqt.framework.TestResult;
import org.jboss.bqt.framework.TransactionAPI;


/**
 * The ErrorWriter is responsible for writing out an error file when a query error 
 * occurs.  The file should be written to the location based on  {@link TestProperties#PROP_ERRORS_DIR errorDirectory}.
 *
 * @author vhalbert
 *
 */
public abstract class ErrorWriter {
	
	private Properties properties;
	private QueryScenario scenario;
	
	public ErrorWriter(QueryScenario scenario, Properties props) {
		this.properties = props;
		this.scenario = scenario;
	}
	
	protected Properties getProperties() {
		return properties;
	}
	
	protected QueryScenario getQueryScenario() {
		return scenario;
	}
	
	/**
	 * Returns the full path to the current location that error files
	 * should be written to.
	 * @return String full directory path
	 * 
	 * @see TestProperties#PROP_ERRORS_DIR
	 */
	public abstract String getErrorDirectory();

	/**
	 * Call to generate an error file as the result of incompatibilities in the
	 * comparison of the expected results to the actual results.
	 * @param testCase is for the test that was run 
	 * @param expectedResults , pass in null when generating error on no expected results are available, as 
	 * 			would be when resultmode = GENERATE 
	 * @param transaction is the current transaction being processed
	 * @param failures that will be written in the error file
	 * @return String name for the error file
	 * @throws QueryTestFailedException could be seen if problems occur accessing resultSet
	 * @throws FrameworkException could be seen if problems occur creating error file
	 */
	
	public abstract String generateErrorFile(final TestCase testCase, ExpectedResults expectedResults,
			final TransactionAPI transaction, List<Throwable> failures) throws QueryTestFailedException, FrameworkException;

	
	/**
	 * Call to generate an error file based on an error that occurred.  
	 * @param testResult is the TestResult being run
	 * @param failures
	 * @return String name for the error file
	 * @throws FrameworkException could be seen if problems occur creating error file
	 */
	public abstract String generateErrorFile(final TestResult testResult, final List<Throwable> failures)
			throws FrameworkException;

	/**
	 * Call to generate a file containing all error messages, in case the test resulted in more than one failure.
	 * @param testResult test result being run
	 * @param failures list of exceptions-failures to print out
	 * @return name of the error messages file
	 * @throws FrameworkException when any I/O error occurs, possibly file permissions / disk access errors
	 */
	public abstract String generateErrorMessagesFile(TestResult testResult, List<Throwable> failures)
			throws FrameworkException;

}
