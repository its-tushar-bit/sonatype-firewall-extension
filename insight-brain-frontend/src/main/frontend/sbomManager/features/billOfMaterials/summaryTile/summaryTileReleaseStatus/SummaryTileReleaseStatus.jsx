/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import * as R from 'ramda';
import { NxH3, NxP, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faCircleExclamation, faCircleCheck } from '@fortawesome/pro-solid-svg-icons';

import PieChart, { NIVO_PERCENTAGE_COLOR_MAP } from '../summaryTilePieChart/summaryTilePieChart';

import './SummaryTileReleaseStatus.scss';

const SummaryTileReleaseStatus = ({ percentage: percentageProp }) => {
  const percentage = R.when(R.isNil, R.always(0))(percentageProp);

  const status = R.cond([
    [R.equals(100), R.always('Release Ready')],
    [R.equals(0), R.always('Needs Attention')],
    [R.T, R.always('Partially Annotated')],
  ])(percentage);

  return (
    <section className="sbom-manager-summary-tile-release-status">
      <header className="sbom-manager-summary-tile-release-status__header">
        <NxH3>Release Status</NxH3>
        <span className="sbom-manager-summary-tile-release-status__status">
          <NxFontAwesomeIcon icon={status === 'Release Ready' ? faCircleCheck : faCircleExclamation} />
          <span data-testid="summary-tile-release-status">{status}</span>
        </span>
      </header>
      <div className="sbom-manager-summary-tile-release-status__content">
        <PieChart total={`${percentage}%`} data={{ percentage }} colorMap={NIVO_PERCENTAGE_COLOR_MAP} />
        <NxP
          className="sbom-manager-summary-tile-release-status__description"
          data-testid="summary-tile-release-status-description"
        >
          {percentage}% of critical and high vulnerabilities have been annotated with exploitability information
        </NxP>
      </div>
    </section>
  );
};

SummaryTileReleaseStatus.propTypes = {
  percentage: PropTypes.number,
};

export default SummaryTileReleaseStatus;
