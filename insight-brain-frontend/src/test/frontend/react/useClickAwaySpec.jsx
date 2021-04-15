/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef } from 'react';
import { mount } from 'enzyme';
import useClickAway from '../../../main/frontend/react/useClickAway';
import * as PropTypes from 'prop-types';

function HookWrapper({ callback }) {
  const ref = useRef(null);
  useClickAway(ref, callback);
  return (
    <div id="parent">
      Parent
      <div ref={ref} id="child">
        Child
      </div>
    </div>
  );
}

HookWrapper.propTypes = {
  callback: PropTypes.func,
};

describe('useClickAway', function () {
  let wrapper, callback, listener;

  beforeEach(function () {
    spyOn(document, 'addEventListener');
    callback = jasmine.createSpy('callback');
    wrapper = mount(<HookWrapper callback={callback} />);
    expect(document.addEventListener).toHaveBeenCalled();
    listener = document.addEventListener.calls.argsFor(0)[1];
  });

  it('adds mousedown listener for event capturing phase', function () {
    const args = document.addEventListener.calls.argsFor(0);
    expect(args[0]).toBe('mousedown');
    expect(args[2]).toBe(true);
  });

  it('invokes callback when parent is clicked', function () {
    listener({ target: wrapper.getDOMNode() });
    expect(callback).toHaveBeenCalled();
  });

  it('does not invoke callback when child is clicked', function () {
    listener({ target: wrapper.find('#child').getDOMNode() });
    expect(callback).not.toHaveBeenCalled();
  });

  it('removes mousedown listener for event capturing phase when unmounted', function () {
    spyOn(document, 'removeEventListener');
    wrapper.unmount();
    expect(document.removeEventListener).toHaveBeenCalled();
    expect(document.removeEventListener.calls.argsFor(0)).toEqual([
      'mousedown',
      listener,
      true,
    ]);
  });
});
