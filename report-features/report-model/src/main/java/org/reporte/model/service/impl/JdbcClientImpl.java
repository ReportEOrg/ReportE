package org.reporte.model.service.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.reporte.model.domain.ColumnMetadata;
import org.reporte.model.domain.Datasource;
import org.reporte.model.service.JdbcClient;
import org.reporte.model.service.exception.JdbcClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcClientImpl implements JdbcClient {
	private static final Logger LOG = LoggerFactory.getLogger(JdbcClientImpl.class);
	private static final int TABLE_NAME = 3;
	private static final String SELECT_ALL = "SELECT * FROM %s";
	private static final int MAX_RESULT_ROWS = 20;
	// Note: Column index starts from 1
	private static final int DB_COLUMN_START_IDX = 1;

	/**
	 * Apache Commons DBCP API helps us in getting rid of tightly coupleness to
	 * respective driver API by providing DataSource implementation that works
	 * as an abstraction layer between our program and different JDBC drivers.
	 * <p>
	 * 
	 * @param ds
	 * @return
	 */
	private DataSource getDatasource(Datasource ds) {
		LOG.trace("Getting datasource for " + ds.getName() + "..");

		BasicDataSource dbcpDs = new BasicDataSource();
		dbcpDs.setDriverClassName(ds.getType().getDriverName());
		dbcpDs.setUrl(String.format(ds.getType().getUrlPattern(), ds.getHostname(), ds.getPort(), ds.getSchema()));
		dbcpDs.setUsername(ds.getUsername());
		dbcpDs.setPassword(ds.getPassword());

		LOG.trace("Datasource returned.");
		return dbcpDs;
	}

	/**
	 * Release the resources.
	 * <p>
	 * 
	 * @param conn
	 * @param stmt
	 * @param rs
	 */
	private void release(Connection conn, Statement stmt, ResultSet rs) {
		try {
			LOG.debug("Attempting to release resources after use..");
			if (rs != null) {
				LOG.trace("Closing Resultset..");
				rs.close();
				LOG.trace("Resultset closed.");
			}
			if (stmt != null) {
				LOG.trace("Closing Statement..");
				stmt.close();
				LOG.trace("Statement closed.");
			}
			if (conn != null) {
				LOG.trace("Closing connection..");
				conn.close();
				LOG.trace("Connection closed.");
			}
		} catch (SQLException e) {
			LOG.warn("Error releasing resources.", e);
		}
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getTableNames(Datasource ds) throws JdbcClientException {
		LOG.debug("Getting existing metadata table names from the schema of given Database..");
		LOG.trace("Target Database Schema Details = {}", ds);
		Connection conn = null;
		ResultSet rs = null;

		try {
			DataSource dbcpDs = getDatasource(ds);
			LOG.trace("Getting connection from DataSource..");
			conn = dbcpDs.getConnection();
			LOG.trace("Getting metadata table names..");
			rs = conn.getMetaData().getTables(null, null, "%", null);

			List<String> tableNames = new ArrayList<String>();
			while (rs.next()) {
				tableNames.add(rs.getString(TABLE_NAME));
			}
			LOG.trace("Table names returned: {}", tableNames);

			return tableNames;
		} catch (SQLException e) {
			throw new JdbcClientException("Failed to get metadata table names for given schema.", e);
		} finally {
			release(conn, null, rs);
		}
	}

	@Override
	public List<ColumnMetadata> getColumns(Datasource ds, String tableName) throws JdbcClientException {
		LOG.debug("Getting existing metadata column names of the given table[" + tableName + "]..");
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			DataSource dbcpDs = getDatasource(ds);
			LOG.trace("Getting connection from DataSource..");
			conn = dbcpDs.getConnection();
			stmt = conn.createStatement();
			LOG.trace("Getting all column names from Table - {}..", tableName);
			rs = stmt.executeQuery(String.format(SELECT_ALL, tableName));
			ResultSetMetaData metadata = rs.getMetaData();
			List<ColumnMetadata> columnNames = new ArrayList<ColumnMetadata>();
			int noOfCols = metadata.getColumnCount();
			
			for (int colIdx = DB_COLUMN_START_IDX; colIdx <= noOfCols; colIdx++) {
				columnNames.add(new ColumnMetadata(metadata.getColumnName(colIdx), 
												   metadata.getColumnTypeName(colIdx), 
												   metadata.getColumnClassName(colIdx), 
												   colIdx));
			}
			LOG.trace("Column names returned: {}", columnNames);

			return columnNames;
		} catch (SQLException e) {
			throw new JdbcClientException("Failed to get metadata table names for given schema.", e);
		} finally {
			release(conn, stmt, rs);
		}
	}

	/**
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private List<Map<ColumnMetadata, String>> process(ResultSet rs) throws SQLException {
		List<Map<ColumnMetadata, String>> rows = new ArrayList<Map<ColumnMetadata, String>>();

		// Get the column names in result set first.
		ResultSetMetaData metadata = rs.getMetaData();
		int noOfCols = metadata.getColumnCount();

		List<ColumnMetadata> columns = new ArrayList<ColumnMetadata>();
		for (int colIdx = DB_COLUMN_START_IDX; colIdx <= noOfCols; colIdx++) {
			ColumnMetadata column = new ColumnMetadata();
			column.setLabel(metadata.getColumnLabel(colIdx));
			column.setTypeName(metadata.getColumnTypeName(colIdx));
			
			columns.add(column);
		}
		
		// Populate the map.
		while (rs.next()) {
			Map<ColumnMetadata, String> row = new LinkedHashMap<ColumnMetadata, String>();
			for (ColumnMetadata column : columns) {
				row.put(column, rs.getString(column.getLabel()));
			}
			rows.add(row);
		}

		LOG.debug("({}) rows returned.", rows.size());
		return rows;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Map<ColumnMetadata, String>> execute(Datasource ds, String query) throws JdbcClientException {
		LOG.trace("Target datasource - {}", ds.getName());
		LOG.trace("Query to be executed - {}", query);

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			DataSource dbcpDs = getDatasource(ds);
			LOG.trace("Getting connection from DataSource..");
			conn = dbcpDs.getConnection();
			stmt = conn.createStatement();
			//limit result rows to max row defined
			stmt.setMaxRows(MAX_RESULT_ROWS);
			LOG.debug("Executing given query in the respective database..");

			rs = stmt.executeQuery(query);

			LOG.debug("Query execution completed.");
			return process(rs);
		} catch (SQLException e) {
			throw new JdbcClientException("Query execution failed.", e);
		} finally {
			release(conn, stmt, rs);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int findQueryCount(Datasource ds, String query) throws JdbcClientException {
		int resultCount = 0;
		
		if(query!=null){
			String tempQuery = query.toUpperCase();
			int idx = tempQuery.indexOf("FROM ");
			
			if(idx!=-1){
				tempQuery = "select count(*) "+query.substring(idx);
				
				Connection conn = null;
				Statement stmt = null;
				ResultSet rs = null;
				try {
					DataSource dbcpDs = getDatasource(ds);
					LOG.trace("Getting connection from DataSource..");
					conn = dbcpDs.getConnection();
					stmt = conn.createStatement();
					
					rs = stmt.executeQuery(tempQuery);
					
					if(rs.next()){
						resultCount = rs.getInt(1);
					}
					
				} catch (SQLException e) {
					throw new JdbcClientException("Query execution failed.", e);
				} finally {
					release(conn, stmt, rs);
				}
			}
		}
		
		return resultCount;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean testConnection(Datasource ds) {
		LOG.debug("Trying to connect to database - {}@{}:{}..", ds.getSchema(), ds.getHostname(), ds.getPort());
		boolean result = false;
		Connection conn = null;

		try {
			conn = getDatasource(ds).getConnection();
			LOG.debug("Successfully established the connection.");
			result = true;
		} catch (SQLException e) {
			LOG.debug("Failed to establish the connection.", e);
		} finally {
			release(conn, null, null);
		}
		return result;
	}

}