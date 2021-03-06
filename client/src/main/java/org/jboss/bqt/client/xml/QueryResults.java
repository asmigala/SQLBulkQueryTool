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

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates results associated with a query.
 * <p>
 * Results are conceptually organized as a table of columns and rows, where the
 * columns are the data fields that were specified in the query select
 * statement, and the rows are individual records returned from the data set.
 * The data values are arbitrary Java objects in each field/record cell.
 * <p>
 * 
 * <pre>
 * 
 * 
 *            Record # |  Field1    Field2    Field3   ...    FieldN
 *           ----------|---------------------------------------------
 *              1      |  Value11   Value12   Value13         Value1N
 *              2      |  Value21   Value22   Value23         Value2N
 *              :      |     :         :         :               :
 *              M      |  ValueM1   ValueM2   ValueM3         ValueMN
 * 
 * 
 * </pre>
 * 
 * <p>
 * Methods are provided to access data by:
 * <p>
 * <ul>
 * <li>Cell value - specify field identifier and record number</li>
 * <li>Field values - specify field identifier</li>
 * <li>Record values - specify record number</li>
 * <li>Record - specify record number; returns field idents mapped to values</li>
 * </ul>
 * <p>
 * Results can be specified to be sorted based on a user-provided ordering. The
 * ordering is a List of ElementSymbols, which should match the identifiers for
 * the results fields. This list will typically be in the order that the
 * parameters were specified in the query select statement. If no ordering list
 * is specified, the order is the same as results fields are added to this
 * object.
 * <p>
 */
public class QueryResults implements Externalizable {

	/**
	 * Serialization ID - this must be changed if this class is no longer
	 * serialization-compatible with old versions.
	 */
	static final long serialVersionUID = 5397138282301824378L;

	/**
	 * The fields in the results object: List of String
	 */
	private List<String> fields;

	/**
	 * The column info for each field: Map of String --> ColumnInfo
	 */
	private Map<String, ColumnInfo> columnInfos;

	/**
	 * The set of results. Each result is keyed off the variable identifier that
	 * was defined in the query's select clause. This field will never be null.
	 */
	private List<List<Object>> records; // Rows of columns: List<List<Object>>

	// =========================================================================
	// C O N S T R U C T O R S
	// =========================================================================

	/**
	 * Construct a default instance of this class.
	 * <p>
	 * The number of fields returned by the {@link #getFieldCount}method will be
	 * 0 after this constructor has completed. The number of records returned by
	 * the {@link #getRecordCount}method will be 0 after this constructor has
	 * completed.
	 * <p>
	 */
	public QueryResults() {
	}

	/**
	 * Construct an instance of this class, specifying the order that the
	 * elements should be inserted into the map. The number of fields returned
	 * by the {@link #getFieldCount}method will be the same as the number of
	 * <code>fields</code> passed in after this constructor has completed. The
	 * number of records returned by the {@link #getRecordCount}method will be 0
	 * after this constructor has completed.
	 * <p>
	 * 
	 * @param fields
	 *            The set of field identifiers that will be in the result set
	 */
	public QueryResults(List<ColumnInfo> fields) {
		this(fields, 0);
	}

	/**
	 * Construct an instance of this class, specifying the fields and the number
	 * of records that the result set should hold. The fields and number of
	 * records are used to pre-allocate memory for all the values that are
	 * expected to be indested into the results set.
	 * <p>
	 * The number of records returned by the {@link #getRecordCount}method will
	 * be <code>numberOfRecords</code> after this constructor has completed. The
	 * number of fields returned by the {@link #getFieldCount}will be the same
	 * as the size of the list of fields passed in after this constructor has
	 * completed.
	 * <p>
	 * 
	 * @param fields
	 *            The ordered list of variables in select statement
	 * @param numberOfRecords
	 *            The number of blank records to create; records will all
	 *            contain <code>null</code> values for all the fields
	 * @see #addField
	 */
	public QueryResults(List<ColumnInfo> fields, int numberOfRecords) {
		if (fields != null) {
			Iterator<ColumnInfo> fieldIter = fields.iterator();
			while (fieldIter.hasNext()) {
				ColumnInfo info = fieldIter.next();
				addField(info);
			}
			for (int k = 0; k < numberOfRecords; k++) {
				addRecord();
			}
		}
	}


