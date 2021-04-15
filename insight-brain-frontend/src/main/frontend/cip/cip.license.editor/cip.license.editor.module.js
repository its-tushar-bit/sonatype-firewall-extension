/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
import cipLicenseEditorDirective from './cip.license.editor.directive';
import licenseEditorController from './license.editor.controller';

export default angular
  .module('cip.license.editor', [
    'CommonServices',
    'HttpInterceptors',
    'UnauthenticatedResponseHttpInterceptor',
    'ui.bootstrap',
    'utility.directives',
  ])
  .directive('cipLicenseEditor', cipLicenseEditorDirective)
  .controller('LicenseEditorController', licenseEditorController);
