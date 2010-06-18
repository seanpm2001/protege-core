package edu.stanford.smi.protege.storage.database;

//ESCA*JAVA0100

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.server.RemoteSession;
import edu.stanford.smi.protege.server.ServerProperties;
import edu.stanford.smi.protege.server.framestore.ServerFrameStore;
import edu.stanford.smi.protege.storage.database.pool.ConnectionPool;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.SystemUtilities;
import edu.stanford.smi.protege.util.transaction.TransactionIsolationLevel;
import edu.stanford.smi.protege.util.transaction.TransactionMonitor;

public class RobustConnection extends SessionConnectionManager {
    private static final transient Logger log = Log.getLogger(RobustConnection.class);


    public static final String OLD_PROPERTY_LONGVARCHAR_TYPE_NAME = "SimpleJdbcDatabaseManager.longvarcharname";
    public static final String PROPERTY_LONGVARCHAR_TYPE_NAME = "Database.typename.longvarchar";
    public static final String PROPERTY_FRAME_NAME_TYPE_NAME = "Database.typename.frame.name.type";
    public static final String PROPERTY_SHORT_VALUE_TYPE_NAME = "Database.typename.short.value.type";
    public static final String PROPERTY_VARCHAR_TYPE_SIZE = "Database.type.varchar.maxsize";
    public static final int DEFAULT_MAX_STRING_SIZE = 255;
    public static final String PROPERTY_INTEGER_TYPE_NAME = "Database.typename.integer";
    public static final String PROPERTY_SMALL_INTEGER_TYPE_NAME = "Database.typename.small_integer";
    public static final String PROPERTY_BIT_TYPE_NAME = "Database.typename.bit";
    
    
    private KnownDatabase dbType;
    private boolean _supportsBatch;
    private char _escapeChar;
    private String _escapeClause;
    private boolean _supportsTransactions;
    
    private String driver;
    private String _driverLongvarcharTypeName;
    private String _driverTinyIntTypeName;
    private String _driverBitTypeName;
    private String _driverSmallIntTypeName;
    private String _driverIntegerTypeName;
    private String _driverVarcharTypeName;
    private String _driverVarBinaryTypeName;
    private String _driverCharTypeName;
    

    @SuppressWarnings("unchecked")
    public RobustConnection(String driver, String url, String username, String password,
                            TransactionMonitor transactionMonitor, RemoteSession session) throws SQLException {
        super(driver, url, username, password, transactionMonitor,session);

        this.driver = driver;
        
        initializeDatabaseType();
        initializeSupportsBatch();
        initializeSupportsEscapeSyntax();
        initializeDriverTypeNames();
        initializeSupportsTransactions();
    }
    
    private void initializeDatabaseType() throws SQLException {
        String  productName = getDatabaseProductName();
        if (productName.equalsIgnoreCase("mysql")) {
            dbType = KnownDatabase.MYSQL;
        }
        else if (productName.equalsIgnoreCase("PostgreSQL")) {
            dbType = KnownDatabase.POSTGRESQL;
        }
        else if (productName.equalsIgnoreCase("Microsoft SQL Server")) {
            dbType = KnownDatabase.SQLSERVER;
        }
        else if  (productName.equalsIgnoreCase("oracle")) {
            dbType = KnownDatabase.ORACLE;
        }
        else if (productName.equalsIgnoreCase("apache derby")) {
            dbType = KnownDatabase.DERBY;
        }
        else {
            dbType = null;
        }
    }
    
    public void setAutoCommit(boolean b) throws SQLException {
        try {
            reference();
            getConnection().setAutoCommit(b);
        }
        finally {
            dereference();
        }
    }

    public void commit() throws SQLException {
        try {
            reference();
            getConnection().commit();
        }
        finally {
            dereference();
        }
    }


    private void initializeSupportsBatch() throws SQLException {
        try {
            reference();
            _supportsBatch = getConnection().getMetaData().supportsBatchUpdates();
            if (!_supportsBatch) {
                String s = "This JDBC driver does not support batch update.";
                s += " For much better performance try using a newer driver";
                Log.getLogger().warning(s);
            }
        }
        finally {
            dereference();
        }
    }

    private void initializeSupportsTransactions() throws SQLException {
        try {
            reference();
            _supportsTransactions = getConnection().getMetaData().supportsTransactions();
            if (!_supportsTransactions) {
                Log.getLogger().warning("This database does not support transactions");
            }
        }
        finally {
            dereference();
        }
    }

