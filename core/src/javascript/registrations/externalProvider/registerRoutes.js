import {registry} from '@jahia/ui-extender';

export const registerRoutes = function (t) {
    const level = 'server';
    const parentTarget = 'administration-server';

    const path = '/administration/external-provider';
    const route = 'external-provider';
    registry.addOrReplace('adminRoute', `${level}-${path.toLowerCase()}`, {
        id: route,
        targets: [`${parentTarget}-systemcomponents:0`],
        path: path,
        route: route,
        defaultPath: path,
        icon: null,
        label: t('externalProvider.label'),
        childrenTarget: 'systemcomponents',
        isSelectable: true,
        level: level
    });
};
