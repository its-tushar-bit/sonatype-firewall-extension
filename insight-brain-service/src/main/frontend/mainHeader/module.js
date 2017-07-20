/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityServicesModule from '../utility/services/utility.services.module';

angular.module('mainHeader', [
  'ui.router', 'ui.utils', 'AngularCommon', 'CLMLocation', 'ProductFeaturesModule', 'PermissionServiceModule',
  'ngSanitize', utilityServicesModule.name
]);
