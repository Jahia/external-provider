import {registry} from '@jahia/ui-extender';

export const registerRoutes = function (t) {
    const level = 'server';
    const parentTarget = 'administration-server';

    const mpPath = '/administration/manageMountPoints';
    const mpRouteId = 'manageMountPoints';
    registry.addOrReplace('adminRoute', `${level}-${mpPath.toLowerCase()}`, {
        id: mpRouteId,
        targets: [`${parentTarget}-systemcomponents:3`],
        path: mpPath,
        route: mpRouteId,
        defaultPath: mpPath,
        icon: null,
        label: t('mountpoints.label'),
        childrenTarget: null,
        isSelectable: true,
        level: level
    });
};
