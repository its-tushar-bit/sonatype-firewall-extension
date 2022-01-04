/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTableCell, NxTableRow, NxThreatIndicator } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { flatten, join, map, pipe, prop } from 'ramda';
import { isNilOrEmpty } from '../../util/jsUtil';
import { reviewStatusDisplayNames } from '../dashboard/legalDashboardConstants';
import LegalBinaryDonutChart from '../shared/LegalBinaryDonutChart';

export default function LegalApplicationDetailsComponentRow({ applicationPublicId, stageTypeId, row, stateGo }) {
  const { displayName, hash, licenses, reviewCompletedCount, reviewStatus, reviewTotalCount } = row;

  const threatGroupLevels = isNilOrEmpty(licenses)
    ? []
    : pipe(map(prop('licenseThreatGroups')), flatten, map(prop('licenseThreatGroupLevel')))(licenses);
  const threatGroupLevel = isNilOrEmpty(threatGroupLevels) ? 0 : Math.max(...threatGroupLevels) || 0;
  const percentage = reviewTotalCount > 0 ? Math.min(100, (reviewCompletedCount * 100) / reviewTotalCount) : 0;
  const reviewProgressRatio = reviewTotalCount === 0 ? '- / -' : `${reviewCompletedCount} / ${reviewTotalCount}`;

  function goToComponentPage() {
    stateGo('legal.applicationStageTypeComponentOverview', {
      applicationPublicId: applicationPublicId,
      stageTypeId: stageTypeId,
      hash: hash,
    });
  }

  return (
    <NxTableRow key={hash} isClickable onClick={goToComponentPage}>
      <NxTableCell className="legal-application-details-component-name nx-truncate-ellipsis">{displayName}</NxTableCell>
      <NxTableCell className="legal-application-details-licenses">
        {!isNilOrEmpty(licenses) && (
          <div className="nx-truncate-ellipsis">
            <NxThreatIndicator policyThreatLevel={threatGroupLevel} />
            {pipe(map(prop('licenseName')), join(', '))(licenses)}
          </div>
        )}
      </NxTableCell>
      <NxTableCell className="legal-application-details-review-progress">
        {!isNilOrEmpty(licenses) && (
          <div className="legal-application-details-review-progress-container">
            <LegalBinaryDonutChart className="legal-application-details-review-progress-chart" percent={percentage} />
            <span className="legal-application-details-review-progress-ratio">{reviewProgressRatio}</span>
          </div>
        )}
      </NxTableCell>
      <NxTableCell className={`legal-application-details-review-status status-${reviewStatus}`}>
        {reviewStatusDisplayNames[reviewStatus]}
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

LegalApplicationDetailsComponentRow.propTypes = {
  applicationPublicId: PropTypes.string,
  stageTypeId: PropTypes.string,
  row: PropTypes.shape({
    displayName: PropTypes.string.isRequired,
    hash: PropTypes.string.isRequired,
    licenses: PropTypes.arrayOf(
      PropTypes.shape({
        licenseId: PropTypes.string,
        licenseName: PropTypes.string,
        licenseThreatGroups: PropTypes.arrayOf(
          PropTypes.shape({
            licenseThreatGroupCategory: PropTypes.string,
            licenseThreatGroupLevel: PropTypes.number,
            licenseThreatGroupName: PropTypes.string,
          })
        ),
      })
    ),
    reviewCompletedCount: PropTypes.number.isRequired,
    reviewStatus: PropTypes.string.isRequired,
    reviewTotalCount: PropTypes.number.isRequired,
  }).isRequired,
  stateGo: PropTypes.func.isRequired,
};
