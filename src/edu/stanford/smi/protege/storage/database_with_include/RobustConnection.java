package edu.stanford.smi.protege.storage.database_with_include;

import java.sql.*;
import java.util.*;

import edu.stanford.smi.protege.util.*;

public class RobustConnection {
    private static final int ALLOWANCE = 100;
    private static final int ORACLE_MAX_VARCHAR_SIZE = 3166 - ALLOWANCE;
    private static final int SQLSERVER_MAX_VARCHAR_SIZE = 900 - ALLOWANCE;
    private static final int DEFAULT_MAX_VARCHAR_SIZE = 255;

    private Map _stringToPreparedStatementMap = new HashMap();
    private Connection _connection;
    private Statement _genericStatement;
    private String _url;
    private String _username;
    private String _password;
    private boolean _supportsBatch;
    private boolean _supportsEscapeClause;
    private boolean _supportsTransactions;
    private int _maxVarcharSize;
    // private int _driverVarcharMaxSize;
    private String _driverLongvarcharTypeName;
    private String _driverTinyIntTypeName;
    private String _driverBitTypeName;
    private String _driverSmallIntTypeName;
    private String _driverIntegerTypeName;
    private String _driverVarcharTypeName;
    private String _driverCharTypeName;
    private final static String OLD_PROPERTY_LONGVARCHAR_TYPE_NAME = "SimpleJdbcDatabaseManager.longvarcharname";
    private final static String PROPERTY_LONGVARCHAR_TYPE_NAME = "Database.typename.longvarchar";
    private final static String PROPERTY_VARCHAR_TYPE_NAME = "Database.typename.varchar";
    private final static String PROPERTY_INTEGER_TYPE_NAME = "Database.typename.integer";
    private final static String PROPERTY_SMALL_INTEGER_TYPE_NAME = "Database.typename.small_integer";
    private final static String PROPERTY_TINY_INTEGER_TYPE_NAME = "Database.typename.tiny_integer";
    private final static String PROPERTY_BIT_TYPE_NAME = "Database.typename.bit";
    private final static String PROPERTY_CHAR_TYPE_NAME = "Database.typename.char";

    public RobustConnection(String driver, String url, String username, String password) throws SQLException {
        _url = url;
        _username = username;
        _password = password;

        Class clas = SystemUtilities.forName(driver);
        if (clas == null) {
            throw new RuntimeException("class not found: " + driver);
        }
        // Log.trace("initializing connection", this, "RobustConnection");
        setupConnection();
        initializeMaxVarcharSize();
        initializeSupportsBatch();
        initializeSupportsEscapeSyntax();
        initializeDriverTypeNames();
        initializeSupportsTransactions();
        // dumpTypes();
    }

    public void setAutoCommit(boolean b) throws SQLException {
        _connection.setAutoCommit(b);
    }

    public void commit() throws SQLException {
        _connection.commit();
    }

    private void setupConnection() throws SQLException {
        _connection = DriverManager.getConnection(_url, _username, _password);
    }

    public void close() throws SQLException {
        closeStatements();
        _connection.close();
        _connection = null;
    }

    public void closeStatements() throws SQLException {
        Iterator i = _stringToPreparedStatementMap.values().iterator();
        while (i.hasNext()) {
            PreparedStatement stmt = (PreparedStatement) i.next();
            stmt.close();
        }
        _stringToPreparedStatementMap.clear();
        if (_genericStatement != null) {
            _genericStatement.close();
            _genericStatement = null;
        }
    }

    private void initializeMaxVarcharSize() throws SQLException {
        String property = SystemUtilities.getSystemProperty("database.varcharsize");
        if (property != null && property.length() != 0) {
            _maxVarcharSize = Integer.valueOf(property).intValue();
        } else if (isOracle()) {
            _maxVarcharSize = ORACLE_MAX_VARCHAR_SIZE;
        } else if (isSqlServer()) {
            _maxVarcharSize = SQLSERVER_MAX_VARCHAR_SIZE;
        } else {
            _maxVarcharSize = DEFAULT_MAX_VARCHAR_SIZE;
        }
    }

    private void initializeSupportsBatch() throws SQLException {
        _supportsBatch = _connection.getMetaData().supportsBatchUpdates();
        if (!_supportsBatch) {
            String s = "This JDBC driver does not support batch update.";
            s += " For much better performance try using a newer driver";
            Log.getLogger().warning(s);
        }
    }

    private void initializeSupportsTransactions() throws SQLException {
        _supportsTransactions = _connection.getMetaData().supportsTransactions();
        if (!_supportsTransactions) {
            Log.getLogger().warning("This database does not support transactions");
        }
    }

