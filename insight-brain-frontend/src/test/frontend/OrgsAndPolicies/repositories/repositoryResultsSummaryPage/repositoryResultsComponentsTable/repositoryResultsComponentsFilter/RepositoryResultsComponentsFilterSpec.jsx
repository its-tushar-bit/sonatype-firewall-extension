/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { getAllByRole, render, screen } from 'TestRoot/SpecUtil';
import RepositoryResultsComponentsFilter from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsComponentsTable/repositoryResultsComponentsFilter/RepositoryResultsComponentsFilter';
import { actions as repositoryComponentsActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import { fireEvent } from '@testing-library/react';

describe('RepositoryResultsComponentsFilter', () => {
  let applyFiltersSpy;
  const repoId = 'testRepo';

  const preloadedState = {
    repositoryResultsSummaryPage: {
      componentsRequestBody: {},
      selectedViolationStateFilters: new Set(),
      selectedThreatLevelFilters: [0, 10],
      showFilterPopover: true,
    },
  };

  // Render the component and trigger its animationEnd event so that it portrays itself as fully visible
  function renderComponent() {
    render(<RepositoryResultsComponentsFilter repositoryId={repoId} />, { preloadedState });

    const drawer = screen.getByRole('dialog', { hidden: true });
    fireEvent.animationEnd(drawer);
  }

  beforeEach(() => {
    applyFiltersSpy = spyOn(repositoryComponentsActions, 'applyFilters').and.callThrough();
  });

  describe('when data are being loaded', () => {
    it('renders collapsible filters and buttons', () => {
      renderComponent();

      const clearButton = screen.getByRole('button', { name: 'Clear' });
      const applyButton = screen.getByRole('button', { name: 'Apply' });

      expect(screen.getByText('Filters')).toBeVisible();
      expect(screen.getByText('Component Match State')).toBeVisible();

      fireEvent.click(screen.getByText('Component Match State'));

      expect(screen.getAllByText('all/none')[0]).toBeVisible();
      expect(screen.getByText('Exact')).toBeVisible();
      expect(screen.getByText('Unknown')).toBeVisible();

      expect(screen.getByText('Violations')).toBeVisible();
      expect(screen.getAllByText('all/none')[1]).toBeVisible();
      expect(screen.getByText('Not Violating')).toBeVisible();
      expect(screen.getByText('Open')).toBeVisible();
      expect(screen.getByText('Quarantined')).toBeVisible();
      expect(screen.getByText('Waived')).toBeVisible();

      expect(screen.getByText('Policy Threat Level')).toBeVisible();

      expect(clearButton).toBeEnabled();
      expect(applyButton).toBeEnabled();
    });

    it('renders the policy threat level slider', () => {
      renderComponent();
      const policyThreat = screen.getAllByRole('list')[0];
      const policyThreatSliders = getAllByRole(policyThreat, 'slider');
      expect(policyThreatSliders[0]).toHaveTextContent('0');
      expect(policyThreatSliders[1]).toHaveTextContent('10');
    });

    it('it renders selection counter', () => {
      renderComponent();
      const exactFilter = screen.getByText('Exact');
      const waivedFilter = screen.getByText('Waived');
      const openFilter = screen.getByText('Open');

      expect(exactFilter).toBeVisible();
      fireEvent.click(exactFilter);
      expect(screen.getByText('1 of 2')).toBeVisible();

      expect(openFilter).toBeVisible();
      fireEvent.click(openFilter);
      expect(waivedFilter).toBeVisible();
      fireEvent.click(waivedFilter);
      expect(screen.getByText('2 of 4')).toBeVisible();
    });

    it('it clears filter selected', () => {
      renderComponent();
      const exactFilter = screen.getByText('Exact');
      expect(exactFilter).toBeVisible();
      fireEvent.click(exactFilter);

      const oneFilterTextSelected = '1 of 2';
      expect(screen.getByText(oneFilterTextSelected)).toBeVisible();
      fireEvent.click(screen.getByText('Clear'));
      expect(screen.queryByText(oneFilterTextSelected)).not.toBeInTheDocument();
    });

    it('it calls  apply filter selected', () => {
      renderComponent();
      const applyButton = screen.getByRole('button', { name: 'Apply' });
      const exactFilter = screen.getByText('Exact');
      expect(exactFilter).toBeVisible();
      fireEvent.click(exactFilter);
      fireEvent.click(applyButton);

      expect(applyFiltersSpy).toHaveBeenCalled();
    });
  });
});
