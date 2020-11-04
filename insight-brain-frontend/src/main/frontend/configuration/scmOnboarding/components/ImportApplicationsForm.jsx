/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import TargetOrganizationDropdown from './TargetOrganizationDropdown';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import React from 'react';
import * as PropTypes from 'prop-types';
import {NxTextInput} from '@sonatype/react-shared-components';
import {pick} from 'ramda';
import {organizationPropType} from '../ScmOnboarding';

export default function ImportApplicationsForm(props) {
  const {
    //config
    defaultHostUrlState,
    provider,

    // organizations
    setSelectedOrganization,
    organizations,
    loadingOrganizations,
    selectedOrganization,

    // repositories
    loadRepositories,
    loadingRepositories,

    // actions
    loadOrgHostUrl
  } = props;

  function onSubmitMainForm() {
    loadRepositories();
  }

  return (
    <form className="nx-form">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Import Applications from SCM</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <fieldset className="nx-fieldset">
          <legend className="nx-label">Target Organization</legend>
          <div>IQ Server will use the SCM configuration associated with the target organization.</div>
          <div className="nx-form-group">
            <TargetOrganizationDropdown
                organizations={organizations}
                loadingOrganizations={loadingOrganizations}
                provider={provider}
                selectedOrganization={selectedOrganization}
                setSelectedOrganization={setSelectedOrganization}
                loadOrgHostUrl={loadOrgHostUrl}
            />
          </div>
        </fieldset>
        <fieldset className="nx-fieldset">
          <legend className="nx-label">SCM Base URL</legend>
          <div>The SCM Base URL for importing repositories.</div>
          <NxTextInput {...defaultHostUrlState}/>
        </fieldset>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton
            id="iq-scm-load-button"
            variant="primary"
            disabled={loadingRepositories}
            onClick={() => onSubmitMainForm()}>
            Load Repositories
          </NxButton>
        </div>
      </footer>
    </form>
  );
}

export const textInputPropType = PropTypes.shape(pick(['value', 'isPristine', 'validationErrors'],
    NxTextInput.propTypes));

ImportApplicationsForm.propTypes = {
  // config
  defaultHostUrlState: textInputPropType,
  provider: PropTypes.string.isRequired,

  // organizations
  setSelectedOrganization: PropTypes.func.isRequired,
  organizations: PropTypes.arrayOf(PropTypes.shape(organizationPropType)).isRequired,
  loadingOrganizations: PropTypes.bool.isRequired,
  selectedOrganization: PropTypes.shape(organizationPropType),
  loadOrgHostUrl: PropTypes.func.isRequired,

  //repositories
  loadRepositories: PropTypes.func.isRequired,
  loadingRepositories: PropTypes.bool.isRequired
};
