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
