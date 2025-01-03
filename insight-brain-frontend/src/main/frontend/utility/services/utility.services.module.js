/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import CLMLocationModule from '../../util/CLMLocation';
import StateHistoryService from './state.history.service';
import UnauthenticatedRequestQueueService from './unauthenticated.request.queue.service';
import routeStateUtilService from './routeStateUtilService';
import ProductLicense from './ProductLicense';

export default angular
  .module('utility.services', [CLMLocationModule.name])
  .service('state.history.service', StateHistoryService)
  .service('UnauthenticatedRequestQueueService', UnauthenticatedRequestQueueService)
  .service('routeStateUtilService', routeStateUtilService)
  .service('ProductLicense', ProductLicense);
