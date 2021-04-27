/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../../../enzymeUtils';
import { mount } from 'enzyme';
import { NavLink } from '../../../../../main/frontend/mainHeader/MenuBar/MenuButton/NavLink';
import { RouterStateProvider } from '../../../../../main/frontend/react/RouterStateContext';

describe('NavLink', function () {
  const mockUIRouterState = { href: () => null, includes: () => false };
  let getShallowComponent;

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(NavLink, { children: 'Foo' });
  });

  it('Renders children as link content', function () {
    const component = getShallowComponent();
    expect(component.find('a').length).toBe(1);
    expect(component.children(0).text()).toBe('Foo');
  });

  it('renders nothing if showIf prop is false', () => {
    const I_HAVE_PERMISSION = false;
    const component = getShallowComponent({ showIf: I_HAVE_PERMISSION });
    expect(component.find('a').length).toBe(0);
    expect(component.children(0).length).toBe(0);
  });

  it('renders with a `disabled` className if `disabled` prop is true', () => {
    const defaultComponent = getShallowComponent();
    expect(defaultComponent.find('a')).not.toHaveClassName('disabled');

    const component = getShallowComponent({ disabled: true });
    expect(component.find('a')).toHaveClassName('disabled');
  });

  it('renders with an `active` className if `stateName` matches the current UI-Router state', () => {
    const mock = { ...mockUIRouterState, includes: (stateName) => stateName === 'my.state' };
    const component = mount(
      <RouterStateProvider value={mock}>
        <NavLink stateName="my.state">Foo</NavLink>
      </RouterStateProvider>
    );
    expect(component.find('a')).toHaveClassName('active');
  });

  it('does NOT render with an `active` className if `stateName` does not match the current UI-Router state', () => {
    const mock = { ...mockUIRouterState, includes: (stateName) => stateName === 'not.a.match' };
    const component = mount(
      <RouterStateProvider value={mock}>
        <NavLink stateName="my.state">Foo</NavLink>
      </RouterStateProvider>
    );
    expect(component.find('a')).not.toHaveClassName('active');
  });

  it('should render an a tag with the attributes target=_blank and rel=noreferrer if `openInNewTab` prop is true', () => {
    const defaultComponent = getShallowComponent();
    expect(defaultComponent.find('a')).not.toHaveProp('target');
    expect(defaultComponent.find('a')).not.toHaveProp('rel');

    const component = getShallowComponent({ openInNewTab: true });
    expect(component.find('a')).toHaveProp('target', '_blank');
    expect(component.find('a')).toHaveProp('rel', 'noreferrer');
  });

  describe('href', () => {
    const mock = {
      ...mockUIRouterState,
      href: (stateName) => (stateName === 'my.state' ? '/my/state' : '#/another/state'),
    };

    it('renders with the href provided by the Router based on the `stateName` prop', () => {
      const component = mount(
        <RouterStateProvider value={mock}>
          <NavLink stateName="my.state">Foo</NavLink>
          <NavLink stateName="another.state">Foo</NavLink>
        </RouterStateProvider>
      );
      expect(component.find('a').at(0)).toHaveProp('href', '/my/state');
      expect(component.find('a').at(1)).toHaveProp('href', '#/another/state');
    });

    it('renders an a tag with `href` prop passed if no `stateName` prop is passed', () => {
      const component = getShallowComponent({ href: 'https://example.com' });
      expect(component.find('a')).toHaveProp('href', 'https://example.com');
    });

    it('renders an a tag with `href` provided by the Router if both `href` and `stateName` props are passed', () => {
      const component = mount(
        <RouterStateProvider value={mock}>
          <NavLink stateName="my.state" href="https://somewhere.else.com">
            Foo
          </NavLink>
        </RouterStateProvider>
      );
      expect(component.find('a')).toHaveProp('href', '/my/state');
    });
  });

  it('renders a with `disabled` className if `disabled` prop is true', () => {
    const component = getShallowComponent({ disabled: true });
    expect(component.find('a')).toHaveClassName('disabled');
  });

  it('renders a element with `href` prop undefined if `disabled` prop is true', () => {
    const component = getShallowComponent({ href: 'https://example.com', disabled: true });
    expect(component.find('a').prop('href')).toBeUndefined();
  });
});
