/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { isEmpty } from 'ramda';
import { NxLoadingSpinner } from '@sonatype/react-shared-components';
import EnterpriseReportingLandingPage from 'MainRoot/enterpriseReporting/EnterpriseReportingLandingPage';
import OperationalReportingLandingPage from 'MainRoot/operationalReporting/OperationalReportingLandingPage';
import {
  selectIsIntegratedEnterpriseReportingSupported,
  selectLoadingFeatures,
  selectProductFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

export function ReportingRoute(): JSX.Element {
  const loading = useSelector(selectLoadingFeatures);
  const productFeatures = useSelector(selectProductFeatures);
  const isIntegratedEnterpriseReportingSupported = useSelector(selectIsIntegratedEnterpriseReportingSupported);

  // Gate on features being unknown, not the transient `loading` flag: the landing pages
  // dispatch fetchProductFeaturesIfNeeded on mount, which flips `loading` true→false even
  // when features are already cached, which would remount this route in an endless loop.
  if (loading && isEmpty(productFeatures)) {
    return (
      <main className="nx-page-main">
        <NxLoadingSpinner />
      </main>
    );
  }

  return isIntegratedEnterpriseReportingSupported ? (
    <EnterpriseReportingLandingPage />
  ) : (
    <OperationalReportingLandingPage />
  );
}
