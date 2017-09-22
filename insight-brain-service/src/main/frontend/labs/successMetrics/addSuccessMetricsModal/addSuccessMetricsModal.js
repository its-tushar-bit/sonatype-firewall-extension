/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './addSuccessMetricsModal.html';

export default {
  template,
  controller: addSuccessMetricsModalController,
  controllerAs: 'vm',
  bindings: {
    close: '&',
    dismiss: '&',
    existingSuccessMetrics: '<'
  }
};

function addSuccessMetricsModalController($q, ApplicationStore, OrganizationStore, successMetricsDataService,
                                          Messages) {
  const vm = this;

  Object.assign(vm, {
    error: undefined,
    loaded: false,

    name: '',
    applications: [],
    organizations: [],
    selectedApplications: new Set(),
    selectedOrganizations: new Set(),
    isAllApplications: true,

    // gets set by form-mask directive in template
    maskController: undefined,

    $onInit() {
      vm.error = undefined;

      $q.all([ApplicationStore.get(), OrganizationStore.get()]).then(function([applications, organizations]) {
        vm.applications = applications;
        vm.organizations = organizations.filter(org => org.id !== 'ROOT_ORGANIZATION_ID');
      }).catch(function(error) {
        vm.error = error;
      }).finally(function() {
        vm.loaded = true;
      });
    },

    onOrgAppSelectionChange(selectedOrganizations, selectedApplications) {
      vm.selectedOrganizations = selectedOrganizations;
      vm.selectedApplications = selectedApplications;
    },

    onSubmit() {
      if (!vm.isCreateEnabled()) {
        return;
      }
      // Ideally we'd use Array.from but its not supported in IE
      function toArray(set) {
        const retval = [];

        set.forEach(function(val) {
          retval.push(val);
        });

        return retval;
      }

      vm.maskController.wrap(successMetricsDataService
          .createSuccessMetricsForCurrentUser({
            name: vm.name,
            scope: vm.isAllApplications ? {} : {
              organizationIds: toArray(vm.selectedOrganizations),
              applicationIds: toArray(vm.selectedApplications)
            }
          }))
          .then(result => vm.close({ result }))
          .catch(error => vm.error = error);
    },

    isCreateEnabled() {
      const form = vm.addSuccessMetricsForm;

      return !!(form && !form.$invalid && (vm.isAllApplications ||
          (vm.selectedApplications.size + vm.selectedOrganizations.size > 0)));
    },

    getErrorMessage() {
      return vm.error && Messages.getHttpErrorMessage(vm.error);
    }
  });
}

addSuccessMetricsModalController.$inject = [
  '$q', 'ApplicationStore', 'OrganizationStore', 'successMetricsDataService', 'Messages'
];
