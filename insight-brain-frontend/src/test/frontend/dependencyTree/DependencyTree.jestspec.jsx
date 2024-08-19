/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, within, fireEvent } from '../SpecUtil';

import DependencyTree from 'MainRoot/DependencyTree/DependencyTree';
import { dependencyTreeData } from './dependencyTreeMockData';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('DependencyTree', () => {
  let minimalProps, renderComponent, treePathToggleAction, stateGoSpy;
  beforeEach(() => {
    treePathToggleAction = jest.fn('treePathToggleAction');
    minimalProps = {
      items: dependencyTreeData,
      treePathToggleAction,
      rootName: 'Root Name',
      searchTerm: '',
    };
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

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
    expect(screen.getAllByRole('treeitem')[0]).toHaveClass('open');
  });

  it('hides children when branch is collapsed', () => {
    renderComponent();
    expect(screen.getAllByRole('treeitem')[3]).not.toHaveClass('open');
  });

  it('renders the clickable tree item', () => {
    renderComponent();
    const clickableTreeNode = screen.getByText('net.sourceforge.jtds : jtds : 1.2.2').closest('a');

    expect(clickableTreeNode).toHaveClass('nx-text-link');
  });

  describe('Clickable Tree Item', () => {
    it('navigates to the applicationReport component details page when isPrioritiesPageContainer is false', () => {
      renderComponent();
      const clickableTreeNode = screen.getByText('net.sourceforge.jtds : jtds : 1.2.2').closest('a');

      fireEvent.click(clickableTreeNode);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails', { hash: 'qwert32143' });
    });

    it('navigates to the priorities page component details page when isPrioritiesPageContainer is true', () => {
      const prioritiesPageContainerName = 'prioritiesPageFromReports';
      jest.spyOn(routerSelectors, 'selectIsPrioritiesPageContainer').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectPrioritiesPageContainerName').mockReturnValue(prioritiesPageContainerName);

      renderComponent();
      const clickableTreeNode = screen.getByText('net.sourceforge.jtds : jtds : 1.2.2').closest('a');

      fireEvent.click(clickableTreeNode);

      expect(stateGoSpy).toHaveBeenCalledWith(`${prioritiesPageContainerName}.componentDetails`, {
        hash: 'qwert32143',
      });
    });

    it('navigates to the SBOM CDP page when it is SBOM Manager', () => {
      const applicationPublicId = 'APPLICATION-PUBLIC-ID';
      const sbomVersion = 'SBOM-VERSION';
      const componentHash = 'qwert32143';

      jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        applicationPublicId,
        sbomVersion,
      });

      renderComponent();

      const clickableTreeNode = screen.getByText('net.sourceforge.jtds : jtds : 1.2.2').closest('a');

      fireEvent.click(clickableTreeNode);

      expect(stateGoSpy).toHaveBeenCalledWith('sbomManager.component', {
        applicationPublicId,
        sbomVersion,
        componentHash,
      });
    });
  });

  describe('Non-clickable Tree Item', () => {
    it('renders non-clickable tree item when hash matches', () => {
      renderComponent({ hashToMatch: 'qwert32143' });

      const nonClickableTreeItem = screen.getByText('net.sourceforge.jtds : jtds : 1.2.2');

      expect(nonClickableTreeItem.closest('a')).toBeNull();
    });

    it('renders non-clickable tree item when isNonClickable is explicitly set to true', () => {
      renderComponent({ isNonClickable: true });

      const nonClickableTreeItem = screen.getByText('net.sourceforge.jtds : jtds : 1.2.2');

      expect(nonClickableTreeItem.closest('a')).toBeNull();
    });
  });

  it('renders threat indicator', async () => {
    const tree = [
      {
        displayName: 'org.apache.commons : commons-lang3 : 3.3.2',
        children: [
          {
            displayName: 'taglibs : standard : 1.1.2.ff',
            children: null,
            isOpen: false,
            treePath: [0, 'children', 0],
            hash: 'qwert32145',
            policyThreatLevel: 10,
          },
          {
            displayName: 'taglibs : standard : 1.1.2.hh',
            children: null,
            isOpen: true,
            treePath: [0, 'children', 1],
            hash: 'qwert321432',
            policyThreatLevel: 6,
          },
        ],
        isOpen: true,
        treePath: [0],
        hash: 'qwert3214',
        policyThreatLevel: 1,
      },
    ];
    SpecUtil.requestIdleCallbackInvokeImmediateJest();

    renderComponent({ items: tree });

    expect(await screen.findByLabelText('Low')).toBeVisible();
    expect(await screen.findByLabelText('Critical')).toBeVisible();
    expect(await screen.findByLabelText('Severe')).toBeVisible();
  });

  it('renders an InnerSource icon', () => {
    const mockTree = [
      {
        displayName: 'taglibs : standard : 1.1.2',
        children: null,
        isOpen: true,
        treePath: [3],
        hash: 'qwert56',
        policyThreatLevel: 10,
        isInnerSource: true,
      },
    ];
    renderComponent({
      items: mockTree,
    });

    const item = screen.getAllByRole('treeitem');

    expect(within(item[0]).getByText('IS')).toBeVisible();
  });

  it('renders highlighted search terms', () => {
    const searchTerm = 'commons';
    renderComponent({
      searchTerm,
    });

    const firstItem = screen.getByRole('treeitem', { name: /org.apache. commons : commons -lang3 : 3.3.2/i });

    expect(firstItem).toBeInTheDocument();

    const highlightedTerms = within(firstItem).getAllByText(searchTerm);

    expect(highlightedTerms.length).toBe(2);
    expect(highlightedTerms[0]).toHaveClass('iq-dependency-tree-page__search-match');
    expect(highlightedTerms[1]).toHaveClass('iq-dependency-tree-page__search-match');
  });
});
