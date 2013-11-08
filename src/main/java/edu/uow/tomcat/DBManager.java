package edu.uow.tomcat;

import org.apache.catalina.*;
import org.apache.catalina.session.StandardSession;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBManager implements Manager, Lifecycle{
    private static Logger log = Logger.getLogger("DBManager");
    protected static String jndi = "jdbc/tomcatSessions";
    private String DAOClass = "edu.uow.tomcat.OracleDAO";
    private String JDBCClass = "oracle.jdbc.driver.OracleDriver";

    private DBSessionTrackerValve trackerValve;
    private ThreadLocal<StandardSession> currentSession = new ThreadLocal<StandardSession>();
    private Serializer serializer;

    private DAO dao;

    //Either 'kryo' or 'java'
    private String serializationStrategyClass = "edu.uow.tomcat.JavaSerializer";

    private Container container;
    private int maxInactiveInterval;

    @Override
    public Container getContainer() {
        return container;
    }

    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    @Override
    public boolean getDistributable() {
        return false;
    }

    @Override
    public void setDistributable(boolean b) {

    }

    @Override
    public String getInfo() {
        return "DB Session Manager";
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public void setMaxInactiveInterval(int i) {
        maxInactiveInterval = i;
    }

    @Override
    public int getSessionIdLength() {
        return 37;
    }

    @Override
    public void setSessionIdLength(int i) {

    }

    @Override
    public long getSessionCounter() {
        return 10000000;
    }

    @Override
    public void setSessionCounter(long i) {

    }

    @Override
    public int getMaxActive() {
        return 1000000;
    }

    @Override
    public void setMaxActive(int i) {

    }

    @Override
    public int getActiveSessions() {
        return 1000000;
    }

    @Override
    public long getExpiredSessions() {
        return 0;
    }

    @Override
    public void setExpiredSessions(long i) {

    }

    public int getRejectedSessions() {
        return 0;
    }

    public void setSerializationStrategyClass(String strategy) {
        this.serializationStrategyClass = strategy;
    }

    public void setRejectedSessions(int i) {
    }

    @Override
    public int getSessionMaxAliveTime() {
        return maxInactiveInterval;
    }

    @Override
    public void setSessionMaxAliveTime(int i) {

    }

    @Override
    public int getSessionAverageAliveTime() {
        return 0;
    }

    @Override
    public int getSessionCreateRate() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getSessionExpireRate() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void load() throws ClassNotFoundException, IOException {
    }

    public void unload() throws IOException {
    }

    @Override
    public void backgroundProcess() {
        processExpires();
    }

    public void addLifecycleListener(LifecycleListener lifecycleListener) {
    }

    public LifecycleListener[] findLifecycleListeners() {
        return new LifecycleListener[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeLifecycleListener(LifecycleListener lifecycleListener) {
    }

    @Override
    public void init() throws LifecycleException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void add(Session session) {
        try {
            save(session);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error adding new session", ex);
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void changeSessionId(Session session) {
        session.setId(UUID.randomUUID().toString());
    }

    @Override
    public Session createEmptySession() {
        DBSession session = new DBSession(this);
        session.setId(UUID.randomUUID().toString());
        session.setMaxInactiveInterval(maxInactiveInterval);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setNew(true);
        currentSession.set(session);
        log.info("Created new empty session " + session.getIdInternal());
        log.info("Created new empty session " + session.getId());
        return session;
    }

    /**
     * @deprecated
     */
    public org.apache.catalina.Session createSession() {
        return createEmptySession();
    }

    public org.apache.catalina.Session createSession(java.lang.String sessionId) {
        StandardSession session = (DBSession) createEmptySession();

        log.info("Created session with id " + session.getIdInternal() + " ( " + sessionId + ")");
        if (sessionId != null) {
            session.setId(sessionId);
        }

        return session;
    }

    public org.apache.catalina.Session[] findSessions() {
        try {
            List<Session> sessions = new ArrayList<Session>();
            for(String sessionId : keys()) {
                sessions.add(loadSession(sessionId));
            }
            return sessions.toArray(new Session[sessions.size()]);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected org.apache.catalina.session.StandardSession getNewSession() {
        log.info("getNewSession()");
        return (DBSession) createEmptySession();
    }

    public void start() throws LifecycleException {
        for (Valve valve : getContainer().getPipeline().getValves()) {
            if (valve instanceof DBSessionTrackerValve) {
                trackerValve = (DBSessionTrackerValve) valve;
                trackerValve.setDBManager(this);
                log.info("Attached to DB Tracker Valve");
                break;
            }
        }
        try {
            initSerializer();
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "Unable to load serializer", e);
            throw new LifecycleException(e);
        } catch (InstantiationException e) {
            log.log(Level.SEVERE, "Unable to load serializer", e);
            throw new LifecycleException(e);
        } catch (IllegalAccessException e) {
            log.log(Level.SEVERE, "Unable to load serializer", e);
            throw new LifecycleException(e);
        }
        log.info("Will expire sessions after " + getMaxInactiveInterval() + " seconds");
        initDbConnection();
    }

    public void stop() throws LifecycleException {
        //close();
    }

    @Override
    public void destroy() throws LifecycleException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LifecycleState getState() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getStateName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Session findSession(String id) throws IOException {
        return loadSession(id);
    }

    public static String getJndi() {
        return jndi;
    }

    public static void setJndi(String jndi) {
        DBManager.jndi = jndi;
    }

    public String[] keys() throws IOException {
        return dao.getAllActive();
    }


    public Session loadSession(String id) throws IOException {

        if (id == null || id.length() == 0) {
            return createEmptySession();
        }

        StandardSession session = currentSession.get();

        if (session != null) {
            if (id.equals(session.getId())) {
                return session;
            } else {
                currentSession.remove();
            }
        }
        try {
            log.info("Loading session " + id + " from DB");

            byte[] sessionData = getDao().getSession(id);
            if (sessionData == null) {
                log.info("Session " + id + " not found in DB");
                StandardSession ret = getNewSession();
                ret.setId(id);
                currentSession.set(ret);
                return ret;
            }

            session = (DBSession) createEmptySession();
            session.setId(id);
            session.setManager(this);
            serializer.deserializeInto(sessionData, session);

            session.setMaxInactiveInterval(-1);
            session.access();
            session.setValid(true);
            session.setNew(false);

            if (log.isLoggable(Level.FINE)) {
                log.info("Session Contents [" + session.getId() + "]:");
                for (Object name : Collections.list(session.getAttributeNames())) {
                    log.info("  " + name);
                }
            }

            log.info("Loaded session id " + id);
            currentSession.set(session);
            return session;
        } catch (IOException e) {
            log.severe(e.getMessage());
            throw e;
        } catch (ClassNotFoundException ex) {
            log.log(Level.SEVERE, "Unable to deserialize session ", ex);
            throw new IOException("Unable to deserializeInto session", ex);
        }
    }

    public void save(Session session) throws IOException {
        try {
            log.info("Saving session " + session + " into DB");

            StandardSession standardsession = (DBSession) session;

            if (log.isLoggable(Level.FINE)) {
                log.info("Session Contents [" + session.getId() + "]:");
                for (Object name : Collections.list(standardsession.getAttributeNames())) {
                    log.info("  " + name);
                }
            }

            byte[] data = serializer.serializeFrom(standardsession);

            String principal = "";
            if (session.getPrincipal()!=null && session.getPrincipal().getName()!=null) {
                principal = session.getPrincipal().getName();
            }
            if (((DBSession) session).isNew()) {
                dao.insertSession(standardsession.getId(),principal,(data));
                log.info("Inserted session with id " + session.getIdInternal());
            } else {
                dao.updateSession(standardsession.getId(),principal, data);
                log.info("Updated session with id " + session.getIdInternal());
            }

        } catch (IOException e) {
            log.severe(e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            currentSession.remove();
            log.info("Session removed from ThreadLocal :" + session.getIdInternal());
        }
    }

    public void remove(Session session) {
        log.info("Removing session ID : " + session.getId());
        dao.deleteSession(session.getId());
        currentSession.remove();
    }

    @Override
    public void remove(Session session, boolean update) {
        remove(session);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void processExpires() {
        long olderThan = System.currentTimeMillis() - (getMaxInactiveInterval() * 1000);
        log.fine("Looking for sessions less than for expiry in DB : " + olderThan);
        getDao().deleteExpiredSessions(olderThan);
    }

    private void initDbConnection() throws LifecycleException {
        if (dao == null) {
            try {
                dao = (DAO) Class.forName(getDAOClass()).newInstance();
                dao.setJndi(getJndi());
                dao.init();
            } catch (InstantiationException e) {
                log.severe(e.getLocalizedMessage());
            } catch (IllegalAccessException e) {
                log.severe(e.getLocalizedMessage());
            } catch (ClassNotFoundException e) {
                log.severe(e.getLocalizedMessage());
            }
        }
    }

    private void initSerializer() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        log.info("Attempting to use serializer :" + serializationStrategyClass);
        serializer = (Serializer) Class.forName(serializationStrategyClass).newInstance();

        Loader loader = null;

        if (container != null) {
            loader = container.getLoader();
        }
        ClassLoader classLoader = null;

        if (loader != null) {
            classLoader = loader.getClassLoader();
        }
        serializer.setClassLoader(classLoader);
    }

    public String getDAOClass() {
        return DAOClass;
    }

    public void setDAOClass(String DAOClass) {
        this.DAOClass = DAOClass;
    }

    public DAO getDao() {
        return dao;
    }

    public void setDao(DAO dao) {
        this.dao = dao;
    }
}