	// =========================================================================
	// D A T A   A C C E S S   M E T H O D S
	// =========================================================================

	/**
	 * Returns all the field identifiers. If the parameters in the query select
	 * statement have been provided, then the set of field identifiers should be
	 * a subset of them, and ordered the same.
	 * <p>
	 * This method will never return <code>null</code>. The list of identifiers
	 * returned is not mutable -- changes made to this list will not affect the
	 * QueryResults object.
	 * 
	 * @return The field identifiers
	 */
	public List<String> getFieldIdents() {
		return (fields != null) ? fields : new ArrayList<String>();
	}

	/**
	 * Returns the number of fields in the result set.
	 * 
	 * @return The number of fields
	 */
	public int getFieldCount() {
		return (fields != null) ? fields.size() : 0;
	}

	/**
	 * Returns the number of records in the result set.
	 * <p>
	 * 
	 * @return The number of records
	 */
	public int getRecordCount() {
		return (records != null) ? records.size() : 0;
	}


	/**
	 * Returns the values for the specified record. The values are ordered the
	 * same as the field identifiers in the result set, which will be the same
	 * as the order of the query select parameters if they have been provided.
	 * <p>
	 * The list of values returned is not mutable -- changes made to this list
	 * will not affect the QueryResults object.
	 * <p>
	 * 
	 * @param recordNumber
	 *            The record number
	 * @return A list containing the field values for the specified record,
	 *         ordered according to the original select parameters, if defined
	 * @throws IndexOutOfBoundsException 
	 */
	public List<Object> getRecordValues(int recordNumber)
			throws IndexOutOfBoundsException {

		if (records != null) {
			return records.get(recordNumber);
		}
		throw new IndexOutOfBoundsException("Record number " + recordNumber
				+ " is not valid.");
	}

	/**
	 * Get the records contained in this result. The records are returned as a
	 * list of field values (a list of lists).
	 * 
	 * @return A list of lists contains the field values for each row.
	 */
	public List<List<Object>> getRecords() {
		return records;
	}


	public List<String> getTypes() {
		List<String> typeNames = new ArrayList<String>();

		int nFields = getFieldCount();
		for (int i = 0; i < nFields; i++) {
			String aField = fields.get(i);
			typeNames.add(((ColumnInfo) columnInfos.get(aField)).getDataType());
		}
		return typeNames;
	}

	// =========================================================================
	// D A T A   M A N I P U L A T I O N   M E T H O D S
	// =========================================================================

	/**
	 * Add a new field into this result set. The field will be inserted in the
	 * order of the parameters in the select statement if those parameters were
	 * specified upon construction of the result set; otherwise, the field will
	 * be appended to the result set.
	 * <p>
	 * 
	 * @param info
	 *            The column information.
	 */
	public void addField(ColumnInfo info) {
		// Add to ordered list of fields
		if (fields == null) {
			fields = new ArrayList<String>();
		}
		fields.add(info.getName());

		// Save column information
		if (columnInfos == null) {
			columnInfos = new HashMap<String, ColumnInfo>();
		}
		columnInfos.put(info.getName(), info);

		// Add new field to each record
		if (records != null) {
			for (int i = 0; i < records.size(); i++) {
				List<Object> record = records.get(i);
				record.add(null);
			}
		}
	}

	/**
	 * Add a set of fields into this result set. The fields will be inserted in
	 * the order of the parameters in the select statement if those parameters
	 * were specified upon construction of the result set; otherwise, the field
	 * will be appended to the result set.
	 * <p>
	 * 
	 * @param fields
	 *            The field identifiers.
	 */
	public void addFields(Collection<ColumnInfo> fields) {
		Iterator<ColumnInfo> idents = fields.iterator();
		while (idents.hasNext()) {
			addField(idents.next());
		}
	}

