/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { assocPath } from 'ramda';

import { axiosMockAdapter, fireEvent, render, screen, setupPortalContainer, waitFor } from 'TestRoot/SpecUtil';

import { getBillOfMaterialsComponentsUrl } from 'MainRoot/util/CLMLocation';
import ComponentsFilterDrawer from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/componentsFilterDrawer/ComponentsFilterDrawer';

import {
  COMPONENTS_PER_PAGE,
  SORT_BY_FIELDS,
  defaultSortConfiguration,
  defaultFilterConfiguration,
  paginationInitialState,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/billOfMaterialsComponentsTileSlice';

describe('ComponentsFilterDrawer', () => {
  let axiosMock, initialState;

  const APPLICATION_INTERNAL_ID = 'APPLICATION-INTERNAL-ID';
  const SBOM_VERSION = 'SBOM-VERSION';

  const filterDrawerInitialState = Object.freeze({
    showDrawer: false,
    collapsibleItems: {
      showVulnerabilityThreatLevels: true,
      showDependencyTypes: true,
    },
  });

  const initialProps = Object.freeze({
    internalAppId: APPLICATION_INTERNAL_ID,
    sbomVersion: SBOM_VERSION,
  });

  const baseUrlParams = [
    APPLICATION_INTERNAL_ID,
    SBOM_VERSION,
    1,
    COMPONENTS_PER_PAGE,
    SORT_BY_FIELDS.vulnerabilities,
    false,
  ];

  const renderComponent = (props, preloadedState) => render(<ComponentsFilterDrawer {...props} />, { preloadedState });

  beforeAll(() => setupPortalContainer());

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    initialState = {
      router: {
        currentParams: {
          versionId: SBOM_VERSION,
        },
      },
      billOfMaterialsComponentsTile: {
        loadingComponents: true,
        loadingComponentsErrorMessage: null,

        components: null,
        totalNumberOfComponents: null,

        sortConfiguration: { ...defaultSortConfiguration },
        filterConfiguration: { ...defaultFilterConfiguration },
        pagination: { ...paginationInitialState },

        filterDrawer: {
          ...filterDrawerInitialState,
          showDrawer: true,
        },
      },
    };

    axiosMock
      .onGet(getBillOfMaterialsComponentsUrl(...baseUrlParams))
      .reply(200, { totalResultsCount: 0, results: [] });
  });

  it('renders the correct initial content', async () => {
    renderComponent(initialProps, initialState);

    await waitFor(() => {
      expect(screen.getByText(/Filter By/)).toBeInTheDocument();
    });

    expect(screen.getByText(/Vulnerability Threat Level/)).toBeInTheDocument();
    const vulnerabilityThreatLevelFilterCount = screen.getByTestId('vulnerability-threat-level-filter-count');
    expect(vulnerabilityThreatLevelFilterCount).toHaveTextContent('4');

    expect(screen.getByText('Critical')).toBeInTheDocument();
    expect(screen.getByText('High')).toBeInTheDocument();
    expect(screen.getByText('Medium')).toBeInTheDocument();
    expect(screen.getByText('Low')).toBeInTheDocument();

    expect(screen.getByText(/Dependency Type/)).toBeInTheDocument();
    const dependencyTypeFilterCount = screen.getByTestId('dependency-type-filter-count');
    expect(dependencyTypeFilterCount).toHaveTextContent('3');

    expect(screen.getByText('Direct')).toBeInTheDocument();
    expect(screen.getByText('Transitive')).toBeInTheDocument();
    expect(screen.getByText('Unspecified')).toBeInTheDocument();

    const checkboxes = screen.getAllByRole('menuitemcheckbox', { hidden: true });
    expect(checkboxes.length).toBe(7);
    for (const checkbox of checkboxes) {
      expect(checkbox).not.toBeChecked();
    }
  });

  it('renders the correct checkbox state that matches filter configuration', async () => {
    const modifiedInitialState = assocPath(
      ['billOfMaterialsComponentsTile', 'filterConfiguration'],
      {
        vulnerabilityThreatLevels: {
          critical: true,
          high: true,
          medium: true,
          low: true,
        },
        dependencyTypes: {
          direct: true,
          transitive: true,
          unspecified: true,
        },
      },
      initialState
    );

    renderComponent(initialProps, modifiedInitialState);

    await waitFor(() => {
      expect(screen.getByText(/Filter By/)).toBeInTheDocument();
    });

    const checkboxes = screen.getAllByRole('menuitemcheckbox', { hidden: true });
    expect(checkboxes.length).toBe(7);
    for (const checkbox of checkboxes) {
      expect(checkbox).toBeChecked();
    }
  });

  it('should have toggleable checkboxes', async () => {
    renderComponent(initialProps, initialState);

    await waitFor(() => {
      expect(screen.getByText(/Filter By/)).toBeInTheDocument();
    });

    const checkboxes = screen.getAllByRole('menuitemcheckbox', { hidden: true });

    for (const checkbox of checkboxes) {
      expect(checkbox).not.toBeChecked();
      await fireEvent.click(checkbox);
    }

    for (const checkbox of checkboxes) {
      expect(checkbox).toBeChecked();
    }
  });
});
