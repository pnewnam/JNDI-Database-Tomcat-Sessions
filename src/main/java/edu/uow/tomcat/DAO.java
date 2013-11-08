package edu.uow.tomcat;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Table structure required
 *
 */
public abstract class DAO {

    String jndi = "";

    void deleteSession(String sessionId) {};

    void insertSession(String sessionId, String principal, byte[] data) {};

    void updateSession(String sessionId, String principal, byte[] data) {};

    byte[] getSession(String sessionId) {return null;}

    void deleteExpiredSessions(long lastModified) {};

    void init() {};

    void setJndi(String jndi) {
        this.jndi = jndi;
    }

    String[] getAllActive() {return null;}

    Connection getConnection() throws NamingException, SQLException {
        Context initContext = new InitialContext();
        Context envContext  = (Context)initContext.lookup("java:/comp/env");
        DataSource ds = (DataSource)envContext.lookup(this.jndi);
        Connection conn = ds.getConnection();
        return conn;
    }

}
