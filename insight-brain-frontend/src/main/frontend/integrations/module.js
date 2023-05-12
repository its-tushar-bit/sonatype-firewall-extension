/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import Integrations from 'MainRoot/integrations/Integrations';

export default angular
  .module('integrationsModule', ['ngRedux'])
  .component('integrations', iqReact2Angular(Integrations, [], ['$ngRedux']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('integrations', {
    component: 'integrations',
    url: '/integrations',
    data: {
      title: 'Integrations',
    },
  });
}

routes.$inject = ['$stateProvider'];
