/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import VulnerabilitiesTableTile from './VulnerabilitiesTableTile';
import { actions } from './vulnerabilitiesSlice';
import { selectVulnerabilitiesSortedSlice } from './vulnerabilitiesSelectors';

function mapStateToProps(state) {
  return {
    vulnerabilities: selectVulnerabilitiesSortedSlice(state),
  };
}

const mapDispatchToProps = {
  loadVulnerabilities: actions.loadVulnerabilities,
  setVulnerabilityIdAndToggleVisibility: actions.setVulnerabilityIdAndToggleVisibility,
};

export const VulnerabilitiesTableTileContainer = connect(mapStateToProps, mapDispatchToProps)(VulnerabilitiesTableTile);
