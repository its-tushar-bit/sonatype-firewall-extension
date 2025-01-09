/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as preferenceStoreFunctions from '../../../../main/frontend/util/preferenceStore';
import * as routerContext from '../../../../main/frontend/react/RouterStateContext';
import IqSidebarNav from '../../../../main/frontend/react/iqSidebarNav/IqSidebarNav';
import { render, within, screen } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import * as ProductLogoUtils from 'MainRoot/util/productLogoUtils';

describe('IqSidebarNav', function () {
  let hrefSpy, includesSpy, getProductLogoSpy;

  beforeEach(function () {
    hrefSpy = jest.fn().mockImplementation((args) => `href-${args}`);
    includesSpy = jest.fn().mockReturnValue(false);

    jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
      href: hrefSpy,
      includes: includesSpy,
    });

    getProductLogoSpy = jest.spyOn(ProductLogoUtils, 'getProductLogo');
  });

  describe('renders an NxGlobalSidebar', function () {
    const { setLeftNavigationOpen } = preferenceStoreFunctions;

    it('renders navigation with product home link', function () {
      renderComponent({ productEdition: 'my product' });

      const sideBar = screen.getByRole('complementary', { name: 'global sidebar' });
      expect(sideBar).toBeVisible();

      const homeLink = within(sideBar).getByRole('link', { name: 'my product' });
      expect(homeLink.getAttribute('href')).toEqual('href-home');
    });

    it('renders the NxGlobalSidebar open if there are no previous preferences set', function () {
      renderComponent();

      const sideBar = screen.getByRole('complementary', { name: 'global sidebar' });
      expect(sideBar).toBeVisible();
      expect(within(sideBar).getByRole('button', { expanded: true })).toBeVisible();
    });

    it('renders the NxGlobalSidebar open if this was the set preference previously', function () {
      setLeftNavigationOpen(true);

      renderComponent();

      const sideBar = screen.getByRole('complementary', { name: 'global sidebar' });
      expect(sideBar).toBeVisible();
      expect(within(sideBar).getByRole('button', { expanded: true })).toBeVisible();
    });

    it('renders the NxGlobalSidebar closed if this was the set preference previously', function () {
      setLeftNavigationOpen(false);

      renderComponent();

      const sideBar = screen.getByRole('complementary', { name: 'global sidebar' });
      expect(sideBar).toBeVisible();
      within(sideBar).getByRole('button', { expanded: false });
    });

    it('saves the NxGlobalSidebar open state preference when collapsing or opening it', async () => {
      setLeftNavigationOpen(false);
      const preferenceStoreSpy = jest.spyOn(preferenceStoreFunctions, 'setLeftNavigationOpen');

      renderComponent();

      const sideBar = screen.getByRole('complementary', { name: 'global sidebar' });
      const toggleButton = within(sideBar).getByRole('button', { expanded: false });

      await userEvent.click(toggleButton);
      expect(preferenceStoreSpy).toHaveBeenCalledWith(true);
      await userEvent.click(toggleButton);
      expect(preferenceStoreSpy).toHaveBeenCalledWith(false);
    });
  });

  it('does not render footer if releaseVersion is not present', () => {
    renderComponent({ productEdition: 'mockProductEdition' });
    expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument();
  });

  it('renders an IqSidebarNavFooter if both productEdition and releaseVersion are specified', function () {
    renderComponent({ productEdition: 'mockProductEdition', releaseVersion: '10x' });
    const footer = screen.getByRole('contentinfo');
    expect(footer.classList).toContain('iq-sidebar-nav-footer');
    expect(footer.textContent).toContain('Powered by Sonatype IQ Server');
  });

  describe('NxGlobalSidebarNavigation', function () {
    it('renders NxGlobalSidebarNavigation if logged-in', function () {
      renderComponent({ isLoggedIn: true, isProductsLoading: false });

      const sideBar = screen.getByRole('complementary', { name: 'global sidebar' });
      const navigationSection = within(sideBar).getByRole('navigation');
      expect(navigationSection.id).toEqual('global-sidebar-buttons');
    });

    it('does not render NxGlobalSidebarNavigation if not logged-in', function () {
      renderComponent({ isLoggedIn: false, isProductsLoading: false });

      const sideBar = screen.getByRole('complementary', { name: 'global sidebar' });
      expect(within(sideBar).queryByRole('navigation')).not.toBeInTheDocument();
    });

    it('renders an NxGlobalSidebarNavigationLink for the api page if allowed', function () {
      renderComponent({ isLoggedIn: true, isProductsLoading: false, isApiPageEnabled: true });

      const sideBar = screen.getByRole('complementary', { name: 'global sidebar' });
      const navigationSection = within(sideBar).getByRole('navigation');
      expect(navigationSection.id).toEqual('global-sidebar-buttons');

      const apiNav = within(navigationSection).getByRole('link', { name: 'API' });
      expect(apiNav.getAttribute('href')).toEqual('href-api');
      const icon = within(apiNav).getByRole('img', { hidden: true });
      expect(icon.getAttribute('data-icon')).toEqual('stars');
    });

    it('does not render an NxGlobalSidebarNavigationLink for the api page if not allowed', function () {
      renderComponent({ isLoggedIn: true, isProductsLoading: false, isApiPageEnabled: false });

      const sideBar = screen.getByRole('complementary', { name: 'global sidebar' });
      const navigationSection = within(sideBar).getByRole('navigation');
      expect(navigationSection.id).toEqual('global-sidebar-buttons');

      expect(within(navigationSection).queryByRole('link', { name: 'API' })).not.toBeInTheDocument();
    });

    it('does not render any navigation links when none are enabled', function () {
      renderComponent({ isLoggedIn: true });

      assertNoLinksInNavigation();
    });

    it('renders a link to the dashboard waivers overview when isDashboardWaiversAvailable is true', function () {
      renderComponent({
        isLoggedIn: true,
        isDashboardWaiversAvailable: true,
      });

      assertNavSectionContainsLink('href-dashboard.overview.waivers', 'Dashboard', 'house');
    });

    it('renders a link to the dashboard violations overview when isDashboardAvailable is true', function () {
      renderComponent({
        isLoggedIn: true,
        isDashboardAvailable: true,
      });

      assertNavSectionContainsLink('href-dashboard.overview.violations', 'Dashboard', 'house');
    });

    it('renders a link to Orgs and Policies when isLicensed is true', function () {
      renderComponent({
        isLoggedIn: true,
        isLicensed: true,
      });

      assertNavSectionContainsLink('href-management.view', 'Orgs and Policies', 'sitemap');
    });

    it('renders a link to Reports when isOrgsAndAppsEnabled and isReportsListAvailable are true', function () {
      renderComponent({
        isLoggedIn: true,
        isReportsListAvailable: true,
        isOrgsAndAppsEnabled: true,
      });

      assertNavSectionContainsLink('href-violations', 'Reports', 'file-chart-column');
    });

    it('renders a link to Success Metrics when isSuccessMetricsEnabled and isSuccessMetricsEnabled are true', function () {
      renderComponent({
        isLoggedIn: true,
        isSuccessMetricsEnabled: true,
        isOrgsAndAppsEnabled: true,
      });

      assertNavSectionContainsLink('href-labs.successMetrics', 'Success Metrics', 'chart-area');
    });

    it('renders a link to Vulnerability Lookup when isLicensed is true', function () {
      renderComponent({
        isLoggedIn: true,
        isLicensed: true,
      });

      assertNavSectionContainsLink('href-vulnerabilitySearch', 'Vulnerability Lookup', 'microscope');
    });

    it('renders a link to Advanced Search when isLicensed and isAdvancedSearchEnabled are true', function () {
      renderComponent({
        isLoggedIn: true,
        isLicensed: true,
        isAdvancedSearchEnabled: true,
      });

      assertNavSectionContainsLink('href-advancedSearch', 'Advanced Search', 'magnifying-glass');
    });

    it('does not render a link to the Dashboard when neither isDashboardAvailable nor isDashboardWaiversAvailable are true', function () {
      renderComponent({
        isLoggedIn: true,
        isLicensed: true,
        isDashboardAvailable: false,
        isDashboardWaiversAvailable: false,
      });

      const navigationSection = screen.getByRole('navigation');
      expect(within(navigationSection).queryByRole('link', { name: 'Dashboard' })).not.toBeInTheDocument();
    });

    it('renders an NxGlobalSidebarNavigationLink for legal if allowed', function () {
      renderComponent({
        isLoggedIn: true,
        isLicensed: true,
        isLegalEnabled: true,
      });

      assertNavSectionContainsLink('href-legal.dashboard', 'Legal', 'gavel');
    });

    it('renders an NxGlobalSidebarNavigationLink for Data Insights (Enterprise Reporting) with "Labs" badge if allowed', function () {
      renderComponent({
        isLoggedIn: true,
        isLicensed: true,
        isIntegratedEnterpriseReportingSupported: true,
      });

      assertNavSectionContainsLink('href-enterpriseReporting', 'Data Insights ( NEW )', 'chart-pie-simple');
    });

    it('does not render an NxGlobalSidebarNavigationLink for Data Insights (Enterprise Reporting) if not allowed', function () {
      renderComponent({
        isLoggedIn: true,
        isLicensed: true,
        isIntegratedEnterpriseReportingSupported: false,
      });

      expect(screen.queryByRole('link', { name: 'Data Insights ( NEW )' })).not.toBeInTheDocument();
    });

    describe('selected state', function () {
      let propsForRenderingAllLinks;
      beforeEach(function () {
        propsForRenderingAllLinks = {
          isLoggedIn: true,
          isLicensed: true,
          isDashboardAvailable: true,
          isReportsListAvailable: true,
          isSuccessMetricsEnabled: true,
          isAdvancedSearchEnabled: true,
          isFirewallEnabled: true,
          isLegalEnabled: true,
          isApiPageEnabled: true,
          isOrgsAndAppsEnabled: true,
        };
      });

      it('renders API link as selected when state matches', function () {
        mockSelectedLink('api');
        renderComponent(propsForRenderingAllLinks);

        assertLinkPresentAndSelected('API');
      });

      it('renders Dashboard link as selected when state matches', function () {
        mockSelectedLink('dashboard');
        renderComponent(propsForRenderingAllLinks);

        assertLinkPresentAndSelected('Dashboard');
      });

      it('renders Orgs and Policies link as selected when state matches', function () {
        mockSelectedLink('management');
        renderComponent(propsForRenderingAllLinks);

        assertLinkPresentAndSelected('Orgs and Policies');
      });

      it('renders Reports link as selected when state matches', function () {
        mockSelectedLink('violations');
        renderComponent(propsForRenderingAllLinks);

        assertLinkPresentAndSelected('Reports');
      });

      it('renders Success Metrics link as selected when state matches', function () {
        mockSelectedLink('labs');
        renderComponent(propsForRenderingAllLinks);

        assertLinkPresentAndSelected('Success Metrics');
      });

      it('renders Vulnerability Lookup link as selected when the state matches vulnerabilitySearch', function () {
        mockSelectedLink('vulnerabilitySearch');
        renderComponent(propsForRenderingAllLinks);

        assertLinkPresentAndSelected('Vulnerability Lookup');
      });

      it('renders Vulnerability Lookup link as selected when the state matches vulnerabilitySearchDetail', function () {
        mockSelectedLink('vulnerabilitySearchDetail');
        renderComponent(propsForRenderingAllLinks);

        assertLinkPresentAndSelected('Vulnerability Lookup');
      });

      it('renders Advanced Search link as selected when the state matches', function () {
        mockSelectedLink('advancedSearch');
        renderComponent(propsForRenderingAllLinks);

        assertLinkPresentAndSelected('Advanced Search');
      });

      it('renders Legal link as selected when the state matches', function () {
        mockSelectedLink('legal');
        renderComponent(propsForRenderingAllLinks);

        assertLinkPresentAndSelected('Legal');
      });
    });
  });

  describe('product logo handling', function () {
    it('gets product logo using to the supplied productEdition prop when Repository Firewall', function () {
      renderComponent({ productEdition: 'Repository Firewall' });
      expect(screen.getByRole('img', { name: 'Repository Firewall' })).toBeInTheDocument();
      expect(getProductLogoSpy).toHaveBeenCalledWith('Repository Firewall');
    });

    it('gets product logo using to the supplied productEdition prop when Lifecycle', function () {
      renderComponent({ productEdition: 'Lifecycle' });
      expect(screen.getByRole('img', { name: 'Lifecycle' })).toBeInTheDocument();
      expect(getProductLogoSpy).toHaveBeenCalledWith('Lifecycle');
    });

    it('gets product logo using to the supplied productEdition prop when Lifecycle Foundation', function () {
      renderComponent({ productEdition: 'Lifecycle Foundation' });
      expect(screen.getByRole('img', { name: 'Lifecycle Foundation' })).toBeInTheDocument();
      expect(getProductLogoSpy).toHaveBeenCalledWith('Lifecycle Foundation');
    });

    it('gets product logo using to the supplied productEdition prop when Auditor', function () {
      renderComponent({ productEdition: 'Auditor' });
      expect(getProductLogoSpy).toHaveBeenCalledWith('Auditor');
    });

    it('gets a default logo if the supplied productEdition is not a known product', function () {
      renderComponent({ productEdition: 'whatever' });
      expect(screen.getByRole('img', { name: 'whatever' })).toBeInTheDocument();
      expect(getProductLogoSpy).toHaveBeenCalledWith('whatever');
    });
  });

  function renderComponent(overrides = {}) {
    const props = {
      productEdition: 'product-edition',
      ...overrides,
    };
    return render(<IqSidebarNav {...props} />);
  }

  function assertNavSectionContainsLink(expectedHref, expectedText, expectedIcon) {
    const navigationSection = screen.getByRole('navigation');
    const navLink = within(navigationSection).getByRole('link', { name: expectedText });
    expect(navLink.getAttribute('href')).toEqual(expectedHref);

    const icon = within(navLink).getByRole('img', { hidden: true });
    expect(icon.getAttribute('data-icon')).toEqual(expectedIcon);
  }

  function assertNoLinksInNavigation() {
    const navigationSection = screen.getByRole('navigation');
    expect(within(navigationSection).queryByRole('link')).not.toBeInTheDocument();
  }

  function mockSelectedLink(linkName) {
    includesSpy.mockImplementation((state) => state === linkName);
  }

  function assertLinkPresentAndSelected(name) {
    expect(screen.getByRole('link', { name })).toHaveClass('selected');
  }
});
