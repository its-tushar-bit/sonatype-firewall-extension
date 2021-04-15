/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import BackButton from '../../../main/frontend/react/BackButton';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import { always } from 'ramda';
import React from 'react';

describe('SidebarNavList', function () {
  let minimalProps,
    loadSidebarNavSpy,
    gotoNewVulnerabilitySpy,
    SidebarNavViolationListMock,
    SidebarNavList,
    getShallowComponent,
    getMountedComponent;

  beforeEach(function () {
    SidebarNavViolationListMock = jasmine
      .createSpy('SidebarNavViolationListMock')
      .and.returnValue(<div>SidebarNavViolationList</div>);

    SidebarNavList = require('inject-loader!../../../main/frontend/sidebarNav/SidebarNavList')({
      './SidebarNavViolationList': SidebarNavViolationListMock,
    }).default;

    loadSidebarNavSpy = jasmine.createSpy('loadSidebarNav');
    gotoNewVulnerabilitySpy = jasmine.createSpy('gotoNewVulnerability');

    minimalProps = {
      stateParams: {
        id: '123456',
        sidebarId: 'foo',
        type: 'violation',
        sidebarReference: 'filter',
      },
      $state: {
        get: always({
          data: {
            title: 'asdf',
          },
        }),
        href: always('qwerty'),
      },
      loadSidebarNav: loadSidebarNavSpy,
      gotoNewVulnerability: gotoNewVulnerabilitySpy,
      loading: false,
      error: null,
      data: [
        {
          policyViolationId: 'aaa',
          threatLevel: 1,
          policyName: 'fooName',
        },
      ],
      contentType: 'violations',
    };

    getShallowComponent = enzymeUtils.getShallowComponent(SidebarNavList, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(SidebarNavList, minimalProps);
  });

  it('renders an aside component with the sidebar-nav-list id', function () {
    expect(getShallowComponent()).toMatchSelector('aside');
    expect(getShallowComponent()).toHaveProp('id', 'sidebar-nav-list');
  });

  it('renders a BackButton using the supplied $state and stateName', function () {
    const backButtonComponent = getShallowComponent({
      ...minimalProps,
      backButtonStateName: 'foo.bar.baz',
    }).find(BackButton);
    expect(backButtonComponent).toHaveProp('$state', minimalProps.$state);
    expect(backButtonComponent).toHaveProp('stateName', 'foo.bar.baz');
  });

  it('does not render a BackButton if backButtonStateName is not in the props', function () {
    const backButtonComponent = getShallowComponent().find(BackButton);
    expect(backButtonComponent).not.toExist();
  });

  it('renders a LoadWrapper', function () {
    expect(getShallowComponent().find(LoadWrapper)).toExist();
  });

  it("sets the LoadWrapper's loading flag based on the loading prop", function () {
    const getLoadWrapper = (props) => getShallowComponent(props).find(LoadWrapper);

    expect(getLoadWrapper({ loading: false })).toHaveProp('loading', false);
    expect(getLoadWrapper({ loading: true })).toHaveProp('loading', true);
  });

  it("sets the LoadWrapper's error flag based on the error prop", function () {
    const getLoadWrapper = (props) => getShallowComponent(props).find(LoadWrapper);

    expect(getLoadWrapper({ error: 'error' })).toHaveProp('error', 'error');
    expect(getLoadWrapper({ error: null })).toHaveProp('error', null);
  });

  it("sets the LoadWrapper's retryHandler to a function that calls loadSidebarNav", function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadSidebarNavSpy).toHaveBeenCalledTimes(0);

    retryHandler();

    expect(loadSidebarNavSpy).toHaveBeenCalledTimes(1);
    expect(loadSidebarNavSpy).toHaveBeenCalledWith(minimalProps.stateParams);
  });

  it('calls loadViolation with the value of the stateParams object on first load', function () {
    getMountedComponent();

    expect(loadSidebarNavSpy).toHaveBeenCalledWith({
      id: '123456',
      sidebarId: 'foo',
      type: 'violation',
      sidebarReference: 'filter',
    });
  });

  it('calls loadViolation if the sidebarId, sidebarReference or type on the stateParams object changes', function () {
    const component = getMountedComponent();

    expect(loadSidebarNavSpy).toHaveBeenCalledTimes(1);
    expect(loadSidebarNavSpy).toHaveBeenCalledWith({
      id: '123456',
      sidebarId: 'foo',
      type: 'violation',
      sidebarReference: 'filter',
    });

    component.setProps({
      ...minimalProps,
      stateParams: {
        id: '123456',
        sidebarId: 'bar',
        type: 'violation',
        sidebarReference: 'filter',
      },
    });
    expect(loadSidebarNavSpy).toHaveBeenCalledTimes(2);
    expect(loadSidebarNavSpy.calls.argsFor(1)[0]).toEqual({
      id: '123456',
      sidebarId: 'bar',
      type: 'violation',
      sidebarReference: 'filter',
    });

    component.setProps({
      ...minimalProps,
      stateParams: {
        id: '123456',
        sidebarId: 'bar',
        type: 'newViolation',
        sidebarReference: 'filter',
      },
    });
    expect(loadSidebarNavSpy).toHaveBeenCalledTimes(3);
    expect(loadSidebarNavSpy.calls.argsFor(2)[0]).toEqual({
      id: '123456',
      sidebarId: 'bar',
      type: 'newViolation',
      sidebarReference: 'filter',
    });

    component.setProps({
      ...minimalProps,
      stateParams: {
        id: '123456',
        sidebarId: 'bar',
        type: 'newViolation',
        sidebarReference: 'newFilter',
      },
    });
    expect(loadSidebarNavSpy).toHaveBeenCalledTimes(4);
    expect(loadSidebarNavSpy.calls.argsFor(3)[0]).toEqual({
      id: '123456',
      sidebarId: 'bar',
      type: 'newViolation',
      sidebarReference: 'newFilter',
    });
  });

  it('does not re-call loadViolation if other attributes (like id) of the $state param object change', function () {
    const component = getMountedComponent();

    expect(loadSidebarNavSpy).toHaveBeenCalledTimes(1);
    expect(loadSidebarNavSpy).toHaveBeenCalledWith({
      id: '123456',
      sidebarId: 'foo',
      type: 'violation',
      sidebarReference: 'filter',
    });

    component.setProps({
      $state: {
        ...minimalProps.$state,
        params: {
          sidebarId: 'foo',
          type: 'violation',
          sidebarReference: 'filter',
          id: '987654',
        },
      },
    });
    expect(loadSidebarNavSpy).toHaveBeenCalledTimes(1);
  });

  it('renders the correct div and h4 elements within the LoadWrapper', function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper);

    const wrappingDiv = loadWrapper.find('div');
    expect(wrappingDiv).toHaveClassName('nx-scrollable');
    expect(wrappingDiv).toHaveClassName('nx-scrollable--nav-list');
    const sidebarTitle = loadWrapper.find('h4');
    expect(sidebarTitle).toMatchSelector('.nx-h4');
    expect(sidebarTitle.text()).toEqual('violations');
  });

  it('properly renders a SidebarNavViolationList component if the contentType is violations', function () {
    const getLoadWrapper = (props) => getShallowComponent(props).find(LoadWrapper);
    const data = [
      {
        policyViolationId: 'aaa',
        threatLevel: 1,
        policyName: 'fooName',
      },
      {
        policyViolationId: 'bbb',
        threatLevel: 2,
        policyName: 'barName',
      },
    ];
    const loadWrapper = getLoadWrapper({
      contentType: 'violations',
      gotoNewVulnerability: gotoNewVulnerabilitySpy,
      data,
    });
    expect(loadWrapper.find(SidebarNavViolationListMock)).toHaveProp('currentViolationId', '123456');
    expect(loadWrapper.find(SidebarNavViolationListMock)).toHaveProp('violations', data);
    expect(loadWrapper.find(SidebarNavViolationListMock)).toHaveProp('onClick', gotoNewVulnerabilitySpy);
  });
});
