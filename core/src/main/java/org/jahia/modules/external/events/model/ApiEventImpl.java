/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.external.events.model;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.hibernate.validator.constraints.NotEmpty;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.events.validation.ValidExternalData;
import org.jahia.modules.external.events.validation.ValidISO8601;
import org.jahia.services.content.ApiEvent;

import javax.jcr.observation.Event;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of event for Rest API
 */
public class ApiEventImpl implements ApiEvent {

    public enum EventType {
        NODE_ADDED(Event.NODE_ADDED),
        NODE_REMOVED(Event.NODE_REMOVED),
        PROPERTY_ADDED(Event.PROPERTY_ADDED),
        PROPERTY_REMOVED(Event.PROPERTY_REMOVED),
        PROPERTY_CHANGED(Event.PROPERTY_CHANGED),
        NODE_MOVED(Event.NODE_MOVED);

        private final int value;

        EventType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Pattern(regexp = "NODE_ADDED|NODE_REMOVED|PROPERTY_ADDED|PROPERTY_REMOVED|PROPERTY_CHANGED|NODE_MOVED")
    private String type = "NODE_ADDED";

    @NotEmpty
    private String path;
    private String userID = StringUtils.EMPTY;
    private String identifier;
    @ValidExternalData
    private Map info = new HashMap();
    private String userData;

    @ValidISO8601
    private String date;

    public ApiEventImpl() {
    }

    public ApiEventImpl(ExternalData data) {
        setPath(data.getPath());
        setIdentifier(data.getId());
        info.put("externalData", data);
    }

    @Override
    public int getType() {
        return EventType.valueOf(type).getValue();
    }

    public void setType(String type) {
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
        return ISO8601.parse(date).getTimeInMillis();
    }

    public void setDate(String date) {
        this.date = date;
    }
}
