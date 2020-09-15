/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import TargetOrganizationDropdown from './TargetOrganizationDropdown';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import React, {Fragment} from 'react';
import * as PropTypes from 'prop-types';
import {NxTextInput} from '@sonatype/react-shared-components';
import {pick} from 'ramda';
import {organizationPropType} from '../ScmOnboarding';

export default function ImportApplicationsForm(props) {
  const {
    //config
    defaultHostUrlState,

    // organizations
    setSelectedOrganization,
    organizations,
    loadingOrganizations,
    selectedOrganization,

    // repositories
    loadRepositories,
    loadingRepositories
  } = props;

  function onSubmitMainForm() {
    loadRepositories();
  }

  return (
    <Fragment>
      <div className="iq-tile-header">
        <div className="iq-tile-header__title">
          <h2>Import Applications from SCM</h2>
        </div>
      </div>
      <form className='nx-form'>
        <fieldset className="nx-fieldset">
          <legend className="nx-label">Target Organization</legend>
          <div>IQ Server will use the SCM configuration associated with the target organization.</div>
          <div className="nx-form-group">
            <TargetOrganizationDropdown
                organizations={organizations}
                loadingOrganizations={loadingOrganizations}
                selectedOrganization={selectedOrganization}
                setSelectedOrganization={setSelectedOrganization}/>
          </div>
        </fieldset>
        <fieldset className="nx-fieldset">
          <legend className="nx-label">SCM Base URL</legend>
          <div>The SCM Base URL for importing repositories.</div>
          <NxTextInput {...defaultHostUrlState}/>
        </fieldset>
        <div className="nx-tile-footer">
          <div className="nx-btn-bar">
            <NxButton
              id="iq-scm-load-button"
              variant="primary"
              disabled={loadingRepositories}
              onClick={() => onSubmitMainForm()}>
              Load Repositories
            </NxButton>
          </div>
        </div>
      </form>
    </Fragment>
  );
}

export const textInputPropType = PropTypes.shape(pick(['value', 'isPristine', 'validationErrors'],
    NxTextInput.propTypes));

ImportApplicationsForm.propTypes = {
  // config
  defaultHostUrlState: textInputPropType,

  // organizations
  setSelectedOrganization: PropTypes.func.isRequired,
  organizations: PropTypes.arrayOf(organizationPropType).isRequired,
  loadingOrganizations: PropTypes.bool.isRequired,
  selectedOrganization: organizationPropType,

  //repositories
  loadRepositories: PropTypes.func.isRequired,
  loadingRepositories: PropTypes.bool.isRequired
};
