/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxToggle, NxButton } from '@sonatype/react-shared-components';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

export default function SuccessMetricsConfiguration(props) {
  const { load, update, toggleIsEnabled, resetForm } = props;
  const { loading, isDirty, loadError, updateError, submitMaskState } = props;
  const { enabled } = props;

  useEffect(() => {
    load();
  }, []);

  return (
    <main id="success-metrics-configuration-container" className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">Success Metrics</h1>
      </div>
      <section id="success-metrics-configuration" className="nx-tile">
        <NxStatefulForm
          onSubmit={update}
          loadError={loadError}
          loading={loading}
          doLoad={load}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          submitError={updateError}
          submitBtnText="Update"
          validationErrors={isDirty ? null : MSG_NO_CHANGES_TO_UPDATE}
          additionalFooterBtns={
            <NxButton type="button" id="success-metrics-cancel" onClick={resetForm} disabled={!isDirty}>
              Cancel
            </NxButton>
          }
        >
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Configure Success Metrics</h2>
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
        </NxStatefulForm>
      </section>
    </main>
  );
}

SuccessMetricsConfiguration.propTypes = {
  load: PropTypes.func.isRequired,
  update: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  enabled: PropTypes.bool.isRequired,
  toggleIsEnabled: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  updateError: PropTypes.string,
  submitMaskState: PropTypes.bool,
};
