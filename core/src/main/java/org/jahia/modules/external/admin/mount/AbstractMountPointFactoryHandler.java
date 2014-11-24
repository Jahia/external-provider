package org.jahia.modules.external.admin.mount;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRCallback;import org.jahia.services.content.JCRContentUtils;import org.jahia.services.content.JCRNodeWrapper;import org.jahia.services.content.JCRSessionWrapper;import org.jahia.services.content.JCRStoreProvider;import org.jahia.services.content.JCRStoreService;import org.jahia.services.content.JCRTemplate;import org.jahia.services.content.ProviderFactory;import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.render.RenderContext;
import org.jahia.utils.Url;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.webflow.execution.RequestContext;

import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.lang.Boolean;import java.lang.Override;import java.lang.String;
import java.util.Locale;

/**
 * @author kevan
 */
public abstract class AbstractMountPointFactoryHandler<T extends AbstractMountPointFactory> implements Serializable{
    private static final long serialVersionUID = 6394236759186947423L;

    public T init(RequestContext requestContext, final T mountPointFactory) throws RepositoryException {
        if(mountPointFactory == null){
            return null;
        }

        final String mountPointIdentifier = requestContext.getRequestParameters().get("edit");
        if (StringUtils.isNotEmpty(mountPointIdentifier)) {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<T>() {
                @Override
                public T doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    mountPointFactory.populate(session.getNodeByIdentifier(mountPointIdentifier));
                    return mountPointFactory;
                }
            });
        } else {
            return mountPointFactory;
        }
    }

    public Boolean save(final AbstractMountPointFactory mountPoint) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
            @Override
            public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                // create mount node
                JCRNodeWrapper jcrMountPointNode;
                JCRNodeWrapper mounts = session.getNode("/mounts");
                String name = mountPoint.getName() + JCRMountPointNode.MOUNT_SUFFIX;
                if (mountPoint.isEdit()) {
                    jcrMountPointNode = session.getNode(mountPoint.getInEditMountPointNodePath());

                    // unmount
                    if (((JCRMountPointNode) jcrMountPointNode).getMountStatus() == JCRMountPointNode.MountStatus.mounted) {
                        ((JCRMountPointNode) jcrMountPointNode).getMountProvider().unmount();
                    }

                    // rename if necessary
                    if (!jcrMountPointNode.getName().equals(name)) {
                        jcrMountPointNode.rename(JCRContentUtils.findAvailableNodeName(mounts, JCRContentUtils.escapeLocalNodeName(name)));
                    }
                } else {
                    jcrMountPointNode = mounts.addNode(JCRContentUtils.findAvailableNodeName(mounts, JCRContentUtils.escapeLocalNodeName(name)), "jnt:remoteJcrMountPoint");
                }

                // local path
                if(StringUtils.isNotEmpty(mountPoint.getLocalPath())){
                    jcrMountPointNode.setProperty("mountPoint", session.getNode(mountPoint.getLocalPath()).getIdentifier());
                }

                // mount point properties
                mountPoint.setProperties(jcrMountPointNode);
                session.save();

                // mount
                if (mountPoint.isEdit()) {
                    ProviderFactory providerFactory = JCRStoreService.getInstance().getProviderFactories().get(jcrMountPointNode.getPrimaryNodeTypeName());
                    JCRStoreProvider mountProvider = providerFactory.mountProvider(((JCRMountPointNode) jcrMountPointNode).getVirtualMountPointNode());
                    return mountProvider.isAvailable();
                } else {
                    final JCRMountPointNode remoteJCRMountPointNode = (JCRMountPointNode) session.getNode(jcrMountPointNode.getPath());
                    final JCRStoreProvider provider = remoteJCRMountPointNode.getMountProvider();
                    provider.mount();
                    return provider.isAvailable();
                }
            }
        });
    }

    public String getAdminURL(RequestContext requestContext) {
        RenderContext renderContext = getRenderContext(requestContext);
        Locale locale = LocaleContextHolder.getLocale();
        String server = Url.getServer(renderContext.getRequest());
        String context = renderContext.getURLGenerator().getContext();
        return server + context + "/cms/adminframe/default/" + locale + "/settings.manageMountPoints.html";
    }

    private RenderContext getRenderContext(RequestContext requestContext) {
        return (RenderContext) requestContext.getExternalContext().getRequestMap().get("renderContext");
    }
}
