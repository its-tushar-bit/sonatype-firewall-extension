/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import changeDefaultAdminPasswordNotice from './changeDefaultAdminPasswordNotice';
import CLMLocationModule from '../util/CLMLocation';
import defaultAdminPasswordChangedServiceModule from '../services/defaultAdminPasswordChangedService';
import telemetryServiceModule from '../services/telemetryService';

export default angular.module('changeDefaultAdminPasswordNoticeModule',
    [CLMLocationModule.name, defaultAdminPasswordChangedServiceModule.name, telemetryServiceModule.name])
    .component('changeDefaultAdminPasswordNotice', changeDefaultAdminPasswordNotice);
