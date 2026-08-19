/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import QuarantinedComponentContainer from './QuarantinedComponentContainer';
import { QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED } from 'MainRoot/utility/services/routeStateUtilService';

router.stateRegistry.register({
  name: 'quarantinedComponentReport',
  url: '/repositories/quarantinedComponent/{token}',
  component: QuarantinedComponentContainer,
  data: {
    title: 'Quarantined Component Report',
    authenticationRequired: QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED,
  },
});
