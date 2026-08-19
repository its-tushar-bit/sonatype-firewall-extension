/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import VulnerabilitiesTableTile from './VulnerabilitiesTableTile';
import { actions } from './vulnerabilitiesSlice';
import { selectVulnerabilitiesSortedSlice } from './vulnerabilitiesSelectors';
import { selectComponentDetailsLoadErrors, selectComponentDetailsLoading } from '../componentDetailsSelectors';
import { actions as componentDetailsActions } from '../componentDetailsSlice';

function mapStateToProps(state) {
  return {
    vulnerabilities: selectVulnerabilitiesSortedSlice(state),
    isLoadingComponentDetails: selectComponentDetailsLoading(state),
    componentDetailsLoadError: selectComponentDetailsLoadErrors(state),
  };
}

const mapDispatchToProps = {
  loadComponentDetails: componentDetailsActions.loadComponentDetails,
  loadVulnerabilities: actions.loadVulnerabilities,
  toggleVulnerabilityPopoverWithEffects: actions.toggleVulnerabilityPopoverWithEffects,
};

export const VulnerabilitiesTableTileContainer = connect(mapStateToProps, mapDispatchToProps)(VulnerabilitiesTableTile);
