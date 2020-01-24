/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './iqOrgAppPickerAngular.html';

import {isSelected, areAllSelected, groupAppsByOrgId, selectedMapToSet} from './utils';

const iqOrgAppPickerAngular = {
  template,
  controller: IqOrgAppPickerAngularController,
  controllerAs: 'vm',
  bindings: {
    organizations: '<',
    applications: '<',
    providedSelectedOrganizations: '<selectedOrganizations',
    providedSelectedApplications: '<selectedApplications',
    onChange: '&'
  }
};

export default iqOrgAppPickerAngular;

function IqOrgAppPickerAngularController() {
  const vm = this;
  vm.selectedOrganizations = new Set();
  vm.selectedApplications = new Set();
  vm.onSelectedOrganizationsChange = onSelectedOrganizationsChange;
  vm.onSelectedApplicationsChange = onSelectedApplicationsChange;

  vm.$onChanges = function({providedSelectedOrganizations, providedSelectedApplications}) {
    if (providedSelectedOrganizations) {
      vm.selectedOrganizations = providedSelectedOrganizations.currentValue instanceof Set ?
        providedSelectedOrganizations.currentValue : selectedMapToSet(providedSelectedOrganizations.currentValue);
    }

    if (providedSelectedApplications) {
      vm.selectedApplications = providedSelectedApplications.currentValue instanceof Set ?
        providedSelectedApplications.currentValue : selectedMapToSet(providedSelectedApplications.currentValue);
    }
  };

  function onSelectedApplicationsChange(selectedApplications) {
    const selectedOrganizations = selectOrganizations(selectedApplications);
    vm.onChange({selectedOrganizations, selectedApplications});
  }

  function onSelectedOrganizationsChange(selectedOrganizations, toggledOrg) {
    const selectedApplications = selectApplications(selectedOrganizations, toggledOrg);
    vm.onChange({selectedOrganizations, selectedApplications});
  }

  function selectOrganizations(selectedApplications) {
    return vm.organizations
        .filter(shouldOrgBeSelected(selectedApplications))
        .reduce((selected, {id}) => selected.add(id), new Set());
  }

  function selectApplications(selectedOrganizations, toggledOrg) {
    // if All Orgs were deselected using the all/none btn - no apps selected
    if (selectedOrganizations.size === 0 && toggledOrg == null) {
      return new Set();
    }

    return groupAppsByOrgId(vm.applications)
        .map(getSelectedApps(selectedOrganizations, toggledOrg))
        .reduce((allApps, apps) => [...allApps, ...apps], []) // flatten array of arrays
        .reduce((selected, {id}) => selected.add(id), new Set());
  }

  /**
   * Given map of selected orgs, returns function that will extract only selected apps from the org
   * @param selectedOrgs map of selected orgs
   * @param toggledOrg the id of toggled Org
   */
  const getSelectedApps = (selectedOrgs, toggledOrg) => ({orgId, apps}) => {
    if (selectedOrgs.has(orgId)) {
      // if Org is selected - select all related apps
      return apps;
    }
    else {
      // if Org was toggled and deselected && all related apps are selected - deselect all related apps
      if (orgId === toggledOrg && areAllSelected(vm.selectedApplications, apps)) {
        return [];
      }
      return apps.filter(isSelected(vm.selectedApplications));
    }
  };

  /**
   * Given map of selected apps, returns predicate function to filter selected orgs
   * @param selectedApps map of selected apps
   */
  const shouldOrgBeSelected = selectedApps => org => {
    const relatedApps = vm.applications.filter(app => app.organizationId === org.id);
    const hasApps = relatedApps.length !== 0;

    // deselect an Org only if it has apps and not all of them are selected
    return (areAllSelected(selectedApps, relatedApps) || !hasApps) ? vm.selectedOrganizations.has(org.id) : false;
  };
}
