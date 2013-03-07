package org.jahia.modules.external.modules;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.quartz.JobExecutionContext;

import javax.jcr.RepositoryException;
import java.io.File;
import java.util.Set;

public class ModulesExportJob extends BackgroundJob {

    private final Set<String> modules = ModulesListener.getInstance().getModules();;

    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) throws Exception {
        if (ModulesListener.getInstance() != null) {
            if (!modules.isEmpty()) {
                synchronized (modules) {
                    JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                        @Override
                        public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                            for (String module : modules) {
                                JahiaTemplateManagerService service = ServicesRegistry.getInstance().getJahiaTemplateManagerService();
                                JahiaTemplatesPackage pack = service.getTemplatePackageByFileName(module);
                                if (pack != null) {
                                    File sources = service.getSources(pack, session);
                                    if (sources != null) {
                                        service.regenerateImportFile(module, sources, session);
                                    }
                                }
                            }
                            return null;
                        }
                    });

                    modules.clear();
                }
            }
        }

    }
}
