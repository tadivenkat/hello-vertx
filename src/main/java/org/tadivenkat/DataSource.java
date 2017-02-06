package org.tadivenkat;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

/**
 * Singleton class containing methods to insert a single record or a batch of records. Uses bonecp connection pooling
 * for connections.
 */

public class DataSource {

    private static final Logger log = LoggerFactory.getLogger(DataSource.class);

    private static DataSource datasource;
    private BoneCP connectionPool;
    private PreparedStatement statement;

    private DataSource() throws Exception {
        this("jdbc:vertica://localhost:5433/hercules", "dbadmin", "hercules");
    }

    private DataSource(String jdbcUrl, String username, String password) throws Exception {
        Class.forName("com.vertica.jdbc.Driver");
        // setup the connection pool using BoneCP Configuration
        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMinConnectionsPerPartition(1);
        config.setMaxConnectionsPerPartition(5);
        config.setPartitionCount(3);
        // setup the connection pool
        connectionPool = new BoneCP(config);
    }

    public static DataSource getInstance() throws Exception {
        if (datasource == null) {
            datasource = new DataSource();
            return datasource;
        } else {
            return datasource;
        }
    }

    public static DataSource getInstance(String jdbcUrl, String username, String password) throws Exception {
        if (datasource == null) {
            datasource = new DataSource(jdbcUrl, username, password);
            return datasource;
        } else {
            return datasource;
        }
    }

    public Connection getConnection() throws SQLException {
        return this.connectionPool.getConnection();
    }

   /**
    * Gets the value from the temp table for the given id
    */
   public String getValue(int id) {
      String value = null;
      try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("select value from temp where id = ?")) {
         statement.setInt(1, id);
         ResultSet resultSet = statement.executeQuery();
         while (resultSet.next()) {
            value = resultSet.getString("value");
         }
      } catch (SQLException sqlException) {
         log.error("Exception while getting the value:" + sqlException.getMessage());
      }
      return value;
   }

    /**
     * Inserts a Record into the specified table. Columns and values should be specified as a Map.
     *
     * @param columnsAndValues
     *            columnsAndValues as a Map
     * @param tableName
     *            tableName
     */
    public void insertRecord(Map<String, String> columnsAndValues, String tableName) {
        try (Connection connection = getConnection();
                PreparedStatement statement = getPreparedStatement(columnsAndValues, tableName, connection)) {
            // Set autoCommit to false. We will manually commit.
            connection.setAutoCommit(false);
            populatePreparedStatement(columnsAndValues.values(), statement);
            statement.executeUpdate();
            connection.commit();
            log.info("Record successfully inserted.");
        } catch (SQLException sqlException) {
            log.error("Exception while inserting a record:" + sqlException.getMessage());
        }
    }

    /**
     * Inserts multiple records as a batch
     *
     * @param records
     * @param tableName
     */
    public void insertRecords(List<Map<String, String>> records, String tableName) {
        if (records == null || records.size() < 1) {
            log.info("DataSource#insertRecords is called with null or zero size records");
            return;
        }
        try (Connection connection = getConnection()) {
            // Set autoCommit to false. We will commit manually.
            connection.setAutoCommit(false);
            Map<String, String> firstRecord = records.get(0);
            try (PreparedStatement statement = getPreparedStatement(firstRecord, tableName, connection)) {
                for (Map<String, String> record : records) {
                    populatePreparedStatement(record.values(), statement);
                    statement.addBatch();
                    statement.clearParameters();
                }
                statement.executeBatch();
                connection.commit();
                log.info("Batch Insert successful. Inserted records count: {}", records.size());
            } catch (SQLException sqlException) {
                log.error("Exception while inserting batch of records:" + sqlException.getMessage());
            }
        } catch (SQLException sqlException) {
            log.error("Exception while inserting batch of records:" + sqlException.getMessage());
        }
    }

    /**
     * Creates a PreparedStatement by constructing the query using the specified columnsAndValues
     *
     * @param columnsAndValues
     * @param tableName
     * @throws SQLException
     */
    private static PreparedStatement getPreparedStatement(Map<String, String> columnsAndValues, String tableName,
            Connection connection) throws SQLException {
        Set<String> columns = columnsAndValues.keySet();
        StringBuilder columnNames = new StringBuilder();
        StringBuilder columnValues = new StringBuilder();
        int numberOfColumns = columnsAndValues.size();
        int index = 0;
        for (String column : columns) {
            String value = columnsAndValues.get(column);
            // Encapsulate column in double quotes like this "column"
            columnNames.append("\"" + escapeString(column, true) + "\"");
            columnValues.append("?");
            if (index < numberOfColumns - 1) {
                columnNames.append(",");
                columnValues.append(",");
            }
            index++;
        }

        String insertQuery = "Insert into " + tableName + "(" + columnNames.toString() + ") values ("
                + columnValues.toString() + ")";
        log.info("Insert query : {}", insertQuery);
        return connection.prepareStatement(insertQuery);
    }

    /**
     * Populates all the column values of the specified PreparedStatement
     *
     * @param values
     * @throws SQLException
     */
    private static void populatePreparedStatement(Collection<String> values, PreparedStatement statement)
            throws SQLException {
        int index = 1;
        for (String value : values) {
            statement.setString(index, value);
            index++;
        }
    }

    /**
     * Escape Unwanted characters to prevent SQL injection
     *
     * @param x
     * @param escapeDoubleQuotes
     * @return
     */
    private static String escapeString(String x, boolean escapeDoubleQuotes) {
        StringBuilder sBuilder = new StringBuilder(x.length() * 11 / 10);

        int stringLength = x.length();

        for (int i = 0; i < stringLength; ++i) {
            char c = x.charAt(i);

            switch (c) {
            case 0:
                sBuilder.append('\\');
                sBuilder.append('0');

                break;

            case '\n': /* Must be escaped for logs */
                sBuilder.append('\\');
                sBuilder.append('n');

                break;

            case '\r':
                sBuilder.append('\\');
                sBuilder.append('r');

                break;

            case '\\':
                sBuilder.append('\\');
                sBuilder.append('\\');

                break;

            case '\'':
                sBuilder.append('\\');
                sBuilder.append('\'');

                break;

            case '"': /* Better safe than sorry */
                if (escapeDoubleQuotes) {
                    sBuilder.append('\\');
                }

                sBuilder.append('"');

                break;

            case '\032': /* This gives problems on Win32 */
                sBuilder.append('\\');
                sBuilder.append('Z');

                break;

            case '\u00a5':
            case '\u20a9':
                // escape characters interpreted as backslash
                // fall through

            default:
                sBuilder.append(c);
            }
        }

        return sBuilder.toString();
    }
}
