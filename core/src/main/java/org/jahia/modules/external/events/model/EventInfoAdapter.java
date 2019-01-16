/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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

import org.jahia.modules.external.ExternalBinaryImpl;
import org.jahia.modules.external.ExternalData;

import javax.jcr.Binary;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adapter for info field, converting externalData into ExternalData object
 */
public class EventInfoAdapter extends XmlAdapter<Map<String, Object>, Map<String, Object>> {


    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> unmarshal(Map<String, Object> info) throws Exception {
        Map<String, Object> dataAsMap = (Map<String, Object>) info.get("externalData");

        if (dataAsMap != null) {
            Map<String, Object> transformed = new HashMap<>(info);

            ExternalData data = new ExternalData((String) dataAsMap.get("id"), (String) dataAsMap.get("path"), (String) dataAsMap.get("type"),
                    dataAsMap.containsKey("properties") ? convertMembersFromListToArray((Map<String, List<String>>) dataAsMap.get("properties"), String.class) : Collections.emptyMap());

            if (dataAsMap.containsKey("mixin")) {
                data.setMixin((List<String>) dataAsMap.get("mixin"));
            }

            if (dataAsMap.containsKey("i18nProperties")) {
                Map<String, Map<String, List<String>>> i18nProperties = (Map<String, Map<String, List<String>>>) dataAsMap.get("i18nProperties");
                Map<String, Map<String, String[]>> convertedI18nProperties = new HashMap<>();
                for (Map.Entry<String, Map<String, List<String>>> entry : i18nProperties.entrySet()) {
                    convertedI18nProperties.put(entry.getKey(), convertMembersFromListToArray(entry.getValue(), String.class));
                }
                data.setI18nProperties(convertedI18nProperties);
            }

            if (dataAsMap.containsKey("binaryProperties")) {
                data.setBinaryProperties(convertMembersFromListToArray((Map<String, List<String>>) dataAsMap.get("binaryProperties"), Binary.class, s-> (new ExternalBinaryImpl(new ByteArrayInputStream(Base64.getDecoder().decode(s))))));
            }

            transformed.put("externalData", data);

            return transformed;
        }

        return info;
    }

    @Override
    public Map<String, Object> marshal(Map<String, Object> info) throws Exception {
        return info;
    }

    private <T> Map<String, T[]> convertMembersFromListToArray(Map<String, List<T>> map, Class<T> itemType) {
        return convertMembersFromListToArray(map,itemType,u->u);
    }

    @SuppressWarnings("unchecked")
    private <T,U> Map<String, T[]> convertMembersFromListToArray(Map<String, List<U>> map, Class<T> itemType, Function<U,T> f) {
        Map<String, T[]> m = new HashMap<>();
        for (Map.Entry<String, List<U>> entry : map.entrySet()) {
            List<U> list = entry.getValue();

            m.put(entry.getKey(), list.stream().map(f).collect(Collectors.toList()).toArray((T[]) Array.newInstance(itemType, list.size())));
        }
        return m;
    }

}
