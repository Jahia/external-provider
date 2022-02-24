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
package org.jahia.modules.external.modules;

import org.jahia.data.templates.JahiaTemplatesPackage;

/**
 * Module sources external provider utilities.
 */
public class ModulesUtils {

    private ModulesUtils() {
    }

    /**
     * Get key of the external provider of module sources.
     *
     * @param module The module
     * @return The key of the module sources provider
     */
    public static String getSourcesProviderKey(JahiaTemplatesPackage module) {
        return ("module-" + module.getId() + "-" + module.getVersion().toString());
    }
}
