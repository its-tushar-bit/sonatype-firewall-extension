/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import LoadWrapper from '../../react/LoadWrapper';
import FirewallAutoUnquarantineStatus from '../FirewallAutoUnquarantineStatus';
import FirewallUnquarantineTable from './FirewallUnquarantineTable';
import * as PropTypes from 'prop-types';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';
import FirewallConfigurationModalContainer from '../config/FirewallConfigurationModalContainer';
import FirewallAutoReleaseQuarantineMtd from './FirewallAutoReleaseQuarantineMtd';
import FirewallAutoReleaseQuarantineYtd from './FirewallAutoReleaseQuarantineYtd';

export default function FirewallAutoUnquarantinePage(props) {
  // Actions
  const { loadAutoUnquarantineData } = props;

  // viewState
  const { isShowConfigurationModal, loadError } = props;

  // autoUnquarantineState.viewState
  const {
    autoReleaseQuarantineCountMTD,
    autoReleaseQuarantineCountYTD,
    loadedReleaseQuarantineSummary,
    loadedConfiguration,
  } = props;

  const dataLoaded = isDataLoaded(loadedReleaseQuarantineSummary, loadedConfiguration);

  useEffect(() => {
    loadAutoUnquarantineData();
  }, []);

  return (
    <main id="firewall-auto-unquarantine-page" className="nx-page-main">
      <LoadWrapper loading={!dataLoaded} error={loadError} retryHandler={loadAutoUnquarantineData}>
        <MenuBarBackButton stateName="firewall.firewallPage" />
        {isShowConfigurationModal && <FirewallConfigurationModalContainer />}
        <div className="nx-page-title">
          <h1 className="nx-h1">Auto Release from Quarantine</h1>
        </div>
        <div className="nx-card-container nx-card-container--no-wrap">
          <FirewallAutoReleaseQuarantineMtd autoReleaseQuarantineCountMTD={autoReleaseQuarantineCountMTD} />
          <FirewallAutoReleaseQuarantineYtd autoReleaseQuarantineCountYTD={autoReleaseQuarantineCountYTD} />
          <FirewallAutoUnquarantineStatus {...props} />
        </div>
        <FirewallUnquarantineTable {...props} />
      </LoadWrapper>
    </main>
  );
}

function isDataLoaded(loadedReleaseQuarantineSummary, loadedConfiguration) {
  return loadedReleaseQuarantineSummary && loadedConfiguration;
}

FirewallAutoUnquarantinePage.propTypes = {
  loadAutoUnquarantineData: PropTypes.func.isRequired,
  autoReleaseQuarantineCountMTD: PropTypes.string.isRequired,
  autoReleaseQuarantineCountYTD: PropTypes.string.isRequired,
  loadedReleaseQuarantineSummary: PropTypes.bool.isRequired,
  isShowConfigurationModal: PropTypes.bool.isRequired,
  loadedConfiguration: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
};
