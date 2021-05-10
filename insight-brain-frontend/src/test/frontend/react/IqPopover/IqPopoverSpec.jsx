/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React, { createRef } from 'react';
import * as enzymeUtils from '../../enzymeUtils';
import IqPopover from '../../../../main/frontend/react/IqPopover';

describe('IqPopover', function () {
  let getShallowComponent;
  let getMountedComponent;

  beforeEach(function () {
    const minProps = {};
    getShallowComponent = enzymeUtils.getShallowComponent(IqPopover, minProps);
    getMountedComponent = enzymeUtils.getMountedComponent(IqPopover, minProps);
  });

  it('renders with a class', () => {
    expect(getShallowComponent()).toMatchSelector('.iq-popover');
  });

  it('forwards all extra props to the root .iq-popover element', () => {
    const component = getShallowComponent({ id: 'thisone', tabIndex: 0, 'data-testid': 'bar' });
    const el = component.find('.iq-popover');
    expect(el).toHaveProp('id', 'thisone');
    expect(el).toHaveProp('tabIndex', 0);
    expect(el).toHaveProp('data-testid', 'bar');
  });

  it('merges the className attribute to the root element if className is passed as props', () => {
    const component = getShallowComponent();
    const currentClassNames = component.find('.iq-popover').prop('className');

    component.setProps({ className: 'my-class' });

    const el = component.find('.iq-popover');
    expect(el).toHaveClassName(currentClassNames);
    expect(el).toHaveClassName('my-class');
  });

  it('if passed a ref it will forward that ref to the root .iq-popover element', () => {
    const ref = createRef();
    const component = getShallowComponent({ ref });
    const el = component.find('.iq-popover');
    expect(el.get(0).ref).toBe(ref);
  });

  describe('onClose', () => {
    it('calls onClose on clickaway event', () => {
      const eventMap = {};
      spyOn(document, 'addEventListener').and.callFake((event, cb) => (eventMap[event] = cb));

      const onClose = jasmine.createSpy('onClose');
      // need mounting to trigger document.addEventlistener mock
      const wrapper = getMountedComponent({ onClose });

      // setProps to trigger useEffect that hooks up document event listener
      wrapper.setProps({});
      // Trigger fake clickaway event
      eventMap.mousedown({ target: null });

      expect(onClose).toHaveBeenCalled();
      wrapper.unmount();
    });

    it('calls onClose on escape key event', () => {
      const eventMap = {};
      spyOn(document, 'addEventListener').and.callFake((event, cb) => (eventMap[event] = cb));

      const onClose = jasmine.createSpy('onClose');
      // need mounting to trigger document.addEventlistener mock
      const wrapper = getMountedComponent({ onClose });

      // setProps to trigger useEffect that hooks up document event listener
      wrapper.setProps({});
      // Trigger fake escape key event
      eventMap.keydown({ key: 'Escape' });

      expect(onClose).toHaveBeenCalled();
      wrapper.unmount();
    });
  });

  describe('IqPopover.Header & IqPopover.Footer', () => {
    it('will render any IqPopoverHeader components passed as children BEFORE the .iq-popover__content element', () => {
      const component = shallow(
        <IqPopover>
          <IqPopover.Header>I am the header</IqPopover.Header>
          <p>I am the egg man</p>
        </IqPopover>
      );

      const el = component.find('.iq-popover');
      const content = component.find('.iq-popover__content');
      expect(content).toContainMatchingElement('p');
      expect(content).not.toContainMatchingElement(IqPopover.Header);
      expect(el.children().first()).toMatchSelector(IqPopover.Header);
    });
    it('will render any IqPopoverFooter components passed as children AFTER the .iq-popover__content element', () => {
      const component = shallow(
        <IqPopover>
          <p>I am the walrus</p>
          <IqPopover.Footer>I am the Footer</IqPopover.Footer>
        </IqPopover>
      );

      const el = component.find('.iq-popover');
      const content = component.find('.iq-popover__content');
      expect(content).toContainMatchingElement('p');
      expect(content).not.toContainMatchingElement(IqPopover.Footer);
      expect(el.children().last()).toMatchSelector(IqPopover.Footer);
    });
    it('will render all children not an IqPopoverHeader or IqPopoverFooter component INSIDE the .iq-popover__content element', () => {
      const component = shallow(
        <IqPopover>
          <IqPopover.Header>I am the header</IqPopover.Header>
          <p>Goo goo g-joob</p>
          <IqPopover.Footer>I am the Footer</IqPopover.Footer>
        </IqPopover>
      );
      const content = component.find('.iq-popover__content');
      expect(content).toContainMatchingElement('p');
      expect(content).not.toContainMatchingElement(IqPopover.Header);
      expect(content).not.toContainMatchingElement(IqPopover.Footer);
    });
  });
});
