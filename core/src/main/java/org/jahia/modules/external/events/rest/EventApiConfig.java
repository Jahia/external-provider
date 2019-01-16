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
package org.jahia.modules.external.events.rest;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OSGI Configuration for event API
 */
public class EventApiConfig implements ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(EventApiConfig.class);

    private class ApiKeyCapabilities {
        Set<String> providers;

        ApiKeyCapabilities(Set<String> providers) {
            this.providers = providers;
        }

        public Set<String> getProviders() {
            return providers;
        }

        public void setProviders(Set<String> providers) {
            this.providers = providers;
        }
    }

    private Map<String, ApiKeyCapabilities> apiKeys = new HashMap<>();

    @Override
    public void updated(Dictionary<String, ?> properties) {
        apiKeys.clear();

        if (properties != null) {
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (StringUtils.endsWith(key, "event.api.key")) {

                    String apiKey = (String) properties.get(key);
                    String apiKeyProvidersValue = (String) properties.get(StringUtils.substringBeforeLast(key, ".key") + ".providers");
                    Set<String> apiKeyProviders =  StringUtils.isNotEmpty(apiKeyProvidersValue) ? Stream.of(apiKeyProvidersValue.split(",")).collect(Collectors.toSet()) : new HashSet<>();

                    if (apiKeys.containsKey(apiKey)) {
                        apiKeys.get(apiKey).getProviders().addAll(apiKeyProviders);
                    } else {
                        apiKeys.put(apiKey, new ApiKeyCapabilities(apiKeyProviders));
                    }
                }
            }
        }

        logger.info("External Provider Events API configuration reloaded");
    }

    /**
     * Checks if apiKey is allowed to access provider event API
     *
     * @return <code>true</code> if the apiKey is allowed to access the provider; <code>false</code> otherwise
     */
    boolean checkApiKey(String apiKey, String providerKey) {
        ApiKeyCapabilities apiKeyCapabilities = apiKeys.get(apiKey);
        if (apiKeyCapabilities != null) {
            return apiKeyCapabilities.getProviders().isEmpty() || apiKeyCapabilities.getProviders().contains(providerKey);
        }

        return false;
    }
}
