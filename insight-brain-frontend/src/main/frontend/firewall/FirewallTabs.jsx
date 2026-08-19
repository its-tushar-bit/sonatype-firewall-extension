/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { forwardRef, useImperativeHandle, useRef } from 'react';

import * as PropTypes from 'prop-types';
import { always, complement, either, equals, filter, map, pipe } from 'ramda';
import { NxTab, NxTabList, NxTabPanel, NxTabs } from '@sonatype/react-shared-components';

import { QUARANTINE, ROI } from 'MainRoot/constants/states';
import FirewallQuarantineTable from 'MainRoot/firewall/FirewallQuarantineTable';
import RoiFirewallMetrics from 'MainRoot/firewall/roiMetrics/RoiFirewallMetrics';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';

const TABS = [QUARANTINE, ROI];

const FirewallTabs = forwardRef(function FirewallTabs({ router, stateGo, ...props }, ref) {
  const firewallTabsRefs = {
    quarantine: {
      panel: useRef(),
      name: capitalizeFirstLetter(QUARANTINE),
    },
    roi: {
      panel: useRef(),
      name: 'Return on Investment',
    },
  };

  useImperativeHandle(
    ref,
    () => ({
      scrollToPanel: (tab) => {
        const panelRef = firewallTabsRefs?.[tab]?.panel?.current?.firstChild;
        panelRef?.scrollIntoView({ behavior: 'smooth' });
      },
    }),
    []
  );

  const renderTab = (tab) => (
    <NxTab key={tab} id={`firewall-${tab}-tab`}>
      {firewallTabsRefs[tab].name}
    </NxTab>
  );

  const renderTabs = pipe(
    filter(either(complement(equals(ROI)), always(router?.currentParams?.roiEnabled))),
    map(renderTab)
  );
  const TAB_STATES = {
    [QUARANTINE]: 'firewall.firewallPage.components.quarantine',
    [ROI]: 'firewall.firewallPage.roi',
  };
  const onTabSelect = (index) => stateGo(TAB_STATES[TABS[index]]);

  const activeTab = router?.currentState?.data?.activeTab;
  const activeTabIndex = TABS.indexOf(activeTab === ROI ? ROI : QUARANTINE);

  return (
    <NxTabs activeTab={activeTabIndex} onTabSelect={onTabSelect}>
      <NxTabList>{renderTabs(TABS)}</NxTabList>
      <NxTabPanel id={`firewall-${QUARANTINE}-tab-panel`}>
        <div ref={firewallTabsRefs.quarantine.panel}>
          <FirewallQuarantineTable {...props} />
        </div>
      </NxTabPanel>
      <NxTabPanel id={`firewall-${ROI}-tab-panel`}>
        <div ref={firewallTabsRefs.roi.panel}>
          <RoiFirewallMetrics />
        </div>
      </NxTabPanel>
    </NxTabs>
  );
});

FirewallTabs.propTypes = {
  router: PropTypes.object,
  stateGo: PropTypes.func.isRequired,
};

export default FirewallTabs;
