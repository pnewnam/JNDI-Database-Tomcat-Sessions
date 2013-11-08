package edu.uow.tomcat;

import javax.naming.NamingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * CREATE TABLE TOMCAT_SESSION
 (
 SESSION_ID VARCHAR2(256) NOT NULL
 , LAST_MODIFIED DATE NOT NULL
 , SESSION_DATA BLOB
 , PRINCIPAL VARCHAR2(256),
 , CONSTRAINT TOMCAT_SESSION_PK PRIMARY KEY
 (
 SESSION_ID
 )
 ENABLE
 );
 */

public class OracleDAO extends DAO {

    private static String SQL_INSERT = "INSERT INTO TOMCAT_SESSION (SESSION_ID, LAST_MODIFIED, SESSION_DATA, PRINCIPAL) VALUES  (?,SYSDATE,?,?)";
    private static String SQL_UPDATE = "UPDATE TOMCAT_SESSION SET LAST_MODIFIED=SYSDATE, SESSION_DATA=?, PRINCIPAL=? WHERE SESSION_ID=?";
    private static String SQL_DELETE = "DELETE FROM TOMCAT_SESSION WHERE SESSION_ID=?";
    private static String SQL_DELETE_EXPIRED = "DELETE FROM TOMCAT_SESSION WHERE LAST_MODIFIED <=?";
    private static String SQL_SELECT = "SELECT SESSION_ID, LAST_MODIFIED, SESSION_DATA FROM TOMCAT_SESSION WHERE SESSION_ID=?";
    private static String SQL_ALL_ACTIVE = "SELECT SESSION_ID FROM TOMCAT_SESSION";
    private static Logger log = Logger.getLogger("OracleDAO");

    @Override
    public void deleteSession(String sessionId) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = getConnection();
            ps = connection.prepareStatement(SQL_DELETE);

            ps.setString(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe(e.getLocalizedMessage());
        } catch (NamingException e) {
            log.severe(e.getLocalizedMessage());
        } finally {
            close(connection,ps,null);
        }
    }

    @Override
    public void insertSession(String sessionId,String principal, byte[] data) {
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(SQL_INSERT);

            ps.setString(1, sessionId);
            ps.setBinaryStream(2, new ByteArrayInputStream(data), data.length);
            ps.setString(3,principal);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe(e.getLocalizedMessage());
        } catch (NamingException e) {
            log.severe(e.getLocalizedMessage());
        } finally {
            close(connection,ps,null);
        }
    }

    @Override
    public void updateSession(String sessionId, String principal, byte[] data) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = getConnection();
            ps = connection.prepareStatement(SQL_UPDATE);

            ps.setString(3, sessionId);
            ps.setString(2, principal);
            ps.setBinaryStream(1, new ByteArrayInputStream(data), data.length);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe(e.getLocalizedMessage());
        } catch (NamingException e) {
            log.severe(e.getLocalizedMessage());
        } finally {
            close(connection,ps,null);
        }
    }

    @Override
    public byte[] getSession(String sessionId) {
        byte[] data = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(SQL_SELECT);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ps.setString(1, sessionId);
            rs = ps.executeQuery();

            while(rs.next()) {
                log.info("load blob");
                InputStream is = rs.getBlob(3).getBinaryStream();
                byte[] b = new byte[10000];
                try {

                    while (is.read(b) > -1) {
                        log.info("read blob" + b.length);
                        byteArrayOutputStream.write(b);
                    }
                    data = byteArrayOutputStream.toByteArray();
                } catch (IOException e) {
                    log.severe(e.getLocalizedMessage());
                }
                log.info("data size for " + sessionId + " " + data.length);
            }

        } catch (SQLException e) {
            log.severe(e.getLocalizedMessage());
        } catch (NamingException e) {
            log.severe(e.getLocalizedMessage());
        } finally {
            close(connection,ps,rs);
        }
        return data;
    }

    @Override
    public void deleteExpiredSessions(long lastModified) {
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(SQL_DELETE_EXPIRED);

            ps.setDate(1, new Date(lastModified));
            int deleted = ps.executeUpdate();
        } catch (SQLException e) {
            log.severe(e.getLocalizedMessage());
        } catch (NamingException e) {
            log.severe(e.getLocalizedMessage());
        } finally {
            close(connection,ps,null);
        }

    }

    @Override
    public String[] getAllActive() {
        List<String> ret = new ArrayList<String>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(SQL_SELECT);

            rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(rs.getString(1));
            }

        } catch (SQLException e) {
            log.severe(e.getLocalizedMessage());
        } catch (NamingException e) {
            log.severe(e.getLocalizedMessage());
        } finally {
            close(connection,ps,null);
        }
        return ret.toArray(new String[ret.size()]);
    }


    private void close(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) {
        try {
            if (resultSet!=null && !resultSet.isClosed()) resultSet.close();
            if (preparedStatement!=null && !preparedStatement.isClosed()) preparedStatement.close();
            if (connection!=null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            log.severe(e.getLocalizedMessage());
        }

    }

    @Override
    void init() {
        super.init();    //To change body of overridden methods use File | Settings | File Templates.

    }

}
