/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {Fragment, useEffect} from 'react';

import MaximizedContainer from '../../react/MaximizedContainer';
import * as PropTypes from 'prop-types';
import {NxErrorAlert} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';

export default function ScmOnboarding(props) {
  const {
        load
      } = props,
      {
        isManifestScanFeatureEnabled,
        isAuthorized,
        loading
      } = props;

  useEffect(() => { load(); }, []);

  return (
    <LoadWrapper loading={loading}>
      <MaximizedContainer id="scm-onboarding-container" className="nx-page-content">
        <div className="nx-page-main" id="scm-onboarding-root">
          {isAuthorized && isManifestScanFeatureEnabled &&
          <Fragment>
            <h1>Import Applications from SCM</h1>
          </Fragment>
          }
          {!isAuthorized &&
            <NxErrorAlert id="scm-onboarding-insufficient-permissions-error">
              <strong>Error</strong> It appears you do not have permission to access this page.
              If you believe this to be incorrect please contact your administrator.
            </NxErrorAlert>
          }
          {!isManifestScanFeatureEnabled &&
          <NxErrorAlert id="scm-onboarding-feature-flag-disabled-error">
            <strong>Error</strong> This feature has not been enabled.
            If you believe this to be incorrect please contact your administrator.
          </NxErrorAlert>
          }
        </div>
      </MaximizedContainer>
    </LoadWrapper>
  );
}

ScmOnboarding.propTypes = {
  isManifestScanFeatureEnabled: PropTypes.bool.isRequired,
  load: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  isAuthorized: PropTypes.bool.isRequired
};
