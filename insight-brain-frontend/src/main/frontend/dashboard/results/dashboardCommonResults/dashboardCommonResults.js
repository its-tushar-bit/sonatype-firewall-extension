/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import template from './dashboardCommonResults.html';

const dashboardCommonResults = {
  bindings: {
    isViolationsTab: '<',
    results: '<',
    numResults: '<',
    needsAcknowledgement: '<',
    maxResults: '<',
    maxDaysOld: '<',
    error: '<',
    reload: '&',
  },
  controllerAs: 'vm',
  controller: DashboardCommonResultsController,
  template: template,
  replace: true,
};

function DashboardCommonResultsController(Dialog, ApplicationStore, Messages, $ngRedux, dashboardFilterActions) {
  const vm = this;

  Object.assign(vm, {
    loadCommonResults() {
      return !vm.results || vm.results.length === 0 || vm.numResults > vm.maxResults || vm.needsAcknowledgement;
    },

    $onChanges({ error }) {
      if (error && error.currentValue) {
        const { currentValue } = error;
        if (currentValue.status && currentValue.status === 403) {
          openFilterInvalidDialog();
        } else {
          vm.errorMessage = Messages.getHttpErrorMessage(currentValue);
        }
      }
    },
  });

  function openFilterInvalidDialog() {
    Dialog.open({
      title: 'Filter invalid',
      body: 'Your filter settings have become invalid because of permission changes, click OK to reload.',
      buttons: [
        {
          name: 'OK',
          click: function () {
            //make sure to get any stale apps out of the app list
            ApplicationStore.refresh();
            reloadFilter();
          },
        },
      ],
    });
  }

  function reloadFilter() {
    $ngRedux.dispatch(dashboardFilterActions.loadFilter());
  }
}

DashboardCommonResultsController.$inject = [
  'Dialog',
  'ApplicationStore',
  'Messages',
  '$ngRedux',
  'dashboardFilterActions',
];

export default dashboardCommonResults;
