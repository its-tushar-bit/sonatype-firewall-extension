/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import { NxLoadWrapper, NxInfoAlert, NxCodeSnippet } from '@sonatype/react-shared-components';

import RequestWaivers from '../../../main/frontend/requestWaivers/RequestWaivers';
import BackButton from '../../../main/frontend/react/BackButton';
import * as routerContext from '../../../main/frontend/react/RouterStateContext';

describe('RequestWaivers', function () {
  let minimalProps,
    getShallowComponent,
    getMountedComponent,
    loadComponentDetailsSpy,
    stateMock,
    stateGetSpy,
    policyViolationMock;

  beforeEach(function () {
    loadComponentDetailsSpy = jasmine.createSpy('loadComponentDetails');

    stateGetSpy = jasmine.createSpy('$state.get').and.returnValue({ data: { title: 'some title' } });
    stateMock = {
      get: stateGetSpy,
      href: () => {},
    };
    policyViolationMock = {
      policyViolationId: 'id',
      policyName: 'name',
      constraints: [{ constraintId: 'id', constraintName: 'name', conditions: [{ conditionReason: 'reason' }] }],
      derivedComponentName: 'derived-component-name',
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(stateMock);

    minimalProps = {
      policyViolation: null,
      loadComponentDetails: loadComponentDetailsSpy,
      loadError: null,
      isLoading: false,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(RequestWaivers, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(RequestWaivers, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('renders a back button', () => {
    const el = getShallowComponent();
    const backBtn = el.find(BackButton);
    expect(backBtn).toHaveProp('stateName', 'applicationReport.policy');
    expect(backBtn).toHaveProp('$state', stateMock);
  });

  it('calls loadComponentDetails', () => {
    const component = getMountedComponent();
    expect(loadComponentDetailsSpy).toHaveBeenCalled();
    component.unmount();
  });

  it('renders a NxInfoAlert', () => {
    expect(getShallowComponent().find(NxInfoAlert)).toExist();
  });

  it('renders Policy label section', () => {
    const el = getShallowComponent();
    const policyIdSection = el.find('.nx-read-only__label').at(1);
    expect(policyIdSection.text()).toEqual('Policy');
  });

  it('renders Constraint Name label section', () => {
    const el = getShallowComponent();
    const policyIdSection = el.find('.nx-read-only__label').at(2);
    expect(policyIdSection.text()).toEqual('Constraint Name');
  });

  it('renders Conditions label section', () => {
    const el = getShallowComponent();
    const policyIdSection = el.find('.nx-read-only__label').at(3);
    expect(policyIdSection.text()).toEqual('Conditions');
  });

  it('renders a NxCodeSnippet for policy violation id section', () => {
    const el = getShallowComponent();
    const policyIdSection = el.find(NxCodeSnippet).first();
    expect(policyIdSection).toHaveProp('label', 'Policy Violation ID');
  });

  it('renders a NxCodeSnippet for curl example section', () => {
    const el = getShallowComponent();
    const curlSection = el.find(NxCodeSnippet).last();
    expect(curlSection).toHaveProp('label', 'Curl Example');
  });

  describe('when policyViolation is not null', () => {
    let element;
    beforeEach(() => {
      element = getShallowComponent({ policyViolation: policyViolationMock });
    });

    it('renders Policy data section', () => {
      const policyIdSection = element.find('.nx-read-only__data').at(1);
      expect(policyIdSection.text()).toEqual(policyViolationMock.policyName);
    });

    it('renders Constraint Name data section', () => {
      const policyIdSection = element.find('.nx-read-only__data').at(2);
      expect(policyIdSection.text()).toEqual(policyViolationMock.constraints[0].constraintName);
    });

    it('renders Conditions data section', () => {
      const policyIdSection = element.find('.nx-read-only__data').at(3);
      expect(policyIdSection.text()).toEqual(policyViolationMock.constraints[0].conditions[0].conditionReason);
    });

    it('renders a NxCodeSnippet for policy violation id section', () => {
      const policyIdSection = element.find(NxCodeSnippet).first();
      expect(policyIdSection).toHaveProp('label', 'Policy Violation ID');
      expect(policyIdSection).toHaveProp('content', policyViolationMock.policyViolationId);
    });

    it('renders a NxCodeSnippet for curl example section', () => {
      const curlSection = element.find(NxCodeSnippet).last();
      expect(curlSection).toHaveProp('label', 'Curl Example');
      expect(curlSection).toHaveProp(
        'content',
        'curl -X POST -u user:pass -H "Content-Type: text/plain; charset=UTF-8" /api/v2/policyWaiver/id/application --data-binary \'waiver comment (optional)\''
      );
    });
  });

  describe('when there is an error loading the report', () => {
    let component;
    beforeEach(() => {
      component = getShallowComponent({ loadError: 'Mock message' });
    });

    it('renders a NxLoadWrapper component', () => {
      const el = component.find(NxLoadWrapper);
      expect(el).toExist();
      expect(el).toHaveProp('error', 'Mock message');
    });

    it('calls loadComponentDetails when the user clicks the retry button', () => {
      component.find(NxLoadWrapper).props().retryHandler();
      expect(loadComponentDetailsSpy).toHaveBeenCalled();
      component.unmount();
    });
  });

  describe('when there are pending loads', () => {
    it('renders a NxLoadWrapper component', () => {
      const el = getShallowComponent({ isLoading: true }).find(NxLoadWrapper);
      expect(el).toExist();
      expect(el).toHaveProp('loading', true);
    });
  });
});
