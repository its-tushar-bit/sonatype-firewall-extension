/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import DashboardTabs from '../../../../../main/frontend/dashboard/results/dashboardTabs/DashboardTabs.jsx';
import { NxTab, NxTabList, NxTabs } from '@sonatype/react-shared-components';

describe('DashboardTabs', () => {
  let getShallow, initialProps;

  beforeEach(() => {
    initialProps = {
      currentTab: 'violations',
      isDashboardEnabled: true,
      isWaiversTabEnabled: true,
    };

    getShallow = enzymeUtils.getShallowComponent(DashboardTabs, initialProps);
  });

  describe('on load successfully', () => {
    let component;
    beforeEach(() => {
      component = getShallow();
    });

    it('renders NxTabs', () => {
      expect(component.find(NxTabs)).toExist();
    });
    it('renders NxTabList', () => {
      expect(component.find(NxTabList)).toExist();
    });
    it('renders all NxTab', () => {
      expect(component.find(NxTab).length).toBe(4);
    });
  });

  describe('NxTabs', () => {
    it('has activeTab equals 0 when currentTab is equals "violations', () => {
      const component = getShallow({ currentTab: 'violations' });
      expect(component.find(NxTabs)).toHaveProp('activeTab', 0);
    });
    it('has activeTab equals 1 when currentTab is equals "components', () => {
      const component = getShallow({ currentTab: 'components' });
      expect(component.find(NxTabs)).toHaveProp('activeTab', 1);
    });
    it('has activeTab equals 2 when currentTab is equals "applications', () => {
      const component = getShallow({ currentTab: 'applications' });
      expect(component.find(NxTabs)).toHaveProp('activeTab', 2);
    });
    it('has activeTab equals 5 when currentTab is equals "waivers', () => {
      const component = getShallow({ currentTab: 'waivers' });
      expect(component.find(NxTabs)).toHaveProp('activeTab', 3);
    });
  });

  describe('when a Tab is clicked', () => {
    let getMounted, component, stateGoSpy;

    beforeEach(() => {
      stateGoSpy = jasmine.createSpy('stateGo');

      initialProps = {
        currentTab: 'components',
        stateGo: stateGoSpy,
        isDashboardEnabled: true,
        isWaiversTabEnabled: true,
      };

      getMounted = enzymeUtils.getMountedComponent(DashboardTabs, initialProps);
    });
    afterEach(() => {
      component.unmount();
    });

    it('navigate to correct dashboard.overview', () => {
      component = getMounted();
      const applicationTab = component.find(NxTab).at(2);
      applicationTab.find('li').simulate('click');
      expect(stateGoSpy).toHaveBeenCalledWith('dashboard.overview.applications');
    });
  });
});
