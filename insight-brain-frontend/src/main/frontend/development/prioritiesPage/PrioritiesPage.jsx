/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxLoadingSpinner, NxLoadWrapper, NxPageMain, NxPageTitle, NxButton } from '@sonatype/react-shared-components';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';
import PrioritiesPageHeader from 'MainRoot/development/prioritiesPage/PrioritiesPageHeader';
import PrioritiesPageTable from 'MainRoot/development/prioritiesPage/PrioritiesPageTable';
import {
  selectLoadingFeatures,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/development/prioritiesPage/slices/prioritiesPageSlice';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';

export default function PrioritiesPage() {
  return (
    <NxPageMain className="iq-priorities-page">
      <PageContents />
    </NxPageMain>
  );
}

function PageContents() {
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const productFeaturesLoading = useSelector(selectLoadingFeatures);

  if (productFeaturesLoading) {
    return <NxLoadingSpinner />;
  } else if (isDeveloperDashboardEnabled) {
    return <PrioritiesPageContents />;
  } else {
    return <LicenseLockScreen />;
  }
}

function PrioritiesPageContents() {
  const dispatch = useDispatch();
  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const { loadingMetadata, loadErrorMetadata, metadata } = useSelector(selectPrioritiesPageSlice);

  const goToFullReport = () => dispatch(stateGo('prioritiesPageContainer.policy', { scanId, publicId: publicAppId }));

  const doLoad = () => {
    dispatch(actions.loadMetadata());
  };

  useEffect(() => {
    doLoad();

    return () => dispatch(actions.resetState());
  }, []);

  return (
    <NxLoadWrapper loading={loadingMetadata} error={loadErrorMetadata} retryHandler={doLoad}>
      {metadata && (
        <>
          <NxPageTitle>
            <PrioritiesPageHeader />
          </NxPageTitle>
          <PrioritiesPageTable />
          <div className="nx-btn-bar">
            <NxButton variant="primary" onClick={goToFullReport}>
              View Full Report
            </NxButton>
          </div>
        </>
      )}
    </NxLoadWrapper>
  );
}
