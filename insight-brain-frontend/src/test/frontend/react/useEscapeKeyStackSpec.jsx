/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import * as enzymeUtils from '../../frontend/enzymeUtils';
import useEscapeKeyStack from '../../../main/frontend/react/useEscapeKeyStack';

function HookWrapper({ isListening, callback }) {
  useEscapeKeyStack(isListening, callback);
  return <div>Hook Wrapper</div>;
}

HookWrapper.propTypes = {
  callback: PropTypes.func,
  isListening: PropTypes.bool,
};

describe('useEscapeKeyStack', function () {
  let getMountedComponent, callback;

  beforeEach(function () {
    callback = jasmine.createSpy('callback');
    getMountedComponent = enzymeUtils.getMountedComponent(HookWrapper, {
      callback,
      isListening: true,
    });
    spyOn(document, 'addEventListener');
    spyOn(document, 'removeEventListener');
  });

  it('adds keydown listener for event bubbling phase', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    const [eventType, , useCapture] = document.addEventListener.calls.argsFor(
      0
    );
    expect(eventType).toBe('keydown');
    expect(useCapture).toBeUndefined();
    wrapper.unmount();
  });

  it('removes keydown listener for event bubbling phase when unmounted', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    const listener = document.addEventListener.calls.argsFor(0)[1];
    wrapper.unmount();
    expect(document.removeEventListener).toHaveBeenCalled();
    expect(document.removeEventListener.calls.argsFor(0)).toEqual([
      'keydown',
      listener,
    ]);
  });

  it('invokes callback on Escape keydown event', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    const listener = document.addEventListener.calls.argsFor(0)[1];
    listener({ key: 'Escape' });
    expect(callback).toHaveBeenCalled();
    wrapper.unmount();
  });

  it('invokes callback on Esc keydown event', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    const listener = document.addEventListener.calls.argsFor(0)[1];
    listener({ key: 'Esc' });
    expect(callback).toHaveBeenCalled();
    wrapper.unmount();
  });

  it('does not invoke callback on non Escape keydown event', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    const listener = document.addEventListener.calls.argsFor(0)[1];
    listener({ key: '3' });
    expect(callback).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it('does not add addEventListener when component is not listening', function () {
    const wrapper = getMountedComponent({
      isListening: false,
    });
    expect(document.addEventListener).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it('invokes callbacks in proper order', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    expect(document.addEventListener.calls.count()).toBe(1);
    const listener = document.addEventListener.calls.argsFor(0)[1];

    const callback2 = jasmine.createSpy('callback2');
    const wrapper2 = getMountedComponent({
      callback: callback2,
    });
    // should not add another eventListener to the document
    expect(document.addEventListener.calls.count()).toBe(1);

    // on first Escape event should call the callback provided by last mounted component
    listener({ key: 'Escape' });
    expect(callback).not.toHaveBeenCalled();
    expect(callback2).toHaveBeenCalled();

    // should not remove eventListener from the document when not all the components unmounted
    wrapper2.unmount();
    expect(document.removeEventListener).not.toHaveBeenCalled();

    // on second Escape event should call the callback provided by first mounted component
    listener({ key: 'Escape' });
    expect(callback).toHaveBeenCalled();

    // should remove event listener when all the components unmounted
    wrapper.unmount();
    expect(document.removeEventListener).toHaveBeenCalled();
  });

  it('uses latest callback when component updates', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    expect(document.addEventListener.calls.count()).toBe(1);
    const listener = document.addEventListener.calls.argsFor(0)[1];

    // on first Escape event should call the callback
    listener({ key: 'Escape' });
    expect(callback).toHaveBeenCalled();
    expect(callback.calls.count()).toBe(1);

    // update component with new callback
    const callback2 = jasmine.createSpy('callback2');
    wrapper.setProps({ callback: callback2 });

    // on second Escape event should call the new callback
    listener({ key: 'Escape' });
    expect(callback.calls.count()).toBe(1);
    expect(callback2).toHaveBeenCalled();

    wrapper.unmount();
  });

  it('does not call useEffect when component re-renders', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    expect(document.addEventListener.calls.count()).toBe(1);

    // update component so it re-renders
    const callback2 = jasmine.createSpy('callback2');
    wrapper.setProps({ callback: callback2 });

    expect(document.removeEventListener).not.toHaveBeenCalled();
    expect(document.addEventListener.calls.count()).toBe(1);

    wrapper.unmount();
  });

  it('when first mounted component is unmounted, second component should still listen', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    const listener = document.addEventListener.calls.argsFor(0)[1];

    const callback2 = jasmine.createSpy('callback2');
    const wrapper2 = getMountedComponent({
      callback: callback2,
    });

    // unmount first component
    wrapper.unmount();
    expect(document.removeEventListener).not.toHaveBeenCalled();

    // second component should still listen
    listener({ key: 'Escape' });
    expect(callback).not.toHaveBeenCalled();
    expect(callback2).toHaveBeenCalled();

    wrapper2.unmount();
  });

  it('does not invoke callback for the component which is not listening', function () {
    const wrapper = getMountedComponent();
    expect(document.addEventListener).toHaveBeenCalled();
    const listener = document.addEventListener.calls.argsFor(0)[1];

    // mount second component which is lot listening to Esc event
    const callback2 = jasmine.createSpy('callback2');
    const wrapper2 = getMountedComponent({
      callback: callback2,
      isListening: false,
    });

    listener({ key: 'Escape' });
    expect(callback).toHaveBeenCalled();
    expect(callback2).not.toHaveBeenCalled();

    wrapper.unmount();
    expect(document.removeEventListener).toHaveBeenCalled();

    wrapper2.unmount();
  });
});
