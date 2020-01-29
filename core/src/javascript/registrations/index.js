import {registerRoutes as registerExternalProviderRoutes} from './externalProvider/registerRoutes';
import {registerRoutes as registerMountpointsRoutes} from './mountpoints/registerRoutes';
import {useTranslation} from 'react-i18next';

export default function () {
    const {t} = useTranslation('external-provider');

    registerExternalProviderRoutes(t);
    registerMountpointsRoutes(t);

    return null;
}
