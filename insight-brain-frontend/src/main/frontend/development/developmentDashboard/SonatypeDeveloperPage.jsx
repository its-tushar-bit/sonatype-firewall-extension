/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH1, NxLoadWrapper, NxPageMain } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import Overview from './sections/overview/Overview';
import {
  selectIsDeveloperDashboardEnabled,
  selectProductFeaturesSlice,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import TryAiDeveloperBanner from 'MainRoot/development/developmentDashboard/TryAiDeveloperBanner';

export default function SonatypeDeveloperPage() {
  const dispatch = useDispatch();

  const { loading, loadError } = useSelector(selectProductFeaturesSlice);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);

  const doLoad = () => dispatch(actions.fetchProductFeaturesIfNeeded());

  return (
    <NxPageMain>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxH1>Dashboard</NxH1>
        <TryAiDeveloperBanner />
        {isDeveloperDashboardEnabled ? <Overview /> : <LicenseLockScreen />}
      </NxLoadWrapper>
    </NxPageMain>
  );
}
