import registrations from './registrations';
import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';

export default function () {
    registry.add('callback', 'external-provider-ui', {
        targets: ['jahiaApp-init:50'],
        callback: () => {
            i18next.loadNamespaces('external-provider-ui');
            registrations();
            console.log('%c External Provider UI routes have been registered', 'color: #3c8cba');
        }
    });
}
