/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import 'jest-enzyme';
import * as enzymeUtils from '../enzymeUtils';
import AddAndRequestWaiversBackButton from 'MainRoot/waivers/AddAndRequestWaiversBackButton';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import { originNamesForAddRequestPages } from 'MainRoot/util/waiverUtils';

describe('AddAndRequestWaiversBackButtonSpec', function () {
  let minimalProps, minimalFirewallProps, getShallowComponent, routerContextMock, hrefSpy;

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
      } else if (stateName === originNamesForAddRequestPages.FIREWALL_VIOLATION_WAIVERS) {
        href = 'firewallViolationWaiversHref';
      } else if (stateName === originNamesForAddRequestPages.REPOSITORY_VIOLATION_WAIVERS) {
        href = 'repositoryViolationWaiversHref';
      }
      return href;
    });
    routerContextMock = { href: hrefSpy };
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue(routerContextMock);

    getShallowComponent = enzymeUtils.getShallowComponent(AddAndRequestWaiversBackButton, minimalProps);
  });

  // Navigated from Violation Details Popover (via app report's Component Details)
  describe('Violation Details Popover params are present (hash, scanId, publicId)', () => {
    describe('if navigated to Request Waivers Page via Violation Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Violations Details Popover`, () => {
        const component = getShallowComponent({
          ...minimalProps,
          prevStateName: originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS, {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Component Details');
        expect(component).toHaveProp('href', 'componentDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page via Security Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Legal Details Popover`, () => {
        const component = getShallowComponent({
          ...minimalProps,
          prevStateName: originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_LEGAL,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_LEGAL, {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Component Details');
        expect(component).toHaveProp('href', 'componentDetailsHrefLegal');
      });
    });

    describe('if navigated to Request Waivers Page via Legal Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Security Details Popover`, () => {
        const component = getShallowComponent({
          ...minimalProps,
          prevStateName: originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_SECURITY,
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_SECURITY, {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Component Details');
        expect(component).toHaveProp('href', 'componentDetailsHrefSecurity');
      });
    });

    describe('if navigated to Request Waivers Page via any other page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        const component = getShallowComponent({
          ...minimalProps,
          prevStateName: 'someState',
        });

        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
          id: 'violationId',
          sidebarReference: undefined,
          type: undefined,
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Violation Details');
        expect(component).toHaveProp('href', 'violationDetailsHref');
      });
    });

    it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if prevStateName is not present`, () => {
      const component = getShallowComponent({
        ...minimalProps,
        prevStateName: null,
      });

      expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
        id: 'violationId',
        sidebarReference: undefined,
        type: undefined,
      });
      expect(component).toMatchSelector(MenuBarBackButton);
      expect(component).toHaveProp('text', 'Back to Violation Details');
      expect(component).toHaveProp('href', 'violationDetailsHref');
    });
  });

  // Navigated from Violation Details Page (via Dashboard's Violations tab)
  describe('Violation Details Page params are present (sidebarReference, type)', () => {
    describe('if navigated to Request Waivers Page via Waivers for Violation page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        const component = getShallowComponent({
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
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Violation Details');
        expect(component).toHaveProp('href', 'violationDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page directly from Violation Details Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Violations Details Page`, () => {
        const component = getShallowComponent({
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
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Violation Details');
        expect(component).toHaveProp('href', 'violationDetailsHref');
      });
    });
  });

  // Navigated from a shareable link or somewhere else
  describe('hash, scanId, publicId, sidebarReference, and type are all not present', () => {
    describe('if navigated to Request Waivers Page via copy/pasted shareable URL', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        const component = getShallowComponent({ violationId: 'violationId' });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
          id: 'violationId',
          sidebarReference: undefined,
          type: undefined,
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Violation Details');
        expect(component).toHaveProp('href', 'violationDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page via Waivers for Violation page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        const component = getShallowComponent({
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
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Violation Details');
        expect(component).toHaveProp('href', 'violationDetailsHref');
      });
    });
  });

  // Navigation from Firewall and Repository Violation Details Popover (via repository Component Details)
  describe('Firewall Violation Details Popover params are present', () => {
    describe('if navigated to Request Waivers Page via Firewall Violation Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Violations Details Popover`, () => {
        const component = getShallowComponent({
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
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Component Details');
        expect(component).toHaveProp('href', 'firewallComponentDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page via Firewall Security Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Legal Details Popover`, () => {
        const component = getShallowComponent({
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
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Component Details');
        expect(component).toHaveProp('href', 'firewallComponentDetailsHrefSecurity');
      });
    });

    describe('if navigated to Request Waivers Page via Firewall Legal Details Popover/Page', () => {
      it(`renders a MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Security Details Popover`, () => {
        const component = getShallowComponent({
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
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Component Details');
        expect(component).toHaveProp('href', 'firewallComponentDetailsHrefLegal');
      });
    });
  });

  // EXTRA CASES
  it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if hash is not present`, () => {
    const component = getShallowComponent({
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
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if scanId is not present`, () => {
    const component = getShallowComponent({
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
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  it(`renders a MenuBarBackButton with title 'Back to Violation Details'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if publicId is not present`, () => {
    const component = getShallowComponent({
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
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Violation Details');
    expect(component).toHaveProp('href', 'violationDetailsHref');
  });

  describe('if navigated to Add Waivers Page via Waivers for Firewall Component Details Page - Component Details Overview', () => {
    it(`renders an MenuBarBackButton with title 'Back to Component Details'
    and navigates from the Add Waiver Page to Waivers for Firewall Component Details Page - Component Details Overview`, () => {
      const component = getShallowComponent({
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
      expect(component).toMatchSelector(MenuBarBackButton);
      expect(component).toHaveProp('text', 'Back to Component Details');
      expect(component).toHaveProp('href', 'firewallViolationWaiversHref');
    });
  });

  describe('if navigated to Add Waivers Page via Waivers for Repository Results View Component Details Page - Component Details Overview', () => {
    it(`renders an MenuBarBackButton with title 'Back to Component Details'
    and navigates from the Add Waiver Page to Waivers for Repository Results View Component Details Page - Component Details Overview`, () => {
      const component = getShallowComponent({
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
      expect(component).toMatchSelector(MenuBarBackButton);
      expect(component).toHaveProp('text', 'Back to Component Details');
      expect(component).toHaveProp('href', 'repositoryViolationWaiversHref');
    });
  });
});
