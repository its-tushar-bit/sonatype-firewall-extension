/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';

import OverviewComponentInformationTile from './componentInformationTile/OverviewComponentInformationTile';
import { RiskRemediationContainer } from './riskRemediation/RiskRemediationContainer';
import SimilarMatchesPopoverContainer from './SimilarMatchesPopoover/SimilarMatchesPopoverContainer';
import DependencyTreeTile from './DependencyTreeTile/DependencyTreeTile';
export default function Overview(props) {
  const { ...componentInformationTileProps } = props;

  const matchState = props?.componentInformation?.matchState;
  const isUnknown = !matchState || matchState === 'unknown';

  return (
    <Fragment>
      <OverviewComponentInformationTile {...componentInformationTileProps} />
      {!isUnknown && (
        <Fragment>
          <RiskRemediationContainer />
          <SimilarMatchesPopoverContainer />
          <DependencyTreeTile />
        </Fragment>
      )}
    </Fragment>
  );
}

Overview.propTypes = {
  ...OverviewComponentInformationTile.propTypes,
};
