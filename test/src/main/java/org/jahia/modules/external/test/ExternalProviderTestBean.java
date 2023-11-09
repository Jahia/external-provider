package org.jahia.modules.external.test;

import org.jahia.test.bin.TestBean;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.List;

/**
 * A specialized instance of a TestBean to be detected by Jahia Test Module's servlet, allowing us to extend the
 * available tests to external provider tests
 */
@Component(service=TestBean.class, immediate=true)
public class ExternalProviderTestBean extends TestBean {

    public ExternalProviderTestBean() {
        super();
    }

    @Override
    public int getPriority() {
        return 56;
    }

    @Override
    public List<String> getTestCases() {
        return Arrays.asList(
                "org.jahia.modules.external.test.vfs.VFSContentStoreProviderTest",
                "org.jahia.modules.external.test.vfs.VFSAclTest",
                "org.jahia.modules.external.test.db.ExternalDatabaseProviderTest",
                "org.jahia.modules.external.test.qom.QOMTest",
                "org.jahia.modules.external.test.listener.ApiEventTest"
                );
    }
}
