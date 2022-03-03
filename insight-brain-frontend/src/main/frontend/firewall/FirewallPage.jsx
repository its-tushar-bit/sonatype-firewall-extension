/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import LoadWrapper from '../react/LoadWrapper';
import FirewallStatus from './FirewallStatus';
import FirewallQuarantineStatus from './FirewallQuarantineStatus';
import FirewallQuarantine from './FirewallQuarantine';
import FirewallAutoReleaseQuarantine from './FirewallAutoReleaseQuarantine';
import FirewallQuarantineTable from './FirewallQuarantineTable';
import * as PropTypes from 'prop-types';
import FirewallConfigurationModalContainer from './config/FirewallConfigurationModalContainer';
import FirewallAutoUnquarantineStatus from './FirewallAutoUnquarantineStatus';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function FirewallPage(props) {
  // Actions
  const { loadFirewallData } = props;

  // viewState
  const { isShowConfigurationModal, loadError } = props;

  // autoUnquarantineState.viewState
  const { autoReleaseQuarantineCountMTD, loadedReleaseQuarantineSummary, loadedConfiguration } = props;

  // quarantineSummaryState
  const { loadedQuarantineSummary } = props;

  // state
  const uiRouterState = useRouterState();

  const dataLoaded = isDataLoaded(loadedReleaseQuarantineSummary, loadedConfiguration, loadedQuarantineSummary);

  useEffect(() => {
    loadFirewallData();
  }, []);

  return (
    <main id="firewall-page" className="nx-page-main">
      {isShowConfigurationModal && <FirewallConfigurationModalContainer />}
      <LoadWrapper loading={!dataLoaded} error={loadError} retryHandler={loadFirewallData}>
        <FirewallStatus {...props} />
        <div className="nx-card-container nx-card-container--no-wrap">
          <FirewallQuarantineStatus {...props} />
          <FirewallAutoUnquarantineStatus {...props} />
          <FirewallQuarantine {...props} />
          <FirewallAutoReleaseQuarantine
            autoReleaseQuarantineCountMTD={autoReleaseQuarantineCountMTD}
            $state={uiRouterState}
          />
        </div>
        <FirewallQuarantineTable {...props} />
      </LoadWrapper>
    </main>
  );
}

function isDataLoaded(loadedReleaseQuarantineSummary, loadedConfiguration, loadedQuarantineSummary) {
  return loadedReleaseQuarantineSummary && loadedConfiguration && loadedQuarantineSummary;
}

FirewallPage.propTypes = {
  loadFirewallData: PropTypes.func.isRequired,
  autoReleaseQuarantineCountMTD: PropTypes.string.isRequired,
  loadedReleaseQuarantineSummary: PropTypes.bool.isRequired,
  isShowConfigurationModal: PropTypes.bool.isRequired,
  loadedConfiguration: PropTypes.bool.isRequired,
  loadedQuarantineSummary: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  uiRouterState: PropTypes.shape({
    href: PropTypes.func.isRequired,
  }),
};
