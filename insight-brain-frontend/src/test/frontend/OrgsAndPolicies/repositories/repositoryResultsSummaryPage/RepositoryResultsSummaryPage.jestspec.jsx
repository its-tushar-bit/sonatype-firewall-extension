/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, setupPortalContainer } from 'TestRoot/SpecUtil';
import { fireEvent } from '@testing-library/react';
import * as repositoriesResultsSummaryPageSelectors from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSelectors';
import RepositoryResultsSummaryPage from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/RepositoryResultsSummaryPage';
import { actions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';

import 'TestRoot/SpecUtil';

describe('RepositoryResultsSummaryPage', () => {
  let renderComponent, spyTileSummary, spyRepoInformation, spyMaskSuccess, toggleAggregateAndGetRepositoryComponentsSpy;
  const repoId = 'testRepo';
  const repositoryInfo = {
    publicId: repoId,
  };

  const repositorySummaryInfo = {
    repositoryInfo,
    affectedComponentCount: 1,
    criticalViolationCount: 1,
    knownComponentCount: 1,
    moderateViolationCount: 1,
    quarantinedComponentCount: 1,
    severeViolationCount: 1,
    totalComponentCount: 6,
    loadingSummaryTile: false,
    loadingRepositoryInformation: false,
    errorSummaryTile: null,
    errorRepositoryInformation: null,
  };

  beforeAll(() => setupPortalContainer());

  beforeEach(() => {
    renderComponent = () => render(<RepositoryResultsSummaryPage repoId={repoId} />);
  });

  describe('when data are being loaded', () => {
    beforeEach(() => {
      spyTileSummary = jest.spyOn(repositoriesResultsSummaryPageSelectors, 'selectRepositoryResultsSummaryPageSlice');

      spyRepoInformation = jest.spyOn(repositoriesResultsSummaryPageSelectors, 'selectRepositoryInformation');

      spyTileSummary.mockReturnValue(repositorySummaryInfo);
      spyRepoInformation.mockReturnValue(repositorySummaryInfo.repositoryInfo);
      toggleAggregateAndGetRepositoryComponentsSpy = jest.spyOn(actions, 'toggleAggregateAndGetRepositoryComponents');
    });

    it('shows proper values for threat counters', () => {
      spyTileSummary.mockReturnValue({
        ...repositorySummaryInfo,
        severeViolationCount: 998,
        moderateViolationCount: 997,
        criticalViolationCount: 996,
      });

      renderComponent();
      expect(screen.getByText('998')).toBeInTheDocument();
      expect(screen.getByText('997')).toBeInTheDocument();
      expect(screen.getByText('996')).toBeInTheDocument();
    });

    it('shows summary information with more than 1 component for each summary count property', () => {
      spyTileSummary.mockReturnValue({
        ...repositorySummaryInfo,
        affectedComponentCount: 2,
        quarantinedComponentCount: 2,
        knownComponentCount: 2,
      });

      renderComponent();
      expect(screen.getByText('Affecting 2 components')).toBeInTheDocument();
      expect(screen.getByText('6 COMPONENTS')).toBeInTheDocument();
      expect(screen.getByText('2 QUARANTINED')).toBeInTheDocument();
      expect(screen.getByText('components')).toBeInTheDocument();
    });

    it('renders page component, title and button ', () => {
      spyTileSummary.mockReturnValue({
        ...repositorySummaryInfo,
        loadingSummaryTile: false,
        loadingRepositoryInformation: false,
      });

      renderComponent();
      expect(screen.getByText('Re-Evaluate Repository')).toBeInTheDocument();
      expect(screen.getByText('testRepo Repository Results')).toBeInTheDocument();
      expect(screen.getByText('1 QUARANTINED')).toBeInTheDocument();
      expect(screen.getByText('component')).toBeInTheDocument();
    });

    it('renders components table and filter button ', () => {
      renderComponent();

      expect(screen.getByRole('button', { name: 'Filter' })).toBeInTheDocument();
      expect(screen.getByTestId('iq-repository-summary-table')).toBeInTheDocument();
    });

    it('renders filter popover', () => {
      renderComponent();
      const filterButton = screen.getByRole('button', { name: 'Filter' });

      fireEvent.click(filterButton);

      // The drawer is not fully open with contents accessible until after a CSS animation completes.
      // We don't load the CSS in unit tests so we have to mock the animationEnd event here
      const drawer = screen.getByRole('dialog', { hidden: true });
      fireEvent.animationEnd(drawer);

      const clearButton = screen.getByRole('button', { name: 'Clear' });
      const applyButton = screen.getByRole('button', { name: 'Apply' });

      expect(screen.getByTestId('components-filter-popover')).toBeVisible();
      expect(screen.getByText('Filters')).toBeVisible();
      expect(screen.getByText('Component Match State')).toBeVisible();
      expect(screen.getByText('Violations')).toBeVisible();
      expect(clearButton).toBeEnabled();
      expect(applyButton).toBeEnabled();

      const closeDrawerButton = screen.getByRole('button', { name: /Close/i });
      closeDrawerButton.click();
      const drawerAfterClose = screen.getByRole('dialog');
      fireEvent.animationEnd(drawerAfterClose);

      expect(drawerAfterClose).not.toBeVisible();
    });

    it('aggregates components', () => {
      renderComponent();
      const aggregateToggle = screen.getByLabelText('Aggregate by component');

      fireEvent.click(aggregateToggle);

      expect(toggleAggregateAndGetRepositoryComponentsSpy).toHaveBeenCalled();
    });

    it('shows summary tile error message', () => {
      spyTileSummary.mockReturnValue({
        ...repositorySummaryInfo,
        errorSummaryTile: 'test',
      });
      renderComponent();
      expect(screen.getByText('An error occurred loading data. test')).toBeInTheDocument();
    });

    it('shows repository information error message', () => {
      spyTileSummary.mockReturnValue({
        ...repositorySummaryInfo,
        errorRepositoryInformation: 'test',
      });
      renderComponent();
      expect(screen.getByText('An error occurred loading data. test')).toBeInTheDocument();
    });

    it('renders loading indicator', () => {
      spyTileSummary.mockReturnValue({
        ...repositorySummaryInfo,
        loadingSummaryTile: true,
        loadingRepositoryInformation: true,
      });
      renderComponent();
      expect(screen.getByText('Loading…')).toBeInTheDocument();
    });
  });

  describe('when report is re-evaluated', () => {
    beforeEach(() => {
      spyTileSummary = jest.spyOn(repositoriesResultsSummaryPageSelectors, 'selectRepositoryResultsSummaryPageSlice');

      spyRepoInformation = jest.spyOn(repositoriesResultsSummaryPageSelectors, 'selectRepositoryInformation');

      spyMaskSuccess = jest.spyOn(repositoriesResultsSummaryPageSelectors, 'selectReEvaluateMaskSuccess');
      spyTileSummary.mockReturnValue(repositorySummaryInfo);
      spyRepoInformation.mockReturnValue(repositorySummaryInfo.repositoryInfo);
    });

    it('shows reevaluation modal success', async () => {
      SpecUtil.requestIdleCallbackInvokeImmediateJest();
      spyMaskSuccess.mockReturnValue(true);
      renderComponent();

      const btnReevaluate = screen.getByRole('button', { name: 'Re-Evaluate Repository' });
      expect(btnReevaluate).toBeInTheDocument();
      fireEvent.click(btnReevaluate);
      const btnModalReevaluate = screen.getByText('Re-evaluate');
      expect(btnModalReevaluate).toBeInTheDocument();
      fireEvent.click(btnModalReevaluate);
      const txtSuccess = screen.getByText('Success!');
      expect(txtSuccess).toBeInTheDocument();
    });

    it('shows reevaluation modal re-evaluating', async () => {
      renderComponent();
      const btnReevaluate = screen.getByRole('button', { name: 'Re-Evaluate Repository' });
      expect(btnReevaluate).toBeInTheDocument();
      fireEvent.click(btnReevaluate);
      const btnModalReevaluate = screen.getByText('Re-evaluate');
      expect(btnModalReevaluate).toBeInTheDocument();
      fireEvent.click(btnModalReevaluate);
      const txtReevaluating = screen.getByText('Re-Evaluating');
      expect(txtReevaluating).toBeInTheDocument();
    });

    it('cancels and hide reevaluation modal success', async () => {
      renderComponent();
      const btnReevaluate = screen.getByRole('button', { name: 'Re-Evaluate Repository' });
      expect(btnReevaluate).toBeInTheDocument();
      fireEvent.click(btnReevaluate);

      const btnModalCancel = screen.getByText('Cancel');
      expect(btnModalCancel).toBeInTheDocument();
      fireEvent.click(btnModalCancel);
      expect(btnModalCancel).not.toBeInTheDocument();
    });
  });
});
