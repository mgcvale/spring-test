package com.github.mgcvale.springstudy.database;;

public abstract class StaticDatabaseInstance {
    private static final DatabaseManager dbm = new DatabaseManager(
            "jdbc:mysql://192.168.0.100:3306/serverdb", "root", "admin"
    );

    public static DatabaseManager getDatabaseManager() {
        return dbm;
    }

}
