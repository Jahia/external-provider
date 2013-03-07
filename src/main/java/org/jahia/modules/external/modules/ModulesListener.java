package org.jahia.modules.external.modules;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.DefaultEventListener;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.HashSet;
import java.util.Set;

public class ModulesListener extends DefaultEventListener {
    private static ModulesListener instance;

    private Set<String> modules = new HashSet<String>();

    public ModulesListener() {
        setWorkspace("default");
    }

    public static ModulesListener getInstance() {
        if (instance== null) {
            instance = new ModulesListener();
        }
        return instance;
    }

    public Set<String> getModules() {
        return modules;
    }

    @Override
    public int getEventTypes() {
        return Event.NODE_ADDED + Event.NODE_REMOVED + Event.PROPERTY_ADDED + Event.PROPERTY_CHANGED +
                Event.PROPERTY_REMOVED + Event.NODE_MOVED;    }

    @Override
    public String getPath() {
        return "/modules";
    }

    @Override
    public void onEvent(EventIterator events) {
        System.out.println("event");

        for (StackTraceElement element : new Exception().getStackTrace()) {
            if (element.getMethodName().equals("initializeModuleContent") && element.getClassName().equals("org.jahia.services.templates.TemplatePackageDeployer")) {
                return;
            }
        }
        synchronized (modules) {
            while (events.hasNext()) {
                Event event = (Event) events.next();
                if (!isExternal(event)) {
                    try {
                        String m = StringUtils.substringAfter(event.getPath(), "/modules/");
                        m  = StringUtils.substringBefore(m, "/");
                        if (!StringUtils.isEmpty(m)) {
                            modules.add(m);
                        }
                    } catch (RepositoryException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        }
    }
}
