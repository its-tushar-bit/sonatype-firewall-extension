/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nexusOneSettingsStates } from 'MainRoot/nexus-one/nexusOneSettingsStates';
import SettingsPage from 'MainRoot/nosc/settings/SettingsPage';

describe('nexusOneSettingsStates', () => {
  it('registers a single /settings state bound to SettingsPage', () => {
    const states = nexusOneSettingsStates();

    expect(states).toHaveLength(1);
    expect(states[0]).toMatchObject({
      name: 'nexusOneSettings',
      url: '/settings',
      component: SettingsPage,
    });
  });
});