    private void initializeSupportsEscapeSyntax() throws SQLException {
        _supportsEscapeClause = _connection.getMetaData().supportsLikeEscapeClause() && !isMySql();
    }

    public boolean supportsBatch() {
        return _supportsBatch;
    }

    public PreparedStatement getPreparedStatement(String text) throws SQLException {
        PreparedStatement stmt = (PreparedStatement) _stringToPreparedStatementMap.get(text);
        if (stmt == null) {
            stmt = _connection.prepareStatement(text);
            _stringToPreparedStatementMap.put(text, stmt);
        }
        return stmt;
    }

    public Statement getStatement() throws SQLException {
        if (_genericStatement == null) {
            _genericStatement = _connection.createStatement();
        }
        return _genericStatement;
    }

    public void checkConnection() throws SQLException {
        if (_connection == null) {
            setupConnection();
        } else if (_connection.isClosed()) {
            close();
            setupConnection();
        }
    }

    public boolean isOracle() throws SQLException {
        return getDatabaseProductName().equalsIgnoreCase("oracle");
    }

    public boolean isSqlServer() throws SQLException {
        // Is this the correct text to test for?  I have nothing to test on...
        return getDatabaseProductName().equalsIgnoreCase("sql server");
    }

    public boolean isMsAccess() throws SQLException {
        return getDatabaseProductName().equalsIgnoreCase("access");
    }

    public boolean isMySql() throws SQLException {
        return getDatabaseProductName().equalsIgnoreCase("mysql");
    }

    private String getDatabaseProductName() throws SQLException {
        return _connection.getMetaData().getDatabaseProductName();
    }

    public int getMaxVarcharSize() {
        return _maxVarcharSize;
    }

    private void initializeDriverTypeNames() throws SQLException {
        String longvarbinaryTypeName = null;
        String blobTypeName = null;
        String clobTypeName = null;

        DatabaseMetaData md = _connection.getMetaData();
        ResultSet rs = md.getTypeInfo();
        while (rs.next()) {
            String name = rs.getString("TYPE_NAME");
            int type = rs.getInt("DATA_TYPE");
            if (name.length() == 0) {
                continue;
            }
            switch (type) {
                case Types.LONGVARCHAR :
                    if (_driverLongvarcharTypeName == null) {
                        _driverLongvarcharTypeName = name;
                    }
                    break;
                case Types.LONGVARBINARY :
                    if (longvarbinaryTypeName == null) {
                        longvarbinaryTypeName = name;
                    }
                    break;
                case Types.CLOB :
                    if (clobTypeName == null) {
                        clobTypeName = name;
                    }
                    break;
                case Types.BLOB :
                    if (blobTypeName == null) {
                        blobTypeName = name;
                    }
                    break;
                case Types.TINYINT :
                    if (_driverTinyIntTypeName == null) {
                        _driverTinyIntTypeName = name;
                    }
                    break;
                case Types.BIT :
                    if (_driverBitTypeName == null) {
                        _driverBitTypeName = name;
                    }
                    break;
                case Types.SMALLINT :
                    if (_driverSmallIntTypeName == null) {
                        _driverSmallIntTypeName = name;
                    }
                    break;
                case Types.INTEGER :
                    if (_driverIntegerTypeName == null) {
                        _driverIntegerTypeName = name;
                    }
                    break;
                case Types.VARCHAR :
                    if (_driverVarcharTypeName == null) {
                        _driverVarcharTypeName = name;
                        /*
                        if (_maxVarcharSize == 0) {
                            _driverVarcharMaxSize = rs.getInt("PRECISION");
                        }
                        */
                    }
                    break;
                case Types.CHAR :
                    if (_driverCharTypeName == null) {
                        _driverCharTypeName = name;
                    }
                default :
                    // do nothing
            }
        }
        rs.close();
        if (_driverLongvarcharTypeName == null) {
            if (longvarbinaryTypeName == null) {
                if (clobTypeName == null) {
                    _driverLongvarcharTypeName = blobTypeName;
                } else {
                    _driverLongvarcharTypeName = clobTypeName;
                }
            } else {
                _driverLongvarcharTypeName = longvarbinaryTypeName;
            }
        }
        if (_driverIntegerTypeName == null) {
            _driverIntegerTypeName = "INTEGER";
        }
        if (_driverSmallIntTypeName == null) {
            _driverSmallIntTypeName = _driverIntegerTypeName;
        }
        if (_driverTinyIntTypeName == null) {
            _driverTinyIntTypeName = _driverSmallIntTypeName;
        }
        if (_driverBitTypeName == null) {
            _driverBitTypeName = _driverTinyIntTypeName;
        }
        if (_driverVarcharTypeName == null) {
            _driverVarcharTypeName = "VARCHAR";
        }
    }

