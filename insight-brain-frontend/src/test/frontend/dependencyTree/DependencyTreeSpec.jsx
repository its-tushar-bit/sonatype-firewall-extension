/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen } from '../SpecUtil';

import DependencyTree from 'MainRoot/DependencyTree/DependencyTree';
import { dependencyTreeData } from '../util/dependencyTreeUtil';

describe('DependencyTree', () => {
  let minimalProps, renderComponent, toggleTreePath;
  beforeEach(() => {
    minimalProps = {
      dependencyTree: dependencyTreeData,
      toggleTreePath,
      rootName: 'Root Name',
    };
    toggleTreePath = jasmine.createSpy('toggleTreePath');
    renderComponent = (additionalProps) => render(<DependencyTree {...minimalProps} {...additionalProps} />);
  });

  it('renders the tree with the correct title', () => {
    renderComponent();
    expect(screen.getByText(minimalProps.rootName)).toBeVisible();
  });

  it('renders all the items in the tree', () => {
    renderComponent();
    expect(screen.getAllByRole('treeitem').length).toBe(7);
  });

  it('show children when branch is not collapsed', () => {
    renderComponent();
    expect(screen.getAllByRole('treeitem')[1]).toHaveClassName('open');
  });

  it('hides children when branch is collapsed', () => {
    renderComponent();
    expect(screen.getAllByRole('treeitem')[3]).not.toHaveClassName('open');
  });

  it('renders the clickable tree item', () => {
    renderComponent();
    const clickableTreeNode = screen.getByText('net.sourceforge.jtds : jtds : 1.2.2').closest('a');

    expect(clickableTreeNode).toHaveClassName('nx-text-link');
  });
});
