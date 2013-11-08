package edu.uow.tomcat;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

import java.util.logging.Logger;

public class DBSession extends StandardSession {
    private static Logger log = Logger.getLogger("DBSession");
    private boolean isValid = true;
    private boolean isNew = true;

    public DBSession(Manager manager) {
        super(manager);
    }

    @Override
    protected boolean isValidInternal() {
        return isValid;
    }

    @Override
    public boolean isValid() {
        return isValidInternal();
    }

    @Override
    public void setValid(boolean isValid) {
        this.isValid = isValid;
        if (!isValid) {
            String keys[] = keys();
            for (String key : keys) {
                removeAttributeInternal(key, false);
            }
            getManager().remove(this);

        }
    }

    @Override
    public void invalidate() {
        setValid(false);
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }
}
