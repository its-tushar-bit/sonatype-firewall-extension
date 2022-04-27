/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
export default function MoveApplicationModalController(
  $rootScope,
  $scope,
  currentApplication,
  MoveApplicationService,
  MoveApplicationErrorModal,
  EventNameConstant,
  MoveApplicationSuccessModalService,
  $ngRedux
) {
  var vm = this;

  vm.formMask = undefined;
  vm.moveApplicationForm = undefined;
  vm.currentOrganizationName = currentApplication.organizationName;
  vm.organizations = undefined;
  vm.selectedOrganization = undefined;
  vm.loadError = undefined;
  vm.saveError = undefined;
  vm.isHidden = false;
  vm.incompatibilities = undefined;
  vm.getError = getError;
  vm.save = save;
  vm.isLoading = isLoading;
  vm.showIncompatibilities = showIncompatibilities;
  vm.unsubscribe = $ngRedux.connect(null, {
    moveApplication: applicationActions.moveApplication,
  })(vm);

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  $scope.$on('pageChangeAccepted', function () {
    $scope.$dismiss();
  });

  doLoad();

  function getError() {
    return vm.saveError || vm.loadError;
  }

  function doLoad() {
    MoveApplicationService.getDestinationOrganizations(currentApplication.id)
      .then(function (organizations) {
        vm.organizations = organizations;
      })
      .catch(function (errorMessage) {
        vm.loadError = errorMessage;
      });
  }

  function save() {
    if (!vm.moveApplicationForm.$valid) {
      return;
    }

    vm.saveError = undefined;
    vm.incompatibilities = undefined;

    vm.formMask
      .wrap(vm.moveApplication({ applicationId: currentApplication.id, organizationId: vm.selectedOrganization.id }))
      .then(function (messages) {
        $scope.$close();
        MoveApplicationSuccessModalService.open(messages);
        // refresh nav and all the tiles
        $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_TREE_DATA);
        $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);
      })
      .catch(function (error) {
        vm.saveError = error.message;
        vm.incompatibilities = error.incompatibilities;
      });
  }

  function isLoading() {
    return vm.organizations === undefined && !vm.getError();
  }

  function showIncompatibilities() {
    vm.isHidden = true;
    MoveApplicationErrorModal.open(vm.incompatibilities)
      .then(function () {
        vm.isHidden = false;
      })
      .catch(function () {
        // in case error dialog fails
        vm.isHidden = false;
      });
  }
}

MoveApplicationModalController.$inject = [
  '$rootScope',
  '$scope',
  'currentApplication',
  'move.application.service',
  'move.application.error.modal.service',
  'event.name.constant',
  'move.application.success.modal.service',
  '$ngRedux',
];
