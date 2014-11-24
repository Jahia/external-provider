package org.jahia.modules.external.admin.mount;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRMountPointNode;

import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.lang.String;

/**
 * Created by kevan on 21/11/14.
 */
public abstract class AbstractMountPointFactory implements Serializable{
    private static final long serialVersionUID = 6745956005105508413L;

    private String inEditMountPointNodePath;

    protected AbstractMountPointFactory() {
    }

    public void populate(JCRNodeWrapper nodeWrapper) throws RepositoryException {
        this.inEditMountPointNodePath = nodeWrapper.getPath();
    }
    public boolean isEdit() {
        return StringUtils.isNotEmpty(inEditMountPointNodePath);
    }
    public String getName(String suffixedName) {
        return suffixedName.endsWith(JCRMountPointNode.MOUNT_SUFFIX) ?
                suffixedName.substring(0, suffixedName.length() - JCRMountPointNode.MOUNT_SUFFIX.length()) : suffixedName;
    }

    public abstract String getName();
    public abstract String getLocalPath();
    public abstract void setProperties(JCRNodeWrapper mountNode) throws RepositoryException;

    public String getInEditMountPointNodePath() {
        return inEditMountPointNodePath;
    }

    public void setInEditMountPointNodePath(String inEditMountPointNodePath) {
        this.inEditMountPointNodePath = inEditMountPointNodePath;
    }
}
