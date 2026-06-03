/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, userEvent, within } from 'TestRoot/SpecUtil';
import AdvancedSearchContainer from 'MainRoot/advancedSearch/AdvancedSearchContainer';
import { assocPath, mergeDeepRight } from 'ramda';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

/**
 * Note: this file currently only holds more-recently written tests for the page as a whole. See
 * the other spec files in this directory and its subdirs for additional Advanced Search unit tests
 */
describe('AdvancedSearch', function () {
  const initialState = {
    advancedSearch: {
      viewState: {
        loading: false,
      },
      configurationState: {
        isEnabled: true,
      },
      formState: {
        searchResult: {
          groupingByDTOS: [],
        },
      },
      easyQueryBuilder: {
        searchItems: [],
      },
    },
  };
  const mockRouterState = {
    get: () => ({}),
    href: () => '#',
  };
  let user;
  let renderComponent;

  beforeEach(() => {
    user = userEvent.setup();

    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);

    renderComponent = (preloadedState = initialState) => render(<AdvancedSearchContainer />, { preloadedState });
  });

  describe('page title', function () {
    it('renders a Sonatype Guide link in the page description', function () {
      renderComponent();

      const guideLink = screen.getByRole('link', { name: /Sonatype Guide/i });
      expect(guideLink).toBeInTheDocument();
      expect(guideLink).toHaveAttribute('href', 'https://links.sonatype.com/products/sonatype-guide');
      expect(guideLink).toHaveAttribute('target', '_blank');
      expect(guideLink).toHaveAttribute('rel', expect.stringMatching(/noreferrer/));
    });
  });

  describe('help', function () {
    it('has a "Search query examples" toggle', function () {
      renderComponent();

      const toggle = screen.getByRole('button', { name: 'Search query examples' });
      expect(toggle).toBeInTheDocument();
    });

    it('does not display the help initially', function () {
      renderComponent();

      expect(screen.queryByText('Find a specific vulnerability')).not.toBeInTheDocument();
    });

    it('displays the help when the toggle is toggled', async function () {
      renderComponent();

      const toggle = screen.getByRole('button', { name: 'Search query examples' });
      await user.click(toggle);

      expect(screen.getByText('Find a specific vulnerability')).toBeVisible();

      await user.click(toggle);
      expect(screen.queryByText('Find a specific vulnerability')).not.toBeInTheDocument();
    });

    it('displays additional help when not in SBOM Manager', async function () {
      renderComponent();

      const toggle = screen.getByRole('button', { name: 'Search query examples' });
      await user.click(toggle);

      expect(screen.getByText('Search by application name focused on security vulnerabilities')).toBeInTheDocument();
    });

    it('does not display additional help when in SBOM Manager', async function () {
      const sbomManagerState = {
        ...initialState,
        router: {
          currentState: { name: 'sbomManager.advancedSearch' },
        },
        productFeatures: { productFeatures: { 'sbom-manager': true } },
      };
      renderComponent(sbomManagerState);

      const toggle = screen.getByRole('button', { name: 'Search query examples' });
      await user.click(toggle);

      expect(
        screen.queryByText('Search by application name focused on security vulnerabilities')
      ).not.toBeInTheDocument();
    });

    it('displays a help doc link when not in SBOM Manager', async function () {
      renderComponent();

      expect(screen.queryByRole('link', { name: 'documentation' })).not.toBeInTheDocument();

      const toggle = screen.getByRole('button', { name: 'Search query examples' });
      await user.click(toggle);

      const helpLink = screen.getByRole('link', { name: 'documentation' });
      expect(helpLink).toBeVisible();
      expect(helpLink).toHaveAttribute('href', 'https://links.sonatype.com/products/nxiq/doc/advanced-search');
    });

    it('displays additional help when in SBOM Manager', async function () {
      const sbomManagerState = {
        ...initialState,
        router: {
          currentState: { name: 'sbomManager.advancedSearch' },
        },
        productFeatures: { productFeatures: { 'sbom-manager': true } },
      };
      renderComponent(sbomManagerState);

      expect(screen.queryByRole('link', { name: 'documentation' })).not.toBeInTheDocument();

      const toggle = screen.getByRole('button', { name: 'Search query examples' });
      await user.click(toggle);

      const helpLink = screen.getByRole('link', { name: 'documentation' });
      expect(helpLink).toBeInTheDocument();
      expect(helpLink).toHaveAttribute('href', 'https://links.sonatype.com/products/sbom/docs/search');
    });

    it('shows error when the SBOM Manager license is disabled', async () => {
      const sbomManagerState = {
        ...initialState,
        router: {
          currentState: { name: 'sbomManager.advancedSearch' },
        },
        productFeatures: { productFeatures: {} },
      };
      renderComponent(sbomManagerState);

      const errorMessage = await screen.findByText(
        'An error occurred loading data. The SBOM Manager license feature is not enabled.'
      );
      expect(errorMessage).toBeVisible();
    });
  });

  describe('search result group', function () {
    it('renders a vulnerability link that opens in same tab by default if the group is of VULNERABILITY_ID or VULNERABILITY_DESCRIPTION types', function () {
      const state = assocPath(
        ['advancedSearch', 'formState', 'searchResult', 'groupingByDTOS'],
        [
          {
            groupIdentifier: 'VULNERABILITY_ID',
            groupBy: 'CVE-111-1111',
            searchResultItemDTOS: [],
          },
          {
            groupIdentifier: 'VULNERABILITY_DESCRIPTION',
            groupBy: 'Foo bar baz.',
            searchResultItemDTOS: [],
          },
          {
            groupIdentifier: 'SOMETHING_ELSE',
            groupBy: 'asdf',
            searchResultItemDTOS: [],
          },
        ],
        initialState
      );

      renderComponent(state);

      const vulnResult = screen.getByRole('region', { name: 'CVE-111-1111' });
      const vulnDescriptionResult = screen.getByRole('region', { name: 'Foo bar baz.' });
      const otherResult = screen.getByRole('region', { name: 'asdf' });

      const vulnResultLink = within(vulnResult).getByRole('link', { name: 'Click here for detailed information.' });
      const vulnDescriptionResultLink = within(vulnDescriptionResult).getByRole('link', {
        name: 'Click here for detailed information.',
      });
      expect(vulnResultLink).toBeInTheDocument();
      expect(vulnResultLink).toHaveAttribute('target', '');
      expect(vulnDescriptionResultLink).toBeInTheDocument();
      expect(vulnDescriptionResultLink).toHaveAttribute('target', '');
      expect(within(otherResult).queryByRole('link')).not.toBeInTheDocument();
    });

    it('renders a vulnerability link that opens in a new tab if isDeveloper is true', function () {
      jest.spyOn(routerSelectors, 'selectIsDeveloper').mockReturnValue(true);
      const state = assocPath(
        ['advancedSearch', 'formState', 'searchResult', 'groupingByDTOS'],
        [
          {
            groupIdentifier: 'VULNERABILITY_ID',
            groupBy: 'CVE-111-1111',
            searchResultItemDTOS: [],
          },
          {
            groupIdentifier: 'VULNERABILITY_DESCRIPTION',
            groupBy: 'Foo bar baz.',
            searchResultItemDTOS: [],
          },
          {
            groupIdentifier: 'SOMETHING_ELSE',
            groupBy: 'asdf',
            searchResultItemDTOS: [],
          },
        ],
        initialState
      );

      renderComponent(state);

      const vulnResult = screen.getByRole('region', { name: 'CVE-111-1111' });
      const vulnDescriptionResult = screen.getByRole('region', { name: 'Foo bar baz.' });
      const otherResult = screen.getByRole('region', { name: 'asdf' });

      const vulnResultLink = within(vulnResult).getByRole('link', { name: 'Click here for detailed information.' });
      const vulnDescriptionResultLink = within(vulnDescriptionResult).getByRole('link', {
        name: 'Click here for detailed information.',
      });
      expect(vulnResultLink).toBeInTheDocument();
      expect(vulnResultLink).toHaveAttribute('target', '_blank');
      expect(vulnDescriptionResultLink).toBeInTheDocument();
      expect(vulnDescriptionResultLink).toHaveAttribute('target', '_blank');
      expect(within(otherResult).queryByRole('link')).not.toBeInTheDocument();
    });

    it('renders no vulnerability link if in SBOM Manager Mode', function () {
      const state = mergeDeepRight(initialState, {
        advancedSearch: {
          formState: {
            searchResult: {
              groupingByDTOS: [
                {
                  groupIdentifier: 'VULNERABILITY_ID',
                  groupBy: 'CVE-111-1111',
                  searchResultItemDTOS: [],
                },
                {
                  groupIdentifier: 'VULNERABILITY_DESCRIPTION',
                  groupBy: 'Foo bar baz.',
                  searchResultItemDTOS: [],
                },
                {
                  groupIdentifier: 'SOMETHING_ELSE',
                  groupBy: 'asdf',
                  searchResultItemDTOS: [],
                },
              ],
            },
          },
        },
        router: {
          currentState: { name: 'sbomManager.advancedSearch' },
        },
        productFeatures: { productFeatures: { 'sbom-manager': true } },
      });

      renderComponent(state);

      const vulnResult = screen.getByRole('region', { name: 'CVE-111-1111' });
      const vulnDescriptionResult = screen.getByRole('region', { name: 'Foo bar baz.' });
      const otherResult = screen.getByRole('region', { name: 'asdf' });

      expect(within(vulnResult).queryByRole('link')).not.toBeInTheDocument();
      expect(within(vulnDescriptionResult).queryByRole('link')).not.toBeInTheDocument();
      expect(within(otherResult).queryByRole('link')).not.toBeInTheDocument();
    });
  });
});
