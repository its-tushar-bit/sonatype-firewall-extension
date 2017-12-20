/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardDataService from './dashboard.data.service';
import CLMLocationModule from '../../util/CLMLocation';
import dashboardUtilsModule from '../utils/dashboard.utils.module';

export default angular.module('dashboardServicesModule', [CLMLocationModule.name, dashboardUtilsModule.name])
    .service('dashboard.data.service', dashboardDataService);
