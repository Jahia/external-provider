/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
