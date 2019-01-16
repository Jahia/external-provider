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
package org.jahia.modules.external.test.db;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.osgi.BundleResource;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WriteableMappedDatabaseProvider extends MappedDatabaseDataSource implements ExternalDataSource.Writable {

    private static final Logger logger = LoggerFactory.getLogger(MappedDatabaseDataSource.class);

    @Override
    public void move(String oldPath, String newPath) throws RepositoryException {
        //
    }

    @Override
    public void order(String path, List<String> children) throws RepositoryException {
        //
    }

    @Override
    public void removeItemByPath(String path) throws RepositoryException {
        deleteRow(path);
    }

    @Override
    public void saveItem(ExternalData data) throws RepositoryException {
        String type = getNodeTypeName(data.getPath());
        if (type.equals(getSchemaNodeType()) || type.equals(getTableNodeType())) {
            throw new UnsupportedRepositoryOperationException();
        }
        if (data.isNew()) {
            insertRow(data.getPath(), data);
            data.setId(data.getPath().replace('/', '_'));
        } else {
            updateRow(data.getPath(), data);
        }

    }

    private void insertRow(String path, ExternalData data) throws RepositoryException {
        String[] pathTokens = StringUtils.split(path, '/');
        if (pathTokens.length != 2) {
            throw new PathNotFoundException(path);
        }
        String table = pathTokens[0];

        Connection conn = null;
        Statement stmt = null;
        Map<String, String> cols = getColumnValues(data, table);
        String v = " (" + StringUtils.join(cols.keySet(), ",") + ") values (" + StringUtils.join(cols.values(), ",") + ")";
        try {
            conn = getConnection();

            String query = "insert into " + table + v;

            stmt = conn.createStatement();
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            logger.debug(e.getMessage(), e);
            throw new RepositoryException(path,e);
        } finally {
            DbUtility.close(conn, stmt, null);
        }
    }


    private void updateRow(String path, ExternalData data) throws RepositoryException {
        String[] pathTokens = StringUtils.split(path, '/');
        if (pathTokens.length != 2) {
            throw new PathNotFoundException(path);
        }
        String table = pathTokens[0];
        String rowId = pathTokens[1];

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            List<String> primaryKeys = getTablePrimaryKeys(table, conn);

            String[] rowData = getValuesForPrimayKeys(rowId);

            Map<String, String> cols = getColumnValues(data, table);
            StringBuilder s = new StringBuilder();
            for (Map.Entry<String, String> entry : cols.entrySet()) {
                if (s.length() > 0) {
                    s.append(" , ");
                }
                s.append(entry.getKey()).append("=").append(entry.getValue());
            }
            String query = null;
            if (primaryKeys.size() == 1) {
                query = "update " + table + " set " + s.toString() + " where " + primaryKeys.get(0) + "='"+rowData[0]+"'";
            } else {
                StringBuilder buff = new StringBuilder();
                for (String col : primaryKeys) {
                    if (buff.length() > 0) {
                        buff.append(" and ");
                    }
                    buff.append(col).append("='"+rowData[primaryKeys.indexOf(col)]+"'");
                }
                query = "update " + table + " set " + s.toString() + " where " + buff.toString();
            }

            stmt = conn.createStatement();

            stmt.executeUpdate(query);
        } catch (SQLException e) {
            logger.debug(e.getMessage(), e);
            throw new RepositoryException(path,e);
        } finally {
            DbUtility.close(conn, stmt, rs);
        }
    }

    private void deleteRow(String path) throws RepositoryException {
        String[] pathTokens = StringUtils.split(path, '/');
        if (pathTokens.length != 2) {
            throw new PathNotFoundException(path);
        }
        String table = pathTokens[0];
        String rowId = pathTokens[1];

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            List<String> primaryKeys = getTablePrimaryKeys(table, conn);

            String[] rowData = getValuesForPrimayKeys(rowId);

            String query = null;
            if (primaryKeys.size() == 1) {
                query = "delete from " + table + " where " + primaryKeys.get(0) + "='"+rowData[0]+"'";
            } else {
                StringBuilder buff = new StringBuilder();
                for (String col : primaryKeys) {
                    if (buff.length() > 0) {
                        buff.append(" and ");
                    }
                    buff.append(col).append("='"+rowData[primaryKeys.indexOf(col)]+"'");
                }
                query = "delete from " + table + " where " + buff.toString();
            }

            stmt = conn.createStatement();

            stmt.executeUpdate(query);
        } catch (SQLException e) {
            logger.debug(e.getMessage(), e);
            throw new RepositoryException(path,e);
        } finally {
            DbUtility.close(conn, stmt, rs);
        }
    }

    private Map<String, String> getColumnValues(ExternalData data, String table) throws RepositoryException {
        ExtendedNodeType nodeType = null;
        try {
            nodeType = NodeTypeRegistry.getInstance().getNodeType(getRowNodeTypeName(table));
        } catch (NoSuchNodeTypeException e) {
            throw new PathNotFoundException(e);
        }

        Map<String,String> cols = new LinkedHashMap<String, String>();
        for (ExtendedPropertyDefinition def : nodeType.getDeclaredPropertyDefinitions()) {
            if (!def.isInternationalized() && data.getProperties().containsKey(def.getName())) {
                String[] s = data.getProperties().get(def.getName());
                if (def.getRequiredType() == PropertyType.STRING) {
                    cols.put(def.getName(), "'" + s[0] + "'");
                } else if (def.getRequiredType() == PropertyType.LONG || def.getRequiredType() == PropertyType.DOUBLE) {
                    cols.put(def.getName(), s[0] );
                }
            }
        }
        return cols;
    }



    @Override
    protected Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:derby:"+databasePath.getPath());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new JahiaRuntimeException(e);
        }
    }

    private File databasePath;

    @Override
    public void start() {
        try {
            long timer = System.currentTimeMillis();

            databasePath = File.createTempFile("derby","");
            databasePath.delete();
            final JahiaTemplatesPackage templatePackageById = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageById("external-provider-test");
            extract(templatePackageById, templatePackageById.getResource("/toursdb"), databasePath);

            Connection connection = null;
            try {
                Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
                connection = getConnection();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new JahiaRuntimeException(e);
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info("Provider successfully started in {} ms", System.currentTimeMillis() - timer);
        } catch (Exception e) {
            logger.error("Cannot start",e);
        }
    }


    @Override
    public void stop() {
        try {
            try {
                DriverManager.getConnection("jdbc:derby:"+databasePath.getPath() + ";shutdown=true");
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }

            FileUtils.deleteDirectory(databasePath);

            logger.info("Provider stopped");
        } catch (IOException e) {
            logger.error("Cannot stop",e);
        }
    }

    private static void extract(JahiaTemplatesPackage p, org.springframework.core.io.Resource r, File f) throws Exception {
        if ((r instanceof BundleResource && r.contentLength() == 0) || (!(r instanceof BundleResource) && r.getFile().isDirectory())) {
            f.mkdirs();
            String path = r.getURI().getPath();
            for (org.springframework.core.io.Resource resource : p.getResources(path.substring(path.indexOf("/toursdb")))) {
                extract(p, resource, new File(f, resource.getFilename()));
            }
        } else {
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(f);
                IOUtils.copy(r.getInputStream(), output);
            } finally {
                IOUtils.closeQuietly(output);
            }
        }
    }
}
