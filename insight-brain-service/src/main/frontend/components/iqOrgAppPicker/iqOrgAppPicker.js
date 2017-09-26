/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './iqOrgAppPicker.html';

import {isSelected, areAllSelected, groupAppsByOrgId, selectedMapToSet} from './utils';

const iqOrgAppPicker = {
  template,
  controller: IqOrgAppPickerController,
  controllerAs: 'vm',
  bindings: {
    organizations: '<',
    applications: '<',
    providedSelectedOrganizations: '<selectedOrganizations',
    providedSelectedApplications: '<selectedApplications',
    onChange: '&'
  }
};

export default iqOrgAppPicker;

function IqOrgAppPickerController() {
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

  function onSelectedOrganizationsChange(selectedOrganizations) {
    const selectedApplications = selectApplications(selectedOrganizations);
    vm.onChange({selectedOrganizations, selectedApplications});
  }

  function selectOrganizations(selectedApplications) {
    return vm.organizations
        .filter(shouldOrgBeSelected(selectedApplications))
        .reduce((selected, {id}) => selected.add(id), new Set());
  }

  function selectApplications(selectedOrganizations) {
    return groupAppsByOrgId(vm.applications)
        .map(getSelectedApps(selectedOrganizations))
        .reduce((allApps, apps) => [...allApps, ...apps], []) // flatten array of arrays
        .reduce((selected, {id}) => selected.add(id), new Set());
  }

  /**
   * Given map of selected orgs, returns function that will extract only selected apps from the org
   * @param selectedOrgs map of selected orgs
   */
  const getSelectedApps = selectedOrgs => ({orgId, apps}) => {
    if (selectedOrgs.has(orgId)) {
      // if Org is selected - select all related apps
      return apps;
    }
    else {
      // if Org is not selected && all related apps are selected - deselect all related apps
      if (areAllSelected(vm.selectedApplications, apps)) {
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
