/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTab, NxTabList, NxTabs } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

const tabs = ['edit-ldap-connection', 'edit-ldap-usermapping'];
const tabsNames = new Map([
  [tabs[0], 'Connection'],
  [tabs[1], 'User & Group Settings'],
]);

export default function LdapTabs({ id, currentTab, stateGo }) {
  const handleTabClick = (index) => {
    stateGo(tabs[index], { ldapId: id });
  };

  return (
    <NxTabs activeTab={tabs.indexOf(currentTab)} onTabSelect={handleTabClick}>
      <NxTabList>
        {tabs.map((tab) => (
          <NxTab key={tab}>{tabsNames.get(tab)}</NxTab>
        ))}
      </NxTabList>
    </NxTabs>
  );
}

LdapTabs.propTypes = {
  id: PropTypes.string,
  currentTab: PropTypes.string.isRequired,
  stateGo: PropTypes.func.isRequired,
};
