/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { forwardRef, useImperativeHandle, useRef } from 'react';

import * as PropTypes from 'prop-types';
import { always, complement, either, equals, filter, map, pipe, replace, toUpper } from 'ramda';
import { NxTab, NxTabList, NxTabPanel, NxStatefulTabs } from '@sonatype/react-shared-components';

import { QUARANTINE, WAIVERS, ROI } from 'MainRoot/firewall/firewallConstants';
import FirewallQuarantineTable from 'MainRoot/firewall/FirewallQuarantineTable';
import DashboardWaivers from 'MainRoot/dashboard/results/waivers/DashboardWaivers';
import RoiFirewallMetrics from 'MainRoot/firewall/roiMetrics/RoiFirewallMetrics';

const TABS = [QUARANTINE, WAIVERS, ROI];

const capitalizeFirstLetter = replace(/^./, toUpper);

const FirewallTabs = forwardRef(function FirewallTabs({ router, stateGo, ...props }, ref) {
  const firewallTabsRefs = {
    quarantine: {
      tab: useRef(),
      panel: useRef(),
      name: capitalizeFirstLetter(QUARANTINE),
    },
    waivers: {
      tab: useRef(),
      panel: useRef(),
      name: capitalizeFirstLetter(WAIVERS),
    },
    roi: {
      tab: useRef(),
      panel: useRef(),
      name: 'Return on Investment',
    },
  };

  useImperativeHandle(
    ref,
    () => ({
      clickTab: (tab) => {
        const tabRef = firewallTabsRefs?.[tab]?.tab?.current?.firstChild;
        tabRef?.click();
      },
      scrollToPanel: (tab) => {
        const panelRef = firewallTabsRefs?.[tab]?.panel?.current?.firstChild;
        panelRef?.scrollIntoView({ behavior: 'smooth' });
      },
    }),
    []
  );

  const renderTab = (tab) => (
    <div ref={firewallTabsRefs[tab].tab} key={tab}>
      <NxTab id={`firewall-${tab}-tab`} data-testid={`firewall-${tab}-tab`}>
        {firewallTabsRefs[tab].name}
      </NxTab>
    </div>
  );

  const renderTabs = pipe(
    filter(either(complement(equals(ROI)), always(router?.currentParams?.roiEnabled))),
    map(renderTab)
  );
  const onTabSelect = (index) => stateGo(`firewall.firewallPage.${TABS[index]}`);

  const activeTab = router?.currentState?.data?.activeTab ?? QUARANTINE;
  const defaultActiveTab = TABS.indexOf(activeTab);

  return (
    <NxStatefulTabs defaultActiveTab={defaultActiveTab} onTabSelect={onTabSelect}>
      <NxTabList>{renderTabs(TABS)}</NxTabList>
      <NxTabPanel id={`firewall-${QUARANTINE}-tab-panel`} data-testid={`firewall-${QUARANTINE}-tab-panel`}>
        <div ref={firewallTabsRefs.quarantine.panel}>
          <FirewallQuarantineTable {...props} />
        </div>
      </NxTabPanel>
      <NxTabPanel id={`firewall-${WAIVERS}-tab-panel`} data-testid={`firewall-${WAIVERS}-tab-panel`}>
        <div ref={firewallTabsRefs.waivers.panel}>
          <DashboardWaivers />
        </div>
      </NxTabPanel>
      <NxTabPanel id={`firewall-${ROI}-tab-panel`} data-testid={`firewall-${ROI}-tab-panel`}>
        <div ref={firewallTabsRefs.roi.panel}>
          <RoiFirewallMetrics />
        </div>
      </NxTabPanel>
    </NxStatefulTabs>
  );
});

FirewallTabs.propTypes = {
  router: PropTypes.object,
  stateGo: PropTypes.func.isRequired,
};

export default FirewallTabs;
