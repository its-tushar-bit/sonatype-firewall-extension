/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import ApiPage from './ApiPage';
import { ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE } from 'MainRoot/utility/services/routeStateUtilService';

router.stateRegistry.register({
  name: 'api',
  url: '/api',
  component: ApiPage,
  data: {
    title: 'API',
    authenticationRequired: ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
  },
});