	/**
	 * Add a new record for all fields. The record is populated with all null
	 * values, which act as placeholders for subsequent <code>setValue
	 * </code> calls.
	 * <p>
	 * Before this method is called, the fields must already be defined.
	 * <p>
	 * 
	 * @return The updated number of records
	 */
	public int addRecord() {
		// Create a place-holder record
		int nField = getFieldCount();
		if (nField == 0) {
			throw new IllegalArgumentException(
					"Cannot add record; no fields have been defined");
		}
		// Create a record with all null values, one for each field
		List<Object> record = new ArrayList<Object>(nField);
		for (int j = 0; j < nField; j++) {
			record.add(null);
		}
		return addRecord(record);
	}

	/**
	 * Add a new record for all fields. The record must contain the same number
	 * of values as there are fields.
	 * <p>
	 * Before this method is called, the fields must already be defined.
	 * <p>
	 * @param record 
	 * 
	 * @return The updated number of records
	 */
	public int addRecord(List<Object> record) {
		if (record == null) {
			throw new IllegalArgumentException("Attempt to add null record.");
		}
		if (record.size() != getFieldCount()) {
			throw new IllegalArgumentException("Attempt to add record with "
					+ record.size() + " values when " + getFieldCount()
					+ " fields are defined.");
		}
		if (records == null) {
			records = new ArrayList<List<Object>>();
		}
		records.add(record);
		return records.size();
	}


	// =========================================================================
	// O V E R R I D D E N   O B J E C T   M E T H O D S
	// =========================================================================