    private void initializeSupportsEscapeSyntax() throws SQLException {
        try {
            reference();
            _escapeChar = 0;
            _escapeClause = "";
            boolean escapeSupported = getConnection().getMetaData().supportsLikeEscapeClause();
            if (escapeSupported) {
                if (isMySql()) {
                    _escapeChar = '\\';
                } else {
                    _escapeChar = '|';
                    _escapeClause = "{ESCAPE '" + _escapeChar + "'}";

                }
            } else {
                Log.getLogger().warning("This driver does not support SQL Escape processing.");
            }
        }
        finally {
            dereference();
        }
    }

    public char getEscapeCharacter() {
        return _escapeChar;
    }

    public String getEscapeClause() {
        return _escapeClause;
    }

    public boolean supportsBatch() {
        return _supportsBatch;
    }

    public  KnownDatabase getKnownDatabaseType() {
        return dbType;
    }

    public boolean isOracle() throws SQLException {
        return dbType == KnownDatabase.ORACLE;
    }

    public boolean isSqlServer() throws SQLException {
        return dbType == KnownDatabase.SQLSERVER;
    }

    public boolean isMsAccess() throws SQLException {
        return getDatabaseProductName().equalsIgnoreCase("access");
    }

    public boolean isMySql() throws SQLException {
        return dbType == KnownDatabase.MYSQL;
    }

    public boolean isPostgres() throws SQLException {
        return dbType == KnownDatabase.POSTGRESQL;
    }

    public String getDatabaseProductName() throws SQLException {
        try {
            reference();
            return getConnection().getMetaData().getDatabaseProductName();
        }
        finally {
            dereference();
        }
    }
    
    public int getDatabaseMajorVersion() throws SQLException {
        try {
            reference();
            return getConnection().getMetaData().getDatabaseMajorVersion();
        }
        finally {
            dereference();
        }
    }

    public int getDatabaseMinorVersion() throws SQLException {
        try {
            reference();
            return getConnection().getMetaData().getDatabaseMinorVersion();
        }
        finally {
            dereference();
        }
    }
    
    private void initializeDriverTypeNames() throws SQLException {
        try {
            reference();
            String longvarbinaryTypeName = null;
            String blobTypeName = null;
            String clobTypeName = null;


            DatabaseMetaData md = getConnection().getMetaData();
            if (log.isLoggable(Level.FINE)) {
                log.fine(" ----------------------- type information for "  +  md.getDatabaseProductName());
                log.fine("See http://java.sun.com/j2se/1.5.0/docs/api/java/sql/Types.html for a list of the sql types");
            }
            ResultSet rs = md.getTypeInfo();
            while (rs.next()) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Info for type " + rs.getString("TYPE_NAME"));
                    log.fine("\tsql data type = " + rs.getInt("DATA_TYPE"));
                    short nullable = rs.getShort("NULLABLE");
                    log.fine("\tnullable = " + (nullable == DatabaseMetaData.typeNoNulls ? "false" : 
                                                (nullable == DatabaseMetaData.typeNullable ? "true" : "maybe")));
                    log.fine("\tcase sensitive = " + rs.getBoolean("CASE_SENSITIVE"));
                }
                String name = rs.getString("TYPE_NAME");
                int type = rs.getInt("DATA_TYPE");
                if (name.length() == 0) {
                    continue;
                }
                switch (type) {
                case Types.LONGVARCHAR:
                    if (_driverLongvarcharTypeName == null) {
                        _driverLongvarcharTypeName = name;
                    }
                    break;
                case Types.LONGVARBINARY:
                    if (longvarbinaryTypeName == null) {
                        longvarbinaryTypeName = name;
                    }
                    break;
                case Types.CLOB:
                    if (clobTypeName == null) {
                        clobTypeName = name;
                    }
                    break;
                case Types.BLOB:
                    if (blobTypeName == null) {
                        blobTypeName = name;
                    }
                    break;
                case Types.TINYINT:
                    if (_driverTinyIntTypeName == null) {
                        _driverTinyIntTypeName = name;
                    }
                    break;
                case Types.BIT:
                    if (_driverBitTypeName == null) {
                        _driverBitTypeName = name;
                    }
                    break;
                case Types.SMALLINT:
                    if (_driverSmallIntTypeName == null) {
                        _driverSmallIntTypeName = name;
                    }
                    break;
                case Types.INTEGER:
                    if (_driverIntegerTypeName == null) {
                        _driverIntegerTypeName = name;
                    }
                    break;
                case Types.VARCHAR:
                    if (_driverVarcharTypeName == null) {
                        _driverVarcharTypeName = name;
                    }
                    break;
                case Types.VARBINARY:
                    if (_driverVarBinaryTypeName == null) {
                        _driverVarBinaryTypeName = name;
                    }
                    break;
                case Types.CHAR:
                    if (_driverCharTypeName == null) {
                        _driverCharTypeName = name;
                    }
                    break;
                default:
                    // do nothing
                    break;
                }
            }
            if (log.isLoggable(Level.FINE)) {
                log.fine(" ----------------------- end of type information for "  +  md.getDatabaseProductName());
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
                if (_driverLongvarcharTypeName == null && isPostgres()) {
                    _driverLongvarcharTypeName = "TEXT";
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
            if (_driverVarcharTypeName == null || isPostgres() || isSqlServer()) {
                _driverVarcharTypeName = "VARCHAR";
            }
            if (_driverVarBinaryTypeName == null) {
                _driverVarBinaryTypeName = "VARCHAR";
            }
            if (isOracle()) { 
                _driverLongvarcharTypeName = "CLOB";  // can't search on the default LONG.
            }
        }
        finally {
            dereference();
        }
    }

