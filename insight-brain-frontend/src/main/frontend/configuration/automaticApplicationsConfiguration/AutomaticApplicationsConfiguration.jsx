/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxStatefulForm,
  NxFormSelect,
  NxToggle,
  NxFormGroup,
  NxButton,
  NxErrorAlert,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { displayName } from '../scmOnboarding/utils/providers';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

const notValidParentOrganizationErrorMessage = 'Unable to update: fields with invalid or missing data.';

export default function AutomaticApplicationsConfiguration(props) {
  const { load, update, toggleAutomaticApplicationEnabled, setParentOrganization, resetForm } = props;
  const { loading, isDirty, loadError, updateError, submitMaskState, organizations } = props;
  const { $state, scmProvider } = props;
  const { enabled, parentOrganizationId } = props.formState;

  const handleParentOrganizationChange = (evt) => {
    setParentOrganization(evt.target.value);
  };

  useEffect(() => {
    load();
  }, []);

  const getValidation = () => {
    const validations = [
      { isInvalid: !isDirty, message: MSG_NO_CHANGES_TO_UPDATE },
      { isInvalid: !parentOrganizationId, message: notValidParentOrganizationErrorMessage },
    ];
    const firstError = validations.find((validation) => validation.isInvalid);
    return (firstError && firstError.message) || null;
  };

  return (
    <main className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">Automatic Applications</h1>
      </div>
      <section id="auto-app-config-configuration" className="nx-tile">
        <NxStatefulForm
          loading={loading}
          doLoad={load}
          loadError={loadError}
          onSubmit={update}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          submitError={updateError}
          submitBtnText="Update"
          validationErrors={getValidation()}
          additionalFooterBtns={
            <NxButton type="button" id="auto-app-config-cancel" onClick={resetForm} disabled={!isDirty}>
              Cancel
            </NxButton>
          }
        >
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Configure Automatic Applications</h2>
            </div>
          </header>
          <div className="nx-tile-content">
            <p id="auto-app-config-explanation" className="nx-p">
              Here you can configure automatic creation of applications. You can enable or disable automatic creation of
              applications using the toggle. You will also need to specify a parent organization for any
              automatically-created applications.
            </p>
            <p id="auto-app-config-source-control-explanation" className="nx-p">
              If you are using{' '}
              <NxTextLink href={$state.href('automaticSourceControlConfiguration')}>
                Automatic Source Control
              </NxTextLink>{' '}
              with Automatic Applications, the new applications will be imported into the below Parent Organization
              {scmProvider && (
                <>
                  {' '}
                  which is configured to use <b>{displayName(scmProvider)}</b>. If you try to automatically create non-
                  {displayName(scmProvider)} applications, the source control features will not work
                </>
              )}
              .
            </p>

            <p id="auto-app-config-scm-onboarding-explanation" className="nx-p">
              Instead, you can use <NxTextLink href={$state.href('scmOnboarding')}>Easy SCM Onboarding</NxTextLink> to
              explicitly import your source control applications into IQ.
            </p>
            <NxToggle
              id="auto-app-config-toggle-checkbox"
              className="nx-toggle--no-gap"
              onChange={toggleAutomaticApplicationEnabled}
              isChecked={enabled}
            >
              Automatic Application Creation
            </NxToggle>
            <NxFormGroup label="Parent Organization" isRequired>
              {organizations && organizations.length ? (
                <NxFormSelect
                  id="parent-organization-selector"
                  value={parentOrganizationId}
                  onChange={handleParentOrganizationChange}
                  disabled={!enabled}
                >
                  {!parentOrganizationId && <option value="">--Select Organization--</option>}
                  {organizations.map((organization) => (
                    <option key={organization.id} value={organization.id}>
                      {organization.name}
                    </option>
                  ))}
                </NxFormSelect>
              ) : (
                <NxErrorAlert>No parent organizations found</NxErrorAlert>
              )}
            </NxFormGroup>
          </div>
        </NxStatefulForm>
      </section>
    </main>
  );
}

AutomaticApplicationsConfiguration.propTypes = {
  load: PropTypes.func.isRequired,
  update: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  formState: PropTypes.shape({
    enabled: PropTypes.bool.isRequired,
    parentOrganizationId: PropTypes.string,
  }),
  organizations: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      name: PropTypes.string,
      nameLowercaseNoWhitespace: PropTypes.string,
    })
  ).isRequired,
  toggleAutomaticApplicationEnabled: PropTypes.func.isRequired,
  setParentOrganization: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  updateError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  scmProvider: PropTypes.string,
  $state: PropTypes.object.isRequired,
};
