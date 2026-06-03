/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */

import React, { useEffect, useRef } from 'react';
import { useSelector } from 'react-redux';

import { compose } from 'ramda';
import * as PropTypes from 'prop-types';
import { NxPageTitle, NxH1, NxTab, NxTabList, NxTabPanel, NxTabs } from '@sonatype/react-shared-components';

import FirewallStatus from './FirewallStatus';
import LoadWrapper from '../react/LoadWrapper';
import FirewallMetrics from './FirewallMetrics';
import FirewallWelcomeModal from './FirewallWelcomeModal';
import FirewallTabs from 'MainRoot/firewall/FirewallTabs';
import { COMPONENTS, CONTAINERS, QUARANTINE } from 'MainRoot/constants/states';
import FirewallConfigurationModalContainer from './config/FirewallConfigurationModalContainer';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';
import { selectIsContainerImagesEvaluationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import LimitedFirewallAccessAlert from 'MainRoot/react/LimitedFirewallAccessAlert';
import { selectShowLimitedFirewallAccessAlert } from 'MainRoot/firewall/firewallSelectors';
import FirewallContainerTabs from 'MainRoot/firewall/FirewallContainerTabs';

const TABS = [COMPONENTS, CONTAINERS];

export default function FirewallPage(props) {
  // Actions
  const {
    loadFirewallData,
    setQuarantineGridPolicyFilter,
    setQuarantineGridPolicyFilterWithProprietaryNameConflict,
    setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode,
  } = props;

  // Welcome Modal
  const { initializeWelcomeModal, showWelcomeModal, closeWelcomeModal } = props;

  // viewState
  const { isShowConfigurationModal, loadError } = props;

  // autoUnquarantineState.viewState
  const { loadedReleaseQuarantineSummary, loadedConfiguration } = props;

  // tileMetricsState
  const {
    componentsAutoReleased,
    componentsQuarantined,
    namespaceAttacksBlocked,
    safeVersionsSelected,
    supplyChainAttacksBlocked,
    waivedComponents,
  } = props;

  // quarantineSummaryState
  const { loadedQuarantineSummary } = props;

  const dataLoaded = isDataLoaded(loadedReleaseQuarantineSummary, loadedConfiguration, loadedQuarantineSummary);

  const { filterPolicies } = props;

  const { router, stateGo } = props;

  const activeTab = router?.currentState?.name?.includes(CONTAINERS) ? CONTAINERS : COMPONENTS;

  const activeTabIndex = TABS.indexOf(activeTab);

  const firewallTabsFuncRefs = useRef();

  const firewallContainerTabsFuncRefs = useRef();

  const isContainerImagesEvalEnabled = useSelector(selectIsContainerImagesEvaluationEnabled);

  const showLimitedFirewallAccessAlert = useSelector(selectShowLimitedFirewallAccessAlert);

  useEffect(() => {
    loadFirewallData();
    initializeWelcomeModal();
  }, []);

  const setQuarantineGridPolicyFilterEmpty = () => {
    if (filterPolicies?.length !== 0) {
      setQuarantineGridPolicyFilter([]);
    }
  };

  const scrollToQuarantineTable = () => {
    firewallTabsFuncRefs?.current?.scrollToPanel(QUARANTINE);
  };

  const onViewQuarantinedComponentsClick = (filterFn) => compose(scrollToQuarantineTable, filterFn);

  const handleTabClick = (index) => stateGo(`firewall.firewallPage.${TABS[index]}`);

  const firewallComponentsTabContent = () => {
    return (
      <>
        {showWelcomeModal && <FirewallWelcomeModal close={closeWelcomeModal} />}
        {isShowConfigurationModal && <FirewallConfigurationModalContainer />}
        <LoadWrapper loading={!dataLoaded} error={loadError} retryHandler={loadFirewallData}>
          <FirewallStatus {...props} />
          <header className="iq-firewall-metrics-header">
            <h2 className="nx-h2 iq-firewall-metrics-label">Component Data Insights</h2>
            <span>
              These totals include quarantined, waived, and auto-released components that differ from those actively in
              quarantine.
            </span>
          </header>
          <FirewallMetrics
            supplyChainAttacksBlocked={supplyChainAttacksBlocked}
            namespaceAttacksBlocked={namespaceAttacksBlocked}
            componentsQuarantined={componentsQuarantined}
            componentsAutoReleased={componentsAutoReleased}
            saferVersionsSelectedAutomatically={safeVersionsSelected}
            waivedComponents={waivedComponents}
            onNamespaceAttacksBlockedLinkClick={onViewQuarantinedComponentsClick(
              setQuarantineGridPolicyFilterWithProprietaryNameConflict
            )}
            onSupplyChainAttacksBlockedLinkClick={onViewQuarantinedComponentsClick(
              setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode
            )}
            onComponentsQuarantinedLinkClick={onViewQuarantinedComponentsClick(setQuarantineGridPolicyFilterEmpty)}
          />
          <FirewallTabs ref={firewallTabsFuncRefs} {...props} />
        </LoadWrapper>
      </>
    );
  };

  const firewallContainersTabContent = () => {
    return (
      <>
        <LoadWrapper loading={!dataLoaded} error={loadError} retryHandler={loadFirewallData}>
          <FirewallContainerTabs ref={firewallContainerTabsFuncRefs} {...props} />
        </LoadWrapper>
      </>
    );
  };

  return (
    <main id="firewall-page" className="nx-page-main">
      <NxPageTitle className="iq-firewall-page__title">
        <NxPageTitle.Headings>
          <NxH1>Repository Firewall</NxH1>
        </NxPageTitle.Headings>
      </NxPageTitle>
      {showLimitedFirewallAccessAlert && <LimitedFirewallAccessAlert />}
      {isContainerImagesEvalEnabled ? (
        <NxTabs id="firewall-page-tabs" activeTab={activeTabIndex} onTabSelect={handleTabClick}>
          <NxTabList>
            <NxTab id={`firewall-${COMPONENTS}-tab`}>{capitalizeFirstLetter(COMPONENTS)}</NxTab>
            <NxTab id={`firewall-${CONTAINERS}-tab`}>{capitalizeFirstLetter(CONTAINERS)}</NxTab>
          </NxTabList>
          <NxTabPanel id={`firewall-${COMPONENTS}-tab-panel`}>{firewallComponentsTabContent()}</NxTabPanel>
          <NxTabPanel id={`firewall-${CONTAINERS}-tab-panel`}>{firewallContainersTabContent()}</NxTabPanel>
        </NxTabs>
      ) : (
        firewallComponentsTabContent()
      )}
    </main>
  );
}

function isDataLoaded(loadedReleaseQuarantineSummary, loadedConfiguration, loadedQuarantineSummary) {
  return loadedReleaseQuarantineSummary && loadedConfiguration && loadedQuarantineSummary;
}

FirewallPage.propTypes = {
  showWelcomeModal: PropTypes.bool.isRequired,
  initializeWelcomeModal: PropTypes.func.isRequired,
  closeWelcomeModal: PropTypes.func.isRequired,
  loadFirewallData: PropTypes.func.isRequired,
  setQuarantineGridPolicyFilter: PropTypes.func.isRequired,
  setQuarantineGridPolicyFilterWithProprietaryNameConflict: PropTypes.func.isRequired,
  setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode: PropTypes.func.isRequired,
  autoReleaseQuarantineCountMTD: PropTypes.string.isRequired,
  loadedReleaseQuarantineSummary: PropTypes.bool.isRequired,
  isShowConfigurationModal: PropTypes.bool.isRequired,
  loadedConfiguration: PropTypes.bool.isRequired,
  loadedQuarantineSummary: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  componentsAutoReleased: PropTypes.number.isRequired,
  componentsQuarantined: PropTypes.number.isRequired,
  namespaceAttacksBlocked: PropTypes.number.isRequired,
  safeVersionsSelected: PropTypes.number.isRequired,
  supplyChainAttacksBlocked: PropTypes.number.isRequired,
  waivedComponents: PropTypes.number.isRequired,
  filterPolicies: PropTypes.array.isRequired,
  uiRouterState: PropTypes.shape({
    href: PropTypes.func.isRequired,
  }),
  isStandaloneFirewall: PropTypes.bool,
  stateGo: PropTypes.func.isRequired,
};
