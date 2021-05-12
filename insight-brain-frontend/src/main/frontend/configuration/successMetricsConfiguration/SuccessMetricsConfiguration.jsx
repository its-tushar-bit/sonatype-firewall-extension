/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxForm, NxToggle, NxButton } from '@sonatype/react-shared-components';
import { Messages } from '../../util/CommonServices';

export const authErrorMessage = `It appears you do not have permission to access this page.
  If you believe this to be incorrect please contact your administrator.`;

export default function SuccessMetricsConfiguration(props) {
  const { load, update, toggleIsEnabled, resetForm } = props;
  const { isAuthorized, loading, isDirty, loadError: loadErrorProp, updateError, submitMaskState } = props;
  const { enabled } = props;

  const loadError = isAuthorized ? loadErrorProp : authErrorMessage;

  useEffect(() => {
    load();
  }, []);

  return (
    <main id="success-metrics-configuration-container" className="nx-page-main">
      <section id="success-metrics-configuration" className="nx-tile">
        <NxForm
          onSubmit={update}
          loadError={Messages.getHttpErrorMessage(loadError)}
          loading={loading}
          doLoad={load}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          submitError={Messages.getHttpErrorMessage(updateError)}
          submitBtnText="Update"
          validationErrors={isDirty ? null : 'There are no changes to update'}
          additionalFooterBtns={
            <NxButton type="button" id="success-metrics-cancel" onClick={resetForm} disabled={!isDirty}>
              Cancel
            </NxButton>
          }
        >
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Success Metrics</h2>
            </div>
          </header>
          <div className="nx-tile-content">
            <NxToggle
              id="success-metrics-toggle"
              className="nx-toggle--no-gap"
              onChange={toggleIsEnabled}
              isChecked={enabled}
            >
              Enable Success Metrics
            </NxToggle>
          </div>
        </NxForm>
      </section>
    </main>
  );
}
SuccessMetricsConfiguration.propTypes = {
  isAuthorized: PropTypes.bool.isRequired,
  load: PropTypes.func.isRequired,
  update: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  enabled: PropTypes.bool.isRequired,
  toggleIsEnabled: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.object,
  updateError: PropTypes.object,
  submitMaskState: PropTypes.bool,
};
