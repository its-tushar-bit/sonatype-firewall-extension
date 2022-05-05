/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTab, NxTabs } from '@sonatype/react-shared-components';

import ComponentDetailsTabs from 'MainRoot/componentDetails/ComponentDetailsTabs';
import { getTabsConfiguration } from 'MainRoot/componentDetails/ComponentDetails';
import * as enzymeUtils from '../enzymeUtils';

describe('ComponentDetailsTabs', function () {
  let minimalProps, getShallowComponent, onTabChangeSpy;

  beforeEach(function () {
    onTabChangeSpy = jasmine.createSpy('onTabChange');

    minimalProps = {
      activeTab: '1',
      onTabChange: onTabChangeSpy,
      tabsConfiguration: getTabsConfiguration(false, true, false),
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentDetailsTabs, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('renders NxTabs tabs with the appropriate props', () => {
    const component = getShallowComponent();
    const tabs = component.find(NxTabs);

    expect(tabs).toHaveProp('activeTab');
    expect(tabs).toHaveProp('onTabSelect');
  });

  it('renders 6 tabs with the appropriate names when component is not unknown and matchState=exact', function () {
    const component = getShallowComponent(),
      tabBar = component.find(NxTabs);

    expect(tabBar).toExist();

    const tabs = tabBar.find(NxTab);

    expect(tabs.at(0)).toHaveProp('children', 'Overview');
    expect(tabs.at(1)).toHaveProp('children', 'Policy Violations');
    expect(tabs.at(2)).toHaveProp('children', 'Security');
    expect(tabs.at(3)).toHaveProp('children', 'Legal');
    expect(tabs.at(4)).toHaveProp('children', 'Labels');
    expect(tabs.at(5)).toHaveProp('children', 'Audit Log');
  });

  it('renders 7 tabs with the appropriate names when component is claimed but not unknown', function () {
    const customMinimalProps = {
      ...minimalProps,
      tabsConfiguration: getTabsConfiguration(false, false, true),
    };

    const component = enzymeUtils.getShallowComponent(ComponentDetailsTabs, customMinimalProps)();

    const tabBar = component.find(NxTabs);

    expect(tabBar).toExist();

    const tabs = tabBar.find(NxTab);

    expect(tabs.at(0)).toHaveProp('children', 'Overview');
    expect(tabs.at(1)).toHaveProp('children', 'Policy Violations');
    expect(tabs.at(2)).toHaveProp('children', 'Security');
    expect(tabs.at(3)).toHaveProp('children', 'Legal');
    expect(tabs.at(4)).toHaveProp('children', 'Labels');
    expect(tabs.at(5)).toHaveProp('children', 'Claim');
    expect(tabs.at(6)).toHaveProp('children', 'Audit Log');
  });

  it('renders 3 tabs with the appropriate names when there is an unknown component', function () {
    const customMinimalProps = {
      ...minimalProps,
      tabsConfiguration: getTabsConfiguration(true, false, true),
    };

    const component = enzymeUtils.getShallowComponent(ComponentDetailsTabs, customMinimalProps)();

    const tabBar = component.find(NxTabs);

    expect(tabBar).toExist();

    const tabs = tabBar.find(NxTab);

    expect(tabs.at(0)).toHaveProp('children', 'Overview');
    expect(tabs.at(1)).toHaveProp('children', 'Policy Violations');
    expect(tabs.at(2)).toHaveProp('children', 'Claim');
  });
});
