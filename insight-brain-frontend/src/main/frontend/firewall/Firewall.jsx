/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {useEffect} from 'react';
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
  const {
    loadStatus,
    loadReleaseQuarantineSummary,
    loadConfiguration
  } = props;

  // viewState
  const {
    loadedStatus,
    loadStatusError,
    isShowConfigurationModal
  } = props;

  // statusState
  const {
    isEnabled
  } = props;

  const {
    autoReleaseQuarantineCountMTD,
    loadedReleaseQuarantineSummary,
    loadReleaseQuarantineSummaryError,
    loadedConfiguration,
    loadConfigurationError
  } = props;

  const error = determineError(loadedStatus, loadedReleaseQuarantineSummary, isEnabled, loadStatusError,
      loadReleaseQuarantineSummaryError, loadConfigurationError);

  function loadData() {
    loadStatus();
    loadReleaseQuarantineSummary();
    loadConfiguration();
  }

  useEffect(() => {
    loadData();
  }, []);

  return (
    <main id="firewall-page" className="nx-page-main">
      {isShowConfigurationModal && <FirewallConfigurationModalContainer/>}
      <LoadWrapper loading={!loadedStatus || !loadedReleaseQuarantineSummary || !loadedConfiguration} error={error}
                   retryHandler={loadData}>
        <FirewallStatus/>
        <div className="nx-card-container nx-card-container--row iq-firewall__horizontal">
          <FirewallQuarantineStatus/>
          <FirewallAutoUnquarantineStatus { ...props }/>
          <FirewallQuarantine/>
          <FirewallAutoReleaseQuarantine autoReleaseQuarantineCountMTD={autoReleaseQuarantineCountMTD}/>
        </div>
        <FirewallQuarantineTable/>
      </LoadWrapper>
    </main>
  );
}

function determineError(loadedStatus, loadedReleaseQuarantineSummary, isEnabled, loadStatusError,
                        loadReleaseQuarantineSummaryError, loadConfigurationError) {
  if (loadedStatus && !isEnabled) {
    return 'The Firewall feature is disabled';
  }
  else if (loadedStatus && loadStatusError) {
    return loadStatusError;
  }
  else if (loadedReleaseQuarantineSummary && loadReleaseQuarantineSummaryError) {
    return loadReleaseQuarantineSummaryError;
  }
  else {
    return loadConfigurationError;
  }
}

Firewall.propTypes = {
  loadStatus: PropTypes.func.isRequired,
  loadedStatus: PropTypes.bool.isRequired,
  loadStatusError: PropTypes.object,
  loadReleaseQuarantineSummary: PropTypes.func.isRequired,
  autoReleaseQuarantineCountMTD: PropTypes.string.isRequired,
  loadedReleaseQuarantineSummary: PropTypes.bool.isRequired,
  loadReleaseQuarantineSummaryError: PropTypes.object,
  isEnabled: PropTypes.bool.isRequired,
  isShowConfigurationModal: PropTypes.bool.isRequired,
  loadConfiguration: PropTypes.func.isRequired,
  loadedConfiguration: PropTypes.bool.isRequired,
  loadConfigurationError: PropTypes.bool
};
