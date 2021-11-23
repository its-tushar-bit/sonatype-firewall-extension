/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LoadWrapper from 'MainRoot/react/LoadWrapper';
import { NxCodeSnippet, NxInfoAlert, NxTextLink } from '@sonatype/react-shared-components';
import RequestWaiverPage from 'MainRoot/waivers/RequestWaiverPage';
import AddAndRequestWaiversBackButton from 'MainRoot/waivers/AddAndRequestWaiversBackButton';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import * as getBaseUrl from '../../../main/frontend/util/urlUtil';

describe('RequestWaiverPage', function () {
  let minimalProps,
    fullProps,
    loadViolationSpy,
    violationDetailsMock,
    getShallowComponent,
    getMountedComponent,
    hrefSpy,
    routerContextMock,
    baseUrl;

  beforeEach(function () {
    loadViolationSpy = jasmine.createSpy('loadViolation');

    violationDetailsMock = {
      constraintViolations: [
        {
          constraintId: 'id',
          constraintName: 'name',
          reasons: [
            {
              reason: 'reason',
              reference: {
                value: 'vulnerabilityId',
              },
            },
          ],
        },
      ],
      filename: 'componentName',
      policyViolationId: 'id',
      policyName: 'name',
    };

    baseUrl = 'localhost:8070';
    hrefSpy = jasmine.createSpy('href').and.callFake(() => 'someHref');
    routerContextMock = { href: hrefSpy };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);
    spyOn(getBaseUrl, 'getBaseUrl').and.returnValue(baseUrl);

    minimalProps = {
      loading: false,
      violationDetailsError: '',
      violationDetails: null,
      violationId: 'foo',
      loadViolation: loadViolationSpy,
      name: null,
      prevParams: {
        publicId: 'publicId',
        scanId: 'scanId',
        hash: 'hash',
      },
    };

    fullProps = {
      loading: false,
      violationDetails: violationDetailsMock,
      prevParams: {
        publicId: 'publicId',
        scanId: 'scanId',
        hash: 'hash',
      },
      name: 'prevStateName',
    };

    getShallowComponent = enzymeUtils.getShallowComponent(RequestWaiverPage, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(RequestWaiverPage, minimalProps);
  });

  describe('when violationId is null', () => {
    it('renders a LoadWrapper with an error message', function () {
      const component = getShallowComponent({ ...minimalProps, violationId: null });
      const loadWrapper = component.find(LoadWrapper);
      expect(loadWrapper).toExist();
      expect(loadWrapper).toHaveProp('error', 'No Violation ID provided.');
    });

    it('does not call `loadAddWaiverData`', function () {
      const component = getMountedComponent({ ...minimalProps, violationId: null });
      expect(loadViolationSpy).not.toHaveBeenCalled();
      component.unmount();
    });
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent()).toMatchSelector('.nx-page-main');
  });

  it('renders a page title', function () {
    const component = getShallowComponent();
    expect(component.find('.nx-page-title')).toExist();
    expect(component.find('.nx-h1')).toHaveText('Request Waiver');
  });

  it('renders a loading LoadWrapper when loading is true', function () {
    const component = getShallowComponent({ loading: true });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when the violationDetails prop is missing', function () {
    const component = getShallowComponent({ violationDetails: null });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('calls loadAddWaiverData when the LoadWrapper retryHandler is invoked', function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadViolationSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadViolationSpy).toHaveBeenCalledWith('foo');
  });

  it('calls `loadViolation` with the violationId on render', function () {
    const component = getMountedComponent();
    expect(loadViolationSpy).toHaveBeenCalledWith('foo');
    component.unmount();
  });

  it('calls `loadViolation` if the violationId changes', function () {
    const component = getMountedComponent();

    expect(loadViolationSpy).toHaveBeenCalledTimes(1);
    expect(loadViolationSpy).toHaveBeenCalledWith('foo');

    component.setProps({
      ...minimalProps,
      violationId: 'bar',
    });
    expect(loadViolationSpy).toHaveBeenCalledTimes(2);
    expect(loadViolationSpy.calls.argsFor(1)[0]).toEqual('bar');
    component.unmount();
  });

  it('does not re-call `loadViolation` when violationId stays the same', function () {
    const component = getMountedComponent();
    expect(loadViolationSpy).toHaveBeenCalledTimes(1);
    expect(loadViolationSpy).toHaveBeenCalledWith('foo');

    component.setProps({
      ...minimalProps,
      loading: true,
    });

    expect(loadViolationSpy).toHaveBeenCalledTimes(1);
    component.unmount();
  });

  it('renders a NxInfoAlert', () => {
    const component = getShallowComponent({ ...fullProps });
    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    expect(loadWrapperChildren.find(NxInfoAlert)).toExist();
  });

  it('renders Policy label section', () => {
    const component = getShallowComponent({ ...fullProps });
    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    const policyIdSection = loadWrapperChildren.find('.nx-read-only__label').at(1);
    expect(policyIdSection).toHaveText('Policy');
  });

  it('renders Constraint Name label section', () => {
    const component = getShallowComponent({ ...fullProps });
    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    const policyIdSection = loadWrapperChildren.find('.nx-read-only__label').at(2);
    expect(policyIdSection).toHaveText('Constraint Name');
  });

  it('renders Conditions label section', () => {
    const component = getShallowComponent({ ...fullProps });
    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    const policyIdSection = loadWrapperChildren.find('.nx-read-only__label').at(3);
    expect(policyIdSection).toHaveText('Conditions');
  });

  it('renders a NxCodeSnippet for policy violation id section', () => {
    const component = getShallowComponent({ ...fullProps });
    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    const policyIdSection = loadWrapperChildren.find(NxCodeSnippet).at(0);
    expect(policyIdSection).toHaveProp('label', 'Policy Violation ID');
  });

  it('renders a NxCodeSnippet for policy violation details page section', () => {
    const component = getShallowComponent({ ...fullProps });
    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    const policyIdSection = loadWrapperChildren.find(NxCodeSnippet).at(1);
    expect(policyIdSection).toHaveProp('label', 'Policy Violation Details Page');
  });

  it('renders a NxCodeSnippet for curl example section', () => {
    const component = getShallowComponent({ ...fullProps });
    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    const curlSection = loadWrapperChildren.find(NxCodeSnippet).at(2);
    expect(curlSection).toHaveProp('label', 'Curl Example');
  });

  it('renders a AddAndRequestWaiversBackButton with correct props', function () {
    let backButton = getShallowComponent().find(AddAndRequestWaiversBackButton);
    expect(backButton).toExist();
    expect(backButton).toHaveProp('violationId', 'foo');

    backButton = getShallowComponent(fullProps).find(AddAndRequestWaiversBackButton);
    expect(backButton).toExist();
    expect(backButton).toHaveProp('violationId', 'foo');
    expect(backButton).toHaveProp('prevStateName', 'prevStateName');
    expect(backButton).toHaveProp('prevParams', {
      publicId: 'publicId',
      scanId: 'scanId',
      hash: 'hash',
    });
  });

  describe('when violationDetails is not null', () => {
    let component, loadWrapperChildren;
    beforeEach(() => {
      component = getShallowComponent({ ...fullProps });
      loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    });

    it('renders Policy data section', () => {
      const policyIdSection = loadWrapperChildren.find('.nx-read-only__data').at(1);
      expect(policyIdSection).toHaveText(violationDetailsMock.policyName);
    });

    it('renders Constraint Name data section', () => {
      const policyIdSection = loadWrapperChildren.find('.nx-read-only__data').at(2);
      expect(policyIdSection).toHaveText(violationDetailsMock.constraintViolations[0].constraintName);
    });

    it('renders Conditions data section', () => {
      const policyIdSection = loadWrapperChildren.find('.nx-read-only__data').at(3);
      expect(policyIdSection).toHaveText(violationDetailsMock.constraintViolations[0].reasons[0].reason);
    });

    it('renders a NxCodeSnippet for policy violation id section', () => {
      const policyIdSection = loadWrapperChildren.find(NxCodeSnippet).at(0);
      expect(policyIdSection).toHaveProp('label', 'Policy Violation ID');
      expect(policyIdSection).toHaveProp('content', violationDetailsMock.policyViolationId);
    });

    it('renders a NxCodeSnippet for policy violation details page section', () => {
      const policyIdSection = loadWrapperChildren.find(NxCodeSnippet).at(1);

      expect(routerContext.useRouterState).toHaveBeenCalled();
      expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
        id: 'foo',
      });
      expect(policyIdSection).toHaveProp('label', 'Policy Violation Details Page');
      expect(policyIdSection).toHaveProp('content', `${baseUrl}/assets/someHref`);
    });

    it('renders a NxCodeSnippet for curl example section', () => {
      const curlSection = loadWrapperChildren.find(NxCodeSnippet).at(2);
      expect(curlSection).toHaveProp('label', 'Curl Example');
      expect(curlSection).toHaveProp(
        'content',
        'curl -X POST -u user:pass -H "Content-Type: text/plain; charset=UTF-8" /api/v2/policyWaiver/id/application --data-binary \'waiver comment (optional)\''
      );
    });

    it('renders a NxTextLink for policy violation details page url section', () => {
      const policyPageLinkSection = loadWrapperChildren.find(NxTextLink).at(1);
      expect(routerContext.useRouterState).toHaveBeenCalled();
      expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
        id: 'foo',
      });
      expect(policyPageLinkSection).toHaveProp('href', `${baseUrl}/assets/someHref`);
    });

    it('renders an input for policy violation details page url section', () => {
      const policyPageUrlSection = loadWrapperChildren.find('.iq-request-waivers-popover__link-input').at(0);
      expect(routerContext.useRouterState).toHaveBeenCalled();
      expect(hrefSpy).toHaveBeenCalledWith('sidebarView.violation', {
        id: 'foo',
      });
      expect(policyPageUrlSection).toHaveProp('value', `${baseUrl}/assets/someHref`);
      expect(policyPageUrlSection).toHaveProp('readOnly', true);
    });
  });
});
