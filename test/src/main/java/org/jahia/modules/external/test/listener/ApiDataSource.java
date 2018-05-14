/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.*;

public class ApiDataSource implements ExternalDataSource {

    private static final Map<String, String> nodes;
    static {
        Map<String, String> initNodes = new HashMap<>();
        initNodes.put("/", "jnt:contentList");
        initNodes.put("/toto", "jnt:bigText");
        initNodes.put("/tata", "jnt:bigText");
        initNodes.put("/titi", "jnt:bigText");
        initNodes.put("/tutu", "jtestnt:binary");
        nodes = Collections.unmodifiableMap(initNodes);
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
            return new ExternalData(identifier, identifier, nodes.get(identifier), new HashMap<>());
        } else {
            throw new ItemNotFoundException(identifier);
        }
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        log.add("ApiDataSource.getItemByPath:"+path);
        if(nodes.containsKey(path)) {
            return new ExternalData(path, path, nodes.get(path), new HashMap<>());
        } else {
            throw new PathNotFoundException(path);
        }
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return Sets.newHashSet("jnt:contentList", "jnt:bigText", "jtestnt:binary");
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
