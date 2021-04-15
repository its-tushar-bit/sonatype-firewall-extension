/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './samlConfiguration.html';
import { omit } from 'ramda';

export default {
  template: template,
  bindings: {
    isAuthorized: '<',
  },
  controllerAs: 'vm',
  controller: SamlConfigurationController,
};

function SamlConfigurationController($scope, BaseUrl, $http, CLMContextLocations, Messages, Dialog, $window) {
  const vm = this;
  const defaultSaml = {
    identityProviderName: 'identity provider',
    identityProviderMetadataXml: undefined,
    entityId: BaseUrl.get() + '/api/v2/config/saml/metadata',
    usernameAttributeName: 'username',
    firstNameAttributeName: 'firstName',
    lastNameAttributeName: 'lastName',
    emailAttributeName: 'email',
    groupsAttributeName: 'groups',
    validateResponseSignature: null,
    validateAssertionSignature: null,
  };
  const validateSignatureOptions = [
    { name: 'Default', value: null },
    { name: 'True', value: true },
    { name: 'False', value: false },
  ];

  let originalSaml = undefined;

  Object.assign(vm, {
    loadError: undefined,
    saveOrDeleteError: undefined,
    saml: undefined,
    defaultSaml: angular.copy(defaultSaml),
    isUpdating: undefined,
    samlConfigurationMask: undefined,
    validateSignatureOptions: angular.copy(validateSignatureOptions),
    load() {
      resetErrors();
      $http.get(CLMContextLocations.getSamlConfigurationUrl()).then(
        function (response) {
          setSaml(response.data, true);
        },
        function (error) {
          if (error.status === 404) {
            setSaml(defaultSaml, false);
            return;
          }
          onError('loadError', error);
        }
      );
    },
    readIdentityProviderMetadataXml(file) {
      if (file !== undefined) {
        getFileReader().readAsText(file);
      }
    },
    isChanged() {
      return !angular.equals(vm.saml, originalSaml);
    },
    save() {
      resetErrors();
      let formData = new FormData();
      formData.append('identityProviderXml', vm.saml.identityProviderMetadataXml);
      let payload = omit(['identityProviderMetadataXml'], vm.saml);
      formData.append('samlConfiguration', JSON.stringify(payload));
      vm.samlConfigurationMask
        .wrap(
          $http
            .put(CLMContextLocations.getSamlConfigurationUrl(), formData, {
              // Angular's default transformRequest will try to serialize our formData, so we override it with the identity
              // function to leave formData intact
              transformRequest: angular.identity,
              // Angular's default Content-Type header for POST/PUT is application/json, by setting it to undefined the
              // browser sets it to multipart/form-data and fills in the correct boundary (which wouldn't happen if we set it
              // manually to multipart/form-data)
              headers: { 'Content-Type': undefined },
            })
            .then(vm.load)
        )
        .catch((error) => onError('saveOrDeleteError', error));
    },
    cancel() {
      resetErrors();
      vm.saml = angular.copy(originalSaml);
    },
    delete() {
      Dialog.open({
        title: 'Delete Configuration',
        body: 'Are you sure you want to delete this SAML configuration?',
        id: 'delete-saml-confirmation',
        buttons: [
          {
            name: 'Delete',
            type: 'primary',
            click: deleteConfiguration,
          },
          {
            name: 'Cancel',
            type: 'cancel',
          },
        ],
      });
    },
    defaultsToTooltipText(defaultValue) {
      return 'If empty will default to "' + defaultValue + '"';
    },
    resetToDefaultValueIfEmpty(name) {
      if (!vm.saml[name]) {
        vm.saml[name] = defaultSaml[name];
      }
    },
    // IE workaround
    downloadMetadataForIE() {
      if (vm.isUpdating && $window.navigator.msSaveBlob) {
        $http
          .get(CLMContextLocations.getSamlConfigurationUrl() + '/metadata')
          .then((response) => $window.navigator.msSaveBlob(new Blob([response.data]), 'metadata.xml'));
      }
    },
    shouldEnableDownloadMetadataLink() {
      return vm.isUpdating && !$window.navigator.msSaveBlob;
    },
  });

  function getFileReader() {
    let fileReader = new FileReader();
    fileReader.addEventListener('load', function () {
      $scope.$apply(function () {
        vm.saml.identityProviderMetadataXml = fileReader.result;
      });
    });
    return fileReader;
  }

  function deleteConfiguration() {
    resetErrors();
    vm.samlConfigurationMask
      .wrap($http.delete(CLMContextLocations.getSamlConfigurationUrl()).then(vm.load))
      .catch((error) => onError('saveOrDeleteError', error));
  }

  function setSaml(saml, exists) {
    vm.saml = angular.copy(saml);
    originalSaml = angular.copy(vm.saml);
    vm.isUpdating = exists;
  }

  function onError(target, error) {
    vm[target] = Messages.getHttpErrorMessage(error);
  }

  function resetErrors() {
    vm.loadError = undefined;
    vm.saveOrDeleteError = undefined;
  }

  vm.load();
}

SamlConfigurationController.$inject = [
  '$scope',
  'BaseUrl',
  '$http',
  'CLMContextLocations',
  'Messages',
  'Dialog',
  '$window',
];
