/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { mount, shallow } from 'enzyme';

describe('MaximizedContainer', function() {
  let mockTeardownMaximizeHeight,
      mockMaximizeHeightServiceInstance,
      MaximizedContainer,
      mountedComponent;

  beforeEach(function() {
    mockTeardownMaximizeHeight = jasmine.createSpy('teardownMaximizeHeight');

    mockMaximizeHeightServiceInstance = {
      maximizeHeight: jasmine.createSpy('maximizeHeight').and.returnValue(mockTeardownMaximizeHeight)
    };

    MaximizedContainer = require('inject-loader!../../../main/frontend/react/MaximizedContainer')({
      '../util/AngularCommon': {
        maximizeHeightServiceInstance: mockMaximizeHeightServiceInstance
      }
    }).default;
  });

  afterEach(function() {
    if (mountedComponent) {
      mountedComponent.unmount();
    }

    mountedComponent = null;
  });

  it('renders a div with the specified children props, and classnames along with the "maximized-container" class',
      function() {
        expect(shallow(<MaximizedContainer id="idid" className="foo"><div>Bar</div></MaximizedContainer>))
            .toMatchElement(
              <div id="idid" className="foo maximized-container">
                <div>Bar</div>
              </div>
            );
      }
  );

  it('calls maximizeHeight on its top-level div', function() {
    mountedComponent = mount(<MaximizedContainer id="foo"/>);
    expect(mockMaximizeHeightServiceInstance.maximizeHeight).toHaveBeenCalled();
    expect(mockMaximizeHeightServiceInstance.maximizeHeight.calls.first().args[0].attr('id')).toBe('foo');
  });

  it('calls the function returned by maximizeHeight when unmounted', function() {
    mountedComponent = mount(<MaximizedContainer id="foo"/>);

    expect(mockTeardownMaximizeHeight).not.toHaveBeenCalled();
    mountedComponent.unmount();
    mountedComponent = undefined;

    expect(mockTeardownMaximizeHeight).toHaveBeenCalled();
  });
});
