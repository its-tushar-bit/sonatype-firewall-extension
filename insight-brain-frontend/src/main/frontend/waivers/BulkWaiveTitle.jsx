/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { NxPageTitle, NxH1 } from '@sonatype/react-shared-components';
import { selectHasBulkWaivers } from 'MainRoot/productFeatures/productFeaturesSelectors';
import TierTag from 'MainRoot/react/shared/TierTag';
import { selectApplicationReportMetaData } from 'MainRoot/applicationReport/applicationReportSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectComponentName } from 'MainRoot/componentDetails/componentDetailsSelectors';
export default function BulkWaiveTitle() {
  const metadata = useSelector(selectApplicationReportMetaData);
  const componentName = useSelector(selectComponentName);
  const routerParams = useSelector(selectRouterCurrentParams);
  const hasBulkWaivers = useSelector(selectHasBulkWaivers);
  const isCdpBulkWaive = !!routerParams.hash;
  const subtitle = isCdpBulkWaive ? componentName : `${metadata?.application.name} ${metadata?.reportTitle}`;

  return (
    <NxPageTitle>
      <NxPageTitle.Headings>
        <NxH1>
          Bulk Waiver
          {!hasBulkWaivers && <TierTag>Enterprise Feature</TierTag>}
        </NxH1>
        <NxPageTitle.Subtitle data-testid="bulk-waiver-subtitle">{subtitle}</NxPageTitle.Subtitle>
      </NxPageTitle.Headings>
    </NxPageTitle>
  );
}
