/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import AddAndRequestWaiversBackButton from 'MainRoot/waivers/AddAndRequestWaiversBackButton';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import { originNamesForAddRequestPages } from 'MainRoot/util/waiverUtils';

describe('AddAndRequestWaiversBackButtonSpec', function () {
  let renderComponent, minimalProps, minimalFirewallProps, minimalWaiverRequestProps, routerContextMock, hrefSpy;

  beforeEach(function () {
    minimalProps = {
      violationId: 'violationId',
      prevStateName: undefined,
      prevParams: {
        publicId: 'publicId',
        scanId: 'scanId',
        hash: 'hash',
      },
      isFirewall: false,
      isFirewallOrRepositoryComponent: false,
    };
    minimalFirewallProps = {
      violationId: 'violationId',
      prevStateName: undefined,
      prevParams: {
        componentDisplayName: 'componentDisplayName',
        componentHash: 'componentHash',
        componentIdentifier: 'componentIdentifier',
        matchState: 'matchState',
        pathname: 'pathname',
        proprietary: 'proprietary',
        repositoryId: 'repositoryId',
        tabId: 'tabId',
      },
      isFirewall: true,
      isFirewallOrRepositoryComponent: true,
    };
    minimalWaiverRequestProps = {
      violationId: 'violationId',
      prevStateName: undefined,
      prevParams: {
        '#': null,
      },
      isWaiverRequestReview: true,
    };
    hrefSpy = jest.fn('href').mockImplementation((stateName) => {
      let href;
      if (stateName === originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS) {
        href = 'componentDetailsHref';
      } else if (stateName === originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_LEGAL) {
        href = 'componentDetailsHrefLegal';
      } else if (stateName === originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_SECURITY) {
        href = 'componentDetailsHrefSecurity';
      } else if (
        stateName === originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS ||
        stateName === originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS
      ) {
        href = 'firewallComponentDetailsHref';
      } else if (
        stateName === originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_SECURITY ||
        stateName === originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS_SECURITY
      ) {
        href = 'firewallComponentDetailsHrefSecurity';
      } else if (
        stateName === originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_LEGAL ||
        stateName === originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS_LEGAL
      ) {
        href = 'firewallComponentDetailsHrefLegal';
      } else if (stateName === originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW) {
        href = 'violationDetailsHref';
      } else if (stateName === originNamesForAddRequestPages.DASHBOARD_WAIVERS_REQUESTS_VIEW) {
        href = 'dashboardWaiversRequestsHref';
      } else if (stateName === originNamesForAddRequestPages.FIREWALL_VIOLATION_WAIVERS) {
        href = 'firewallViolationWaiversHref';
      } else if (stateName === originNamesForAddRequestPages.REPOSITORY_VIOLATION_WAIVERS) {
        href = 'repositoryViolationWaiversHref';
      } else if (stateName === originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS) {
        href = 'componentDetailsHrefPrioritiesPageFromReports';
      } else if (stateName === originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD) {
        href = 'componentDetailsHrefPrioritiesPageFromDashboard';
      } else if (stateName === originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_SECURITY) {
        href = 'componentDetailsHrefPrioritiesPageFromReports_SecurityTab';
      } else if (
        stateName === originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_SECURITY
      ) {
        href = 'componentDetailsHrefPrioritiesPageFromDashboard_SecurityTab';
      } else if (stateName === originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_LEGAL) {
        href = 'componentDetailsHrefPrioritiesPageFromReports_LegalTab';
      } else if (
        stateName === originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_LEGAL
      ) {
        href = 'componentDetailsHrefPrioritiesPageFromDashboard_LegalTab';
      } else if (stateName === originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW) {
        href = 'nexusOneViolationDetailOverviewHref';
      } else if (stateName === originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_VULNERABILITY) {
        href = 'nexusOneViolationDetailVulnerabilityHref';
      } else if (stateName === originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_WAIVERS) {
        href = 'nexusOneViolationDetailWaiversHref';
      }
      return href;
    });
    routerContextMock = { href: hrefSpy, get: jest.fn() };
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue(routerContextMock);

    renderComponent = (props) => render(<AddAndRequestWaiversBackButton {...props} />);
  });

  // Navigated from Violation Details Popover (via app report's Component Details)
  describe('Violation Details Popover params are present (hash, scanId, publicId)', () => {
    describe('if navigated to Request Waivers Page via Violation Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Violations Details Popover`, () => {
        renderComponent({
          ...minimalProps,
          prevStateName: originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS, {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
        });

        const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'componentDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page via Security Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Legal Details Popover`, () => {
        renderComponent({
          ...minimalProps,
          prevStateName: originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_LEGAL,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_LEGAL, {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
        });

        const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'componentDetailsHrefLegal');
      });
    });

    describe('if navigated to Request Waivers Page via Legal Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Security Details Popover`, () => {
        renderComponent({
          ...minimalProps,
          prevStateName: originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_SECURITY,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_SECURITY, {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
        });

        const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'componentDetailsHrefSecurity');
      });
    });

    describe('if navigated to Request Waivers Page via any other page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        renderComponent({
          ...minimalProps,
          prevStateName: 'someState',
        });

        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
          id: 'violationId',
          sidebarReference: undefined,
          type: undefined,
        });

        const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'violationDetailsHref');
      });
    });

    it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if prevStateName is not present`, () => {
      renderComponent({
        ...minimalProps,
        prevStateName: null,
      });

      expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
        id: 'violationId',
        sidebarReference: undefined,
        type: undefined,
      });

      const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
      expect(backBtnLink).toBeInTheDocument();
      expect(backBtnLink).toHaveAttribute('href', 'violationDetailsHref');
    });
  });

  // Navigated from Violation Details Page (via Dashboard's Violations tab)
  describe('Violation Details Page params are present (sidebarReference, type)', () => {
    describe('if navigated to Request Waivers Page via Waivers for Violation page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        renderComponent({
          ...minimalProps,
          prevParams: {
            sidebarReference: 'sidebarReference',
            type: 'type',
          },
          prevStateName: originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
          id: 'violationId',
          sidebarReference: 'sidebarReference',
          type: 'type',
        });
        const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'violationDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page directly from Violation Details Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Violations Details Page`, () => {
        renderComponent({
          ...minimalProps,
          prevParams: {
            sidebarReference: 'sidebarReference',
            type: 'type',
          },
          prevStateName: originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
          id: 'violationId',
          sidebarReference: 'sidebarReference',
          type: 'type',
        });
        const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'violationDetailsHref');
      });
    });
  });

  // Navigated from a shareable link or somewhere else
  describe('hash, scanId, publicId, sidebarReference, and type are all not present', () => {
    describe('if navigated to Request Waivers Page via copy/pasted shareable URL', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        renderComponent({ violationId: 'violationId' });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
          id: 'violationId',
          sidebarReference: undefined,
          type: undefined,
        });
        const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'violationDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page via Waivers for Violation page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        renderComponent({
          violationId: 'violationId',
          prevStateName: originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW,
          prevParams: {},
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
          id: 'violationId',
          sidebarReference: undefined,
          type: undefined,
        });
        const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'violationDetailsHref');
      });
    });
  });

  // Navigation from Firewall and Repository Violation Details Popover (via repository Component Details)
  describe('Firewall Violation Details Popover params are present', () => {
    describe('if navigated to Request Waivers Page via Firewall Violation Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Violations Details Popover`, () => {
        renderComponent({
          ...minimalFirewallProps,
          prevStateName: originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS, {
          violationId: 'violationId',
          componentDisplayName: 'componentDisplayName',
          componentHash: 'componentHash',
          componentIdentifier: 'componentIdentifier',
          matchState: 'matchState',
          pathname: 'pathname',
          proprietary: 'proprietary',
          repositoryId: 'repositoryId',
          tabId: 'tabId',
        });
        const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'firewallComponentDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page via Firewall Security Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Legal Details Popover`, () => {
        renderComponent({
          ...minimalFirewallProps,
          prevStateName: originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_SECURITY,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_SECURITY, {
          violationId: 'violationId',
          componentDisplayName: 'componentDisplayName',
          componentHash: 'componentHash',
          componentIdentifier: 'componentIdentifier',
          matchState: 'matchState',
          pathname: 'pathname',
          proprietary: 'proprietary',
          repositoryId: 'repositoryId',
          tabId: 'tabId',
        });
        const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'firewallComponentDetailsHrefSecurity');
      });
    });

    describe('if navigated to Request Waivers Page via Firewall Legal Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Security Details Popover`, () => {
        renderComponent({
          ...minimalFirewallProps,
          prevStateName: originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_LEGAL,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_LEGAL, {
          violationId: 'violationId',
          componentDisplayName: 'componentDisplayName',
          componentHash: 'componentHash',
          componentIdentifier: 'componentIdentifier',
          matchState: 'matchState',
          pathname: 'pathname',
          proprietary: 'proprietary',
          repositoryId: 'repositoryId',
          tabId: 'tabId',
        });

        const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
        expect(backBtnLink).toBeInTheDocument();
        expect(backBtnLink).toHaveAttribute('href', 'firewallComponentDetailsHrefLegal');
      });
    });
  });

  // EXTRA CASES
  it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if hash is not present`, () => {
    renderComponent({
      violationId: 'violationId',
      prevStateName: originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS,
      prevParams: {
        publicId: 'publicId',
        scanId: 'scanId',
      },
    });

    expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
      id: 'violationId',
      sidebarReference: undefined,
      type: undefined,
    });

    const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
    expect(backBtnLink).toBeInTheDocument();
    expect(backBtnLink).toHaveAttribute('href', 'violationDetailsHref');
  });

  it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if scanId is not present`, () => {
    renderComponent({
      violationId: 'violationId',
      prevStateName: originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS,
      prevParams: {
        hash: 'hash',
        publicId: 'publicId',
      },
    });

    expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
      id: 'violationId',
      sidebarReference: undefined,
      type: undefined,
    });

    const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
    expect(backBtnLink).toBeInTheDocument();
    expect(backBtnLink).toHaveAttribute('href', 'violationDetailsHref');
  });

  it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if publicId is not present`, () => {
    renderComponent({
      violationId: 'violationId',
      prevStateName: originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS,
      prevParams: {
        hash: 'hash',
        scanId: 'scanId',
      },
    });

    expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
      id: 'violationId',
      sidebarReference: undefined,
      type: undefined,
    });

    const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
    expect(backBtnLink).toBeInTheDocument();
    expect(backBtnLink).toHaveAttribute('href', 'violationDetailsHref');
  });

  describe('if navigated to Add Waivers Page via Waivers for Firewall Component Details Page - Component Details Overview', () => {
    it(`renders an MenuBarBackButton with title 'Back to Component Details'
    and navigates from the Add Waiver Page to Waivers for Firewall Component Details Page - Component Details Overview`, () => {
      renderComponent({
        violationId: 'violationId',
        prevStateName: originNamesForAddRequestPages.FIREWALL_VIOLATION_WAIVERS,
        prevParams: {
          hash: 'hash',
          repositoryPolicyId: 'repositoryPolicyId',
        },
        isFirewall: true,
        isFirewallOrRepositoryComponent: true,
      });

      expect(routerContext.useRouterState).toHaveBeenCalled();
      expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.FIREWALL_VIOLATION_WAIVERS, {
        violationId: 'violationId',
        prevStateName: 'firewall.componentDetailsPage',
        prevParams: {
          hash: 'hash',
          repositoryPolicyId: 'repositoryPolicyId',
        },
        isFirewall: true,
        isFirewallOrRepositoryComponent: true,
      });

      const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
      expect(backBtnLink).toBeInTheDocument();
      expect(backBtnLink).toHaveAttribute('href', 'firewallViolationWaiversHref');
    });
  });

  describe('if navigated to Add Waivers Page via Waivers for Repository Results View Component Details Page - Component Details Overview', () => {
    it(`renders an MenuBarBackButton with title 'Back to Component Details'
    and navigates from the Add Waiver Page to Waivers for Repository Results View Component Details Page - Component Details Overview`, () => {
      renderComponent({
        violationId: 'violationId',
        prevStateName: originNamesForAddRequestPages.REPOSITORY_VIOLATION_WAIVERS,
        prevParams: {
          hash: 'hash',
          repositoryPolicyId: 'repositoryPolicyId',
        },
        isFirewall: false,
        isFirewallOrRepositoryComponent: true,
      });

      expect(routerContext.useRouterState).toHaveBeenCalled();
      expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.REPOSITORY_VIOLATION_WAIVERS, {
        violationId: 'violationId',
        prevStateName: 'repository.componentDetailsPage',
        prevParams: {
          hash: 'hash',
          repositoryPolicyId: 'repositoryPolicyId',
        },
        isFirewall: false,
        isFirewallOrRepositoryComponent: true,
      });
      const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
      expect(backBtnLink).toBeInTheDocument();
      expect(backBtnLink).toHaveAttribute('href', 'repositoryViolationWaiversHref');
    });
  });

  describe('when originated from priorities page from reports page', () => {
    describe('and clicking on a row to go to the component details page', () => {
      describe('and clicking on Policy Violations tab and clicking on Add Waiver', () => {
        it(`renders a MenuBarBackButton with title 'Back to Component Details'
          and navigates from the Add Waiver Page to the Component page`, () => {
          renderComponent({
            ...minimalProps,
            prevStateName: originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS,
          });

          expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS, {
            publicId: 'publicId',
            scanId: 'scanId',
            hash: 'hash',
          });

          const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
          expect(backBtnLink).toBeInTheDocument();
          expect(backBtnLink).toHaveAttribute('href', 'componentDetailsHrefPrioritiesPageFromReports');
        });
      });

      describe('and clicking on Security tab and clicking on Add Waiver', () => {
        it(`renders a MenuBarBackButton with title 'Back to Component Details'
          and navigates from the Add Waiver Page to the Component page`, () => {
          renderComponent({
            ...minimalProps,
            prevStateName: originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_SECURITY,
          });

          expect(hrefSpy).toHaveBeenCalledWith(
            originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_SECURITY,
            {
              publicId: 'publicId',
              scanId: 'scanId',
              hash: 'hash',
            }
          );

          const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
          expect(backBtnLink).toBeInTheDocument();
          expect(backBtnLink).toHaveAttribute('href', 'componentDetailsHrefPrioritiesPageFromReports_SecurityTab');
        });
      });

      describe('and clicking on Legal tab and clicking on Add Waiver', () => {
        it(`renders a MenuBarBackButton with title 'Back to Component Details'
          and navigates from the Add Waiver Page to the Component page`, () => {
          renderComponent({
            ...minimalProps,
            prevStateName: originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_LEGAL,
          });

          expect(hrefSpy).toHaveBeenCalledWith(
            originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_LEGAL,
            {
              publicId: 'publicId',
              scanId: 'scanId',
              hash: 'hash',
            }
          );

          const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
          expect(backBtnLink).toBeInTheDocument();
          expect(backBtnLink).toHaveAttribute('href', 'componentDetailsHrefPrioritiesPageFromReports_LegalTab');
        });
      });
    });
  });

  describe('when originated from priorities page from developer dashboard', () => {
    describe('and clicking on a row to go to the component details page', () => {
      describe('and clicking on Policy Violations tab and clicking on Add Waiver', () => {
        it(`renders a MenuBarBackButton with title 'Back to Component Details'
          and navigates from the Add Waiver Page to the Component page`, () => {
          renderComponent({
            ...minimalProps,
            prevStateName: originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD,
          });

          expect(hrefSpy).toHaveBeenCalledWith(
            originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD,
            {
              publicId: 'publicId',
              scanId: 'scanId',
              hash: 'hash',
            }
          );

          const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
          expect(backBtnLink).toBeInTheDocument();
          expect(backBtnLink).toHaveAttribute('href', 'componentDetailsHrefPrioritiesPageFromDashboard');
        });
      });

      describe('and clicking on Security tab and clicking on Add Waiver', () => {
        it(`renders a MenuBarBackButton with title 'Back to Component Details'
          and navigates from the Add Waiver Page to the Component page`, () => {
          renderComponent({
            ...minimalProps,
            prevStateName: originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_SECURITY,
          });

          expect(hrefSpy).toHaveBeenCalledWith(
            originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_SECURITY,
            {
              publicId: 'publicId',
              scanId: 'scanId',
              hash: 'hash',
            }
          );

          const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
          expect(backBtnLink).toBeInTheDocument();
          expect(backBtnLink).toHaveAttribute('href', 'componentDetailsHrefPrioritiesPageFromDashboard_SecurityTab');
        });
      });

      describe('and clicking on Legal tab and clicking on Add Waiver', () => {
        it(`renders a MenuBarBackButton with title 'Back to Component Details'
          and navigates from the Add Waiver Page to the Component page`, () => {
          renderComponent({
            ...minimalProps,
            prevStateName: originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_LEGAL,
          });

          expect(hrefSpy).toHaveBeenCalledWith(
            originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_LEGAL,
            {
              publicId: 'publicId',
              scanId: 'scanId',
              hash: 'hash',
            }
          );

          const backBtnLink = screen.getByRole('link', { name: 'Back to Component Details' });
          expect(backBtnLink).toBeInTheDocument();
          expect(backBtnLink).toHaveAttribute('href', 'componentDetailsHrefPrioritiesPageFromDashboard_LegalTab');
        });
      });
    });
  });

  describe('Navigated to Request Waivers Review Page', () => {
    it(`renders a MenuBarBackButton with title 'Back to Waiver Requests'
    and navigates from the Request Waiver Review Page to the Dashboard Requested Waivers tab`, () => {
      renderComponent({
        ...minimalWaiverRequestProps,
        prevStateName: originNamesForAddRequestPages.DASHBOARD_WAIVERS_REQUESTS_VIEW,
      });

      expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_WAIVERS_REQUESTS_VIEW, {
        violationId: 'violationId',
        prevStateName: 'dashboard.overview.waiverRequests',
        prevParams: {
          '#': null,
        },
        isWaiverRequestReview: true,
      });

      const backBtnLink = screen.getByRole('link', { name: 'Back to Waiver Requests' });
      expect(backBtnLink).toBeInTheDocument();
      expect(backBtnLink).toHaveAttribute('href', 'dashboardWaiversRequestsHref');
    });

    it(`renders a MenuBarBackButton with title 'Back to Waiver Requests'
    and navigates from the Request Waiver Review Page to the Dashboard Requested Waivers tab
    if prevStateName is not present`, () => {
      renderComponent({
        ...minimalWaiverRequestProps,
        prevStateName: null,
      });

      expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_WAIVERS_REQUESTS_VIEW, {
        violationId: 'violationId',
        prevStateName: null,
        prevParams: {
          '#': null,
        },
        isWaiverRequestReview: true,
      });

      const backBtnLink = screen.getByRole('link', { name: 'Back to Waiver Requests' });
      expect(backBtnLink).toBeInTheDocument();
      expect(backBtnLink).toHaveAttribute('href', 'dashboardWaiversRequestsHref');
    });
  });

  describe('when originated from the Nexus One violation detail page', () => {
    it(`renders a MenuBarBackButton with title 'Back to Violation Details'
    and navigates to the Nexus One violation overview state (not the Classic sidebarView.violation state)
    when prevStateName is nexusOneViolationDetail.overview`, () => {
      renderComponent({
        violationId: 'violationId',
        prevStateName: originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW,
        prevParams: {
          type: 'type',
          sidebarReference: 'sidebarReference',
          sidebarId: 'sidebarId',
          page: 'page',
        },
      });

      expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW, {
        id: 'violationId',
        type: 'type',
        sidebarReference: 'sidebarReference',
        sidebarId: 'sidebarId',
        page: 'page',
      });
      expect(hrefSpy).not.toHaveBeenCalledWith(
        originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW,
        expect.anything()
      );

      const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
      expect(backBtnLink).toBeInTheDocument();
      expect(backBtnLink).toHaveAttribute('href', 'nexusOneViolationDetailOverviewHref');
    });

    it(`renders a MenuBarBackButton that navigates back to the vulnerability tab
    when prevStateName is nexusOneViolationDetail.vulnerability`, () => {
      renderComponent({
        violationId: 'violationId',
        prevStateName: originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_VULNERABILITY,
        prevParams: {},
      });

      expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_VULNERABILITY, {
        id: 'violationId',
        type: undefined,
        sidebarReference: undefined,
        sidebarId: undefined,
        page: undefined,
      });

      const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
      expect(backBtnLink).toHaveAttribute('href', 'nexusOneViolationDetailVulnerabilityHref');
    });

    it(`renders a MenuBarBackButton that navigates back to the waivers tab
    when prevStateName is nexusOneViolationDetail.waivers`, () => {
      renderComponent({
        violationId: 'violationId',
        prevStateName: originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_WAIVERS,
        prevParams: {},
      });

      expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_WAIVERS, {
        id: 'violationId',
        type: undefined,
        sidebarReference: undefined,
        sidebarId: undefined,
        page: undefined,
      });

      const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
      expect(backBtnLink).toHaveAttribute('href', 'nexusOneViolationDetailWaiversHref');
    });

    it(`normalizes the abstract nexusOneViolationDetail parent state to its overview child
    (the abstract state itself cannot be navigated to)`, () => {
      renderComponent({
        violationId: 'violationId',
        prevStateName: originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL,
        prevParams: {},
      });

      expect(hrefSpy).toHaveBeenCalledWith(
        originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW,
        expect.objectContaining({ id: 'violationId' })
      );
      expect(hrefSpy).not.toHaveBeenCalledWith(
        originNamesForAddRequestPages.NEXUS_ONE_VIOLATION_DETAIL,
        expect.anything()
      );

      const backBtnLink = screen.getByRole('link', { name: 'Back to Violation Details' });
      expect(backBtnLink).toHaveAttribute('href', 'nexusOneViolationDetailOverviewHref');
    });
  });
});
