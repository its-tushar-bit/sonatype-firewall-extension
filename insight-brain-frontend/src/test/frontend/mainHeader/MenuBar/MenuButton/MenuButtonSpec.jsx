/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { mount } from 'enzyme';

import { faUserCircle } from '@fortawesome/pro-solid-svg-icons';
import * as enzymeUtils from '../../../enzymeUtils';
import { MenuButton } from '../../../../../main/frontend/mainHeader/MenuBar/MenuButton/MenuButton';

describe('MenuButton', function () {
  let minimalProps;
  let getMountedComponent;

  beforeEach(function () {
    minimalProps = {
      icon: faUserCircle,
      iconLabel: 'User Profile',
      iconSize: '2x',
      onChange: jasmine.createSpy('onChange'),
      closeOnClick: true,
    };
    getMountedComponent = enzymeUtils.getMountedComponent(MenuButton, minimalProps);
  });

  it('renders a button by default, but not the menu', () => {
    const component = getMountedComponent();
    const button = component.find('button');
    const menu = component.find('.iq-dropdown-menu');

    expect(button).toExist();
    expect(menu).not.toExist();
  });

  it('shows the menu when the button is clicked', () => {
    const component = getMountedComponent();
    const button = component.find('button');

    button.simulate('click');
    const menu = component.find('.iq-dropdown-menu');

    expect(menu).toExist();
  });

  describe('onChange', () => {
    it('will call onChange callback when the menu opens', () => {
      const onChangeSpy = jasmine.createSpy('onChangeSpy');
      const component = getMountedComponent({ onChange: onChangeSpy });
      const button = component.find('button');

      button.simulate('click');

      expect(onChangeSpy.calls.count()).toBe(1);
      expect(onChangeSpy.calls.argsFor(0)).toEqual([true]);
    });

    it('will call onChange callback when the menu closes', () => {
      const onChangeSpy = jasmine.createSpy('onChangeSpy');
      const component = getMountedComponent({ onChange: onChangeSpy });
      const button = component.find('button');

      button.simulate('click'); // open
      button.simulate('click'); // close

      expect(onChangeSpy.calls.count()).toBe(2);
      expect(onChangeSpy.calls.argsFor(1)).toEqual([false]);
    });
  });

  describe('closeOnClick', () => {
    it('will close menu after clicking anything IN the menu when true (default)', () => {
      const component = mount(
        <MenuButton {...minimalProps} closeOnClick={true}>
          <p>Hello</p>
        </MenuButton>
      );
      const button = component.find('button');
      button.simulate('click'); // open

      expect(component.find('.iq-dropdown-menu')).toExist();
      const p = component.find('p');
      p.simulate('click');

      expect(component.find('.iq-dropdown-menu')).not.toExist();
    });

    it('will NOT close menu after clicking anything IN the menu when false', () => {
      const component = mount(
        <MenuButton {...minimalProps} closeOnClick={false}>
          <p>Hello</p>
        </MenuButton>
      );
      const button = component.find('button');
      button.simulate('click'); // open

      expect(component.find('.iq-dropdown-menu')).toExist();
      const p = component.find('p');
      p.simulate('click');

      expect(component.find('.iq-dropdown-menu')).toExist();
    });
  });
});
