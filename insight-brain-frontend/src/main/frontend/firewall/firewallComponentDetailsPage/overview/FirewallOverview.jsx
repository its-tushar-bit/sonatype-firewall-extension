/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { currentFirewallCDPComponentVersion } from 'MainRoot/firewall/firewallSelectors';
import {
  selectSelectedVersionData,
  selectCurrentVersionComparisonData,
  selectSelectedVersionComparisonData,
  selectVersionExplorerData,
} from 'MainRoot/componentDetails/overview/overviewSelectors';
import { actions } from 'MainRoot/componentDetails/overview/overviewSlice';
import FirewallOverviewComponentInformationTile from './componentInformationTile/FirewallOverviewComponentInformationTile';
import { selectFirewallCDP } from '../../firewallSelectors';
import { RiskRemediation } from 'MainRoot/componentDetails/overview/riskRemediation/RiskRemediation';

export default function FirewallOverview() {
  const { componentDetails } = useSelector(selectFirewallCDP);
  const matchState = componentDetails?.matchState;
  const isUnknown = !matchState || matchState === 'unknown';
  const versionExplorerData = useSelector(selectVersionExplorerData);
  const currentVersion = useSelector(currentFirewallCDPComponentVersion);
  const currentVersionComparisonData = useSelector(selectCurrentVersionComparisonData);
  const selectedVersionComparisonData = useSelector(selectSelectedVersionComparisonData);
  const dispatch = useDispatch();
  const selectedVersionData = useSelector(selectSelectedVersionData);

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
