package org.jahia.modules.external.admin;

import java.io.Serializable;

public class MountInfo implements Serializable {
    private String mountPoint;
    private String key;
    private int id;
    private DataSourceInfo dataSource;
    private boolean dynamic;

    public String getMountPoint() {
        return mountPoint;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public DataSourceInfo getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSourceInfo dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }
}
