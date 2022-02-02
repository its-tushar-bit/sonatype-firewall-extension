/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import { selectDependencyTreeSubset } from '../../componentDetailsSelectors';
import { selectCurrentRouteName } from '../../../reduxUiRouter/routerSelectors';
import { actions } from '../overviewSlice';
import { actions as componenDetailsActions } from '../../componentDetailsSlice';
import {
  selectExpanded,
  selectVersionExplorerData,
  selectSelectedVersionData,
  selectRemediationData,
  selectCurrentVersionComparisonData,
  selectSelectedVersionComparisonData,
} from '../overviewSelectors';
import { RiskRemediation } from './RiskRemediation';
import { selectSelectedComponent } from 'MainRoot/applicationReport/applicationReportSelectors';

const { visitAncestorAction } = componenDetailsActions;

function mapStateToProps(state) {
  const { currentVersion, stageId } = selectRemediationData(state);
  return {
    componentInformation: selectSelectedComponent(state),
    dependencyTreeSubset: selectDependencyTreeSubset(state),
    routeName: selectCurrentRouteName(state),
    currentVersion,
    stageId,
    versionExplorerData: selectVersionExplorerData(state),
    selectedVersionData: selectSelectedVersionData(state),
    currentVersionComparisonData: selectCurrentVersionComparisonData(state),
    selectedVersionComparisonData: selectSelectedVersionComparisonData(state),
    expanded: selectExpanded(state),
  };
}

const mapDispatchToProps = {
  loadVersionExplorerData: actions.loadVersionExplorerData,
  loadSelectedVersionData: actions.loadSelectedVersionData,
  resetSelectedVersionData: actions.resetSelectedVersionData,
  ancestorOnClick: visitAncestorAction,
  toggleAncestorsList: actions.toggleAncestorsList,
};

export const RiskRemediationContainer = connect(mapStateToProps, mapDispatchToProps)(RiskRemediation);
