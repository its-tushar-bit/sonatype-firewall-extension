/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../../enzymeUtils';

import {
  ComponentDetailsFooter,
  PaginationCounter,
  PaginationLink,
} from 'MainRoot/componentDetails/ComponentDetailsFooter';

describe('ComponentDetailsFooter', () => {
  let minimalProps;
  let getShallowComponent;

  beforeEach(() => {
    minimalProps = {};

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentDetailsFooter, minimalProps);
  });

  it('renders a pagination counter when currentPage and pageCount props are passed', () => {
    const component = getShallowComponent({ currentPage: 2, pageCount: 5 });
    expect(component).toContainReact(<PaginationCounter currentPage={2} pageCount={5} />);
  });

  it('does NOT render a pagination counter when currentPage prop is NOT passed', () => {
    const component = getShallowComponent({ pageCount: 5 });
    expect(component.find(PaginationCounter)).not.toExist();
  });

  it('does NOT render a pagination counter when pageCount prop is NOT passed', () => {
    const component = getShallowComponent({ currentPage: 2 });
    expect(component.find(PaginationCounter)).not.toExist();
  });

  it('renders a pagination for prev and next and passes the corresponding href prop to each', () => {
    const component = getShallowComponent({ next: '/next-url', prev: 'prev-url' });
    expect(component).toContainReact(<PaginationLink href="prev-url" text="Previous Component" direction="prev" />);
    expect(component).toContainReact(<PaginationLink href="/next-url" text="Next Component" />);
  });
});
