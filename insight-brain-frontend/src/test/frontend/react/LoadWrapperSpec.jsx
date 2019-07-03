import React from 'react';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../enzymeUtils';

describe('LoadWrapper', function() {
  let LoadWrapper,
      getShallowComponent,
      mockMessages;

  beforeEach(function() {
    mockMessages = jasmine.createSpyObj('Messages', ['getHttpErrorMessage']);

    LoadWrapper = require('inject-loader!../../../main/frontend/react/LoadWrapper')({
      '../util/CommonServices': { Messages: mockMessages }
    }).default;

    getShallowComponent = enzymeUtils.getShallowComponent(LoadWrapper, {});
  });

  it('renders a NxLoadWrapper, passing through all props other than error', function() {
    const child = <div className="child"/>,
        component = getShallowComponent({ error: 'foo', bar: 'baz', children: child });

    expect(component).toMatchSelector(NxLoadWrapper);
    expect(component).toHaveProp('bar', 'baz');
    expect(component).toHaveProp('children', child);
  });

  it('passes the error through Messages.getHttpErrorMessage before passing it to the NxLoadWrapper', function() {
    mockMessages.getHttpErrorMessage.and.returnValue('FOOO');

    expect(getShallowComponent({ error: 'foo', bar: 'baz' })).toHaveProp('error', 'FOOO');
    expect(mockMessages.getHttpErrorMessage).toHaveBeenCalledWith('foo');
  });
});
