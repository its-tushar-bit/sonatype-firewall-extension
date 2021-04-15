/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as enzymeUtils from '../../../enzymeUtils';
import { NxFilterInput } from '@sonatype/react-shared-components';
import DropdownFilterInput from '../../../../../main/frontend/configuration/scmOnboarding/components/DropdownFilterInput';

describe('DropdownFilterInput', function () {
  let container;

  const minimalProps = {
    label: 'dropdown',
    isOpen: false,
    onToggleCollapse: () => {},
    isElementFiltered: () => true,
  };
  const getShallowComponent = enzymeUtils.getShallowComponent(
    DropdownFilterInput,
    minimalProps
  );

  beforeEach(() => {
    container = document.createElement('div');
    document.body.append(container);
  });

  afterEach(() => {
    if (container) {
      document.body.removeChild(container);
      container = null;
    }
  });

  it('filtering is called when a single child is passed', () => {
    // given an dropdown that is open
    const filterFn = jasmine.createSpy('filterFn'),
      child = <button>foo</button>,
      component = getShallowComponent({
        filterFn,
        isOpen: true,
        children: [child],
      });

    // when the filter is updated
    expect(component.find(NxFilterInput)).toExist();
    component.find(NxFilterInput).simulate('change', 'foo');

    // then the filter function is called
    expect(filterFn).toHaveBeenCalledWith(child, 'foo');
  });

  it('filtering is called when multiple children are passed', () => {
    // given an dropdown that is open
    const filterFn = jasmine.createSpy('filterFn'),
      children = [<button key="1">foo</button>, <button key="2">bar</button>],
      component = getShallowComponent({
        filterFn,
        isOpen: true,
        children: [children],
      });

    // when the filter is updated
    expect(component.find(NxFilterInput)).toExist();
    component.find(NxFilterInput).simulate('change', 'foo');

    // then the filter function is called
    expect(filterFn).toHaveBeenCalledWith(children, 'foo');
  });
});
