/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import NavPills from 'MainRoot/navPills/NavPills';
import { render, screen } from 'TestRoot/SpecUtil';

const exampleNavList = [
  {
    label: 'Section 1 Pill',
    target: 'example-pills-page-1',
    isDisplayed: true,
  },
  {
    label: 'Section 2 Pill',
    target: 'example-pills-page-2',
    isDisplayed: true,
  },
  {
    label: 'Section 3 Pill',
    target: 'example-pills-page-3',
    isDisplayed: true,
  },
  {
    label: 'Section 4 Pill',
    target: 'example-pills-page-4',
    isDisplayed: true,
  },
  {
    label: 'Section 5 Pill',
    target: 'example-pills-page-5',
    isDisplayed: false,
  },
  {
    label: 'Section 6 Pill',
    target: 'example-pills-page-6',
    isDisplayed: true,
  },
];

describe('NavPills', () => {
  it('render pills only with isDisplayed true', () => {
    render(<NavPills list={exampleNavList} root="example-pills-page" />);

    expect(screen.getByText('Section 1 Pill')).toBeVisible();
    expect(screen.getByText('Section 2 Pill')).toBeVisible();
    expect(screen.getByText('Section 3 Pill')).toBeVisible();
    expect(screen.getByText('Section 4 Pill')).toBeVisible();
    expect(screen.queryAllByText('Section 5 Pill').length).toBe(0);
    expect(screen.getByText('Section 6 Pill')).toBeVisible();
  });

  it('sizes the scroll container trailing space to its own visible height (CLM-43505)', () => {
    const scrollRoot = document.createElement('div');
    scrollRoot.id = 'nav-scroll-root';
    Object.defineProperty(scrollRoot, 'clientHeight', { configurable: true, value: 640 });
    document.body.appendChild(scrollRoot);

    try {
      render(<NavPills list={exampleNavList} root="#nav-scroll-root" />);
      expect(scrollRoot.style.paddingBottom).toBe('640px');
    } finally {
      document.body.removeChild(scrollRoot);
    }
  });
});
