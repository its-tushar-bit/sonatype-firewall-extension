/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTableCell, NxTableRow, NxThreatIndicator } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import LegalBinaryDonutChart from '../shared/LegalBinaryDonutChart';
import { isNilOrEmpty } from '../../util/jsUtil';
import { flatten, map, pipe, prop } from 'ramda';

export default function LegalDashboardComponentRow({ row, stateGo }) {
  const { licenses } = row;
  const threatGroupLevels = isNilOrEmpty(licenses)
    ? []
    : pipe(map(prop('licenseThreatGroups')), flatten, map(prop('licenseThreatGroupLevel')))(licenses);
  const threatGroupLevel = isNilOrEmpty(threatGroupLevels) ? 0 : Math.max(...threatGroupLevels) || 0;
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
        <NxThreatIndicator policyThreatLevel={threatGroupLevel} />
        <span>{row.licenseNames.join(', ')}</span>
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
