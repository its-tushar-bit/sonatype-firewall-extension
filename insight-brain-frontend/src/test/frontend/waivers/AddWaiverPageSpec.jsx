/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import React from 'react';
import { NxButton } from '@sonatype/react-shared-components';

describe('AddWaiverPage', function() {
  let minimalProps,
      AddWaiverPage,
      MaximizedContainerMock,
      getShallowComponent,
      addWaiverSpy;

  beforeEach(function() {
    MaximizedContainerMock = jasmine.createSpy('MaximizedContainerMock')
        .and.returnValue(<div>MaximizedContainer</div>);

    AddWaiverPage = require('inject-loader!../../../main/frontend/waivers/AddWaiverPage')({
      '../react/MaximizedContainer': MaximizedContainerMock
    }).default;

    addWaiverSpy = jasmine.createSpy('addWaiver');

    minimalProps = {
      stateParams: {
        policyViolationId: 'foo'
      },
      addWaiver: addWaiverSpy
    };

    getShallowComponent = enzymeUtils.getShallowComponent(AddWaiverPage, minimalProps);
  });

  it('renders a component with the "nx-page-content" class', function() {
    expect(getShallowComponent()).toMatchSelector('.nx-page-content');
  });

  it('calls addWaiver when the button is clicked', function() {
    const button = getShallowComponent().find(NxButton);
    button.simulate('click');
    expect(addWaiverSpy).toHaveBeenCalled();
  });
});
