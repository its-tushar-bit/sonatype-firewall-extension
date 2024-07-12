/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import AdvancedSearchExportButton from 'MainRoot/advancedSearch/AdvancedSearchExportButton';

describe('AdvancedSearchExportButton', () => {
  let renderComponent;

  beforeEach(() => {
    const exportButtonProps = {
      loading: false,
      waitingSearchResponse: false,
      totalNumberOfHits: 20,
      searchedQuery: 'current query',
      searchIncludedAllComponents: false,
    };
    renderComponent = (props) => render(<AdvancedSearchExportButton {...exportButtonProps} {...props} />);
  });

  describe('renders an <a> tag as a button', () => {
    it('renders the button as disabled when the page is loading', () => {
      renderComponent({ loading: true });

      const aTag = screen.getByText('Export Results').closest('a');
      expect(aTag).toHaveClass('disabled');
      expect(aTag).toHaveAttribute('aria-disabled', 'true');
    });

    it('renders the button as disabled when the page is loading results of a search', () => {
      renderComponent({ waitingSearchResponse: true });

      const aTag = screen.getByText('Export Results').closest('a');
      expect(aTag).toBeVisible();
      expect(aTag).toHaveClass('disabled');
      expect(aTag).toHaveAttribute('aria-disabled', 'true');
    });

    it('renders the button as disabled when there are no results to show in the page', () => {
      renderComponent({ totalNumberOfHits: 0 });

      const aTag = screen.getByText('Export Results').closest('a');
      expect(aTag).toBeVisible();
      expect(aTag).toHaveClass('disabled');
      expect(aTag).toHaveAttribute('aria-disabled', 'true');
    });

    it('renders the button as enabled with a url in href ready for download', () => {
      const expectedHref = '/api/v2/search/advanced/export/csv?query=componentName%3A%20*&allComponents=true';
      renderComponent({ searchedQuery: 'componentName: *', searchIncludedAllComponents: true });

      const aTag = screen.getByText('Export Results').closest('a');
      expect(aTag).toBeVisible();
      expect(aTag).not.toHaveClass('disabled');
      expect(aTag).toHaveAttribute('aria-disabled', 'false');
      expect(aTag).toHaveAttribute('download');

      expect(aTag).toHaveAttribute('href', expectedHref);
    });
  });
});
