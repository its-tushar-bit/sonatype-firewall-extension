/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';

import template from './applicationReport.html';
import reevaluationErrorModalWrapperTemplate from './reevaluationErrorModal/reevaluationErrorModalWrapper.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: ApplicationReportController
};

function ApplicationReportController($scope, $ngRedux, applicationReportActions, Modal) {
  const vm = this;

  let reevaluationErrorModal = undefined;

  Object.assign(vm, {
    availableProprietaryFilterOptions: [
      { id: false, name: 'non proprietary' },
      { id: true, name: 'proprietary' }
    ],

    availableMatchStateFilterOptions: [
      { id: 'exact', name: 'Exact' },
      { id: 'similar', name: 'Similar' },
      { id: 'unknown', name: 'Unknown' }
    ],

    $onInit() {
      const actions = pick(
          ['setAggregateReportEntries', 'setExactValueFilter', 'reevaluateReport', 'reevaluateReportCancelled'],
          applicationReportActions);

      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);

      $scope.$watch('vm.reevaluating', function(reevaluating) {
        if (reevaluating) {
          vm.formMaskController.activateMask();
        }
        else if (!vm.reevaluationError) {
          vm.formMaskController.showSuccessMaskBriefly();
        }
        else {
          vm.formMaskController.removeMask();
        }
      });

      $scope.$watch('vm.reevaluationError', function(reevaluationError) {
        if (reevaluationError && !reevaluationErrorModal) {
          vm.openReevaluationErrorModal();
        }
        else if (!reevaluationError) {
          vm.dismissReevaluationErrorModal();
        }
      });
    },

    $onDestroy() {
      vm.dismissReevaluationErrorModal();
      vm.unsubscribe();
    },

    openReevaluationErrorModal() {
      function modalController($scope) {
        Object.assign($scope, {
          retry: vm.reevaluateReport,
          cancel: vm.reevaluateReportCancelled
        });
      }

      modalController.$inject = ['$scope'];

      reevaluationErrorModal = Modal.open({
        template: reevaluationErrorModalWrapperTemplate,
        controller: modalController
      });
    },

    dismissReevaluationErrorModal() {
      if (reevaluationErrorModal) {
        reevaluationErrorModal.dismiss();
        reevaluationErrorModal = undefined;
      }
    },

    setProprietaryFilterOptions(selectedIds) {
      vm.setExactValueFilter('proprietary', selectedIds);
    },

    setMatchStateFilterOptions(selectedIds) {
      vm.setExactValueFilter('matchState', selectedIds);
    }
  });
}

function mapStateToThis(state) {
  return pick(['aggregate', 'reevaluating', 'reevaluationError', 'exactValueFilters'], state.applicationReport || {});
}

ApplicationReportController.$inject = ['$scope', '$ngRedux', 'applicationReportActions', 'Modal'];
