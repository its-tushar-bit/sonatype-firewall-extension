/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { forwardRef, useImperativeHandle, useRef } from 'react';

import * as PropTypes from 'prop-types';
import { map, pipe } from 'ramda';
import { NxTab, NxTabList, NxTabPanel, NxTabs } from '@sonatype/react-shared-components';

import { QUARANTINE } from 'MainRoot/constants/states';
import FirewallContainerQuarantineTable from 'MainRoot/firewall/FirewallContainerQuarantineTable';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';

const TABS = [QUARANTINE];

const FirewallContainerTabs = forwardRef(function FirewallContainerTabs({ stateGo, ...props }, ref) {
  const firewallTabsRefs = {
    quarantine: {
      panel: useRef(),
      name: capitalizeFirstLetter(QUARANTINE),
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
    <NxTab key={tab} id={`firewall-container-${tab}-tab`}>
      {firewallTabsRefs[tab].name}
    </NxTab>
  );

  const renderTabs = pipe(map(renderTab));
  const onTabSelect = (index) => stateGo(`firewall.firewallPage.containers.${TABS[index]}`);

  const activeTabIndex = 0;

  return (
    <NxTabs activeTab={activeTabIndex} onTabSelect={onTabSelect}>
      <NxTabList>{renderTabs(TABS)}</NxTabList>
      <NxTabPanel id={`firewall-container-${QUARANTINE}-tab-panel`}>
        <div ref={firewallTabsRefs.quarantine.panel}>
          <FirewallContainerQuarantineTable {...props} />
        </div>
      </NxTabPanel>
    </NxTabs>
  );
});

FirewallContainerTabs.propTypes = {
  router: PropTypes.object,
  stateGo: PropTypes.func.isRequired,
};

export default FirewallContainerTabs;
