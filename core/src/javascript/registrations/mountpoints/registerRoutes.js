import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    registry.add('adminRoute', 'manageMountPoints', {
        targets: ['administration-server-systemComponents:3'],
        requiredPermission: 'adminMountPoints',
        icon: null,
        label: 'external-provider:mountpoints.label',
        isSelectable: true,
        iframeUrl: window.contextJsParameters.contextPath + '/cms/adminframe/default/en/settings.manageMountPoints.html?redirect=false'
    });
};
