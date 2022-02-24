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
