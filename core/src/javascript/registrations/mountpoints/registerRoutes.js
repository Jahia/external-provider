import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    const level = 'server';
    const parentTarget = 'administration-server';

    const mpPath = '/administration/manageMountPoints';
    const mpRouteId = 'manageMountPoints';
    registry.add('adminRoute', `${level}-${mpPath.toLowerCase()}`, {
        id: mpRouteId,
        targets: [`${parentTarget}-systemcomponents:3`],
        path: mpPath,
        route: mpRouteId,
        defaultPath: mpPath,
        icon: null,
        label: 'external-provider:mountpoints.label',
        childrenTarget: null,
        isSelectable: true,
        level: level
    });
};
