package fe.igscraper.sqlite;

import fe.logger.*;
import java.sql.*;

public class SQLiteDatabase
{
    private Connection connection;
    private Logger logger;
    
    public SQLiteDatabase(final String path) {
        this.logger = new Logger("Database", true);
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", path));
            this.logger.print(Logger.Type.INFO, "Connected to database");
        }
        catch (SQLException | ClassNotFoundException e) {
            this.logger.print(Logger.Type.ERROR, e.getMessage());
        }
    }
    
    public PreparedStatement prepareStatement(final String statement, final Object... args) throws SQLException {
        return this.prepareStatement(String.format(statement, args));
    }
    
    public PreparedStatement prepareStatement(final String statement) throws SQLException {
        return this.connection.prepareStatement(statement);
    }
}
