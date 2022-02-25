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
