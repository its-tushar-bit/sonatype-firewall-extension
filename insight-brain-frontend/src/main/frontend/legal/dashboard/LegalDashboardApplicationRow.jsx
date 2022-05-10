/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import { join } from 'ramda';
import * as PropTypes from 'prop-types';
import { applicationPropType } from '../advancedLegalPropTypes';
import { terseAgo } from '../../utilAngular/CommonServices';
import LegalBinaryDonutChart from '../shared/LegalBinaryDonutChart';

export default function LegalDashboardApplicationRow({ row, stateGo }) {
  const percentage =
    row.componentsTotalCount > 0 ? Math.min(100, (row.componentsReviewedCount * 100) / row.componentsTotalCount) : 0;

  const scanTimeDisplay = (row.lastScanTime ? terseAgo(row.lastScanTime) + ' - ' : '') + row.stageTypeName;

  function goToApplicationDetailsPage() {
    stateGo('legal.applicationDetails', {
      applicationPublicId: row.applicationPublicId,
      stageTypeId: row.stageTypeId,
    });
  }

  return (
    <NxTableRow key={`${row.applicationId}-${row.stageTypeId}`} isClickable onClick={goToApplicationDetailsPage}>
      <NxTableCell className="legal-dashboard-applications-application-name nx-truncate-ellipsis">
        {row.applicationName}
      </NxTableCell>
      <NxTableCell className="legal-dashboard-applications-last-scan">{scanTimeDisplay}</NxTableCell>
      <NxTableCell className="legal-dashboard-applications-category nx-truncate-ellipsis">
        {join(', ', row.applicationTagNames)}
      </NxTableCell>
      <NxTableCell className="legal-dashboard-applications-review-progress">
        <LegalBinaryDonutChart percent={percentage} />
        <span>
          {row.componentsReviewedCount} / {row.componentsTotalCount}
        </span>
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

LegalDashboardApplicationRow.propTypes = {
  row: applicationPropType,
  stateGo: PropTypes.func.isRequired,
};
