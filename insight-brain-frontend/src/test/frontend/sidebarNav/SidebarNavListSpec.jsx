/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import LoadWrapper from 'MainRoot/react/LoadWrapper';
import { always } from 'ramda';
import React from 'react';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';

describe('SidebarNavList', function () {
  let minimalProps,
    loadSidebarNavSpy,
    gotoNewVulnerabilitySpy,
    SidebarNavViolationListMock,
    gotoWaiverSpy,
    SidebarNavWaiversListMock,
    SidebarNavList,
    getShallowComponent,
    getMountedComponent;

  describe('violations', function () {
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

    it('renders a MenuBarBackButton with the supplied stateName', function () {
      const menuBarBackButtonComponent = getShallowComponent({
        ...minimalProps,
        backButtonStateName: 'foo.bar.baz',
      }).find(MenuBarBackButton);

      expect(menuBarBackButtonComponent).toExist();
      expect(menuBarBackButtonComponent).toHaveProp('stateName', 'foo.bar.baz');
    });

    it('does not render a MenuBarBackButton if stateName is not in the props', function () {
      const backButtonComponent = getShallowComponent().find(MenuBarBackButton);
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

  describe('waivers', function () {
    beforeEach(function () {
      SidebarNavWaiversListMock = jasmine
        .createSpy('SidebarNavWaiversListMock')
        .and.returnValue(<div>SidebarNavViolationList</div>);

      SidebarNavList = require('inject-loader!../../../main/frontend/sidebarNav/SidebarNavList')({
        './SidebarNavWaiversList': SidebarNavWaiversListMock,
      }).default;

      loadSidebarNavSpy = jasmine.createSpy('loadSidebarNav');
      gotoWaiverSpy = jasmine.createSpy('gotoWaiver');

      minimalProps = {
        stateParams: {
          waiverId: '35513cecc0214e0cb0207238dc1fba6e',
          sidebarId: 'foo',
          type: 'waiver',
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
        gotoWaiver: gotoWaiverSpy,
        loading: false,
        error: null,
        data: [
          {
            id: '35513cecc0214e0cb0207238dc1fba6e',
            threatLevel: 7,
            policyId: '67a74447c2bf4c53b8e26f93b16ad4ee',
            policyName: 'Component-Similar',
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            componentMatchStrategy: 'ALL_VERSIONS',
            displayName: 'org.sonatype.nexus : nexus-rest-client',
          },
        ],
        contentType: 'waivers',
      };

      getShallowComponent = enzymeUtils.getShallowComponent(SidebarNavList, minimalProps);
      getMountedComponent = enzymeUtils.getMountedComponent(SidebarNavList, minimalProps);
    });

    it('properly renders a SidebarNavWaiversList component if the contentType is waivers', function () {
      const getLoadWrapper = (props) => getShallowComponent(props).find(LoadWrapper);
      const data = [
        {
          id: '35513cecc0214e0cb0207238dc1fba6e',
          threatLevel: 7,
          policyId: '67a74447c2bf4c53b8e26f93b16ad4ee',
          policyName: 'Component-Similar',
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          componentMatchStrategy: waiverMatcherStrategy.ALL_VERSIONS,
          displayName: 'org.sonatype.nexus : nexus-rest-client',
        },
        {
          id: 'bbb045cb733d4868bd6d30e4384e19f4',
          threatLevel: 9,
          policyId: '358f08a34c7b47739f6962b35b84fbea',
          policyName: 'Security-High',
          ownerId: '79e2b6864a4d4f5fbce461cf930c3f2c',
          ownerName: 'unprotected zip big java app',
          ownerType: 'application',
          componentMatchStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
          displayName: 'commons-beanutils : commons-beanutils : 1.8.3',
        },
      ];
      const loadWrapper = getLoadWrapper({
        contentType: 'waivers',
        gotoWaiver: gotoWaiverSpy,
        data,
      });
      expect(loadWrapper.find(SidebarNavWaiversListMock)).toHaveProp(
        'currentWaiverId',
        '35513cecc0214e0cb0207238dc1fba6e'
      );
      expect(loadWrapper.find(SidebarNavWaiversListMock)).toHaveProp('waivers', data);
      expect(loadWrapper.find(SidebarNavWaiversListMock)).toHaveProp('onClick', gotoWaiverSpy);
    });
  });
});
