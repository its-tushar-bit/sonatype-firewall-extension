/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { faArrowToLeft, faBars } from '@fortawesome/pro-regular-svg-icons';
import {
  faChartArea,
  faFileChartLine,
  faGavel,
  faHome,
  faMicroscope,
  faSearch,
  faShieldCheck,
  faSitemap,
} from '@fortawesome/pro-solid-svg-icons';
import * as preferenceStoreFunctions from '../../../../main/frontend/util/preferenceStore';

import * as enzymeUtils from '../../enzymeUtils';
import IqSidebarNav from '../../../../main/frontend/react/iqSidebarNav/IqSidebarNav';
import {
  NxButton,
  NxGlobalSidebar,
  NxGlobalSidebarNavigation,
  NxGlobalSidebarNavigationLink,
} from '@sonatype/react-shared-components';
import IqSidebarNavFooter from '../../../../main/frontend/react/iqSidebarNav/IqSidebarNavFooter';

describe('IqSidebarNav', function () {
  let getShallowComponent, getMountedComponent, hrefSpy, includesSpy;

  beforeEach(function () {
    hrefSpy = jasmine.createSpy('href').and.callFake((args) => `href-${args}`);
    includesSpy = jasmine.createSpy('includes').and.returnValue(false);

    spyOn(React, 'useContext').and.returnValue({
      href: hrefSpy,
      includes: includesSpy,
    });

    getShallowComponent = enzymeUtils.getShallowComponent(IqSidebarNav, {});
    getMountedComponent = enzymeUtils.getMountedComponent(IqSidebarNav, {});
  });

  describe('renders an NxGlobalSidebar', function () {
    const { setLeftNavigationOpen } = preferenceStoreFunctions;
    const deleteSideBarOpenStoredPreference = () => {
      localStorage.removeItem('leftNavigation.isOpen');
    };

    beforeEach(function () {
      deleteSideBarOpenStoredPreference();
    });

    it('renders an NxGlobalSidebar with props', function () {
      let component, globalSidebar;

      (component = getShallowComponent()), (globalSidebar = component.find(NxGlobalSidebar));

      expect(component).toExist();
      expect(globalSidebar).toExist();
      expect(globalSidebar).toHaveProp('isOpen', true);
      expect(globalSidebar).toHaveProp('onToggleClick', jasmine.any(Function));
      expect(globalSidebar).toHaveProp('toggleOpenIcon', faArrowToLeft);
      expect(globalSidebar).toHaveProp('toggleCloseIcon', faBars);
      expect(globalSidebar).toHaveProp('logoImg');
      expect(globalSidebar).toHaveProp('logoAltText', undefined);
      expect(globalSidebar).toHaveProp('logoLink', '#');

      component = getShallowComponent({ productEdition: 'mockProductEdition' });
      globalSidebar = component.find(NxGlobalSidebar);

      expect(globalSidebar).toHaveProp('logoAltText', 'mockProductEdition');
    });

    it('renders the NxGlobalSidebar open if there are no previous preferences set', function () {
      const component = getShallowComponent(),
        globalSidebar = component.find(NxGlobalSidebar);

      expect(globalSidebar).toExist();
      expect(globalSidebar).toHaveProp('isOpen', true);
    });

    it('renders the NxGlobalSidebar open if this was the set preference previously', function () {
      setLeftNavigationOpen(true);

      const component = getShallowComponent(),
        globalSidebar = component.find(NxGlobalSidebar);

      expect(globalSidebar).toExist();
      expect(globalSidebar).toHaveProp('isOpen', true);
    });

    it('renders the NxGlobalSidebar closed if this was the set preference previously', function () {
      setLeftNavigationOpen(false);

      const component = getShallowComponent(),
        globalSidebar = component.find(NxGlobalSidebar);

      expect(globalSidebar).toExist();
      expect(globalSidebar).toHaveProp('isOpen', false);
    });

    it('saves the NxGlobalSidebar open state preference when collapsing or opening it', function () {
      setLeftNavigationOpen(false);
      const preferenceStoreSpy = spyOn(preferenceStoreFunctions, 'setLeftNavigationOpen');

      const component = getMountedComponent({ logoAltText: 'product version -' }),
        sidebar = component.find(NxGlobalSidebar),
        toggleButton = sidebar.find(NxButton);

      toggleButton.simulate('click');
      expect(preferenceStoreSpy).toHaveBeenCalledWith(true);

      toggleButton.simulate('click');
      expect(preferenceStoreSpy).toHaveBeenCalledWith(false);
    });
  });

  it('renders an IqSidebarNavFooter if both productEdition and releaseVersion are specified', function () {
    expect(getShallowComponent().find(IqSidebarNavFooter)).not.toExist();

    expect(getShallowComponent({ productEdition: 'mockProductEdition' }).find(IqSidebarNavFooter)).not.toExist();
    expect(getShallowComponent({ releaseVersion: '10x' }).find(IqSidebarNavFooter)).not.toExist();

    expect(
      getShallowComponent({
        productEdition: 'mockProductEdition',
        releaseVersion: '10x',
      }).find(IqSidebarNavFooter)
    ).toExist();
  });

  describe('NxGlobalSidebarNavigation', function () {
    it('renders NxGlobalSidebarNavigation if logged-in', function () {
      expect(getShallowComponent().find(NxGlobalSidebarNavigation)).not.toExist();
      expect(getShallowComponent({ isLoggedIn: false }).find(NxGlobalSidebarNavigation)).not.toExist();
      expect(getShallowComponent({ isLoggedIn: true }).find(NxGlobalSidebarNavigation)).toExist();
    });

    it('renders an NxGlobalSidebarNavigationLink for the dashboard if allowed', function () {
      expect(getShallowComponent({ isLoggedIn: true }).find('#dashboard-navigation-button')).not.toExist();

      const component = getShallowComponent({
        isLoggedIn: true,
        isDashboardAvailable: true,
      });
      const navLink = component.find('#dashboard-navigation-button');

      expect(navLink).toMatchSelector(NxGlobalSidebarNavigationLink);
      expect(navLink).toHaveProp('icon', faHome);
      expect(navLink).toHaveProp('text', 'Dashboard');
      expect(navLink).toHaveProp('href', 'href-dashboard.overview.violations');
      expect(navLink).toHaveProp('isSelected', false);
    });

    it('renders an NxGlobalSidebarNavigationLink for orgs & policies if allowed', function () {
      expect(getShallowComponent({ isLoggedIn: true }).find('#policies-navigation-button')).not.toExist();

      const component = getShallowComponent({
        isLoggedIn: true,
        isLicensed: true,
      });
      const navLink = component.find('#policies-navigation-button');

      expect(navLink).toMatchSelector(NxGlobalSidebarNavigationLink);
      expect(navLink).toHaveProp('icon', faSitemap);
      expect(navLink).toHaveProp('text', 'Orgs and Policies');
      expect(navLink).toHaveProp('href', 'href-management.view');
      expect(navLink).toHaveProp('isSelected', false);
    });

    it('renders an NxGlobalSidebarNavigationLink for reports if allowed', function () {
      expect(getShallowComponent({ isLoggedIn: true }).find('#reporting-navigation-button')).not.toExist();

      const component = getShallowComponent({
        isLoggedIn: true,
        isReportsListAvailable: true,
      });
      const navLink = component.find('#reporting-navigation-button');

      expect(navLink).toMatchSelector(NxGlobalSidebarNavigationLink);
      expect(navLink).toHaveProp('icon', faFileChartLine);
      expect(navLink).toHaveProp('text', 'Reports');
      expect(navLink).toHaveProp('href', 'href-violations');
      expect(navLink).toHaveProp('isSelected', false);
    });

    it('renders an NxGlobalSidebarNavigationLink for success metrics if allowed', function () {
      expect(getShallowComponent({ isLoggedIn: true }).find('#labs-navigation-button')).not.toExist();

      const component = getShallowComponent({
        isLoggedIn: true,
        isSuccessMetricsEnabled: true,
      });
      const navLink = component.find('#labs-navigation-button');

      expect(navLink).toMatchSelector(NxGlobalSidebarNavigationLink);
      expect(navLink).toHaveProp('icon', faChartArea);
      expect(navLink).toHaveProp('text', 'Success Metrics');
      expect(navLink).toHaveProp('href', 'href-labs.successMetrics');
      expect(navLink).toHaveProp('isSelected', false);
    });

    it('renders an NxGlobalSidebarNavigationLink for vulnerability search if allowed', function () {
      expect(getShallowComponent({ isLoggedIn: true }).find('#vulnerability-navigation-button')).not.toExist();

      const component = getShallowComponent({
        isLoggedIn: true,
        isLicensed: true,
      });
      const navLink = component.find('#vulnerability-navigation-button');

      expect(navLink).toMatchSelector(NxGlobalSidebarNavigationLink);
      expect(navLink).toHaveProp('icon', faMicroscope);
      expect(navLink).toHaveProp('text', 'Vulnerability Search');
      expect(navLink).toHaveProp('href', 'href-vulnerabilitySearch');
      expect(navLink).toHaveProp('isSelected', false);
    });

    it('renders an NxGlobalSidebarNavigationLink for advanced search if allowed', function () {
      expect(getShallowComponent({ isLoggedIn: true }).find('#search-navigation-button')).not.toExist();

      const component = getShallowComponent({
        isLoggedIn: true,
        isLicensed: true,
        isAdvancedSearchEnabled: true,
      });
      const navLink = component.find('#search-navigation-button');

      expect(navLink).toMatchSelector(NxGlobalSidebarNavigationLink);
      expect(navLink).toHaveProp('icon', faSearch);
      expect(navLink).toHaveProp('text', 'Advanced Search');
      expect(navLink).toHaveProp('href', 'href-advancedSearch');
      expect(navLink).toHaveProp('isSelected', false);
    });

    it('renders an NxGlobalSidebarNavigationLink for firewall if allowed', function () {
      expect(getShallowComponent({ isLoggedIn: true }).find('#firewall-navigation-button')).not.toExist();

      const component = getShallowComponent({
        isLoggedIn: true,
        isFirewallEnabled: true,
        isLicensed: true,
      });
      const navLink = component.find('#firewall-navigation-button');

      expect(navLink).toMatchSelector(NxGlobalSidebarNavigationLink);
      expect(navLink).toHaveProp('icon', faShieldCheck);
      expect(navLink).toHaveProp('text', 'Firewall');
      expect(navLink).toHaveProp('href', 'href-firewall.firewallPage');
      expect(navLink).toHaveProp('isSelected', false);
    });

    it('renders an NxGlobalSidebarNavigationLink for legal if allowed', function () {
      expect(getShallowComponent({ isLoggedIn: true }).find('#advanced-legal-navigation-button')).not.toExist();

      const component = getShallowComponent({
        isLoggedIn: true,
        isLicensed: true,
        isLegalEnabled: true,
      });
      const navLink = component.find('#advanced-legal-navigation-button');

      expect(navLink).toMatchSelector(NxGlobalSidebarNavigationLink);
      expect(navLink).toHaveProp('icon', faGavel);
      expect(navLink).toHaveProp('text', 'Legal');
      expect(navLink).toHaveProp('href', 'href-legal.dashboard');
      expect(navLink).toHaveProp('isSelected', false);
    });

    describe('selected state', function () {
      let renderAllLinks;
      beforeEach(function () {
        renderAllLinks = enzymeUtils.getShallowComponent(IqSidebarNav, {
          isLoggedIn: true,
          isLicensed: true,
          isDashboardAvailable: true,
          isReportsListAvailable: true,
          isSuccessMetricsEnabled: true,
          isAdvancedSearchEnabled: true,
          isFirewallEnabled: true,
          isLegalEnabled: true,
        });
      });

      it('renders Dashboard link as selected when state matches', function () {
        includesSpy.and.callFake((state) => state === 'dashboard');
        expect(renderAllLinks().find('#dashboard-navigation-button')).toHaveProp('isSelected', true);
      });

      it('renders Orgs and Policies link as selected when state matches', function () {
        includesSpy.and.callFake((state) => state === 'management');
        expect(renderAllLinks().find('#policies-navigation-button')).toHaveProp('isSelected', true);
      });

      it('renders Reports link as selected when state matches', function () {
        includesSpy.and.callFake((state) => state === 'violations');
        expect(renderAllLinks().find('#reporting-navigation-button')).toHaveProp('isSelected', true);
      });

      it('renders Success Metrics link as selected when state matches', function () {
        includesSpy.and.callFake((state) => state === 'labs');
        expect(renderAllLinks().find('#labs-navigation-button')).toHaveProp('isSelected', true);
      });

      it('renders Vulnerability Search link as selected when the state matches vulnerabilitySearch', function () {
        includesSpy.and.callFake((state) => state === 'vulnerabilitySearch');
        expect(renderAllLinks().find('#vulnerability-navigation-button')).toHaveProp('isSelected', true);
      });

      it('renders Vulnerability Search link as selected when the state matches vulnerabilitySearchDetail', function () {
        includesSpy.and.callFake((state) => state === 'vulnerabilitySearchDetail');
        expect(renderAllLinks().find('#vulnerability-navigation-button')).toHaveProp('isSelected', true);
      });

      it('renders Advanced Search link as selected when the state matches', function () {
        includesSpy.and.callFake((state) => state === 'advancedSearch');
        expect(renderAllLinks().find('#search-navigation-button')).toHaveProp('isSelected', true);
      });

      it('renders Firewall link as selected when the state matches firewall', function () {
        includesSpy.and.callFake((state) => state === 'firewall');
        expect(renderAllLinks().find('#firewall-navigation-button')).toHaveProp('isSelected', true);
      });

      it('renders Firewall link as selected when the state matches firewallAutoUnquarantine', function () {
        includesSpy.and.callFake((state) => state === 'firewallAutoUnquarantine');
        expect(renderAllLinks().find('#firewall-navigation-button')).toHaveProp('isSelected', true);
      });

      it('renders Legal link as selected when the state matches', function () {
        includesSpy.and.callFake((state) => state === 'legal');
        expect(renderAllLinks().find('#advanced-legal-navigation-button')).toHaveProp('isSelected', true);
      });
    });
  });

  describe('product logo handling', function () {
    it('gets product logo using to the supplied productEdiction prop', function () {
      expect(getShallowComponent({ productEdition: 'Firewall' })).toHaveProp('logoImg', 'images/nexus_firewall.svg');
      expect(getShallowComponent({ productEdition: 'Lifecycle' })).toHaveProp('logoImg', 'images/nexus_lifecycle.svg');
      expect(getShallowComponent({ productEdition: 'Lifecycle Foundation' })).toHaveProp(
        'logoImg',
        'images/nexus_lifecycle.svg'
      );
      expect(getShallowComponent({ productEdition: 'Auditor' })).toHaveProp('logoImg', 'images/nexus_auditor.svg');
    });

    it('gets a default logo if the supplied productEdition is not a known product', function () {
      expect(getShallowComponent()).toHaveProp('logoImg', 'images/sonatype.svg');
      expect(getShallowComponent({ productEdition: 'whatever' })).toHaveProp('logoImg', 'images/sonatype.svg');
    });
  });
});
