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
package org.jahia.modules.external.test.listener;

import com.google.common.collect.Sets;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.services.SpringContextSingleton;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.*;

public class ApiDataSource implements ExternalDataSource {

    private static final Map<String, ExternalData> nodes;
    static {
        Map<String, ExternalData> initNodes = new HashMap<>();

        Map<String, String[]> properties = new HashMap<>();
        properties.put("jcr:created", new String[] {"2017-10-10T10:50:43.000+02:00"});
        properties.put("jcr:lastModified", new String[] {"2017-10-10T10:50:43.000+02:00"});

        initNodes.put("/", new ExternalData("/", "/", "jnt:contentFolder", new HashMap<>()));
        initNodes.put("/toto", new ExternalData("/toto", "/toto", "jnt:bigText", properties));
        initNodes.put("/tata", new ExternalData("/tata", "/tata", "jnt:bigText", properties));
        initNodes.put("/titi", new ExternalData("/titi", "/titi", "jnt:bigText", properties));
        initNodes.put("/tutu", new ExternalData("/tutu", "/tutu", "jtestnt:binary", new HashMap<>()));

        initNodes.get("/toto").setI18nProperties(geti18nProps("/toto"));
        initNodes.get("/tata").setI18nProperties(geti18nProps("/tata"));
        initNodes.get("/titi").setI18nProperties(geti18nProps("/titi"));

        nodes = Collections.unmodifiableMap(initNodes);
    }

    private static Map<String, Map<String, String[]>> geti18nProps(String path) {
        Map<String, Map<String, String[]>> i18nProperties = new HashMap<>();

        Map<String, String[]> enProps = new HashMap<>();
        enProps.put("text", new String[] {path + " en"});
        Map<String, String[]> frProps = new HashMap<>();
        frProps.put("text", new String[] {path + " fr"});

        i18nProperties.put("en", enProps);
        i18nProperties.put("fr", frProps);

        return i18nProperties;
    }

    private List<String> log = new ArrayList<>();

    public List<String> getLog() {
        return log;
    }

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        log.add("ApiDataSource.getChildren:"+path);
        if (path.equals("/")) {
            return Arrays.asList("toto", "tata", "titi", "tutu");
        }
        return Collections.emptyList();
    }

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        log.add("ApiDataSource.getItemByIdentifier:"+identifier);
        if(nodes.containsKey(identifier)) {
            return nodes.get(identifier);
        } else {
            throw new ItemNotFoundException(identifier);
        }
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        log.add("ApiDataSource.getItemByPath:"+path);
        if(nodes.containsKey(path)) {
            return nodes.get(path);
        } else {
            throw new PathNotFoundException(path);
        }
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return Sets.newHashSet("jnt:contentFolder", "jnt:bigText", "jtestnt:binary");
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return true;
    }

    @Override
    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean itemExists(String path) {
        log.add("ApiDataSource.itemExists:"+path);
        return nodes.containsKey(path);
    }
}
