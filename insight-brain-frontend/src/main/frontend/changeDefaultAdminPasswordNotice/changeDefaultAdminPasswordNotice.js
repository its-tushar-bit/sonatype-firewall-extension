/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './changeDefaultAdminPasswordNotice.html';

export default {
  template,
  controller: changeDefaultAdminPasswordNoticeController,
  controllerAs: 'vm'
};

function changeDefaultAdminPasswordNoticeController($q, $http, CurrentUser, CLMLocations, telemetryService,
                                                    defaultAdminPasswordChangedService) {
  const vm = this;

  Object.assign(vm, {
    isDefaultUser: false,
    shouldDisplayNotice: false,

    $onInit() {
      $q.all([CurrentUser, defaultAdminPasswordChangedService.shouldDisplayDefaultPasswordWarning()]).then(results => {
        vm.isDefaultUser = results[0].username === 'admin';
        vm.shouldDisplayNotice = results[1];

        if (vm.shouldDisplayNotice) {
          fireTelemetryEvent();
        }
      });
    }
  });

  function fireTelemetryEvent() {
    telemetryService.submitData('ADMIN_PASSWORD_CHANGE', {
      action: 'WARNING_SHOWN'
    });
  }
}

changeDefaultAdminPasswordNoticeController.$inject = [
  '$q', '$http', 'CurrentUser', 'CLMLocations', 'telemetryService', 'defaultAdminPasswordChangedService'
];
