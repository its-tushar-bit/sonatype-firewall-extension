/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { getAllByRole, render, screen, setupPortalContainer } from 'TestRoot/SpecUtil';
import RepositoryResultsComponentsFilter from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsComponentsTable/repositoryResultsComponentsFilter/RepositoryResultsComponentsFilter';
import { actions as repositoryComponentsActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import { fireEvent } from '@testing-library/react';

describe('RepositoryResultsComponentsFilter', () => {
  let applyFiltersSpy, getRepositoryComponentsSpy, getRepositoryComponentsForBulkWaiveSpy;
  const repoId = 'testRepo';

  const preloadedState = {
    repositoryResultsSummaryPage: {
      componentsRequestBody: {},
      selectedMatchStateFilters: new Set(),
      selectedViolationStateFilters: new Set(),
      selectedThreatLevelFilters: [0, 10],
      showFilterPopover: true,
    },
  };

  // Render the component and trigger its animationEnd event so that it portrays itself as fully visible
  function renderComponent(props = {}) {
    const defaultProps = {
      repositoryId: repoId,
      isBulkWaivePage: false,
      ...props,
    };

    render(<RepositoryResultsComponentsFilter {...defaultProps} />, { preloadedState });

    const drawer = screen.getByRole('dialog', { hidden: true });
    fireEvent.animationEnd(drawer);
  }

  beforeAll(() => setupPortalContainer());

  beforeEach(() => {
    applyFiltersSpy = jest.spyOn(repositoryComponentsActions, 'applyFilters').mockImplementation(() => ({
      type: 'repositoryResultsSummaryPage/applyFilters',
    }));
    getRepositoryComponentsSpy = jest
      .spyOn(repositoryComponentsActions, 'getRepositoryComponents')
      .mockImplementation(() => ({
        type: 'repositoryResultsSummaryPage/getRepositoryComponents',
      }));
    getRepositoryComponentsForBulkWaiveSpy = jest
      .spyOn(repositoryComponentsActions, 'getRepositoryComponentsForBulkWaive')
      .mockImplementation(() => ({
        type: 'repositoryResultsSummaryPage/getRepositoryComponentsForBulkWaive',
      }));
  });

  describe('when data are being loaded', () => {
    it('renders collapsible filters and buttons', async () => {
      const user = userEvent.setup();
      renderComponent();

      const clearButton = screen.getByRole('button', { name: 'Clear' });
      const applyButton = screen.getByRole('button', { name: 'Apply' });

      expect(screen.getByText('Filters')).toBeVisible();
      expect(screen.getByText('Component Match State')).toBeVisible();

      await user.click(screen.getByText('Component Match State'));
      await user.click(screen.getByText('Violations'));

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

    it('renders the policy threat level slider', async () => {
      const user = userEvent.setup();
      renderComponent();
      await user.click(screen.getByRole('button', { name: /Policy Threat Level/ }));

      const policyThreat = screen.getAllByRole('list')[0];
      const policyThreatSliders = getAllByRole(policyThreat, 'slider');
      expect(policyThreatSliders).toHaveLength(2);
      expect(policyThreatSliders[0]).toHaveAttribute('aria-valuemin', '0');
      expect(policyThreatSliders[1]).toHaveAttribute('aria-valuemax', '10');
      expect(policyThreat).toHaveTextContent('0');
      expect(policyThreat).toHaveTextContent('10');
    });

    it('it renders selection counter', async () => {
      const user = userEvent.setup();
      renderComponent();
      await user.click(screen.getByRole('button', { name: /Component Match State/ }));
      await user.click(screen.getByRole('button', { name: /Violations/ }));

      const exactFilter = screen.getByText('Exact');
      const waivedFilter = screen.getByText('Waived');
      const openFilter = screen.getByText('Open');

      expect(exactFilter).toBeVisible();
      await user.click(exactFilter);
      expect(screen.getByText('1 of 2')).toBeVisible();

      expect(openFilter).toBeVisible();
      await user.click(openFilter);
      expect(waivedFilter).toBeVisible();
      await user.click(waivedFilter);
      expect(screen.getByText('2 of 4')).toBeVisible();
    });

    it('it clears filter selected', async () => {
      const user = userEvent.setup();
      renderComponent();
      await user.click(screen.getByRole('button', { name: /Component Match State/ }));

      const exactFilter = screen.getByText('Exact');
      expect(exactFilter).toBeVisible();
      await user.click(exactFilter);

      const oneFilterTextSelected = '1 of 2';
      expect(screen.getByText(oneFilterTextSelected)).toBeVisible();
      await user.click(screen.getByText('Clear'));
      expect(screen.queryByText(oneFilterTextSelected)).not.toBeInTheDocument();
    });

    it('it calls  apply filter selected', async () => {
      const user = userEvent.setup();
      renderComponent();
      const applyButton = screen.getByRole('button', { name: 'Apply' });

      await user.click(screen.getByRole('button', { name: /Component Match State/ }));

      const exactFilter = screen.getByText('Exact');
      expect(exactFilter).toBeVisible();
      await user.click(exactFilter);
      await user.click(applyButton);

      expect(applyFiltersSpy).toHaveBeenCalled();
    });

    it('should call getRepositoryComponents when isBulkWaivePage is false', async () => {
      const user = userEvent.setup();
      renderComponent({ isBulkWaivePage: false });

      const applyButton = screen.getByRole('button', { name: 'Apply' });
      await user.click(applyButton);

      expect(applyFiltersSpy).toHaveBeenCalled();
      expect(getRepositoryComponentsSpy).toHaveBeenCalledWith(repoId);
      expect(getRepositoryComponentsForBulkWaiveSpy).not.toHaveBeenCalled();
    });

    it('should call getRepositoryComponentsForBulkWaive when isBulkWaivePage is true', async () => {
      const user = userEvent.setup();
      renderComponent({ isBulkWaivePage: true });

      const applyButton = screen.getByRole('button', { name: 'Apply' });
      await user.click(applyButton);

      expect(applyFiltersSpy).toHaveBeenCalled();
      expect(getRepositoryComponentsForBulkWaiveSpy).toHaveBeenCalledWith(repoId);
      expect(getRepositoryComponentsSpy).not.toHaveBeenCalled();
    });
  });
});
