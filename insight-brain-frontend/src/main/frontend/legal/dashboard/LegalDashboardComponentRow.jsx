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
import { flatten, map, pipe, prop, join } from 'ramda';

export default function LegalDashboardComponentRow({ row, stateGo }) {
  const { applicationOccurrences, displayName, hash, licenses, reviewCompletedCount, reviewTotalCount } = row;
  const threatGroupLevels = isNilOrEmpty(licenses)
    ? []
    : pipe(map(prop('licenseThreatGroups')), flatten, map(prop('licenseThreatGroupLevel')))(licenses);
  const threatGroupLevel = isNilOrEmpty(threatGroupLevels) ? 0 : Math.max(...threatGroupLevels) || 0;

  const percentage = reviewTotalCount ? Math.min(100, (reviewCompletedCount * 100) / reviewTotalCount) : 0;
  const reviewProgressRatio = reviewTotalCount === 0 ? '- / -' : `${reviewCompletedCount} / ${reviewTotalCount}`;

  function goToComponentPage() {
    stateGo('legal.componentOverview', {
      hash: row.hash,
    });
  }

  return (
    <NxTableRow key={hash} isClickable onClick={goToComponentPage}>
      <NxTableCell className="legal-dashboard-components-component-name nx-truncate-ellipsis">
        {displayName}
      </NxTableCell>
      <NxTableCell className="legal-dashboard-components-licenses nx-truncate-ellipsis">
        <NxThreatIndicator policyThreatLevel={threatGroupLevel} />
        <span>{pipe(map(prop('licenseName')), join(', '))(licenses)}</span>
      </NxTableCell>
      <NxTableCell className="legal-dashboard-components-occurrences isNumeric">{applicationOccurrences}</NxTableCell>
      <NxTableCell className="legal-dashboard-components-review-progress">
        <LegalBinaryDonutChart percent={percentage} />
        <span>{reviewProgressRatio}</span>
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

LegalDashboardComponentRow.propTypes = {
  row: PropTypes.any,
  stateGo: PropTypes.func.isRequired,
};
