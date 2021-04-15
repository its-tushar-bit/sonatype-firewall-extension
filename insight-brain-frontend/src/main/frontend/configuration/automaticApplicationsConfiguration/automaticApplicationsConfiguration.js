/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './automaticApplicationsConfiguration.html';

const automaticApplicationsConfiguration = {
  controller: AutomaticApplicationsConfigurationController,
  bindings: {
    isAuthorized: '<',
  },
  controllerAs: 'vm',
  template: template,
};

function AutomaticApplicationsConfigurationController(
  $q,
  OrganizationStore,
  automaticApplicationsConfigurationService
) {
  const vm = this;

  Object.assign(vm, {
    error: undefined,
    organizationOptions: undefined,
    loaded: false,

    automaticApplicationCreationEnabled: undefined,
    automaticApplicationCreationOrganizationId: undefined,
    savedAutomaticApplicationCreationEnabled: undefined,
    savedAutomaticApplicationCreationOrganizationId: undefined,

    $onInit() {
      vm.load();
    },

    load() {
      vm.error = undefined;
      vm.loaded = false;

      const organizationPromise = OrganizationStore.get().then(function (data) {
        vm.organizationOptions = data.filter((org) => org.id !== 'ROOT_ORGANIZATION_ID');
      });
      const configurationPromise = automaticApplicationsConfigurationService.getConfiguration().then(function (data) {
        vm.automaticApplicationCreationEnabled = data.enabled;
        vm.automaticApplicationCreationOrganizationId = data.parentOrganizationId;
        vm.savedAutomaticApplicationCreationEnabled = vm.automaticApplicationCreationEnabled;
        vm.savedAutomaticApplicationCreationOrganizationId = vm.automaticApplicationCreationOrganizationId;
      });

      $q.all([organizationPromise, configurationPromise])
        .then(function () {
          vm.loaded = true;
        })
        .catch(function (error) {
          vm.error = error;
        });
    },

    save() {
      if (!vm.isChanged() || !vm.automaticApplicationsConfigurationForm.$valid) {
        return;
      }

      vm.error = undefined;

      const configuration = {
        enabled: vm.automaticApplicationCreationEnabled,
        parentOrganizationId: vm.automaticApplicationCreationOrganizationId,
      };

      const savePromise = automaticApplicationsConfigurationService.saveConfiguration(configuration);
      vm.automaticApplicationsConfigurationFormMask
        .wrap(savePromise)
        .then(function (data) {
          vm.savedAutomaticApplicationCreationEnabled = data.enabled;
          vm.savedAutomaticApplicationCreationOrganizationId = data.parentOrganizationId;
        })
        .catch(function (error) {
          vm.error = error;
        });
    },

    cancel() {
      vm.automaticApplicationCreationEnabled = vm.savedAutomaticApplicationCreationEnabled;
      vm.automaticApplicationCreationOrganizationId = vm.savedAutomaticApplicationCreationOrganizationId;
    },

    isChanged() {
      return (
        vm.savedAutomaticApplicationCreationEnabled !== vm.automaticApplicationCreationEnabled ||
        vm.savedAutomaticApplicationCreationOrganizationId !== vm.automaticApplicationCreationOrganizationId
      );
    },
  });
}

AutomaticApplicationsConfigurationController.$inject = [
  '$q',
  'OrganizationStore',
  'automaticApplicationsConfigurationService',
];

export default automaticApplicationsConfiguration;
