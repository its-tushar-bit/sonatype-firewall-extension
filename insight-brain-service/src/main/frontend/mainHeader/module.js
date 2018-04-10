/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityServicesModule from '../utility/services/utility.services.module';
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import permissionServiceModule from '../util/PermissionService';
import productFeaturesModule from '../util/ProductFeatures';
import telemetryServiceModule from '../services/telemetryService';
import defaultAdminPasswordChangedServiceModule from '../services/defaultAdminPasswordChangedService';

angular.module('mainHeader', [
  'ui.router', 'ui.validate', angularCommonModule.name, CLMLocationModule.name, productFeaturesModule.name,
  permissionServiceModule.name, 'ngSanitize', utilityServicesModule.name, telemetryServiceModule.name,
  defaultAdminPasswordChangedServiceModule.name
]);
