/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {NxDropdown} from '@sonatype/react-shared-components';
import React, {useState} from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import {organizationPropType} from '../ScmOnboarding';

export default function TargetOrganizationDropdown(props) {
  const {
    // input: organizations
    organizations,
    loadingOrganizations,

    // output: selected organization
    selectedOrganization,

    // action
    setSelectedOrganization
  } = props;

  const [isOpen, setOpen] = useState(false),
      onToggleCollapse = () => { setOpen(!isOpen); },
      onClick = (event) => {
        setSelectedOrganization(event);
        setOpen(false);
      };

  function getOptionClassNames(isSelected) {
    return classnames('nx-dropdown-button', 'iq-scm-onboarding-dropdown__option', {
      'iq-scm-onboarding-dropdown__option--selected': isSelected
    });
  }

  return (
    <NxDropdown
      id='iq-scm-target-organization'
      label={ loadingOrganizations
        ? 'Loading...'
        : selectedOrganization ? selectedOrganization.name : 'Select' }
      disabled={ loadingOrganizations }
      isOpen={isOpen}
      onToggleCollapse={onToggleCollapse}
      variant="secondary"
      className="nx-dropdown--navigation">
      { organizations
          .filter(org => org.id !== 'ROOT_ORGANIZATION_ID')
          .map(org =>
            <button key={org.id}
                    onClick={() => {onClick(org);}}
                    className={getOptionClassNames(selectedOrganization && selectedOrganization.id === org.id)}>
              {org.name}
            </button>
          )}
    </NxDropdown>
  );
}

TargetOrganizationDropdown.propTypes = {
  organizations: PropTypes.arrayOf(organizationPropType).isRequired,
  loadingOrganizations: PropTypes.bool.isRequired,
  setSelectedOrganization: PropTypes.func.isRequired,
  selectedOrganization: organizationPropType
};
