package com.github.mgcvale.springstudy.database;
import java.sql.*;
import java.util.*;

public class DatabaseManager implements Cloneable {
    private String url;
    private String dbName;
    private Properties connProperties;
    private Connection connection = null;

    //
    //Constructors
    //
    public DatabaseManager(){
        connProperties = new Properties();
        url = "jdbc:mysql://127.0.0.0:3306/database";
    }

    public DatabaseManager(String url, String uname, String password){
        this();
        setUrl(url);
        connProperties.put("user", uname);
        connProperties.put("password", password);
    }

    public DatabaseManager(String url, Properties connProperties){
        this.connProperties = connProperties;
        this.url = url;
    }

    //
    //Getters and Setters
    //
    public void setUrl(String url) {
        this.url = url;
        this.dbName = url.substring(url.lastIndexOf("/")+1);
        System.out.println(dbName);
    }

    public String getUrl(){
        return url;
    }

    public void setUsername(String uname){
        connProperties.put("user", uname);
    }

    public void setPassword(String password){
        connProperties.put("password", password);
    }

    public String getUsername(){
        return connProperties.getProperty("user");
    }

    public String getPassword(){
        return connProperties.getProperty("password");
    }

    public void setConnProperties(Properties connProperties){
        this.connProperties = connProperties;
    }

    public Properties getConnProperties(){
        return connProperties;
    }


    private Connection getConnection() throws SQLException {
        if(connection == null) {
            System.out.println("CREATING A NEW CONNECTION!!!!");
            connection =  DriverManager.getConnection(url, connProperties);
        }
        return connection;
    }

    public void refreshConnection() throws SQLException {
        this.connection = DriverManager.getConnection(url, connProperties);
    }

    public boolean tableContains(String tableName, String columnName, String entry) throws SQLException {
        String statement = "SELECT * FROM " + tableName;
        Connection con = getConnection();
        PreparedStatement ps = con.prepareStatement(statement);
        ResultSet result = ps.executeQuery();

        while(result.next()) {
            String value = result.getString(columnName);
            if (Objects.equals(entry, value))
                return true;
        }

        return false;
    }

    public LinkedHashMap<Object[], Object[]> getTable(String tableName,
                                                      String distinct, String condition,
                                                      String orderBy) throws SQLException {

        String whereStr = condition.isEmpty() ? "" : " WHERE " + condition;
        String orderByStr = orderBy.isEmpty() ? "" : " ORDER BY " + orderBy;
        String statement = "SELECT " + distinct + " * FROM " + tableName + whereStr + orderByStr;

        String[] pkNames;
        String[] columnNames;
        LinkedHashMap<Object[], Object[]> map = new LinkedHashMap<>();
        ResultSet result;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(statement)) {

            String[][] colNames = getColNamesPK(con, tableName);
            pkNames = colNames[0];
            columnNames = colNames[1];

            result = ps.executeQuery();
            int index = 0;
            while (result.next()) {
                Object[] keys = new Object[pkNames.length];
                Object[] values = new Object[columnNames.length];

                //populate keys
                for (int i = 0; i < pkNames.length; i++) {
                    keys[i] = result.getObject(pkNames[i]);
                }

                //populate values
                for (int i = 0; i < columnNames.length; i++) {
                    values[i] = result.getObject(columnNames[i]);
                }

                map.put(keys, values);
                index++;
            }
        }

