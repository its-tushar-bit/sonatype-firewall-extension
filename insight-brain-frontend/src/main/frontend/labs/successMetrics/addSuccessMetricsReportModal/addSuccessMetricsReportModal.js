/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './addSuccessMetricsReportModal.html';

export default {
  template,
  controller: addSuccessMetricsReportModalController,
  controllerAs: 'vm',
  bindings: {
    close: '&',
    dismiss: '&',
    existingReports: '<',
  },
};

function addSuccessMetricsReportModalController(
  $q,
  ApplicationStore,
  OrganizationStore,
  successMetricsDataService,
  Messages
) {
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
    includeLatestData: false,

    maskController: undefined, // gets set by form-mask directive in template
    addSuccessMetricsReportForm: undefined, // gets set by name attr on form element

    $onInit() {
      vm.error = undefined;

      $q.all([ApplicationStore.get(), OrganizationStore.get()])
        .then(function ([applications, organizations]) {
          vm.applications = applications;
          vm.organizations = organizations.filter((org) => org.id !== 'ROOT_ORGANIZATION_ID');
        })
        .catch(function (error) {
          vm.error = error;
        })
        .finally(function () {
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

        set.forEach(function (val) {
          retval.push(val);
        });

        return retval;
      }

      vm.maskController
        .wrap(
          successMetricsDataService.createSuccessMetricsReportForCurrentUser({
            name: vm.name,
            scope: vm.isAllApplications
              ? {}
              : {
                  organizationIds: toArray(vm.selectedOrganizations),
                  applicationIds: toArray(vm.selectedApplications),
                },
            includeLatestData: vm.includeLatestData,
          })
        )
        .then((result) => vm.close({ result }))
        .catch((error) => (vm.error = error));
    },

    isCreateEnabled() {
      const form = vm.addSuccessMetricsReportForm;

      return !!(
        form &&
        !form.$invalid &&
        (vm.isAllApplications || vm.selectedApplications.size + vm.selectedOrganizations.size > 0)
      );
    },

    getErrorMessage() {
      return vm.error && Messages.getHttpErrorMessage(vm.error);
    },
  });
}

addSuccessMetricsReportModalController.$inject = [
  '$q',
  'ApplicationStore',
  'OrganizationStore',
  'successMetricsDataService',
  'Messages',
];
