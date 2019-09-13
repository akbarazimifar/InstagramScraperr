package fe.igscraper.sqlite;

import fe.logger.*;

import java.sql.*;

public class SQLiteDatabase {
    private Connection connection;
    private Logger logger = new Logger("Database", true);

    public SQLiteDatabase(String path) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", path));
            this.logger.print(Logger.Type.INFO, "Connected to database");
        } catch (SQLException | ClassNotFoundException e) {
            this.logger.print(Logger.Type.ERROR, e.getMessage());
        }
    }

    public PreparedStatement prepareStatement(String statement, Object... args) throws SQLException {
        return this.prepareStatement(String.format(statement, args));
    }

    public PreparedStatement prepareStatement(String statement) throws SQLException {
        return this.connection.prepareStatement(statement);
    }
}