        return map;
    }

    public <T> T getEntryAt(String tableName, String identifierColumnName, String value, String entryColumnName, Class<T> type) throws SQLException {
        String statement = "SELECT * FROM " + tableName + " WHERE " + identifierColumnName + "=?";
        Connection con = getConnection();
        try (PreparedStatement ps = con.prepareStatement(statement)){
        ps.setString(1, value);
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    return result.getObject(entryColumnName, type);
                }
            }
        }
        return null; // or throw an exception if the entry is not found
    }


    public void insertInto(String tableName, Object[] values) throws SQLException {
        insertInto(tableName, null, values);
    }
    public void insertInto(String tableName, String[] columns, Object[] values) throws SQLException {
        Connection con = getConnection();
        String columnsStr = "";
        String valuesStr = "";

        //stringify columns array
        if(columns != null) {
            for (int i = 0; i < columns.length; i++) {
                String columnName = ((i > 0) ? ", " : "(") + columns[i];
                columnsStr = columnsStr.concat(columnName);
            }
            columnsStr = columnsStr.concat(") ");
        }

        //stringify values array
        for(int i = 0; i < values.length; i++) {
            String valueName;
            if(values[i].getClass() == String.class) {
                valueName = ((i > 0) ? ",'" : "('") + values[i] + "'";
            } else {
                valueName = ((i > 0) ? "," : "(") + values[i];
            }
            valuesStr = valuesStr.concat(valueName);
        }
        valuesStr = valuesStr.concat("); ");


        Statement statement = con.createStatement();
        statement.execute("INSERT INTO " + tableName + " " + columnsStr + "VALUES " + valuesStr);
    }

    public void updateTable(String tableName, String[] columns, Object[] values, String where) throws SQLException{
        Connection con = getConnection();
        String statementStr = "UPDATE " + tableName + " SET ";

        for(int i=0; i<columns.length; i++) {
            statementStr = statementStr.concat(columns[i] + " = " + ((values[i].getClass() == String.class) ? "'" + values[i] + "'" : values[i]) + ((i+1 != columns.length) ? ", " : " "));
        }
        System.out.println(statementStr);
        statementStr = statementStr.concat("where " + where);

        PreparedStatement statement = con.prepareStatement(statementStr);
        statement.executeUpdate();
    }

    public void deleteFromTable(String tableName, String where, String whereValue) throws SQLException {
        deleteFromTable(tableName, where, whereValue,false);
    }

    public void deleteFromTable(String tableName, String where, String whereValue, boolean resetAutoIncrement) throws SQLException {
        Connection con = getConnection();
        String statementStr = "DELETE FROM " + tableName + (!where.isEmpty() ? " WHERE " + where : "");
        PreparedStatement statement = con.prepareStatement(statementStr);
        statement.setString(1, whereValue);
        System.out.println(statement.toString());
        statement.executeUpdate();
        if(resetAutoIncrement)
            con.prepareStatement("ALTER TABLE " + tableName + " AUTO_INCREMENT = 1").executeUpdate();
    }

    public void clearTable(String tableName) throws SQLException{
        deleteFromTable(tableName, "" , "");
    }

    private String[] getColNames(Connection con, String tableName) throws SQLException {
        ArrayList<String> names = new ArrayList<>();
        DatabaseMetaData metaData = con.getMetaData();

        //get col names
        ResultSet result = metaData.getColumns(null, null, tableName, null);
        while (result.next()) {
            names.add(result.getString("COLUMN_NAME"));
        }

        return names.toArray(new String[0]);
    }

    public String[] getColNames(String tableName) throws SQLException {
        return getColNames(getConnection(), tableName);
    }

    private String[][] getColNamesPK(Connection con, String tableName) throws SQLException{
        ArrayList[] names = new ArrayList[2];
        names[0] = new ArrayList<String>();
        names[1] = new ArrayList<String>();
        DatabaseMetaData metaData = con.getMetaData();
        String name;

        //get pk col names
        ResultSet result = metaData.getPrimaryKeys(dbName, "", tableName);
        while(result.next()) {
            names[0].add(result.getString("COLUMN_NAME"));
        }

        //get other col names
        result = metaData.getColumns(null, null, tableName, null);
        while(result.next()) {
            name = result.getString("COLUMN_NAME");
            if(!names[0].contains(name))
                names[1].add(result.getString("COLUMN_NAME"));
        }

        Object[][] retObj = new Object[][] {names[0].toArray(), names[1].toArray()};
        String[][] retStr = new String[2][];
        retStr[0] = Arrays.stream(retObj[0]).map(Object::toString).toArray(String[]::new);
        retStr[1] = Arrays.stream(retObj[1]).map(Object::toString).toArray(String[]::new);

        return retStr;
    }

    public String[][] getColNamesPK(String tableName) throws SQLException {
        return getColNamesPK(getConnection(), tableName);
    }

    private String[] getColNamesNOPK(Connection con, String tableName) throws SQLException {
        String[][] colNames = getColNamesPK(con, tableName);
        return colNames[1];
    }

    public String[] getColNamesNOPK(String tableName) throws SQLException {
        return getColNamesNOPK(getConnection(), tableName);
    }

    private String[] getTables(Connection con) throws SQLException{
        DatabaseMetaData meta = con.getMetaData();
        ResultSet result = meta.getTables(null, null, null, new String[]{"TABLE"});
        ArrayList<String> tables = new ArrayList<>();

        while(result.next()) {
            tables.add(result.getString("TABLE_NAME"));
        }

        return tables.toArray(new String[0]);
    }

    public String[] getTables() throws SQLException {
        return getTables(getConnection());
    }

    //Overrides
    @Override
    public DatabaseManager clone() {
        try {
            return (DatabaseManager) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}