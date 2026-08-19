/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';
import {
  selectSelectedVersionData,
  selectCurrentVersionComparisonData,
  selectSelectedVersionComparisonData,
  selectVersionExplorerData,
} from 'MainRoot/componentDetails/overview/overviewSelectors';
import { actions } from 'MainRoot/componentDetails/overview/overviewSlice';
import FirewallOverviewComponentInformationTile from './componentInformationTile/FirewallOverviewComponentInformationTile';
import { selectFirewallComponentDetailsPage } from '../../firewallSelectors';
import { RiskRemediation } from 'MainRoot/componentDetails/overview/riskRemediation/RiskRemediation';

export default function FirewallOverview() {
  const { componentDetails } = useSelector(selectFirewallComponentDetailsPage);
  const matchState = componentDetails?.matchState;
  const isUnknown = !matchState || matchState === 'unknown';
  const versionExplorerData = useSelector(selectVersionExplorerData);
  const currentVersionComparisonData = useSelector(selectCurrentVersionComparisonData);
  const selectedVersionComparisonData = useSelector(selectSelectedVersionComparisonData);
  const dispatch = useDispatch();
  const selectedVersionData = useSelector(selectSelectedVersionData);
  // Always take component information from URL. There are some race conditions where the information required hasn't
  // arrived the component takes old information from the redux store
  const routeParams = useSelector(selectFirewallComponentDetailsPageRouteParams);
  const componentIdentifier = JSON.parse(routeParams.componentIdentifier);
  const currentVersion = componentIdentifier?.coordinates?.version;

  return (
    <Fragment>
      <FirewallOverviewComponentInformationTile />
      {!isUnknown && (
        <RiskRemediation
          stageId="proxy"
          currentVersion={currentVersion}
          routeName=""
          componentInformation={{}}
          versionExplorerData={versionExplorerData}
          selectedVersionData={selectedVersionData}
          loadVersionExplorerData={() => dispatch(actions.firewallLoadVersionExplorerData())}
          loadSelectedVersionData={(version) => dispatch(actions.firewallLoadSelectedVersionData(version))}
          currentVersionComparisonData={currentVersionComparisonData}
          selectedVersionComparisonData={selectedVersionComparisonData}
        />
      )}
    </Fragment>
  );
}
