import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    registry.add('adminRoute', 'manageMountPoints', {
        targets: ['administration-server-systemComponents:20'],
        requiredPermission: 'adminMountPoints',
        icon: null,
        label: 'external-provider-ui:mountpoints.label',
        isSelectable: true,
        iframeUrl: window.contextJsParameters.contextPath + '/cms/adminframe/default/en/settings.manageMountPoints.html?redirect=false'
    });
};
