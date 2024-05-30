/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  NxButton,
  NxH1,
  NxP,
  NxPageTitle,
  NxStatefulForm,
  NxTextLink,
  NxToggle,
} from '@sonatype/react-shared-components';
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { displayName } from '../scmOnboarding/utils/providers';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

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

  const linkToDocs = (
    <NxTextLink href="https://links.sonatype.com/products/nxiq/doc/scm" external>
      See Nexus IQ for SCM
    </NxTextLink>
  );

  return (
    <main className="nx-page-main" id="automatic-source-control-configuration-container">
      <NxPageTitle>
        <NxH1>Automatic Source Control</NxH1>
        <NxPageTitle.Description>
          <NxP>
            All your scanned applications that contain git repository, will be automatically integrated with the SCM
            system in IQ Server/Lifecycle. {linkToDocs}.
          </NxP>
        </NxPageTitle.Description>
      </NxPageTitle>
      <section className="nx-tile" id="automatic-source-control-configuration">
        <NxStatefulForm
          onSubmit={update}
          submitBtnText="Update"
          loading={loading}
          loadError={loadError}
          submitError={updateError}
          submitMaskState={submitMaskState}
          doLoad={load}
          submitMaskMessage="Saving…"
          validationErrors={isDirty ? null : MSG_NO_CHANGES_TO_UPDATE}
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
                <NxTextLink href={$state.href('automaticApplicationsConfiguration')}>Automatic Applications</NxTextLink>
                , the applications will all be imported into {parentOrganization.name} Organization which uses{' '}
                {displayName(scmProvider)}. If you wish to use a different provider, you will need to manually create
                the application first. You may wish to use the{' '}
                <NxTextLink href={$state.href('scmOnboarding')}>Easy SCM Onboarding tool</NxTextLink> to create the IQ
                Applications.
              </p>
            )}
          </div>
        </NxStatefulForm>
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
