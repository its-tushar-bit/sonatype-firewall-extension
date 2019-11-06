/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './automaticSourceControlConfiguration.html';

const automaticSourceControlConfiguration = {
  controller: AutomaticSourceControlConfigurationController,
  bindings: {
    isAuthorized: '<'
  },
  controllerAs: 'vm',
  template: template
};

function AutomaticSourceControlConfigurationController(automaticSourceControlConfigurationService) {
  const vm = this;

  Object.assign(vm, {
    error: undefined,
    loaded: false,

    automaticSourceControlEnabled: undefined,
    savedAutomaticSourceControlEnabled: undefined,

    $onInit() {
      vm.load();
    },

    load() {
      vm.error = undefined;
      vm.loaded = false;

      automaticSourceControlConfigurationService.getConfiguration().then(function(data) {
        vm.automaticSourceControlEnabled = data.enabled;
        vm.savedAutomaticSourceControlEnabled = vm.automaticSourceControlEnabled;
        vm.loaded = true;
      }).catch(function(error) {
        vm.error = error;
      });
    },

    save() {
      if (!vm.isChanged() || !vm.automaticSourceControlConfigurationForm.$valid) {
        return;
      }

      vm.error = undefined;

      const configuration = {
        enabled: vm.automaticSourceControlEnabled
      };

      const savePromise = automaticSourceControlConfigurationService.saveConfiguration(configuration);
      vm.automaticSourceControlConfigurationFormMask.wrap(savePromise).then(function(data) {
        vm.savedAutomaticSourceControlEnabled = data.enabled;
      }).catch(function(error) {
        vm.error = error;
      });
    },

    cancel() {
      vm.automaticSourceControlEnabled = vm.savedAutomaticSourceControlEnabled;
    },

    isChanged() {
      return vm.savedAutomaticSourceControlEnabled !== vm.automaticSourceControlEnabled;
    }
  });
}

AutomaticSourceControlConfigurationController.$inject = ['automaticSourceControlConfigurationService'];

export default automaticSourceControlConfiguration;
