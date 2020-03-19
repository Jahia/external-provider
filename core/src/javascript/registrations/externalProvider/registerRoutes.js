import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    registry.add('adminRoute', 'external-provider', {
        targets: ['administration-server-systemComponents:0'],
        requiredPermission: 'adminRoles',
        icon: null,
        label: 'external-provider:externalProvider.label',
        isSelectable: true,
        iframeUrl: window.contextJsParameters.contextPath + '/cms/adminframe/default/en/settings.external-provider.html?redirect=false'
    });
};
