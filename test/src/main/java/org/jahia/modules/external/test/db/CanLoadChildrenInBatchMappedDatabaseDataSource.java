/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.external.test.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;

/**
 * @author Christophe Laprun
 */
public class CanLoadChildrenInBatchMappedDatabaseDataSource extends MappedDatabaseDataSource implements ExternalDataSource.CanLoadChildrenInBatch {
    @Override
    public List<ExternalData> getChildrenNodes(String path) throws RepositoryException {
        String nodeType;
        try {
            nodeType = getNodeTypeName(path);
        } catch (PathNotFoundException e) {
            // cannot handle that path
            return Collections.emptyList();
        }
        if (nodeType.equals(getSchemaNodeType())) {
            final List<String> tableNames = getTableNames();
            final List<ExternalData> children = new ArrayList<ExternalData>(tableNames.size());
            for (String tableName : tableNames) {
                children.add(getItemByPath("/" + tableName));
            }
            return children;
        } else if (nodeType.equals(getTableNodeType())) {
            final List<String> rowIDs = getRowIDs(StringUtils.substringAfterLast(path, "/"), Collections.<String, Value>emptyMap());
            final List<ExternalData> children = new ArrayList<ExternalData>(rowIDs.size());
            for (String rowID : rowIDs) {
                children.add(getItemByPath(path + "/" + rowID));
            }
            return children;
        } else {
            return Collections.emptyList();
        }
    }
}
