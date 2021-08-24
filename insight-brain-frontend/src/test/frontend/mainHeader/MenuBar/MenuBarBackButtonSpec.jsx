/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxBackButton } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';
import MenuBarBackButton from '../../../../main/frontend/mainHeader/MenuBar/MenuBarBackButton';
import * as routerContext from '../../../../main/frontend/react/RouterStateContext';

const setupPortalContainer = () => {
  const backButtonRoot = global.document.createElement('div');
  backButtonRoot.setAttribute('id', 'menu-bar__back-button-container');
  const body = global.document.querySelector('body');
  body.appendChild(backButtonRoot);
};

describe('MenuBarBackButton', function () {
  setupPortalContainer();

  let getShallowComponent, mockState, stateGetSpy, stateHrefSpy, mockData;

  beforeEach(() => {
    mockData = { data: { title: 'some page' } };
    stateGetSpy = jasmine.createSpy('$state.get').and.returnValue(mockData);
    stateHrefSpy = jasmine.createSpy('$state.href').and.returnValue('/noop');
    mockState = {
      get: stateGetSpy,
      href: stateHrefSpy,
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(mockState);

    getShallowComponent = enzymeUtils.getShallowComponent(MenuBarBackButton);
  });

  it('renders a NxBackButton with href and page title from the state, and the specified text', () => {
    const wrapper = getShallowComponent({ stateName: 'mockStateName' });
    const component = wrapper.find(NxBackButton);

    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('href', '/noop');
    expect(component).toHaveProp('targetPageTitle', 'some page');
    expect(stateGetSpy).toHaveBeenCalledWith('mockStateName');
    expect(stateHrefSpy).toHaveBeenCalledWith(mockData);
  });

  it('renders a NxBackButton with page title from the state, and href prop', () => {
    const wrapper = getShallowComponent({ stateName: 'mockStateName', href: '/noop' });
    const component = wrapper.find(NxBackButton);

    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('href', '/noop');
    expect(component).toHaveProp('targetPageTitle', 'some page');
    expect(stateGetSpy).toHaveBeenCalledWith('mockStateName');
    expect(stateHrefSpy).not.toHaveBeenCalled();
  });

  it('renders a NxBackButton with href and text prop', () => {
    const wrapper = getShallowComponent({ text: 'someText', href: '/noop' });
    const component = wrapper.find(NxBackButton);

    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('href', '/noop');
    expect(component).toHaveProp('text', 'someText');
    expect(stateGetSpy).not.toHaveBeenCalled();
    expect(stateHrefSpy).not.toHaveBeenCalled();
  });

  it('should not render a NxBackButton when no props were passed in', () => {
    const wrapper = getShallowComponent();
    const component = wrapper.find(NxBackButton);

    expect(component).not.toExist();
  });
});
