/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxToggle, NxButton, NxWarningAlert } from '@sonatype/react-shared-components';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';
import { useDispatch, useSelector } from 'react-redux';
import { selectIsOrgsAndAppsEnabled, selectLoadingFeatures } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectSuccessMetricsConfigurationFormState,
  selectSuccessMetricsConfigurationViewState,
} from 'MainRoot/configuration/successMetricsConfiguration/successMetricsConfigurationSelectors';
import {
  load,
  update,
  toggleIsEnabled,
  resetForm,
} from 'MainRoot/configuration/successMetricsConfiguration/successMetricsConfigurationActions';

export default function SuccessMetricsConfiguration(props) {
  const dispatch = useDispatch();

  const { loading, isDirty, loadError, updateError, submitMaskState } = useSelector(
    selectSuccessMetricsConfigurationViewState
  );
  const { enabled } = useSelector(selectSuccessMetricsConfigurationFormState);
  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);

  const shouldShowLicenseAlert = !isProductFeaturesLoading && !isOrgsAndAppsEnabled;
  const formLoading = loading || isProductFeaturesLoading;

  const doUpdate = () => dispatch(update());
  const onToggleChanged = (value) => {
    dispatch(toggleIsEnabled(value));
  };
  const doResetForm = () => {
    dispatch(resetForm());
  };

  useEffect(() => {
    if (!shouldShowLicenseAlert && !isProductFeaturesLoading) {
      dispatch(load());
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
            onSubmit={doUpdate}
            loadError={loadError}
            loading={formLoading}
            doLoad={load}
            submitMaskMessage="Saving…"
            submitMaskState={submitMaskState}
            submitError={updateError}
            submitBtnText="Update"
            validationErrors={isDirty ? null : MSG_NO_CHANGES_TO_UPDATE}
            additionalFooterBtns={
              <NxButton type="button" id="success-metrics-cancel" onClick={doResetForm} disabled={!isDirty}>
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
                onChange={onToggleChanged}
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
