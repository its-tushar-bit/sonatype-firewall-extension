/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
import addWaiverController from './add.waiver.controller';
import cipPolicyViolationsDirective from './cip.policy.violations.directive';
import policyViolationsController from './policy.violations.controller';
import viewWaiverController from './view.waiver.controller';
import releaseQuarantineController from './release.quarantine.controller';
import ciPolicyViolationsService from './ci.policy.violations.service';
import requestWaiverController from './request.waiver.controller';

export default angular
  .module('cip.policy.violations', [
    'CommonServices',
    'HttpInterceptors',
    'UnauthenticatedResponseHttpInterceptor',
    'ui.bootstrap',
  ])
  .controller('AddWaiverController', addWaiverController)
  .directive('cipPolicyViolations', cipPolicyViolationsDirective)
  .controller('PolicyViolationsController', policyViolationsController)
  .controller('ViewWaiverController', viewWaiverController)
  .controller('release.quarantine.controller', releaseQuarantineController)
  .service('PolicyViolations', ciPolicyViolationsService)
  .controller('RequestWaiverController', requestWaiverController);
