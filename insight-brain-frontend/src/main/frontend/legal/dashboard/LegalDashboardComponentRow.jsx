/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import LegalBinaryDonutChart from '../shared/LegalBinaryDonutChart';

export default function LegalDashboardComponentRow({ row, stateGo }) {
  function goToComponentPage() {
    stateGo('legal.componentOverview', {
      hash: row.hash,
    });
  }

  return (
    <NxTableRow key={row.hash} isClickable onClick={goToComponentPage}>
      <NxTableCell className="legal-dashboard-components-component-name nx-truncate-ellipsis">
        {row.displayName}
      </NxTableCell>
      <NxTableCell className="legal-dashboard-components-licenses nx-truncate-ellipsis">
        {row.licenseNames.join(', ')}
      </NxTableCell>
      <NxTableCell className="legal-dashboard-components-occurrences isNumeric">
        {row.applicationOccurrences}
      </NxTableCell>
      <NxTableCell className="legal-dashboard-components-review-progress">
        <LegalBinaryDonutChart
          percent={row.reviewTotalCount ? Math.min(100, (row.reviewCompletedCount * 100) / row.reviewTotalCount) : 0}
        />
        <span>
          {row.reviewCompletedCount} / {row.reviewTotalCount}
        </span>
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

LegalDashboardComponentRow.propTypes = {
  row: PropTypes.any,
  stateGo: PropTypes.func.isRequired,
};
