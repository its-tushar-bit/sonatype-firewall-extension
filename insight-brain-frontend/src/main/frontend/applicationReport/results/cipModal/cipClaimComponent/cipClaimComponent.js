/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import 'bootstrap-datepicker';
import moment from 'moment';
import { any, path, pick, props } from 'ramda';
import template from './cipClaimComponent.html';

const DATE_FORMAT = 'MM/DD/YYYY';

export default {
  template,
  controller: ClaimComponentController,
  controllerAs: 'vm',
  bindings: {
    component: '<',
  },
};

function ClaimComponentController(
  $scope,
  $http,
  Dialog,
  CLMLocations,
  $timeout,
  Messages
) {
  const vm = this;

  Object.assign(vm, {
    claimData: {},
    loading: false,
    serverClaimData: undefined,
    claimForm: undefined,
    error: undefined,
    datePickerElement: undefined,

    $onInit() {
      vm.initializeDatepicker();
      $scope.$watch('vm.component', fetchClaim);
    },

    initializeDatepicker() {
      const context = $('#claim-component-editor');
      vm.datePickerElement = $('.input-append.date', context);
      vm.datePickerElement
        .datepicker({
          format: 'mm/dd/yyyy',
          autoclose: true,
          endDate: new Date(),
          clearBtn: true,
          forceParse: false,
        })
        .on('clearDate', function () {
          // use $timeout instead of $scope.$apply to avoid "digest already in progress" error
          // (occurs when Revoking claim with createdDate)
          $timeout(function () {
            vm.claimForm.$setDirty();
            vm.claimData.createTimeText = '';
          });
        });
    },

    setServerData(serverClaimData) {
      vm.serverClaimData = serverClaimData;
      vm.resetFormFromServerData();
    },

    resetFormFromServerData() {
      vm.claimForm.$setPristine();

      // If we have previously claimed this component, use the stored values
      if (vm.isClaimedComponent()) {
        vm.claimData = pick(
          ['groupId', 'artifactId', 'version', 'classifier', 'extension'],
          vm.serverClaimData.componentIdentifier.coordinates
        );
        vm.claimData.comment = vm.serverClaimData.comment;
      } else {
        vm.claimData = {};
      }

      // always show createDate, if available
      const createTime = vm.serverClaimData
        ? vm.serverClaimData.createTime
        : vm.component.createTime;
      const createDate = createTime != null ? new Date(createTime) : '';
      vm.datePickerElement.datepicker('update', createDate);
      vm.claimData.createTimeText =
        createTime != null ? moment(createTime).format(DATE_FORMAT) : null;
    },

    isClaimedComponent() {
      return !!vm.serverClaimData;
    },

    /**
     * Claim the presently selected component
     */
    claimComponent() {
      if (vm.claimForm.$valid) {
        return handleHttpRequest(
          $http.post(
            CLMLocations.getClaimComponentUrl(),
            getClaimDataForServer()
          )
        );
      }
    },

    /**
     * Update the claim information for the presently selected component
     */
    updateComponent() {
      if (vm.claimForm.$valid) {
        handleHttpRequest(
          $http.put(
            CLMLocations.getClaimComponentUrl(),
            getClaimDataForServer()
          )
        );
      }
    },

    /**
     * Remove(delete) an existing claim on a component
     */
    revokeClaim() {
      handleHttpRequest(
        $http.delete(CLMLocations.getClaimComponentUrl(vm.component.hash))
      );
    },

    openRevokeClaimDialog() {
      Dialog.open({
        id: 'confirm-revoke-claim-dialog',
        title: 'Revoke Claim',
        body:
          'Are you sure you want to revoke the claim on this component?' +
          ' This change will not be reflected until a new policy evaluation is triggered.',
        buttons: [
          {
            name: 'Revoke',
            type: 'primary',
            click: vm.revokeClaim,
          },
          {
            name: 'Cancel',
            type: 'cancel',
          },
        ],

        windowClass: 'iq-modal',
        backdropClass: null,
      });
    },

    getValidationMessage() {
      const claimForm = vm.claimForm;
      const hasRequiredError = any(
        path(['$error', 'required']),
        props(['groupId', 'artifactId', 'version', 'extension'], claimForm)
      );
      if (claimForm.$submitted && hasRequiredError) {
        return 'Group ID, Artifact ID, Version and Extension are required';
      } else if (
        claimForm.createTimeText.$dirty &&
        claimForm.createTimeText.$error.pattern
      ) {
        return 'Date format is MM/DD/YYYY';
      }
    },
  });

  function fetchClaim() {
    return handleHttpRequest(
      $http.get(CLMLocations.getClaimComponentUrl(vm.component.hash))
    );
  }

  function handleHttpRequest(promise) {
    vm.loading = true;
    vm.error = undefined;

    return promise
      .then(function ({ data }) {
        vm.setServerData(data);
      })
      .catch(function (err) {
        if (err.status === 404) {
          // no claim for this hash
          vm.setServerData();
        } else {
          vm.error = Messages.getHttpErrorMessage(err);
        }
      })
      .finally(function () {
        vm.loading = false;
      });
  }

  function getClaimDataForServer() {
    const { createTimeText } = vm.claimData;
    const coordinates = pick(
      ['groupId', 'artifactId', 'version', 'extension'],
      vm.claimData
    );

    // as classifier is optional, we want to enforce an empty string when the user has not touched the field
    coordinates.classifier = vm.claimData.classifier || '';

    return {
      hash: vm.component.hash,
      componentIdentifier: {
        format: 'maven',
        coordinates,
      },
      createTime: createTimeText
        ? moment(createTimeText, DATE_FORMAT).valueOf()
        : null,
      comment: vm.claimData.comment,
    };
  }
}

ClaimComponentController.$inject = [
  '$scope',
  '$http',
  'Dialog',
  'CLMLocations',
  '$timeout',
  'Messages',
];
