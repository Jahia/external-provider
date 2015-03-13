/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.external.test.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.jahia.modules.external.ExternalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the external data source that uses sample database to fetch the data with a generic mappings of table and row.
 * 
 * @author Sergiy Shyrkov
 */
public class GenericDatabaseDataSource extends BaseDatabaseDataSource {

    static final String DATA_TYPE_ROW = "jtestnt:dbRow".intern();

    static final String DATA_TYPE_SCHEMA = "jtestnt:dbSchema".intern();

    static final String DATA_TYPE_TABLE = "jtestnt:dbTable".intern();

    static final Logger logger = LoggerFactory.getLogger(GenericDatabaseDataSource.class);

    private static final Set<String> SUPPORTED_NODETYPES = new HashSet<String>(Arrays.asList(DATA_TYPE_SCHEMA,
            DATA_TYPE_TABLE, DATA_TYPE_ROW));

    @Override
    protected String getRowID(ResultSet rs, List<String> primaryKeys) throws SQLException {
        String val = null;
        if (primaryKeys.size() == 1) {
            val = rs.getString(primaryKeys.get(0));
            if (val != null) {
                val = Base64.encodeBase64URLSafeString(val.getBytes(Charsets.UTF_8));
            }
        } else {
            StringBuilder buff = new StringBuilder();
            for (String col : primaryKeys) {
                if (buff.length() > 0) {
                    buff.append("\n");
                }
                buff.append(rs.getString(col));
            }
            val = Base64.encodeBase64URLSafeString(buff.toString().getBytes(Charsets.UTF_8));
        }

        return val;
    }

    @Override
    protected String getRowNodeTypeName(String tableName) {
        return DATA_TYPE_ROW;
    }

    @Override
    protected ExternalData getRowProperties(String path, String type, String table, ResultSet rs) throws SQLException {
        Map<String, String[]> props = new LinkedHashMap<String, String[]>();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            String value = rs.getString(columnName);
            if (value != null) {
                props.put(columnName.toLowerCase(), new String[] { value });
            }
        }
        return new ExternalData(path.replace('/','_'), path, type, props);
    }

    @Override
    protected String getSchemaNodeType() {
        return DATA_TYPE_SCHEMA;
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return SUPPORTED_NODETYPES;
    }

    @Override
    protected List<String> getTableNames() {
        List<String> tables = new LinkedList<String>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement("select * from sys.systables where tabletype = 'T'");
            rs = stmt.executeQuery();
            while (rs.next()) {
                tables.add(rs.getString("tablename"));
            }
            return tables;
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            DbUtility.close(conn, stmt, rs);
        }
    }

    @Override
    protected String getTableNodeType() {
        return DATA_TYPE_TABLE;
    }

    @Override
    protected String[] getValuesForPrimayKeys(String rowId) {
        return StringUtils.split(new String(Base64.decodeBase64(rowId), Charsets.UTF_8), '\n');
    }

}
