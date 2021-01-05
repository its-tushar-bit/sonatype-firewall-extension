/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxBinaryDonutChart, NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import { join } from 'ramda';
import moment from 'moment';
import { applicationPropType } from '../advancedLegalPropTypes';

export default function LegalDashboardApplicationRow({ row }) {
  const percentage =
      row.reviewTotalCount > 0 ? Math.min(100, row.reviewCompletedCount * 100 / row.reviewTotalCount) : 0;

  return (
    <NxTableRow key={ row.applicationId }>
      <NxTableCell className="legal-dashboard-applications-application-name nx-truncate-ellipsis">
        { row.applicationName }
      </NxTableCell>
      <NxTableCell className="legal-dashboard-applications-last-scan">
        { row.lastScanTime ? moment(row.lastScanTime).fromNow() : ''}
      </NxTableCell>
      <NxTableCell className="legal-dashboard-applications-category nx-truncate-ellipsis">
        { join(', ', row.applicationTagNames) }
      </NxTableCell>
      <NxTableCell className="legal-dashboard-applications-review-progress">
        <NxBinaryDonutChart percent = { percentage } />
        <span>{ row.reviewCompletedCount } / { row.reviewTotalCount }</span>
      </NxTableCell>
    </NxTableRow>
  );
}

LegalDashboardApplicationRow.propTypes = {
  row: applicationPropType
};
