/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo, useState } from 'react';
import * as PropTypes from 'prop-types';
import { NxCollapsibleMultiSelect, NxFontAwesomeIcon, useToggle, NxTooltip } from '@sonatype/react-shared-components';
import { faSitemap, faTerminal } from '@fortawesome/pro-regular-svg-icons';
import { faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import { areAllSelected, groupAppsByOrgId, isSelected } from './utils';
import { fuzzyFilter } from 'MainRoot/OrgsAndPolicies/ownerSideNav/utils';
import { faLevelUp } from '@fortawesome/pro-regular-svg-icons';
import { filter as ramdaFilter, take } from 'ramda';

const MAX_APPS = 500;
const MAX_APPS_TOOLTIP_MESSAGE = (
  <span>
    Apps displayed in filter list are limited to {MAX_APPS}.
    <br />
    Enter the app name below to narrow your search.
  </span>
);

export default function IqOrgAppPicker(props) {
  const [orgFilter, setOrgFilter] = useState('');
  const [isOrgsOpen, onOrgsToggleCollapse] = useToggle(false);
  const [appFilter, setAppFilter] = useState('');
  const [isAppsOpen, onAppsToggleCollapse] = useToggle(false);

  const {
    applications,
    selectedOrganizations,
    selectedApplications,
    onChange,
    id,
    ownersMap,
    topParentOrganizationId,
  } = props;

  const shouldOrgBePushedIntoFormattedOrgs = (org) =>
    org.id !== 'ROOT_ORGANIZATION_ID' && (!org.synthetic || org.applicationIds?.length > 0);
  function onSelectedApplicationsChange(selectedApplications) {
    const selectedOrganizations = selectOrganizations(selectedApplications);
    onChange(selectedOrganizations, selectedApplications);
  }

  function onSelectedOrganizationsChange(selectedOrganizations, toggledOrg) {
    const [newSelectedOrganizations, toggledOrgs] = selectOrganizationsByOrg(selectedOrganizations, toggledOrg);
    const selectedApplications = selectApplications(newSelectedOrganizations, toggledOrg, toggledOrgs);
    // The Set received from selectOrganizationsByOrg includes synthetic orgs, this filter removes them
    const filteredNewSelectedOrganizations = ramdaFilter((orgId) =>
      shouldOrgBePushedIntoFormattedOrgs(ownersMap[orgId])
    )([...newSelectedOrganizations]);
    onChange(new Set(filteredNewSelectedOrganizations), selectedApplications);
  }

  function selectApplications(selectedOrganizations, toggledOrg, toggledOrgs) {
    // if All Orgs were deselected using the all/none btn - no apps selected
    if (selectedOrganizations.size === 0 && toggledOrg == null) {
      return new Set();
    }
    return groupAppsByOrgId(applications)
      .map(getSelectedApps(selectedOrganizations, toggledOrgs))
      .reduce((allApps, apps) => [...allApps, ...apps], []) // flatten array of arrays
      .reduce((selected, { id }) => selected.add(id), new Set());
  }

  function selectOrganizations(selectedApplications) {
    return formattedOrganizations
      .filter(shouldOrgBeSelected(selectedApplications))
      .reduce((selected, { id }) => selected.add(id), new Set());
  }

  /**
   * Given a list of organizations and an org id returns the selected orgs and an set of the toggled orgs
   * (the toggledOrg and its children)
   * @param selectedOrganizations List of selected orgs
   * @param toggledOrg the id of toggled Org
   */
  function selectOrganizationsByOrg(selectedOrganizations, toggledOrg) {
    const isOrgSelected = selectedOrganizations.has(toggledOrg);
    let toggledOrgs = new Set([toggledOrg]);
    let selectedOrgsToReturn;
    // Select al orgs for which parentOrganizationId is the current selected org
    // The fullFormattedOrganizations Set is used to be able to traverse all the tree when selecting an org
    // even adding the synthetic orgs
    const newSelectedOrgs = fullFormattedOrganizations
      .filter((org) => org.parentOrganizationId === toggledOrg)
      .reduce((selected, { id }) => selected.add(id), new Set());
    if (isOrgSelected) {
      selectedOrgsToReturn = new Set([...selectedOrganizations, ...newSelectedOrgs]);
      // every new selected org needs to select all of its children so a recursive call is in place for that
      newSelectedOrgs.forEach((org) => {
        const [orgs, newToggledOrgs] = selectOrganizationsByOrg(selectedOrgsToReturn, org);
        // Keep a track of all the children orgs that were selected in cascade
        toggledOrgs = new Set([...toggledOrgs, ...newToggledOrgs]);
        selectedOrgsToReturn = new Set([...selectedOrgsToReturn, ...orgs]);
      });
    } else {
      selectedOrgsToReturn = new Set(selectedOrganizations);
      newSelectedOrgs.forEach((org) => {
        selectedOrgsToReturn.delete(org);
        const [orgs, newToggledOrgs] = selectOrganizationsByOrg(selectedOrgsToReturn, org);
        // Keep a track of all the children orgs that were unselected in cascade
        toggledOrgs = new Set([...toggledOrgs, ...newToggledOrgs]);
        selectedOrgsToReturn = new Set(orgs);
      });
    }
    return [selectedOrgsToReturn, toggledOrgs];
  }

  /**
   * Given map of selected orgs, returns function that will extract only selected apps from the org
   * @param selectedOrgs map of selected orgs
   * @param toggledOrg the id of toggled Org
   */
  const getSelectedApps = (selectedOrgs, toggledOrgs) => ({ orgId, apps }) => {
    if (selectedOrgs.has(orgId)) {
      // if Org is selected - select all related apps
      return apps;
    } else {
      // if Org was toggled and deselected && all related apps are selected - deselect all related apps
      if (toggledOrgs.has(orgId) && areAllSelected(selectedApplications, apps)) {
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
    const relatedApps = applications.filter((app) => app.organizationId === org.id);
    const hasApps = relatedApps.length !== 0;

    // deselect an Org only if it has apps and not all of them are selected
    return areAllSelected(selectedApps, relatedApps) || !hasApps ? selectedOrganizations.has(org.id) : false;
  };

  /**
   * formats the name of the organizations to show the padding for the children orgs
   * @param owners a map of owners with id as lookup
   * @param parentId The id of the current level parent
   * @param formattedOrgs the array to push the owner to
   * @param depth the level of depth in the hierarchical structure
   */
  const formatOrganizationsName = (owners, parentId, formattedOrgs, fullFormattedOrgs, appliedFilter, depth = 0) => {
    //The padding width for each level in the hierarchy
    const TAB_WIDTH = 17;
    const childrenIcon = <NxFontAwesomeIcon icon={faLevelUp} className="iq-filter-children-icon" />;
    const parentOrg = owners[parentId];
    if (!parentOrg) return [formattedOrgs, fullFormattedOrgs];
    const shouldBePushedIntoFormattedOrgs = shouldOrgBePushedIntoFormattedOrgs(parentOrg);
    const depthLevelToIncrease = shouldBePushedIntoFormattedOrgs ? 1 : 0;
    const name = appliedFilter ? (
      parentOrg.name
    ) : (
      <span style={{ paddingLeft: `${(depth - 1) * TAB_WIDTH}px` }}>
        {depth > 0 && childrenIcon}
        {parentOrg.name}
      </span>
    );
    if (shouldBePushedIntoFormattedOrgs) {
      formattedOrgs.push({
        ...parentOrg,
        name,
      });
    }
    fullFormattedOrgs.push({
      ...parentOrg,
      name,
    });

    parentOrg.organizationIds.forEach((orgId) => {
      formatOrganizationsName(
        owners,
        orgId,
        formattedOrgs,
        fullFormattedOrgs,
        appliedFilter,
        depth + depthLevelToIncrease
      );
    });

    return [formattedOrgs, fullFormattedOrgs];
  };

  const memoizedFormatOrganizationsName = useMemo(
    () => formatOrganizationsName(ownersMap, topParentOrganizationId, [], [], orgFilter),
    [ownersMap, orgFilter, topParentOrganizationId]
  );

  const [formattedOrganizations, fullFormattedOrganizations] = memoizedFormatOrganizationsName;

  function filterAndLimitApps() {
    const filtered = fuzzyFilter(applications, appFilter, 'name');
    return take(MAX_APPS, filtered);
  }

  return (
    <div id={id}>
      <NxCollapsibleMultiSelect
        name="organizations"
        options={formattedOrganizations}
        onChange={onSelectedOrganizationsChange}
        selectedIds={selectedOrganizations}
        isOpen={isOrgsOpen}
        onToggleCollapse={onOrgsToggleCollapse}
        filterPlaceholder="Organization Name"
        disabledTooltip="There are no organizations to filter"
        filter={orgFilter}
        onFilterChange={setOrgFilter}
        filteredOptions={fuzzyFilter(formattedOrganizations, orgFilter, 'name')}
      >
        <NxFontAwesomeIcon icon={faSitemap} />
        <span>Organizations</span>
      </NxCollapsibleMultiSelect>
      <NxCollapsibleMultiSelect
        id="application-filter"
        name="applications"
        options={applications}
        onChange={onSelectedApplicationsChange}
        selectedIds={selectedApplications}
        filterPlaceholder="Application Name"
        disabledTooltip="There are no applications to filter"
        isOpen={isAppsOpen}
        onToggleCollapse={onAppsToggleCollapse}
        filter={appFilter}
        onFilterChange={setAppFilter}
        filteredOptions={filterAndLimitApps()}
      >
        <NxFontAwesomeIcon icon={faTerminal} />
        <span>Applications</span>
        {applications.length > MAX_APPS && (
          <NxTooltip title={MAX_APPS_TOOLTIP_MESSAGE}>
            <NxFontAwesomeIcon
              icon={faExclamationTriangle}
              className="iq-limited-apps-warning-icon"
              data-testid="iq-limited-apps-warning-icon"
            />
          </NxTooltip>
        )}
      </NxCollapsibleMultiSelect>
    </div>
  );
}

IqOrgAppPicker.propTypes = {
  applications: PropTypes.array,
  selectedOrganizations: PropTypes.instanceOf(Set).isRequired,
  selectedApplications: PropTypes.instanceOf(Set).isRequired,
  onChange: PropTypes.func.isRequired,
  id: PropTypes.string,
  ownersMap: PropTypes.object,
  topParentOrganizationId: PropTypes.string,
};
