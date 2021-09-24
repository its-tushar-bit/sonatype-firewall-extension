/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import { selectComponentAncestors } from '../../componentDetailsSelectors';
import { selectCurrentRouteName } from '../../../reduxUiRouter/routerSelectors';
import { actions } from '../overviewSlice';
import { selectVersionExplorerData, selectRemediationData } from '../overviewSelectors';
import { RiskRemediation } from './RiskRemediation';

function mapStateToProps(state) {
  const { currentVersion, stageId } = selectRemediationData(state);
  return {
    ancestors: selectComponentAncestors(state),
    routeName: selectCurrentRouteName(state),
    currentVersion,
    stageId,
    versionExplorerData: selectVersionExplorerData(state),
  };
}

const mapDispatchToProps = {
  loadVersionExplorerData: actions.loadVersionExplorerData,
};

export const RiskRemediationContainer = connect(mapStateToProps, mapDispatchToProps)(RiskRemediation);
