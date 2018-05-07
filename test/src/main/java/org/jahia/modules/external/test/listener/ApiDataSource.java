package org.jahia.modules.external.test.listener;

import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.*;

public class ApiDataSource implements ExternalDataSource {

    private static List<String> nodes = Arrays.asList("/","/toto","/tata","/titi");

    private List<String> log = new ArrayList<>();

    public List<String> getLog() {
        return log;
    }

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        log.add("ApiDataSource.getChildren:"+path);
        if (path.equals("/")) {
            return Arrays.asList("toto", "tata", "titi");
        }
        return Collections.emptyList();
    }

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        log.add("ApiDataSource.getItemByIdentifier:"+identifier);
        if (nodes.contains(identifier)) {
            return new ExternalData(identifier, identifier, "jnt:bigText", new HashMap<>());
        } else {
            throw new ItemNotFoundException(identifier);
        }
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        log.add("ApiDataSource.getItemByPath:"+path);
        if (nodes.contains(path)) {
            return new ExternalData(path, path, "jnt:bigText", new HashMap<>());
        } else {
            throw new PathNotFoundException(path);
        }
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return Collections.singleton("jnt:bigText");
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
        return nodes.contains(path);
    }
}
