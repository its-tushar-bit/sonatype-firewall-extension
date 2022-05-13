/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './retentionTile.html';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default {
  template: template,
  controllerAs: 'vm',
  controller: RetentionTileController,
};

function RetentionTileController(CLMContextLocations, $scope, EventNameConstant, retentionService, Messages, $ngRedux) {
  const NOT_APPLICABLE = 'N/A';
  const NOT_ENABLED = "Don't Purge";

  const vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);

  Object.assign(vm, {
    isOrganization: CLMContextLocations.isOrganization(),
    applicationReports: undefined,
    successMetrics: undefined,
    error: undefined,

    load() {
      if (!vm.isOrganization) {
        return;
      }

      vm.error = undefined;

      retentionService
        .getRetentionPolicies()
        .then(({ applicationReports, successMetrics }) => {
          vm.applicationReports = applicationReports;
          vm.successMetrics = successMetrics;
        })
        .catch((error) => {
          vm.error = Messages.getHttpErrorMessage(error);
        });
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
      return vm.successMetrics.enablePurging ? 'Max Age ' + vm.successMetrics.maxAge : NOT_ENABLED;
    },
  });

  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, vm.load);

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  vm.load();
}

export const mapStateToThis = (state) => ({
  ownerName: selectSelectedOwnerName(state),
});

RetentionTileController.$inject = [
  'CLMContextLocations',
  '$scope',
  'event.name.constant',
  'retentionService',
  'Messages',
  '$ngRedux',
];
