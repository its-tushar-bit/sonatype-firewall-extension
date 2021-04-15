/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { shallow } from 'enzyme';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../enzymeUtils';

describe('LoadWrapper', function () {
  let LoadWrapper, getShallowComponent, mockMessages;

  beforeEach(function () {
    mockMessages = jasmine.createSpyObj('Messages', ['getHttpErrorMessage']);

    LoadWrapper = require('inject-loader!../../../main/frontend/react/LoadWrapper')(
      {
        '../util/CommonServices': { Messages: mockMessages },
      }
    ).default;

    getShallowComponent = enzymeUtils.getShallowComponent(LoadWrapper, {});
  });

  it('renders a NxLoadWrapper, passing through all props other than error', function () {
    const child = <div className="child" />,
      component = getShallowComponent({
        error: 'foo',
        bar: 'baz',
        children: child,
      });

    expect(component).toMatchSelector(NxLoadWrapper);
    expect(component).toHaveProp('bar', 'baz');
    expect(component).toHaveProp('children', child);
  });

  it('passes the error through Messages.getHttpErrorMessage before passing it to the NxLoadWrapper', function () {
    mockMessages.getHttpErrorMessage.and.returnValue('FOOO');

    expect(getShallowComponent({ error: 'foo', bar: 'baz' })).toHaveProp(
      'error',
      'FOOO'
    );
    expect(mockMessages.getHttpErrorMessage).toHaveBeenCalledWith('foo');
  });

  it('passes error through as-is if it is a react node', function () {
    const component = getShallowComponent({ error: <em>foo</em> });

    expect(shallow(component.prop('error'))).toMatchElement(<em>foo</em>);

    expect(mockMessages.getHttpErrorMessage).not.toHaveBeenCalled();
  });
});