	/** Compares with another result set 
	 * @param object 
	 * @return boolean
	 *  
	 */
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!this.getClass().isInstance(object)) {
			return false;
		}

		QueryResults other = (QueryResults) object;

		// First compare fields
		if (!this.getFieldIdents().equals(other.getFieldIdents())) {
			return false;
		}

		List<List<Object>> thisRecords = this.getRecords();
		List<List<Object>> otherRecords = other.getRecords();

		if (thisRecords == null) {
			if (otherRecords == null) {
				return true;
			}
			return false;
		}
		if (otherRecords == null) {
			return false;
		}
		return thisRecords.equals(otherRecords);
	}

	/** Returns a string representation of an instance of this class. 
	 * @return  String
	 * */
	public String toString() {
		StringBuffer buffer = new StringBuffer("Query Results...\n"); //$NON-NLS-1$
		buffer.append(printFieldIdentsAndTypes(this.getFieldIdents(),
				this.columnInfos));
		buffer.append("\n"); //$NON-NLS-1$
		for (int r = 0; r < this.getRecordCount(); r++) {
			buffer.append(r);
			buffer.append(": "); //$NON-NLS-1$

			List<Object> record = this.getRecordValues(r);
			for (int c = 0; c < this.getFieldCount(); c++) {
				buffer.append(record.get(c));
				if (c < this.getFieldCount() - 1) {
					buffer.append(", "); //$NON-NLS-1$
				}
			}
			buffer.append("\n"); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	private static String printFieldIdentsAndTypes(List<String> fieldIdents,
			Map<String, ColumnInfo> columnInfos) {
		StringBuffer buf = new StringBuffer();
		Iterator<String> fieldItr = fieldIdents.iterator();
		while (fieldItr.hasNext()) {
			String aField = fieldItr.next();
			if (aField != null) {
				buf.append("["); //$NON-NLS-1$
				buf.append(aField);
				buf.append(" - ["); //$NON-NLS-1$
				ColumnInfo colInfo = columnInfos.get(aField);
				buf.append(colInfo.getDataType());
				buf.append(", "); //$NON-NLS-1$
				buf.append(colInfo.getJavaClass());
				buf.append("]"); //$NON-NLS-1$
			}
			buf.append("] "); //$NON-NLS-1$
		}

		return buf.toString();
	}

	// =========================================================================
	// S E R I A L I Z A T I O N
	// =========================================================================

	/**
	 * Implements Externalizable interface to read serialized form
	 * 
	 * @param s
	 *            Input stream to serialize from
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public void readExternal(java.io.ObjectInput s)
			throws ClassNotFoundException, IOException {
		int numFields = s.readInt();
		if (numFields > 0) {
			fields = new ArrayList<String>(numFields);
			columnInfos = new HashMap<String, ColumnInfo>();
			for (int i = 0; i < numFields; i++) {
				String fieldName = s.readUTF();
				fields.add(fieldName);

				Object colInfo = s.readObject();
				columnInfos.put(fieldName, (ColumnInfo) colInfo);
			}
		}

		int numRows = s.readInt();
		if (numRows > 0) {
			records = new ArrayList<List<Object>>(numRows);
			for (int row = 0; row < numRows; row++) {
				List<Object> record = new ArrayList<Object>(numFields);
				for (int col = 0; col < numFields; col++) {
					record.add(s.readObject());
				}
				records.add(record);
			}
		}
	}

	/**
	 * Implements Externalizable interface to write serialized form
	 * 
	 * @param s
	 *            Output stream to serialize to
	 * @throws IOException 
	 */
	public void writeExternal(java.io.ObjectOutput s) throws IOException {
		// Write column names and column information
		int numFields = 0;
		if (fields == null) {
			s.writeInt(0);
		} else {
			numFields = fields.size();
			s.writeInt(numFields);
			for (int i = 0; i < numFields; i++) {
				String fieldName = (String) fields.get(i);
				s.writeUTF(fieldName);
				s.writeObject(columnInfos.get(fieldName));
			}
		}

		// Write record data
		if (records == null) {
			s.writeInt(0);
		} else {
			int numRows = records.size();
			s.writeInt(numRows);
			for (int row = 0; row < numRows; row++) {
				List<Object> record = records.get(row);
				for (int col = 0; col < numFields; col++) {
					s.writeObject(record.get(col));
				}
			}
		}
	}

	// =========================================================================
	// I N N E R   C L A S S E S
	// =========================================================================

	/**
	 * Represents all information about a column.
	 */
	public static class ColumnInfo implements Serializable {

		/**
		 * Serialization ID - this must be changed if this class is no longer
		 * serialization-compatible with old versions.
		 */
		static final long serialVersionUID = -7131157612965891051L;

		private String name;
		private String dataType;
		private Class<?> javaClass;

		private Object groupID; // fully qualified group name
		private Object elementID; // short name
		
		public ColumnInfo(String name, String dataType) {
			if (name == null) {
				throw new IllegalArgumentException(
						"QueryResults column cannot have name==null");
			}
			this.name = name;
			this.dataType = dataType;
			this.javaClass = java.lang.String.class;
		}

		public ColumnInfo(String name, String dataType, Class<?> javaClass) {
			if (name == null) {
				throw new IllegalArgumentException(
						"QueryResults column cannot have name==null");
			}
			this.name = name;
			this.dataType = dataType;
			this.javaClass = javaClass;
		}

		public ColumnInfo(String name, String dataType, Class<?> javaClass,
				Object groupID, Object elementID) {
			this(name, dataType, javaClass);
			this.groupID = groupID;
			this.elementID = elementID;
		}

		public String getName() {
			return this.name;
		}

		public String getDataType() {
			return this.dataType;
		}

		public Class<?> getJavaClass() {
			return this.javaClass;
		}

		/**
		 * May be null
		 * @return Object may be null
		 */
		public Object getGroupID() {
			return this.groupID;
		}

		/**
		 * May be null
		 * @return Object may be null
		 */
		public Object getElementID() {
			return this.elementID;
		}

		public String toString() {
			StringBuffer str = new StringBuffer("Column["); //$NON-NLS-1$
			str.append(this.name);
			str.append(", "); //$NON-NLS-1$
			str.append(this.dataType);
			if (this.groupID != null) {
				str.append(", "); //$NON-NLS-1$
				str.append(this.groupID);
				str.append("."); //$NON-NLS-1$
				str.append(this.elementID);
			}
			str.append("]"); //$NON-NLS-1$
			return str.toString();
		}
	}

} // END CLASS
