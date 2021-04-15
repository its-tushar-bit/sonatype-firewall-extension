/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxFontAwesomeIcon,
  NxStatefulTreeViewMultiSelect,
} from '@sonatype/react-shared-components';
import { faSitemap, faTerminal } from '@fortawesome/pro-regular-svg-icons';
import { areAllSelected, groupAppsByOrgId, isSelected } from './utils';

export default function IqOrgAppPicker(props) {
  const {
    organizations,
    applications,
    selectedOrganizations,
    selectedApplications,
    onChange,
    id,
  } = props;

  function onSelectedApplicationsChange(selectedApplications) {
    const selectedOrganizations = selectOrganizations(selectedApplications);
    onChange(selectedOrganizations, selectedApplications);
  }

  function onSelectedOrganizationsChange(selectedOrganizations, toggledOrg) {
    const selectedApplications = selectApplications(
      selectedOrganizations,
      toggledOrg
    );
    onChange(selectedOrganizations, selectedApplications);
  }

  function selectApplications(selectedOrganizations, toggledOrg) {
    // if All Orgs were deselected using the all/none btn - no apps selected
    if (selectedOrganizations.size === 0 && toggledOrg == null) {
      return new Set();
    }

    return groupAppsByOrgId(applications)
      .map(getSelectedApps(selectedOrganizations, toggledOrg))
      .reduce((allApps, apps) => [...allApps, ...apps], []) // flatten array of arrays
      .reduce((selected, { id }) => selected.add(id), new Set());
  }

  function selectOrganizations(selectedApplications) {
    return organizations
      .filter(shouldOrgBeSelected(selectedApplications))
      .reduce((selected, { id }) => selected.add(id), new Set());
  }

  /**
   * Given map of selected orgs, returns function that will extract only selected apps from the org
   * @param selectedOrgs map of selected orgs
   * @param toggledOrg the id of toggled Org
   */
  const getSelectedApps = (selectedOrgs, toggledOrg) => ({ orgId, apps }) => {
    if (selectedOrgs.has(orgId)) {
      // if Org is selected - select all related apps
      return apps;
    } else {
      // if Org was toggled and deselected && all related apps are selected - deselect all related apps
      if (orgId === toggledOrg && areAllSelected(selectedApplications, apps)) {
        return [];
      }
      return apps.filter(isSelected(selectedApplications));
    }
  };

  /**
   * Given map of selected apps, returns predicate function to filter selected orgs
   * @param selectedApps map of selected apps
   */
  const shouldOrgBeSelected = (selectedApps) => (org) => {
    const relatedApps = applications.filter(
      (app) => app.organizationId === org.id
    );
    const hasApps = relatedApps.length !== 0;

    // deselect an Org only if it has apps and not all of them are selected
    return areAllSelected(selectedApps, relatedApps) || !hasApps
      ? selectedOrganizations.has(org.id)
      : false;
  };

  return (
    <div id={id}>
      <NxStatefulTreeViewMultiSelect
        name="organizations"
        options={organizations}
        onChange={onSelectedOrganizationsChange}
        selectedIds={selectedOrganizations}
        filterPlaceholder="Organization Name"
        disabledTooltip="There are no organizations to filter"
      >
        <NxFontAwesomeIcon icon={faSitemap} />
        <span>Organizations</span>
      </NxStatefulTreeViewMultiSelect>
      <NxStatefulTreeViewMultiSelect
        name="applications"
        options={applications}
        onChange={onSelectedApplicationsChange}
        selectedIds={selectedApplications}
        filterPlaceholder="Application Name"
        disabledTooltip="There are no applications to filter"
      >
        <NxFontAwesomeIcon icon={faTerminal} />
        <span>Applications</span>
      </NxStatefulTreeViewMultiSelect>
    </div>
  );
}

IqOrgAppPicker.propTypes = {
  organizations: PropTypes.array,
  applications: PropTypes.array,
  selectedOrganizations: PropTypes.instanceOf(Set).isRequired,
  selectedApplications: PropTypes.instanceOf(Set).isRequired,
  onChange: PropTypes.func.isRequired,
  id: PropTypes.string,
};
