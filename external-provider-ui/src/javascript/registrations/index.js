import {registerRoutes as registerExternalProviderRoutes} from './externalProvider/registerRoutes';
import {registerRoutes as registerMountpointsRoutes} from './mountpoints/registerRoutes';

export default function () {
    registerExternalProviderRoutes();
    registerMountpointsRoutes();
}
