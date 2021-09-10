import registrations from './registrations';
import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';

export default function () {
    registry.add('callback', 'external-provider', {
        targets: ['jahiaApp-init:50'],
        callback: () => {
            i18next.loadNamespaces('external-provider');
            registrations();
            console.log('%c External Provider routes have been registered', 'color: #3c8cba');
        }
    });
}
