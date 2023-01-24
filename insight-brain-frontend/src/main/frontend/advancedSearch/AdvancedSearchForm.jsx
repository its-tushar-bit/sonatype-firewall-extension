/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { Fragment, useState } from 'react';
import { NxButton, NxTextInput, NxRadio, NxButtonBar } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import AdvancedSearchHelp from './AdvancedSearchHelp';
import AdvancedSearchCriteriaBuilder from './AdvancedSearchCriteriaBuilder';

export default function AdvancedSearchForm(props) {
  const {
    searchFormSubmit,
    setCurrentQuery,
    currentQuery,
    setShowAllComponentResults,
    isShowingAllComponentResults,
    isToggleComponentResultsEnabled,
    searchResult: { page, totalNumberOfHits },
  } = props;

  const [showCriteriaBuilder, setShowCriteriaBuilder] = useState(false);
  const inputFieldId = 'advanced-search-input';

  function queryInputOnChangeHandler(e) {
    setCurrentQuery(e);
  }

  function setShowAllComponentResultsHandler(e) {
    const shouldShowAllComponentResults = e === 'show-all-components-true';
    setShowAllComponentResults(shouldShowAllComponentResults);
  }

  function formOnSubmitHandler(e) {
    e.preventDefault();
    if (currentQuery) {
      searchFormSubmit();
      setShowCriteriaBuilder(false);
    }
  }

  function nextPageHandler(e) {
    e.preventDefault();
    searchFormSubmit(1);
  }

  function previousPageHandler(e) {
    e.preventDefault();
    searchFormSubmit(-1);
  }

  function numberOfPages() {
    // Default page size is 10 and we are using the default
    const pageSize = 10;
    return Math.ceil(props.searchResult.totalNumberOfHits / pageSize);
  }

  return (
    <Fragment>
      <form id="advanced-search-form" onSubmit={formOnSubmitHandler}>
        <div className="nx-form-row">
          <div className="nx-form-group">
            <label className="nx-label">
              <NxTextInput
                id={inputFieldId}
                className="nx-text-input--advanced-search"
                isPristine={currentQuery === ''}
                onChange={queryInputOnChangeHandler}
                value={currentQuery}
              />
            </label>
          </div>
          <NxButtonBar>
            <NxButton id="advanced-search-button" variant="primary" disabled={!currentQuery}>
              Search
            </NxButton>
          </NxButtonBar>
        </div>
        {isToggleComponentResultsEnabled && (
          <fieldset className="nx-fieldset" id="filter-component-results-options">
            <NxRadio
              name="filter-component-results"
              value="show-all-components-false"
              onChange={setShowAllComponentResultsHandler}
              isChecked={!isShowingAllComponentResults}
              id="show-all-components-false"
            >
              Limit search results to components that have security vulnerabilities
            </NxRadio>
            <NxRadio
              name="filter-component-results"
              value="show-all-components-true"
              onChange={setShowAllComponentResultsHandler}
              isChecked={isShowingAllComponentResults}
              id="show-all-components-true"
              aria-label="show all components in search results"
            >
              Show all components
            </NxRadio>
          </fieldset>
        )}
      </form>
      <AdvancedSearchCriteriaBuilder
        {...props}
        inputFieldId={inputFieldId}
        showCriteriaBuilder={showCriteriaBuilder}
        setShowCriteriaBuilder={setShowCriteriaBuilder}
      />
      <AdvancedSearchHelp {...props} />
      <section className="nx-tile iq-adv-search__results-control-tile">
        <div className="nx-tile-content">
          <h2 id="advanced-search-result-count" className="nx-h2">
            Results: {totalNumberOfHits}
          </h2>
          <div className="nx-btn-bar">
            {numberOfPages() !== 0 && (
              <span id="advanced-search-current-page-info">
                Page {page} of {numberOfPages()}
              </span>
            )}
            <NxButton id="advanced-search-previous-page-button" disabled={page <= 1} onClick={previousPageHandler}>
              Previous
            </NxButton>
            <NxButton
              id="advanced-search-next-page-button"
              disabled={page >= numberOfPages()}
              onClick={nextPageHandler}
            >
              Next
            </NxButton>
          </div>
        </div>
      </section>
    </Fragment>
  );
}

AdvancedSearchForm.propTypes = {
  // actions
  setCurrentQuery: PropTypes.func.isRequired,
  setShowAllComponentResults: PropTypes.func.isRequired,
  searchFormSubmit: PropTypes.func.isRequired,
  // formState
  currentQuery: PropTypes.string.isRequired,
  searchResult: PropTypes.object,
  totalNumberOfHits: PropTypes.number,
  isShowingAllComponentResults: PropTypes.bool.isRequired,
  isToggleComponentResultsEnabled: PropTypes.bool.isRequired,
};
