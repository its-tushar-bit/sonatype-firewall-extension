/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { forwardRef, useImperativeHandle, useRef } from 'react';

import * as PropTypes from 'prop-types';
import { map, pipe } from 'ramda';
import { NxTab, NxTabList, NxTabPanel, NxTabs } from '@sonatype/react-shared-components';

import { QUARANTINE, WAIVERS } from 'MainRoot/constants/states';
import FirewallContainerQuarantineTable from 'MainRoot/firewall/FirewallContainerQuarantineTable';
import FirewallContainerWaiverTable from 'MainRoot/firewall/FirewallContainerWaiverTable';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';

const TABS = [QUARANTINE, WAIVERS];

const FirewallContainerTabs = forwardRef(function FirewallContainerTabs({ router, stateGo, ...props }, ref) {
  const firewallTabsRefs = {
    quarantine: {
      panel: useRef(),
      name: capitalizeFirstLetter(QUARANTINE),
    },
    waivers: {
      panel: useRef(),
      name: capitalizeFirstLetter(WAIVERS),
    },
  };

  useImperativeHandle(ref, () => ({
    scrollToPanel: (tab) => {
      const panelRef = firewallTabsRefs?.[tab]?.panel?.current?.firstChild;
      panelRef?.scrollIntoView({ behavior: 'smooth' });
    },
  }), []);

  const renderTab = (tab) => (
    <NxTab key={tab} id={`firewall-container-${tab}-tab`}>
      {firewallTabsRefs[tab].name}
    </NxTab>
  );

  const renderTabs = pipe(map(renderTab));
  const onTabSelect = (index) => stateGo(`firewall.firewallPage.containers.${TABS[index]}`);

  const activeTab = router?.currentState?.data?.activeTab === WAIVERS ? WAIVERS : QUARANTINE;
  const activeTabIndex = TABS.indexOf(activeTab);

  return (
    <NxTabs activeTab={activeTabIndex} onTabSelect={onTabSelect}>
      <NxTabList>{renderTabs(TABS)}</NxTabList>
      <NxTabPanel id={`firewall-container-${QUARANTINE}-tab-panel`}>
        <div ref={firewallTabsRefs.quarantine.panel}>
          <FirewallContainerQuarantineTable {...props} />
        </div>
      </NxTabPanel>
      <NxTabPanel id={`firewall-container-${WAIVERS}-tab-panel`}>
        <FirewallContainerWaiverTable {...props} stateGo={stateGo} />
      </NxTabPanel>
    </NxTabs>
  );
});

FirewallContainerTabs.propTypes = {
  router: PropTypes.object,
  stateGo: PropTypes.func.isRequired,
};

export default FirewallContainerTabs;
