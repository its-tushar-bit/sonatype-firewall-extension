/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxLoadError } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../enzymeUtils';

describe('LoadError', function() {
  let LoadError,
      getShallowComponent,
      mockMessages;

  beforeEach(function() {
    mockMessages = jasmine.createSpyObj('Messages', ['getHttpErrorMessage']);

    LoadError = require('inject-loader!../../../main/frontend/react/LoadError')({
      '../util/CommonServices': { Messages: mockMessages }
    }).default;

    getShallowComponent = enzymeUtils.getShallowComponent(LoadError, {});
  });

  it('renders a NxLoadError, passing through all props other than error', function() {
    const child = <div className="child"/>,
        component = getShallowComponent({ error: 'foo', bar: 'baz', children: child });

    expect(component).toMatchSelector(NxLoadError);
    expect(component).toHaveProp('bar', 'baz');
    expect(component).toHaveProp('children', child);
  });

  it('passes the error through Messages.getHttpErrorMessage before passing it to the NxLoadError', function() {
    mockMessages.getHttpErrorMessage.and.returnValue('FOOO');

    expect(getShallowComponent({ error: 'foo', bar: 'baz' })).toHaveProp('error', 'FOOO');
    expect(mockMessages.getHttpErrorMessage).toHaveBeenCalledWith('foo');
  });
});
