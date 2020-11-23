/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { find, isNil, propEq, reject } from 'ramda';

import template from './cipTabPanel.html';

/**
 * This component is analogous to the older componentInformationPanelDirective, but for the policy-centric app report.
 * The main difference is that rather than take the tab configurations as data, the template is hard-coded
 * with the needed tabs.  This enables data to be passed to the tabs as bindings rather than having to get
 * the data via stateful services or globals like the old implementation
 */
export default {
  template,
  controller: CipTabPanelController,
  controllerAs: 'vm',
  bindings: {
    selectedComponent: '<',
    scanId: '<',
    applicationPublicId: '<',
    reloadReport: '&',
    closeCipModal: '&'
  }
};

function CipTabPanelController($scope, CLMLocations) {
  const vm = this;

  Object.assign(vm, {
    selectedTab: 'componentInfo'
  });

  function updateTabs() {
    const { selectedComponent } = vm,
        { matchState } = selectedComponent,
        unknown = matchState === 'unknown',
        exact = matchState === 'exact',
        claimed = selectedComponent.identificationSource === 'Manual';

    vm.tabs = reject(isNil, [{
      name: 'componentInfo',
      displayName: 'Component Info'
    }, {
      name: 'policy',
      displayName: 'Policy'
    }, {
      name: 'similar',
      displayName: 'Similar'
    }, {
      name: 'occurrences',
      displayName: 'Occurrences'
    }, unknown ? null : {
      name: 'licenses',
      displayName: 'Licenses'
    }, unknown || claimed ? null : {
      name: 'vulnerabilities',
      displayName: 'Vulnerabilities'
    }, unknown ? null : {
      name: 'labels',
      displayName: 'Labels'
    }, exact && !claimed ? null : {
      name: 'claimComponent',
      displayName: 'Claim'
    }, unknown ? null : {
      name: 'auditLog',
      displayName: 'Audit Log'
    }]);
  }

  function latestReportUrl() {
    if (vm.selectedComponent != null && vm.selectedComponent.latestReport) {
      return CLMLocations.getAbsoluteUrl(vm.selectedComponent.latestReport.url);
    }
  }

  $scope.$watch('vm.selectedComponent', function() {
    if (vm.selectedComponent) {
      updateTabs();

      if (!find(propEq('name', vm.selectedTab), vm.tabs)) {
        vm.selectedTab = vm.tabs[0].name;
      }
    }
    vm.latestReportUrl = latestReportUrl();
  });
}

CipTabPanelController.$inject = ['$scope', 'CLMLocations'];
