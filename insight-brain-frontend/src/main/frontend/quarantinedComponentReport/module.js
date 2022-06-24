/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import QuarantinedComponentContainer from './QuarantinedComponentContainer';
import { QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED } from 'MainRoot/utility/services/routeStateUtilService';

export default angular
  .module('quarantinedComponentReportModule', ['ngRedux'])
  .component('quarantinedComponentReport', iqReact2Angular(QuarantinedComponentContainer, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('quarantinedComponentReport', {
    component: 'quarantinedComponentReport',
    data: {
      title: 'Quarantined Component Report',
      authenticationRequired: QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED,
    },
    url: '/repositories/quarantinedComponent/{token}',
  });
}

routes.$inject = ['$stateProvider'];
