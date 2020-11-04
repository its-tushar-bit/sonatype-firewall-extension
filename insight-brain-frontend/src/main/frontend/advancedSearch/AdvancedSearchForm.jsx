/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { Fragment, useState } from 'react';
import {NxButton, NxTextInput} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import AdvancedSearchHelp from './AdvancedSearchHelp';
import AdvancedSearchCriteriaBuilder from './AdvancedSearchCriteriaBuilder';

export default function AdvancedSearchForm(props) {
  const {
    searchFormSubmit,
    setCurrentQuery,
    currentQuery,
    searchResult: {
      page,
      totalNumberOfHits
    }
  } = props;

  const [showCriteriaBuilder, setShowCriteriaBuilder] = useState(false);

  const inputFieldId = 'advanced-search-input';

  function queryInputOnChangeHandler(e) {
    setCurrentQuery(e);
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
      <form className="nx-form nx-form--advanced-search" id="advanced-search-form" onSubmit={formOnSubmitHandler}>
        <div className="nx-form-row">
          <div className="nx-form-group">
            <label className="nx-label">
              <NxTextInput id={inputFieldId}
                           className="nx-text-input--advanced-search"
                           isPristine={currentQuery === ''}
                           onChange={queryInputOnChangeHandler}
                           value={currentQuery}/>
            </label>
          </div>
          <div className="nx-btn-bar">
            <NxButton id="advanced-search-button"
                      inline
                      variant="primary"
                      disabled={!currentQuery}>
              Search
            </NxButton>
          </div>
        </div>
      </form>
      <AdvancedSearchCriteriaBuilder {...props}
                                     inputFieldId={inputFieldId}
                                     showCriteriaBuilder={showCriteriaBuilder}
                                     setShowCriteriaBuilder={setShowCriteriaBuilder}/>
      <AdvancedSearchHelp {...props} />
      <section className="nx-tile">
        <header className="nx-tile-header">
          <div className="nx-tile-header__title">
            <h2 id="advanced-search-result-count" className="nx-h2">Results: {totalNumberOfHits}</h2>
          </div>
          <div className="nx-tile__actions">
            { numberOfPages() !== 0 &&
              <span id="advanced-search-current-page-info">Page {page} of {numberOfPages()}</span>
            }
            <NxButton id="advanced-search-previous-page-button" disabled={page <= 1} onClick={previousPageHandler}>
              Previous
            </NxButton>
            <NxButton id="advanced-search-next-page-button"
                      disabled={page >= numberOfPages()}
                      onClick={nextPageHandler}>
              Next
            </NxButton>
          </div>
        </header>
      </section>
    </Fragment>
  );
}

AdvancedSearchForm.propTypes = {
  // actions
  setCurrentQuery: PropTypes.func.isRequired,
  searchFormSubmit: PropTypes.func.isRequired,
  // formState
  currentQuery: PropTypes.string.isRequired,
  searchResult: PropTypes.object,
  totalNumberOfHits: PropTypes.number
};
