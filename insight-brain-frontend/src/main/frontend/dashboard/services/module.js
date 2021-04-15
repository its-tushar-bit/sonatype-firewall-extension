/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as dashboardDataService from './dashboard.data.service';

export default angular
  .module('dashboardServicesModule', [])
  .value('dashboard.data.service', dashboardDataService);
