/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { Fragment } from 'react';
import {NxButton, NxTextInput} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function AdvancedSearchForm(props) {
  const {
    searchFormSubmit,
    setCurrentQuery,
    getQuerySuggestions,
    currentQuery,
    querySuggestions
  } = props;

  function queryInputOnChangeHandler(e) {
    setCurrentQuery(e);
    getQuerySuggestions();
  }

  function formOnSubmitHandler(e) {
    e.preventDefault();
    if (currentQuery) {
      searchFormSubmit();
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
      <form className="nx-form nx-form--simple nx-form--advanced-search"
            id="advanced-search-form"
            onSubmit={formOnSubmitHandler}>
        <NxTextInput id="advanced-search-input"
                     className="nx-text-input nx-text-input--advanced-search"
                     list="advanced-search-suggestions-list"
                     isPristine={currentQuery === ''}
                     onChange={queryInputOnChangeHandler}
                     value={currentQuery}
                     placeholder="Start typing your query for suggestions"
        />
        <datalist id="advanced-search-suggestions-list">
          {
            querySuggestions &&
            querySuggestions.map(suggestion => {
              return <option key={suggestion} value={suggestion}/>;
            })
          }
        </datalist>
        <NxButton id="advanced-search-button"
                  inline
                  variant="primary"
                  disabled={!currentQuery}>
          Search
        </NxButton>
      </form>
      <div className="nx-tile">
        <div className="nx-tile__actions">
          {
            numberOfPages() !== 0 &&
              <span id="advanced-search-current-page-info">
                Page {props.searchResult.page} of {numberOfPages()}
              </span>
          }
          <NxButton id="advanced-search-previous-page-button"
                    disabled={props.searchResult.page <= 1}
                    onClick={previousPageHandler}>
            Previous
          </NxButton>
          <NxButton id="advanced-search-next-page-button"
                    disabled={props.searchResult.page >= numberOfPages() }
                    onClick={nextPageHandler}>
            Next
          </NxButton>
        </div>
        <div className="nx-tile-header">
          <div className="nx-tile-header__title">
            <h2 id="advanced-search-result-count" className="nx-h2">Results: {props.searchResult.totalNumberOfHits}</h2>
          </div>
        </div>
      </div>
    </Fragment>
  );
}

AdvancedSearchForm.propTypes = {
  // actions
  setCurrentQuery: PropTypes.func.isRequired,
  getQuerySuggestions: PropTypes.func.isRequired,
  searchFormSubmit: PropTypes.func.isRequired,
  // formState
  querySuggestions: PropTypes.array,
  currentQuery: PropTypes.string.isRequired,
  searchResult: PropTypes.object,
  totalNumberOfHits: PropTypes.number
};
