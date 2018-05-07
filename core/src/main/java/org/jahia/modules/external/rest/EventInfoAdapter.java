package org.jahia.modules.external.rest;

import org.jahia.modules.external.ExternalData;

import javax.jcr.Binary;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    convertMembersFromListToArray((Map<String, List<String>>) dataAsMap.get("properties"), String.class));

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
                data.setBinaryProperties(convertMembersFromListToArray((Map<String, List<Binary>>) dataAsMap.get("binaryProperties"), Binary.class));
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

    @SuppressWarnings("unchecked")
    private <T> Map<String, T[]> convertMembersFromListToArray(Map<String, List<T>> map, Class<T> itemType) {
        Map<String, T[]> m = new HashMap<>();
        for (Map.Entry<String, List<T>> entry : map.entrySet()) {
            List<T> list = entry.getValue();
            m.put(entry.getKey(), list.toArray((T[]) Array.newInstance(itemType, list.size())));
        }
        return m;
    }

}