    private static String getName(String typeName, String driverName) {
        String userTypeName = ApplicationProperties.getApplicationOrSystemProperty(typeName);
        return (userTypeName == null || userTypeName.length() == 0) ? driverName : userTypeName;
    }

    public String getLongvarcharTypeName() {
        String name = SystemUtilities.getSystemProperty(OLD_PROPERTY_LONGVARCHAR_TYPE_NAME);
        if (name == null || name.length() == 0) {
            name = getName(PROPERTY_LONGVARCHAR_TYPE_NAME, _driverLongvarcharTypeName);
        }
        return name;
    }

    public String getSmallIntTypeName() {
        return getName(PROPERTY_SMALL_INTEGER_TYPE_NAME, _driverSmallIntTypeName);
    }

    public String getIntegerTypeName() {
        return getName(PROPERTY_INTEGER_TYPE_NAME, _driverIntegerTypeName);
    }
    public String getTinyIntTypeName() {
        return getName(PROPERTY_TINY_INTEGER_TYPE_NAME, _driverTinyIntTypeName);
    }
    public String getBitTypeName() {
        return getName(PROPERTY_BIT_TYPE_NAME, _driverBitTypeName);
    }
    public String getVarcharTypeName() {
        return getName(PROPERTY_VARCHAR_TYPE_NAME, _driverVarcharTypeName);
    }
    public String getCharTypeName() {
        return getName(PROPERTY_CHAR_TYPE_NAME, _driverCharTypeName);
    }

    public boolean supportsEscapeClause() {
        return _supportsEscapeClause;
    }

    /*
    private void dumpTypes() throws SQLException {
        ResultSet rs = _connection.getMetaData().getTypeInfo();
        while (rs.next()) {
            System.out.println("TYPE_NAME: " + rs.getString(1));
            System.out.println("\tDATA_TYPE: " + rs.getInt(2));
            System.out.println("\tPRECISION: " + rs.getLong(3));
            System.out.println("\tLITERAL_PREFIX: " + rs.getString(4));
            System.out.println("\tLITERAL_SUFFIX: " + rs.getString(5));
            System.out.println("\tCREATE_PARAMS: " + rs.getString(6));
            System.out.println("\tNULLABLE: " + rs.getShort(7));
            System.out.println("\tCASE_SENSITIVE: " + rs.getBoolean(8));
            System.out.println("\tSEARCHABLE: " + rs.getShort(9));
            System.out.println("\tUNSIGNED_ATTRIBUTE: " + rs.getBoolean(10));
            System.out.println("\tFIXED_PREC_SCALE: " + rs.getBoolean(11));
            System.out.println("\tAUTO_INCREMENT: " + rs.getBoolean(12));
            System.out.println("\tLOCAL_TYPE_NAME: " + rs.getString(13));
            System.out.println("\tMINIMUM_SCALE: " + rs.getShort(14));
            System.out.println("\tMAXIMUM_SCALE: " + rs.getShort(15));
            System.out.println("\tSQL_DATA_TYPE: " + rs.getShort(16));
            System.out.println("\tSQL_DATETIME_SUB: " + rs.getShort(17));
            System.out.println("\tNUM_PREC_RADIX: " + rs.getInt(18));
        }
        rs.close();
    }
    */

    public boolean supportsCaseInsensitiveMatches() throws SQLException {
        return !isOracle();
    }

    public boolean supportsIndexOnFunction() throws SQLException {
        return isOracle();
    }

    public boolean beginTransaction() {
        boolean begun = false;
        try {
            if (isMsAccess()) {
                closeStatements();
            }
            _connection.setAutoCommit(false);
            begun = true;
        } catch (SQLException e) {
            Log.getLogger().warning(e.toString());
        }
        return begun;
    }

    public boolean commitTransaction() {
        boolean committed = false;
        try {
            _connection.commit();
            committed = true;
            _connection.setAutoCommit(true);
        } catch (SQLException e) {
            Log.getLogger().warning(e.toString());
        }
        return committed;
    }

    public boolean rollbackTransaction() {
        boolean rolledBack = false;
        try {
            _connection.rollback();
            rolledBack = true;
            _connection.setAutoCommit(true);
        } catch (SQLException e) {
            Log.getLogger().warning(e.toString());
        }
        return rolledBack;
    }
}