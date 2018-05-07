package org.jahia.modules.external.rest;

import javax.jcr.observation.Event;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of event for Rest API
 */
public class ExternalEvent implements Event {

    private int type = Event.NODE_ADDED;
    private String path;
    private String userID;
    private String identifier;
    private Map info = new HashMap();
    private String userData;
    private long date = 0L;

    @Override
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    @XmlJavaTypeAdapter(EventInfoAdapter.class)
    public Map getInfo() {
        return info;
    }

    public void setInfo(Map info) {
        this.info = info;
    }

    @Override
    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    @Override
    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
