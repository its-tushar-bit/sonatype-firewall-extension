/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import ComponentDetailsTabs from '../../componentDetails/ComponentDetailsTabs';
import {
  ComponentDetailsReportInfo,
  ComponentDetailsHeader,
  ComponentDetailsTags,
  Title,
} from '../../componentDetails/ComponentDetailsHeader';
import { NxLoadWrapper } from '@sonatype/react-shared-components';
import { createTabConfiguration } from '../../componentDetails/componentDetailsUtils';
import FirewallOverview from './overview/FirewallOverview';
import FirewallPolicyViolations from './policyViolations/FirewallPolicyViolations';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

export const tabsConfiguration = [
  createTabConfiguration('overview', 'Overview', <FirewallOverview />),
  createTabConfiguration('violations', 'Policy Violations', <FirewallPolicyViolations />),
  createTabConfiguration('security', 'Security'),
  createTabConfiguration('legal', 'Legal'),
  createTabConfiguration('labels', 'Labels'),
];

export default function FirewallComponentDetailPage(props) {
  const { loadComponentDetails, CDPResponseState, onCDPTabChange, routeParams, previousPage } = props;
  const { tabId } = routeParams;
  const { componentDetails, isLoadingComponentDetails, componentDetailsError } = CDPResponseState;
  const componentCoordinates =
    componentDetails?.displayName?.parts?.reduce((prev, part) => prev + part.value, '') || '';
  const backButtonText =
    previousPage === 'firewall.firewallPage' ? 'Back to Firewall Dashboard' : 'Back to Repository results';
  const stateName = previousPage === 'firewall.firewallPage' ? 'firewall.firewallPage' : 'repository-report';

  useEffect(() => {
    loadComponentDetails(routeParams);
  }, []);

  const handleTabChange = (tabIdToMoveTo) => {
    if (tabIdToMoveTo === tabId) {
      return;
    }
    onCDPTabChange(tabIdToMoveTo);
  };

  return (
    <main id="firewall-component-details-page" className="nx-viewport-sized nx-page-main">
      <MenuBarBackButton text={backButtonText} stateName={stateName} />
      <div className="nx-viewport-sized__scrollable nx-scrollable firewall-component-details-page__container">
        <NxLoadWrapper
          loading={isLoadingComponentDetails}
          error={componentDetailsError}
          retryHandler={() => loadComponentDetails(routeParams)}
        >
          {() => (
            <ComponentDetailsHeader>
              <Title id="component-details-title">{componentCoordinates}</Title>
              <ComponentDetailsReportInfo {...componentDetails?.metadata} />
              <ComponentDetailsTags
                format={componentDetails?.componentIdentifier?.format}
                dependencyType={componentDetails?.dependencyType}
                isInnerSource={componentDetails?.isInnerSource}
                labels={componentDetails?.labels}
              />
            </ComponentDetailsHeader>
          )}
        </NxLoadWrapper>
        {componentDetails && (
          <ComponentDetailsTabs
            tabsConfiguration={tabsConfiguration}
            onTabChange={handleTabChange}
            activeTabId={tabId}
          />
        )}
      </div>
    </main>
  );
}

FirewallComponentDetailPage.propTypes = {
  loadComponentDetails: PropTypes.func,
  previousPage: PropTypes.string,
  onCDPTabChange: PropTypes.func.isRequired,
  routeParams: PropTypes.shape({
    repositoryId: PropTypes.string.isRequired,
    componentHash: PropTypes.string.isRequired,
    matchState: PropTypes.string.isRequired,
    proprietary: PropTypes.string,
    identificationSource: PropTypes.string,
    scanId: PropTypes.string,
    tabId: PropTypes.string.isRequired,
    componentIdentifier: PropTypes.string.isRequired,
  }).isRequired,
  CDPResponseState: PropTypes.shape({
    componentDetails: PropTypes.object,
    isLoadingComponentDetails: PropTypes.bool.isRequired,
    componentDetailsError: PropTypes.string,
  }).isRequired,
};
