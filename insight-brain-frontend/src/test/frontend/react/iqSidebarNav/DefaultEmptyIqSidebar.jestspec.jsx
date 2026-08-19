/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import DefaultEmptyIqSidebar from 'MainRoot/react/iqSidebarNav/DefaultEmptyIqSidebar';

describe('DefaultEmptyIqSidebar', function () {
  describe('renders an DefaultEmptyIqSidebar', function () {
    it('renders an NxGlobalSidebar2 with no links to pages', function () {
      render(<DefaultEmptyIqSidebar />, {});

      const sidebar = screen.getByRole('navigation', { name: 'global sidebar' });
      expect(sidebar).toBeInTheDocument();

      expect(within(sidebar).queryByRole('link')).not.toBeInTheDocument();
    });
  });
});