    private String getName(String typeName, String driverName) {
        String userTypeName = ApplicationProperties.getApplicationOrSystemProperty(typeName + "." + driver);
        return (userTypeName == null || userTypeName.length() == 0) ? driverName : userTypeName;
    }

    public String getBitTypeName() {
        String defaultValue;
        if (dbType != null) {
            defaultValue = dbType.getBitType();
        }
        else  {
            defaultValue = _driverBitTypeName;
        }
        return getName(PROPERTY_BIT_TYPE_NAME, defaultValue);
    }
    
    public String getSmallIntTypeName() {
        String defaultValue;
        if (dbType != null) {
            defaultValue = dbType.getSmallIntType();
        }
        else  {
            defaultValue = _driverSmallIntTypeName;
        }
        return getName(PROPERTY_SMALL_INTEGER_TYPE_NAME, defaultValue);
    }

    public String getIntegerTypeName() {
        String defaultValue;
        if (dbType != null) {
            defaultValue = dbType.getIntType();
        }
        else  {
            defaultValue = _driverIntegerTypeName;
        }
        return getName(PROPERTY_INTEGER_TYPE_NAME, defaultValue);
    }

    public String getFrameNameType() {
        String defaultValue;
        if (dbType != null) {
            defaultValue = dbType.getFrameNameType();
        }
        else  {
            defaultValue = _driverVarcharTypeName;
        }
        return getName(PROPERTY_FRAME_NAME_TYPE_NAME, defaultValue);
    }
    
    public String getShortValueType() {
        String defaultValue;
        if (dbType != null) {
            defaultValue = dbType.getShortValueType();
        }
        else  {
            defaultValue = _driverVarcharTypeName;
        }
        return getName(PROPERTY_SHORT_VALUE_TYPE_NAME, defaultValue);
    }
    
    public int getMaxVarcharSize() {
        String propValue = ApplicationProperties.getApplicationOrSystemProperty(PROPERTY_VARCHAR_TYPE_SIZE + "." + driver);
        if (propValue  != null) {
            try {
                return Integer.parseInt(propValue);
            }
            catch (NumberFormatException nfe) {
                ;
            }
        }
        if (dbType != null) {
            return dbType.getMaxShortValueSize();
        }
        else {
            return DEFAULT_MAX_STRING_SIZE;
        }
    }
    
    public String getLongvarcharTypeName() {
        String defaultValue;
        if (dbType != null) {
            defaultValue = dbType.getLongStringType();
        }
        else  {
            defaultValue =  SystemUtilities.getSystemProperty(OLD_PROPERTY_LONGVARCHAR_TYPE_NAME + "." + driver);
            if (defaultValue == null || defaultValue.length() == 0) {
                defaultValue = _driverLongvarcharTypeName;
            }
            if  (defaultValue == null) {
                defaultValue = getShortValueType();
                Log.getLogger().warning("Using VARCHAR in place of LONGVARCHAR, long strings will be truncated.");
            }
        }
        return getName(PROPERTY_LONGVARCHAR_TYPE_NAME, defaultValue);
    }
    public boolean supportsCaseInsensitiveMatches() throws SQLException {
        return !(isOracle() || isPostgres());
    }

    public boolean supportsIndexOnFunction() throws SQLException {
        return isOracle() || isPostgres();
    }

    
    public boolean supportsTransactions() {
        return _supportsTransactions;
    }
    
    

    // String driver, String url, String username, String password
    public static void main(String[] args) throws SQLException {
        RobustConnection connection = new RobustConnection(args[0], args[1], args[2], args[3], null, null);
        ResultSet rs = connection.getConnection().getMetaData().getTypeInfo();
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
    
}