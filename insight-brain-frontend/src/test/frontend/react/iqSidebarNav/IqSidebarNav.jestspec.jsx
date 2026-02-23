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

describe('IqSidebarNav', function () {
  let hrefSpy;

  beforeEach(function () {
    hrefSpy = jest.fn().mockImplementation((args) => `href-${args}`);

    jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
      href: hrefSpy,
    });
  });

  describe('renders an NxGlobalSidebar2', function () {
    const { setLeftNavigationOpen } = preferenceStoreFunctions;

    it('renders navigation', function () {
      renderComponent();

      const sideBar = screen.getByRole('navigation', { name: 'global sidebar' });
      expect(sideBar).toBeVisible();
    });

    it('renders the NxGlobalSidebar2 open if there are no previous preferences set', function () {
      renderComponent();

      const sideBar = screen.getByRole('navigation', { name: 'global sidebar' });
      expect(sideBar).toBeVisible();
      expect(screen.getByRole('button', { expanded: true, name: /collapse menu/i })).toBeVisible();
    });

    it('renders the NxGlobalSidebar2 open if this was the set preference previously', function () {
      setLeftNavigationOpen(true);

      renderComponent();

      const sideBar = screen.getByRole('navigation', { name: 'global sidebar' });
      expect(sideBar).toBeVisible();
      expect(screen.getByRole('button', { expanded: true, name: /collapse menu/i })).toBeVisible();
    });

    it('renders the NxGlobalSidebar closed if this was the set preference previously', function () {
      setLeftNavigationOpen(false);

      renderComponent();

      const sideBar = screen.getByRole('navigation', { name: 'global sidebar' });
      expect(sideBar).toBeVisible();
      expect(screen.getByRole('button', { expanded: false, name: /expand menu/i })).toBeVisible();
    });

    it('saves the NxGlobalSidebar open state preference when collapsing or opening it', async () => {
      setLeftNavigationOpen(false);
      const preferenceStoreSpy = jest.spyOn(preferenceStoreFunctions, 'setLeftNavigationOpen');

      renderComponent();

      const toggleButton = screen.getByRole('button', { expanded: false, name: /expand menu/i });

      await userEvent.click(toggleButton);
      expect(preferenceStoreSpy).toHaveBeenCalledWith(true);
      await userEvent.click(toggleButton);
      expect(preferenceStoreSpy).toHaveBeenCalledWith(false);
    });
  });

  it('renders NxGlobalSidebarNavigationLinks if logged-in', function () {
    renderComponent({ isLoggedIn: true, isProductsLoading: false, isLicensed: true });

    const sideBar = screen.getByRole('navigation', { name: 'global sidebar' });
    const nxGlobalSidebarNavigationLinks = within(sideBar).getAllByRole('link');
    expect(nxGlobalSidebarNavigationLinks.length).toBeGreaterThan(0);
  });

  it('does not render NxGlobalSidebarNavigationLinks if not logged-in', function () {
    renderComponent({ isLoggedIn: false, isProductsLoading: false });

    const sideBar = screen.getByRole('navigation', { name: 'global sidebar' });
    expect(within(sideBar).queryByRole('link')).not.toBeInTheDocument();
  });

  it('renders an NxGlobalSidebarNavigationLink for the api page if allowed', function () {
    renderComponent({ isLoggedIn: true, isProductsLoading: false, isApiPageEnabled: true });

    const sideBar = screen.getByRole('navigation', { name: 'global sidebar' });

    const apiNav = within(sideBar).getByRole('link', { name: 'API ( NEW )' });
    expect(apiNav.getAttribute('href')).toEqual('href-api');
    const icon = within(apiNav).getByRole('img', { hidden: true });
    expect(icon.getAttribute('data-icon')).toEqual('stars');
  });

  it('does not render an NxGlobalSidebarNavigationLink for the api page if not allowed', function () {
    renderComponent({ isLoggedIn: true, isProductsLoading: false, isApiPageEnabled: false });

    const sideBar = screen.getByRole('navigation', { name: 'global sidebar' });
    expect(within(sideBar).queryByRole('link', { name: 'API ( NEW )' })).not.toBeInTheDocument();
  });

  it('does not render any navigation links when none are enabled', function () {
    renderComponent({ isLoggedIn: true });

    assertNoLinksInNavigation();
  });

  it('renders a link to the dashboard violations overview when isDashboardAvailable is true', function () {
    renderComponent({
      isLoggedIn: true,
      isDashboardAvailable: true,
    });

    assertNavSectionContainsLink('href-dashboard.overview.violations', 'Dashboard', 'house');
  });

  it('does not render a link to the dashboard violations overview when isDashboardAvailable is false', function () {
    renderComponent({
      isLoggedIn: true,
      isDashboardAvailable: false,
    });

    const sideBar = screen.getByRole('navigation', { name: 'global sidebar' });
    expect(within(sideBar).queryByRole('link', { name: 'Dashboard' })).not.toBeInTheDocument();
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

    assertNavSectionContainsLink('href-violations', 'Reports', 'chart-column');
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

  it('renders an NxGlobalSidebarNavigationLink for legal if allowed', function () {
    renderComponent({
      isLoggedIn: true,
      isLicensed: true,
      isLegalEnabled: true,
    });

    assertNavSectionContainsLink('href-legal.dashboard', 'Legal', 'gavel');
  });

  it('renders an NxGlobalSidebarNavigationLink for Enterprise Reporting with "Labs" badge if allowed', function () {
    renderComponent({
      isLoggedIn: true,
      isLicensed: true,
      isIntegratedEnterpriseReportingSupported: true,
    });

    assertNavSectionContainsLink('href-enterpriseReporting', 'Enterprise Reporting', 'chart-pie');
  });

  it('does not render an NxGlobalSidebarNavigationLink for Enterprise Reporting if not allowed', function () {
    renderComponent({
      isLoggedIn: true,
      isLicensed: true,
      isIntegratedEnterpriseReportingSupported: false,
    });

    expect(screen.queryByRole('link', { name: 'Enterprise Reporting' })).not.toBeInTheDocument();
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
      renderComponent({ ...propsForRenderingAllLinks, ...mockSelectedLink('api') });

      assertLinkPresentAndSelected('API ( NEW )');
    });

    it('renders Dashboard link as selected when state matches', function () {
      renderComponent({ ...propsForRenderingAllLinks, ...mockSelectedLink('dashboard') });

      assertLinkPresentAndSelected('Dashboard');
    });

    it('renders Orgs and Policies link as selected when state matches', function () {
      renderComponent({ ...propsForRenderingAllLinks, ...mockSelectedLink('management') });

      assertLinkPresentAndSelected('Orgs and Policies');
    });

    it('renders Reports link as selected when state matches', function () {
      renderComponent({ ...propsForRenderingAllLinks, ...mockSelectedLink('violations') });

      assertLinkPresentAndSelected('Reports');
    });

    it('renders Success Metrics link as selected when state matches', function () {
      renderComponent({ ...propsForRenderingAllLinks, ...mockSelectedLink('labs') });

      assertLinkPresentAndSelected('Success Metrics');
    });

    it('renders Vulnerability Lookup link as selected when the state matches vulnerabilitySearch', function () {
      renderComponent({ ...propsForRenderingAllLinks, ...mockSelectedLink('vulnerabilitySearch') });

      assertLinkPresentAndSelected('Vulnerability Lookup');
    });

    it('renders Vulnerability Lookup link as selected when the state matches vulnerabilitySearchDetail', function () {
      renderComponent({ ...propsForRenderingAllLinks, ...mockSelectedLink('vulnerabilitySearchDetail') });

      assertLinkPresentAndSelected('Vulnerability Lookup');
    });

    it('renders Advanced Search link as selected when the state matches', function () {
      renderComponent({ ...propsForRenderingAllLinks, ...mockSelectedLink('advancedSearch') });

      assertLinkPresentAndSelected('Advanced Search');
    });

    it('renders Legal link as selected when the state matches', function () {
      renderComponent({ ...propsForRenderingAllLinks, ...mockSelectedLink('legal') });

      assertLinkPresentAndSelected('Legal');
    });
  });

  function renderComponent(overrides = {}) {
    return render(<IqSidebarNav {...overrides} />);
  }

  function assertNavSectionContainsLink(expectedHref, expectedText, expectedIcon) {
    const navLink = screen.getByRole('link', { name: expectedText });
    expect(navLink.getAttribute('href')).toEqual(expectedHref);

    const icon = within(navLink).getByRole('img', { hidden: true });
    expect(icon.getAttribute('data-icon')).toEqual(expectedIcon);
  }

  function assertNoLinksInNavigation() {
    const navigationSection = screen.getByRole('navigation', { name: 'global sidebar' });
    expect(within(navigationSection).queryByRole('link')).not.toBeInTheDocument();
  }

  function mockSelectedLink(stateName) {
    return { currentState: { name: stateName } };
  }

  function assertLinkPresentAndSelected(name) {
    expect(screen.getByRole('link', { name })).toHaveClass('selected');
  }
});
