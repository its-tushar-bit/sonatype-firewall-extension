/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxPageTitle, NxH1 } from '@sonatype/react-shared-components';
import { selectRepositoryInformation } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSelectors';

export default function BulkWaiveTitle() {
  const repositoryInfo = useSelector(selectRepositoryInformation);
  const subtitle = repositoryInfo ? `${repositoryInfo.publicId} Repository Results` : '';

  return (
    <NxPageTitle>
      <NxPageTitle.Headings>
        <NxH1>Bulk Waiver</NxH1>
        <NxPageTitle.Subtitle data-testid="bulk-waiver-subtitle">{subtitle}</NxPageTitle.Subtitle>
      </NxPageTitle.Headings>
    </NxPageTitle>
  );
}
