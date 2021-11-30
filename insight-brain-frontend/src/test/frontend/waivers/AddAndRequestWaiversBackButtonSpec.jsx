/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import AddAndRequestWaiversBackButton from 'MainRoot/waivers/AddAndRequestWaiversBackButton';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import * as routerContext from 'MainRoot/react/RouterStateContext';

describe('AddAndRequestWaiversBackButtonSpec', function () {
  let minimalProps, getShallowComponent, routerContextMock, hrefSpy;

  beforeEach(function () {
    minimalProps = {
      violationId: 'violationId',
      prevStateName: undefined,
      prevParams: {
        publicId: 'publicId',
        scanId: 'scanId',
        hash: 'hash',
      },
    };
    hrefSpy = jasmine.createSpy('href').and.callFake((stateName) => {
      let href;
      if (stateName === 'applicationReport.componentDetails.violations') {
        href = 'componentDetailsHref';
      } else if (stateName === 'applicationReport.violationWaivers') {
        href = 'listWaiversComponentDetailsHref';
      } else if (stateName === 'listWaivers') {
        href = 'listWaiversViolationDetailsHref';
      }
      return href;
    });
    routerContextMock = { href: hrefSpy };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);

    getShallowComponent = enzymeUtils.getShallowComponent(AddAndRequestWaiversBackButton, minimalProps);
  });

  describe('hash, scanId and publicId props are not present', () => {
    describe('if navigated to Request Waivers Page via copy/pasted shareable URL', () => {
      it(`renders an MenuBarBackButton with title 'Back to Waivers'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        const component = getShallowComponent({ violationId: 'violationId' });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith('listWaivers', {
          violationId: 'violationId',
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Waivers');
        expect(component).toHaveProp('href', 'listWaiversViolationDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page via Waivers for Violation page', () => {
      it(`renders an MenuBarBackButton with title 'Back to Waivers'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        const component = getShallowComponent({
          violationId: 'violationId',
          prevStateName: 'listWaivers',
          prevParams: {},
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith('listWaivers', {
          violationId: 'violationId',
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Waivers');
        expect(component).toHaveProp('href', 'listWaiversViolationDetailsHref');
      });
    });
  });

  describe('hash, scanId and publicId props are all present', () => {
    describe('if navigated to Request Waivers Page via Waivers for Violation page', () => {
      it(`renders an MenuBarBackButton with title 'Back to Waivers'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        const component = getShallowComponent({ ...minimalProps, prevStateName: 'applicationReport.violationWaivers' });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith('applicationReport.violationWaivers', {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Waivers');
        expect(component).toHaveProp('href', 'listWaiversComponentDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page via Violation Details Popover/Page', () => {
      it(`renders an MenuBarBackButton with title 'Back to Component Details'
      and navigates from the Request Waiver Page to Violations Details Popover`, () => {
        const component = getShallowComponent({
          ...minimalProps,
          prevStateName: 'applicationReport.componentDetails.violations',
        });

        expect(routerContext.useRouterState).toHaveBeenCalled();
        expect(hrefSpy).toHaveBeenCalledWith('applicationReport.componentDetails.violations', {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Component Details');
        expect(component).toHaveProp('href', 'componentDetailsHref');
      });
    });

    describe('if navigated to Request Waivers Page via any other page', () => {
      it(`renders an MenuBarBackButton with title 'Back to Waivers'
      and navigates from the Request Waiver Page to Waivers for Violation page`, () => {
        const component = getShallowComponent({
          ...minimalProps,
          prevStateName: 'someState',
        });

        expect(hrefSpy).toHaveBeenCalledWith('listWaivers', {
          violationId: 'violationId',
        });
        expect(component).toMatchSelector(MenuBarBackButton);
        expect(component).toHaveProp('text', 'Back to Waivers');
        expect(component).toHaveProp('href', 'listWaiversViolationDetailsHref');
      });
    });

    it(`renders an MenuBarBackButton with title 'Back to Waivers'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if prevStateName is not present`, () => {
      const component = getShallowComponent({
        ...minimalProps,
        prevStateName: null,
      });

      expect(hrefSpy).toHaveBeenCalledWith('listWaivers', {
        violationId: 'violationId',
      });
      expect(component).toMatchSelector(MenuBarBackButton);
      expect(component).toHaveProp('text', 'Back to Waivers');
      expect(component).toHaveProp('href', 'listWaiversViolationDetailsHref');
    });
  });

  it(`renders an MenuBarBackButton with title 'Back to Waivers'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if hash is not present`, () => {
    const component = getShallowComponent({
      violationId: 'violationId',
      prevStateName: 'applicationReport.componentDetails.violations',
      prevParams: {
        publicId: 'publicId',
        scanId: 'scanId',
      },
    });

    expect(hrefSpy).toHaveBeenCalledWith('listWaivers', {
      violationId: 'violationId',
    });
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Waivers');
    expect(component).toHaveProp('href', 'listWaiversViolationDetailsHref');
  });

  it(`renders an MenuBarBackButton with title 'Back to Waivers'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if scanId is not present`, () => {
    const component = getShallowComponent({
      violationId: 'violationId',
      prevStateName: 'applicationReport.componentDetails.violations',
      prevParams: {
        hash: 'hash',
        publicId: 'publicId',
      },
    });

    expect(hrefSpy).toHaveBeenCalledWith('listWaivers', {
      violationId: 'violationId',
    });
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Waivers');
    expect(component).toHaveProp('href', 'listWaiversViolationDetailsHref');
  });

  it(`renders an MenuBarBackButton with title 'Back to Waivers'
      and navigates from the Request Waiver Page to Waivers for Violation page
      if publicId is not present`, () => {
    const component = getShallowComponent({
      violationId: 'violationId',
      prevStateName: 'applicationReport.componentDetails.violations',
      prevParams: {
        hash: 'hash',
        scanId: 'scanId',
      },
    });

    expect(hrefSpy).toHaveBeenCalledWith('listWaivers', {
      violationId: 'violationId',
    });
    expect(component).toMatchSelector(MenuBarBackButton);
    expect(component).toHaveProp('text', 'Back to Waivers');
    expect(component).toHaveProp('href', 'listWaiversViolationDetailsHref');
  });
});
