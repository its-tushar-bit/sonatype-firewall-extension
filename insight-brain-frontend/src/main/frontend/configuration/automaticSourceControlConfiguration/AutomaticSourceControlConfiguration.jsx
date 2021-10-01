/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxForm, NxButton, NxToggle } from '@sonatype/react-shared-components';
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { displayName } from '../scmOnboarding/utils/providers';

export default function AutomaticSourceControlConfiguration({
  load,
  enabled,
  toggleEnabled,
  isDirty,
  submitMaskState,
  update,
  resetForm,
  loading,
  loadError,
  updateError,
  parentOrganization,
  automaticApplicationsEnabled,
  scmProvider,
  $state,
}) {
  useEffect(() => {
    load();
  }, []);

  const cancelButton = (
    <NxButton type="button" id="automatic-source-control-cancel" disabled={!isDirty} onClick={resetForm}>
      Cancel
    </NxButton>
  );

  return (
    <main className="nx-page-main" id="automatic-source-control-configuration-container">
      <div className="nx-page-title">
        <h1 className="nx-h1">Automatic Source Control</h1>
      </div>
      <section className="nx-tile" id="automatic-source-control-configuration">
        <NxForm
          onSubmit={update}
          submitBtnText="Update"
          loading={loading}
          loadError={loadError}
          submitError={updateError}
          submitMaskState={submitMaskState}
          doLoad={load}
          submitMaskMessage="Saving…"
          validationErrors={isDirty ? null : 'There are no changes to update'}
          additionalFooterBtns={cancelButton}
        >
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Automatic Source Control Configuration</h2>
            </div>
          </header>
          <div className="nx-tile-content" id="automatic-source-control-explanation">
            <NxToggle
              id="automatic-source-control-toggle-checkbox"
              className="nx-toggle--no-gap"
              isChecked={enabled}
              onChange={toggleEnabled}
            >
              Enable Automatic Source Control Configuration
            </NxToggle>
            {automaticApplicationsEnabled && scmProvider && (
              <p className="nx-p" id="automatic-source-control-automatic-applications-explanation">
                Because you have selected{' '}
                <a href={$state.href('automaticApplicationsConfiguration')}>Automatic Applications</a>, the applications
                will all be imported into {parentOrganization.name} Organization which uses {displayName(scmProvider)}.
                If you wish to use a different provider, you will need to manually create the application first. You may
                wish to use the <a href={$state.href('scmOnboarding')}>Easy SCM Onboarding tool</a> to create the IQ
                Applications.
              </p>
            )}
          </div>
        </NxForm>
      </section>
    </main>
  );
}
AutomaticSourceControlConfiguration.propTypes = {
  load: PropTypes.func.isRequired,
  update: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  enabled: PropTypes.bool.isRequired,
  toggleEnabled: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  updateError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  parentOrganization: PropTypes.shape({
    id: PropTypes.string,
    name: PropTypes.string,
    nameLowercaseNoWhitespace: PropTypes.string,
  }),
  automaticApplicationsEnabled: PropTypes.bool,
  scmProvider: PropTypes.string,
  $state: PropTypes.object.isRequired,
};
