/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
import requestWaiverTemplate from './cip-request-waiver-modal.html';
import getThreatColor from './threatColorUtil';
import { getApplicationSummaryUrl } from '../../util/CLMLocation';

export default function PolicyViolationsController(
  $http,
  $scope,
  $q,
  Modal,
  SelectedComponent,
  OwnerContext,
  PolicyViolations,
  Messages,
  $state,
  PermissionService,
  ProductFeatures
) {
  const vm = this;

  $scope.isAddWaiverAuthorized = false;

  function getContextData(ownerType, ownerId) {
    const dataRequest = $q.defer();
    if (ownerType === 'application') {
      // applications need to use internal ID for permissions check
      $http.get(getApplicationSummaryUrl(ownerId)).then(function ({ data }) {
        dataRequest.resolve({ ownerId: data.id, ownerType });
      });
    } else if (ownerType === 'repository') {
      // repositories don't use id for permissions check and need the `repository_container` type
      dataRequest.resolve({ ownerId: '', ownerType: 'repository_container' });
    } else {
      dataRequest.resolve({ ownerId, ownerType });
    }

    return dataRequest.promise;
  }

  function assignWaivePermissions(permissions, ownerType, ownerId) {
    return getContextData(ownerType, ownerId).then(function (data) {
      const { ownerId, ownerType } = data;
      return PermissionService.isContextAuthorized(permissions, ownerType, ownerId).then((response) => {
        $scope.isAddWaiverAuthorized = response;
      });
    });
  }

  Object.assign(vm, {
    $onInit() {
      const { ownerType, ownerId } = OwnerContext;
      assignWaivePermissions(['WAIVE_POLICY_VIOLATIONS'], ownerType, ownerId);
    },
  });

  $scope.getThreatColor = getThreatColor;

  function sortPolicyAlerts() {
    $scope.processedPolicyAlerts.sort(function (a, b) {
      return b.threatLevel - a.threatLevel;
    });
  }

  $scope.quarantined = SelectedComponent.get().quarantined;

  $scope.doLoad = function () {
    $scope.processedPolicyAlerts = null;
    $scope.error = null;

    return PolicyViolations.get().then(
      function (policyThreats) {
        $scope.processedPolicyAlerts = policyThreats;
        sortPolicyAlerts();
      },
      function (err) {
        $scope.error = Messages.getHttpErrorMessage(err);
      }
    );
  };

  $scope.hasQuarantiningViolations = function () {
    return (
      !angular.isArray($scope.processedPolicyAlerts) ||
      $scope.processedPolicyAlerts.some(function (alert) {
        return alert.blocksUnquarantine;
      })
    );
  };

  $scope.waiveComponent = function (policyAlert) {
    // if in new policy centric report
    if ($scope.useNewWaiverPages) {
      $scope.closeCipModal();
      $state.go('addWaiver', { violationId: policyAlert.policyViolationId });
    } else {
      Modal.open({
        templateUrl: 'add-waiver-modal-tmpl',
        controller: 'AddWaiverController',
        backdrop: 'static',
        keyboard: false,
        resolve: {
          policy: function () {
            return policyAlert;
          },
        },
      });
    }
  };

  $scope.releaseQuarantine = function () {
    Modal.open({
      templateUrl: 'release-quarantine-tmpl',
      controller: 'release.quarantine.controller as vm',
      backdrop: 'static',
      keyboard: false,
    }).result.then(function () {
      $scope.quarantined = false;
    });
  };

  $scope.requestWaiver = function (policyAlert) {
    Modal.open({
      template: requestWaiverTemplate,
      controller: 'RequestWaiverController',
      backdrop: 'static',
      keyboard: false,
      resolve: {
        policy: function () {
          return policyAlert;
        },
      },
    });
  };

  $scope.viewWaivers = function () {
    Modal.open({
      templateUrl: 'view-waivers-modal-tmpl',
      controller: 'ViewWaiverController',
      backdrop: 'static',
      keyboard: false,
    });
  };
  $scope.innerSourceTransitiveWaiver = ProductFeatures.isAvailable('inner-source-transitive-waiver');
  $scope.isInnerSource = function () {
    const component = SelectedComponent.get();
    return !!(component && component.innerSource);
  };
  $scope.hasComponentIdentifier = function () {
    const component = SelectedComponent.get();
    return !!(component && component.componentIdentifier);
  };
  $scope.viewTransitiveViolations = function () {
    const hash = SelectedComponent.get().hash;
    $scope.closeCipModal();
    $state.go('transitiveViolations', {
      ownerType: OwnerContext.ownerType,
      ownerId: OwnerContext.ownerId,
      scanId: OwnerContext.scanId,
      hash: hash,
    });
  };
  $scope.alerts = [];

  $scope.$on('component.evaluation.updated', function (event, componentKey, promises) {
    promises.push($scope.doLoad());
  });

  $scope.$watch(
    function () {
      return SelectedComponent.get();
    },
    function (component) {
      if (component) {
        $scope.doLoad();
      }
    }
  );
}

PolicyViolationsController.$inject = [
  '$http',
  '$scope',
  '$q',
  'Modal',
  'SelectedComponent',
  'OwnerContext',
  'PolicyViolations',
  'Messages',
  '$state',
  'PermissionService',
  'ProductFeatures',
];
