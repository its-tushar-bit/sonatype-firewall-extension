/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular*/

import systemConfigurationPropertyService from './systemConfigurationPropertyService';
import CLMLocationModule from '../../util/CLMLocation';
import utilityDirectivesModule from '../directives/utility.directives.module';
import JiraService from './jira.service';
import LoginModalController from './login.modal.controller';
import LoginModalService from './login.modal.service';
import StateHistoryService from './state.history.service';
import UnauthenticatedRequestQueueService from './unauthenticated.request.queue.service';
import routeStateUtilService from './routeStateUtilService';
import ProductLicense from './ProductLicense';

export default angular
  .module('utility.services', [
    utilityDirectivesModule.name,
    CLMLocationModule.name,
  ])
  .service(
    'systemConfigurationPropertyService',
    systemConfigurationPropertyService
  )
  .service('jira.service', JiraService)
  .controller('login.modal.controller', LoginModalController)
  .service('LoginModalService', LoginModalService)
  .service('state.history.service', StateHistoryService)
  .service(
    'UnauthenticatedRequestQueueService',
    UnauthenticatedRequestQueueService
  )
  .service('routeStateUtilService', routeStateUtilService)
  .service('ProductLicense', ProductLicense);
