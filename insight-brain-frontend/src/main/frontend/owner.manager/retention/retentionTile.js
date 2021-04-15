/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './retentionTile.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: RetentionTileController,
};

function RetentionTileController(
  CLMContextLocations,
  $scope,
  EventNameConstant,
  OrganizationStore,
  retentionService,
  $q,
  Messages
) {
  const NOT_APPLICABLE = 'N/A';
  const NOT_ENABLED = "Don't Purge";

  const vm = this;

  Object.assign(vm, {
    isOrganization: CLMContextLocations.isOrganization(),
    ownerName: undefined,
    applicationReports: undefined,
    successMetrics: undefined,
    error: undefined,

    load() {
      if (!vm.isOrganization) {
        return;
      }

      vm.error = undefined;
      const promises = [];
      promises.push(
        OrganizationStore.getById(CLMContextLocations.getEntityId())
      );
      promises.push(retentionService.getRetentionPolicies());
      $q.all(promises).then(
        function (results) {
          vm.ownerName = results[0].name;
          vm.applicationReports = results[1].applicationReports;
          vm.successMetrics = results[1].successMetrics;
        },
        function (error) {
          vm.error = Messages.getHttpErrorMessage(error);
        }
      );
    },

    getMaxReports(applicationReport) {
      return applicationReport.enablePurging
        ? applicationReport.maxCount
          ? applicationReport.maxCount
          : NOT_APPLICABLE
        : NOT_ENABLED;
    },

    getMaxAge(applicationReport) {
      return applicationReport.enablePurging
        ? applicationReport.maxAge
          ? applicationReport.maxAge
          : NOT_APPLICABLE
        : NOT_ENABLED;
    },

    getSuccessMetricsMaxAge() {
      return vm.successMetrics.enablePurging
        ? 'Max Age ' + vm.successMetrics.maxAge
        : NOT_ENABLED;
    },
  });

  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, vm.load);

  vm.load();
}

RetentionTileController.$inject = [
  'CLMContextLocations',
  '$scope',
  'event.name.constant',
  'OrganizationStore',
  'retentionService',
  '$q',
  'Messages',
];
