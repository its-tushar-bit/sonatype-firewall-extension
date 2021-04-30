/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { comparator, find, isNil, propEq, reject, sort } from 'ramda';

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
    closeCipModal: '&',
  },
};

function CipTabPanelController($scope, CLMLocations, $http, Messages, OwnerContext) {
  const vm = this;

  Object.assign(vm, {
    selectedTab: 'componentInfo',
  });

  function updateTabs() {
    const { selectedComponent } = vm,
      { matchState } = selectedComponent,
      unknown = matchState === 'unknown',
      exact = matchState === 'exact',
      claimed = selectedComponent.identificationSource === 'Manual',
      isRepository = OwnerContext.ownerType === 'repository';

    vm.tabs = reject(isNil, [
      {
        name: 'componentInfo',
        displayName: 'Component Info',
      },
      {
        name: 'policy',
        displayName: 'Policy',
      },
      {
        name: 'similar',
        displayName: 'Similar',
      },
      isRepository
        ? null
        : {
            name: 'occurrences',
            displayName: 'Occurrences',
          },
      unknown
        ? null
        : {
            name: 'licenses',
            displayName: 'Licenses',
          },
      unknown || claimed
        ? null
        : {
            name: 'vulnerabilities',
            displayName: 'Vulnerabilities',
          },
      unknown
        ? null
        : {
            name: 'labels',
            displayName: 'Labels',
          },
      exact && !claimed
        ? null
        : {
            name: 'claimComponent',
            displayName: 'Claim',
          },
      unknown || isRepository
        ? null
        : {
            name: 'auditLog',
            displayName: 'Audit Log',
          },
    ]);

    vm.useNewWaiverPages = OwnerContext.ownerType !== 'repository';
  }

  const stagesOrder = {
    operate: 1,
    release: 2,
    stage: 3,
    build: 4,
    develop: 5,
    proxy: 6,
  };

  const getStageOrder = (report) => {
    return stagesOrder[report['stage']] !== undefined ? stagesOrder[report['stage']] : 7;
  };

  const byStage = comparator((reportA, reportB) => getStageOrder(reportA) < getStageOrder(reportB));

  function loadInnerSourceReportUrl() {
    if (vm.selectedComponent && vm.selectedComponent.latestReport) {
      return;
    }

    const innerSourceData = vm.selectedComponent.innerSourceData;
    if (vm.selectedComponent.innerSource && innerSourceData && innerSourceData.ownerApplicationId) {
      $http.get(CLMLocations.getApplicationReportsUrl(innerSourceData.ownerApplicationId)).then(
        function (response) {
          const { data } = response;
          if (data && data.length > 0) {
            const lastInnerSourceReportData = sort(byStage, data)[0];
            vm.selectedComponent.latestReport = {
              stage: lastInnerSourceReportData.stage,
              url: CLMLocations.getAbsoluteUrl(lastInnerSourceReportData.latestReportHtmlUrl),
            };
          }
        },
        function (response) {
          vm.error = Messages.getHttpErrorMessage(response);
        }
      );
    }
  }

  $scope.$watch('vm.selectedComponent', function () {
    if (vm.selectedComponent) {
      updateTabs();
      loadInnerSourceReportUrl();

      if (!find(propEq('name', vm.selectedTab), vm.tabs)) {
        vm.selectedTab = vm.tabs[0].name;
      }
    }
  });
}

CipTabPanelController.$inject = ['$scope', 'CLMLocations', '$http', 'Messages', 'OwnerContext'];
