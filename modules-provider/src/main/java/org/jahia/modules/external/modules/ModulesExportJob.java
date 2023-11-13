/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.external.modules;

import org.jahia.osgi.BundleUtils;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.Set;

/**
 * Background task for regenerating initial import of the modules.
 */
@Component(immediate = true)
public class ModulesExportJob extends BackgroundJob {
    private static final Logger logger = LoggerFactory.getLogger(ModulesExportJob.class);

    private SchedulerService schedulerService;
    private JobDetail jobDetail;

    @Reference
    public void setSchedulerService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Activate
    public void start() throws Exception {
        if (SettingsBean.getInstance().getOperatingMode().equals("development")) {
            jobDetail = BackgroundJob.createJahiaJob("ModulesAutoExport", ModulesExportJob.class);
            jobDetail.setGroup("Studio");
            jobDetail.setJobDataMap(new JobDataMap());
            if (schedulerService.getAllJobs(jobDetail.getGroup(), true).isEmpty() && SettingsBean.getInstance().isProcessingServer()) {
                Trigger trigger = new CronTrigger("StudioExportJobTrigger", jobDetail.getGroup(), "0/5 * * * * ?");
                schedulerService.getRAMScheduler().scheduleJob(jobDetail, trigger);
            }
        }
    }

    @Deactivate
    public void stop() throws Exception {
        if (!schedulerService.getAllJobs(jobDetail.getGroup(), true).isEmpty() && SettingsBean.getInstance().isProcessingServer()) {
            schedulerService.getRAMScheduler().deleteJob(jobDetail.getName(), jobDetail.getGroup());
        }
    }

    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) throws Exception {
        ModulesListener modulesListener = BundleUtils.getOsgiService(ModulesListener.class, null);
        Set<String> modules = modulesListener.getModules();
        synchronized (modules) {
            try {
                if (!modules.isEmpty()) {
                    ModulesImportExportHelper.getInstance().regenerateImportFiles(modules);
                    modules.clear();
                }
            } catch (NoSuchBeanDefinitionException e) {
                logger.error("Cannot get ModulesImportExportHelper " + e.getMessage());
            }
        }
    }
}
