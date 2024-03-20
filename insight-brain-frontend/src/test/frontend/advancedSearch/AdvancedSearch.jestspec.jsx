/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import AdvancedSearchContainer from 'MainRoot/advancedSearch/AdvancedSearchContainer';

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
    },
  };
  const renderComponent = () => render(<AdvancedSearchContainer />, { preloadedState: initialState });

  describe('help', function () {
    it('has a "Craft your search terms…" toggle', function () {
      renderComponent();

      // Ideally this would have a role, but it does not
      const toggle = screen.getByText('Craft your search terms for the best results.');
      expect(toggle).toBeInTheDocument();
    });

    it('has a "Craft your search terms…" toggle', function () {
      renderComponent();

      // Ideally this would have a role, but it does not
      const toggle = screen.getByText('Craft your search terms for the best results.');

      expect(toggle).toBeInTheDocument();
    });

    it('does not display the help initially', function () {
      renderComponent();

      const vulnHelp = screen.queryByText('Find a specific vulnerability');

      expect(vulnHelp).not.toBeInTheDocument();
    });

    it('displays the help when the toggle is toggled', function () {
      renderComponent();

      const toggle = screen.getByText('Craft your search terms for the best results.');
      fireEvent.click(toggle);

      const vulnHelp = screen.getByText('Find a specific vulnerability');
      expect(vulnHelp).toBeInTheDocument();

      fireEvent.click(toggle);
      expect(screen.queryByText('Find a specific vulnerability')).not.toBeInTheDocument();
    });

    it('displays additional help when not in SBOM Manager', function () {
      renderComponent();

      const toggle = screen.getByText('Craft your search terms for the best results.');
      fireEvent.click(toggle);

      const vulnHelp = screen.getByText('Search by application name focused on security vulnerabilities');
      expect(vulnHelp).toBeInTheDocument();
    });

    it('does not display additional help when in SBOM Manager', function () {
      const sbomManagerState = {
        ...initialState,
        router: {
          currentState: { name: 'sbomManager.advancedSearch' },
        },
      };
      render(<AdvancedSearchContainer />, { preloadedState: sbomManagerState });

      const toggle = screen.getByText('Craft your search terms for the best results.');
      fireEvent.click(toggle);

      const vulnHelp = screen.queryByText('Search by application name focused on security vulnerabilities');
      expect(vulnHelp).not.toBeInTheDocument();
    });

    it('displays a help doc link when not in SBOM Manager', function () {
      renderComponent();

      let helpLink = screen.queryByRole('link', { name: 'documentation' });
      expect(helpLink).not.toBeInTheDocument();

      const toggle = screen.getByText('Craft your search terms for the best results.');
      fireEvent.click(toggle);

      helpLink = screen.getByRole('link', { name: 'documentation' });
      expect(helpLink).toBeInTheDocument();
    });

    it('does not display additional help when in SBOM Manager', function () {
      const sbomManagerState = {
        ...initialState,
        router: {
          currentState: { name: 'sbomManager.advancedSearch' },
        },
      };
      render(<AdvancedSearchContainer />, { preloadedState: sbomManagerState });

      let helpLink = screen.queryByRole('link', { name: 'documentation' });
      expect(helpLink).not.toBeInTheDocument();

      const toggle = screen.getByText('Craft your search terms for the best results.');
      fireEvent.click(toggle);

      helpLink = screen.queryByRole('link', { name: 'documentation' });
      expect(helpLink).not.toBeInTheDocument();
    });
  });
});
