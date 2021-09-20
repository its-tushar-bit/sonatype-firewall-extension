/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTab, NxTabList, NxTabs } from '@sonatype/react-shared-components';
import { getShallowComponent, getMountedComponent } from '../../../frontend/enzymeUtils';
import LdapTabs from '../../../../main/frontend/configuration/ldap/LdapTabs';

describe('LdapTabs', () => {
  let getShallow, minimalProps;

  beforeEach(() => {
    minimalProps = {
      id: '200',
      currentTab: 'edit-ldap-connection',
    };

    getShallow = getShallowComponent(LdapTabs, minimalProps);
  });

  describe('on load', () => {
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
      expect(component.find(NxTab).length).toBe(2);
    });
  });

  describe('NxTabs', () => {
    it('has activeTab equals 0 when currentTab is equals "edit-ldap-connection"', () => {
      const component = getShallow({ currentTab: 'edit-ldap-connection' });
      expect(component.find(NxTabs)).toHaveProp('activeTab', 0);
    });

    it('has activeTab equals 1 when currentTab is equals "edit-ldap-usermapping"', () => {
      const component = getShallow({ currentTab: 'edit-ldap-usermapping' });
      expect(component.find(NxTabs)).toHaveProp('activeTab', 1);
    });
  });

  describe('when a Tab is clicked', () => {
    let getMounted, component, stateGoSpy;

    beforeEach(() => {
      stateGoSpy = jasmine.createSpy('stateGo');

      minimalProps = {
        id: '200',
        currentTab: 'edit-ldap-usermapping',
        stateGo: stateGoSpy,
      };

      getMounted = getMountedComponent(LdapTabs, minimalProps);
    });

    afterEach(() => {
      component.unmount();
    });

    it('navigate to edit-ldap-connection', () => {
      component = getMounted();
      const applicationTab = component.find(NxTab).at(0);
      applicationTab.find('li').simulate('click');
      expect(stateGoSpy).toHaveBeenCalledWith('edit-ldap-connection', { ldapId: '200' });
    });

    it('navigate to edit-ldap-usermapping', () => {
      component = getMounted();
      const applicationTab = component.find(NxTab).at(1);
      applicationTab.find('li').simulate('click');
      expect(stateGoSpy).toHaveBeenCalledWith('edit-ldap-usermapping', { ldapId: '200' });
    });
  });
});
