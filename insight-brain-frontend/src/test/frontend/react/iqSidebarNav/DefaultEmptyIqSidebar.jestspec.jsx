/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from '@testing-library/react';
import DefaultEmptyIqSidebar from 'MainRoot/react/iqSidebarNav/DefaultEmptyIqSidebar';

describe('DefaultEmptyIqSidebar', function () {
  describe('renders an DefaultEmptyIqSidebar', function () {
    it('renders an NxGlobalSidebar with no links to pages', function () {
      const screen = render(<DefaultEmptyIqSidebar></DefaultEmptyIqSidebar>, {});
      const container = screen.container;
      const divNavBar = container.querySelector('div.nx-global-sidebar');
      expect(divNavBar).toHaveClass('nx-global-sidebar');

      const asideElement = container.querySelector('aside');
      expect(asideElement).not.toBeNull();
      expect(asideElement).toHaveAttribute('aria-label', 'global sidebar');

      const linksSideBar = container.querySelectorAll('a.nx-global-header');
      expect(linksSideBar.length).toBe(0);
    });
  });
});
