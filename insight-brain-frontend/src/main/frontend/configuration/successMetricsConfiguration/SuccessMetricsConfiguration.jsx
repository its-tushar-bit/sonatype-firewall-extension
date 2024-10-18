/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxStatefulForm,
  NxToggle,
  NxButton,
  NxStatefulWarningAlert,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';
import { useSelector } from 'react-redux';
import { selectIsOrgsAndAppsEnabled, selectLoadingFeatures } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function SuccessMetricsConfiguration(props) {
  const { load, update, toggleIsEnabled, resetForm } = props;
  const { loading, isDirty, loadError, updateError, submitMaskState } = props;
  const { enabled } = props;

  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const shouldShowLicenseAlert = !isProductFeaturesLoading && !isOrgsAndAppsEnabled;
  const formLoading = loading || isProductFeaturesLoading;

  useEffect(() => {
    if (!shouldShowLicenseAlert && !isProductFeaturesLoading) {
      load();
    }
  }, [shouldShowLicenseAlert, isProductFeaturesLoading]);

  return (
    <main id="success-metrics-configuration-container" className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">Success Metrics</h1>
      </div>
      {shouldShowLicenseAlert ? (
        <NxWarningAlert role="alert" type="error">
          This feature is not supported by your product license.
        </NxWarningAlert>
      ) : (
        <section id="success-metrics-configuration" className="nx-tile">
          <NxStatefulForm
            onSubmit={update}
            loadError={loadError}
            loading={formLoading}
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
      )}
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
