/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { organizationPropType } from '../scmPropTypes';
import DropdownFilterInput from './DropdownFilterInput';

export default function TargetOrganizationDropdown(props) {
  const {
    // input: organizations
    organizations,
    loadingOrganizations,

    // output: selected organization
    selectedOrganization,

    // angular state
    $state,
  } = props;

  const [isOpen, setOpen] = useState(false),
    onToggleCollapse = () => {
      setOpen(!isOpen);
    };

  function getOptionClassNames(isSelected) {
    return classnames('nx-dropdown-button', 'iq-scm-onboarding-dropdown__option', {
      'iq-scm-onboarding-dropdown__option--selected': isSelected,
    });
  }

  const filterFn = (orgAnchor, filterValue) => {
    if (!filterValue) {
      return true;
    }
    if (!orgAnchor || !orgAnchor.props || !orgAnchor.props.children) {
      return false;
    }
    return orgAnchor.props.children.toLowerCase().includes(filterValue.toLowerCase());
  };

  return (
    <DropdownFilterInput
      filterFn={filterFn}
      id="iq-scm-target-organization"
      label={
        loadingOrganizations ? 'Loading...' : selectedOrganization ? selectedOrganization.organization.name : 'Select'
      }
      disabled={loadingOrganizations}
      isOpen={isOpen}
      onToggleCollapse={onToggleCollapse}
      variant="secondary"
      className="nx-dropdown--navigation"
    >
      {organizations
        .filter((org) => org.id !== 'ROOT_ORGANIZATION_ID')
        .map((org) => (
          <a
            key={org.organization.id}
            href={$state.href('scmOnboardingOrg', {
              organizationId: org.organization.id,
            })}
            className={getOptionClassNames(
              selectedOrganization && selectedOrganization.organization.id === org.organization.id
            )}
          >
            {org.organization.name}
          </a>
        ))}
    </DropdownFilterInput>
  );
}

TargetOrganizationDropdown.propTypes = {
  organizations: PropTypes.arrayOf(PropTypes.shape(organizationPropType)),
  loadingOrganizations: PropTypes.bool,
  selectedOrganization: PropTypes.shape(organizationPropType),
  $state: PropTypes.object.isRequired,
};
