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
package org.jboss.bqt.framework;

import java.util.Properties;

import org.jboss.bqt.framework.transaction.JNDITransaction;
import org.jboss.bqt.framework.transaction.LocalTransaction;
import org.jboss.bqt.framework.transaction.OnWrapTransaction;
import org.jboss.bqt.framework.transaction.UseDefaultTransaction;


/**
 * TransactionFactory is used so that the type of {@link TransactionAPI }
 * can be dynamically loaded based on a property.
 * 
 * Specify the property {@link #TRANSACTION_TYPE} in order to set the
 * transaction type to use.
 * 
 * @author vanhalbert
 * 
 */
public class TransactionFactory {

	/**
	 * Transaction Type indicates the type of transaction container to use
	 * 
	 * @see TransactionFactory
	 */
	public static final String TRANSACTION_TYPE = "transaction-option"; //$NON-NLS-1$
	public static interface TRANSACTION_TYPES {
		public static final String LOCAL_TRANSACTION = "local"; //$NON-NLS-1$
//		public static final String XATRANSACTION = "xa"; //$NON-NLS-1$
		public static final String JNDI_TRANSACTION = "jndi"; //$NON-NLS-1$
//		public static final String OFFWRAP_TRANSACTION = "off"; //$NON-NLS-1$
		public static final String ONWRAP_TRANSACTION = "on"; //$NON-NLS-1$
		public static final String USEDEFAULT_TRANSACTION = "auto"; //$NON-NLS-1$
	}
	private TransactionFactory() {
	}

	public static TransactionAPI create(Properties props)  {
		TransactionAPI transacton = null;

		String type = props.getProperty(TRANSACTION_TYPE);
		if (type == null) {
            transacton = new UseDefaultTransaction();
            FrameworkPlugin.LOGGER.debug("====  Create UseDefaultTransaction-Option: not defined");
		} else {

            FrameworkPlugin.LOGGER.debug("====  Create Transaction-Option: " + type);

            if (type.equalsIgnoreCase(TRANSACTION_TYPES.LOCAL_TRANSACTION)) {
                transacton = new LocalTransaction();
    //		} else if (type.equalsIgnoreCase(TRANSACTION_TYPES.XATRANSACTION)) {
    //			transacton = new XATransaction();
            } else if (type.equalsIgnoreCase(TRANSACTION_TYPES.JNDI_TRANSACTION)) {
                transacton = new JNDITransaction();
            } else if (type.equalsIgnoreCase(TRANSACTION_TYPES.ONWRAP_TRANSACTION)) {
                transacton = new OnWrapTransaction();

            } else {
            	 transacton = new UseDefaultTransaction();
            }
        }
		
		FrameworkPlugin.LOGGER.info("====  TransactionFactory: "
				+ transacton.getClass().getName() + " option:" + (type==null?TRANSACTION_TYPES.USEDEFAULT_TRANSACTION:type));
		return transacton;
	}

}
