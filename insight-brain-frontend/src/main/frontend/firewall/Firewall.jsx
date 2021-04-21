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

export default function Firewall(props) {
  // Actions
  const { loadFirewallData } = props;

  // viewState
  const { loadedStatus, isShowConfigurationModal, loadError } = props;

  // statusState
  const { isEnabled } = props;

  // autoUnquarantineState.viewState
  const { autoReleaseQuarantineCountMTD, loadedReleaseQuarantineSummary, loadedConfiguration } = props;

  // quarantineSummaryState
  const { loadedQuarantineSummary } = props;

  // state
  const { $state } = props;

  const dataLoaded = isDataLoaded(
    loadedStatus,
    loadedReleaseQuarantineSummary,
    loadedConfiguration,
    loadedQuarantineSummary
  );

  const error = determineError(loadedStatus, isEnabled, loadError);

  useEffect(() => {
    loadFirewallData();
  }, []);

  return (
    <main id="firewall-page" className="nx-page-main">
      {isShowConfigurationModal && <FirewallConfigurationModalContainer />}
      <LoadWrapper loading={!dataLoaded} error={error} retryHandler={loadFirewallData}>
        <FirewallStatus {...props} />
        <div className="nx-card-container nx-card-container--no-wrap">
          <FirewallQuarantineStatus {...props} />
          <FirewallAutoUnquarantineStatus {...props} />
          <FirewallQuarantine {...props} />
          <FirewallAutoReleaseQuarantine
            autoReleaseQuarantineCountMTD={autoReleaseQuarantineCountMTD}
            $state={$state}
          />
        </div>
        <FirewallQuarantineTable {...props} />
      </LoadWrapper>
    </main>
  );
}

function determineError(loadedStatus, isEnabled, loadError) {
  if (loadError) {
    return loadError;
  }
  if (loadedStatus && !isEnabled) {
    return 'The Firewall feature is disabled';
  }
}

function isDataLoaded(loadedStatus, loadedReleaseQuarantineSummary, loadedConfiguration, loadedQuarantineSummary) {
  return loadedStatus && loadedReleaseQuarantineSummary && loadedConfiguration && loadedQuarantineSummary;
}

Firewall.propTypes = {
  loadFirewallData: PropTypes.func.isRequired,
  loadedStatus: PropTypes.bool.isRequired,
  autoReleaseQuarantineCountMTD: PropTypes.string.isRequired,
  loadedReleaseQuarantineSummary: PropTypes.bool.isRequired,
  isEnabled: PropTypes.bool.isRequired,
  isShowConfigurationModal: PropTypes.bool.isRequired,
  loadedConfiguration: PropTypes.bool.isRequired,
  loadedQuarantineSummary: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  $state: PropTypes.shape({
    href: PropTypes.func.isRequired,
  }).isRequired,
};
