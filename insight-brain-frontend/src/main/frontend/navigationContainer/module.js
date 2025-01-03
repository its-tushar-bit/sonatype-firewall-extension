/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityServicesModule from '../utility/services/utility.services.module';
import telemetryServiceModule from '../services/telemetryService';
import currentUserService from '../user/CurrentUserService';
import navigationContainer from './navigationContainer';
import reactComponentsModule from '../react/module.js';

export default angular
  .module('navigationContainer', [
    'ui.router',
    'ui.validate',
    utilityServicesModule.name,
    telemetryServiceModule.name,
    reactComponentsModule.name,
  ])
  .factory('CurrentUser', currentUserService)
  .component('navigationContainer', navigationContainer);
