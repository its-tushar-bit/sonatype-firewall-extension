/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { Fragment, useEffect, useRef, useState } from 'react';
import {
  NxButton,
  NxTextInput,
  NxRadio,
  NxButtonBar,
  NxTile,
  NxH2,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import AdvancedSearchHelp from './AdvancedSearchHelp';
import AdvancedSearchCriteriaEasyBuilder from './AdvancedSearchCriteriaEasyBuilder';
import AdvancedSearchCriteriaSearchTermsBuilder from './AdvancedSearchCriteriaSearchTermsBuilder';
import { faCaretDown, faCaretRight } from '@fortawesome/pro-solid-svg-icons';

export default function AdvancedSearchForm(props) {
  const {
    searchFormSubmit,
    setCurrentQuery,
    currentQuery,
    setShowAllComponentResults,
    isShowingAllComponentResults,
    isToggleComponentResultsEnabled,
    removeSearchItem,
    addSearchItem,
    searchItems,
    setEasyQueryField,
    setEasyQueryValue,
    searchResult: { page, totalNumberOfHits },
  } = props;

  const [selectedCriteriaBuilder, setSelectedCriteriaBuilder] = useState(null);
  const inputFieldId = 'advanced-search-input';
  const builderRef = useRef(null);
  const inputFieldRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      // Check if the target still exists in the DOM before using it
      if (
        builderRef.current &&
        event.target &&
        document.contains(event.target) &&
        !builderRef.current.contains(event.target)
      ) {
        setSelectedCriteriaBuilder(null);
      }
    };
    if (selectedCriteriaBuilder) {
      // Defer adding the listener so the click event that opened the builder
      // finishes propagating first. In React 19, useEffect can run as a
      // microtask before the originating click event reaches the document,
      // which would immediately trigger handleClickOutside and close the builder.
      const timeoutId = setTimeout(() => {
        document.addEventListener('click', handleClickOutside);
      }, 0);
      return () => {
        clearTimeout(timeoutId);
        document.removeEventListener('click', handleClickOutside);
      };
    }
  }, [selectedCriteriaBuilder]);

  useEffect(() => {
    if (searchItems.length === 0) {
      addSearchItem();
    }
  }, []);

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
      setSelectedCriteriaBuilder(null);
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
      <NxTile>
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
                  ref={inputFieldRef}
                  placeholder={'Enter CVE ID or use the "Use query builder" or "Add search terms" buttons below.'}
                />
              </label>
              <section role="region" aria-label="Advanced Search Builder">
                {selectedCriteriaBuilder === 'queryBuilder' && (
                  <AdvancedSearchCriteriaEasyBuilder
                    searchItems={searchItems}
                    setField={setEasyQueryField}
                    setValue={setEasyQueryValue}
                    removeSearchItem={removeSearchItem}
                    addSearchItem={addSearchItem}
                    builderRef={builderRef}
                  />
                )}
                {selectedCriteriaBuilder === 'searchTerms' && (
                  <AdvancedSearchCriteriaSearchTermsBuilder
                    setCurrentQuery={setCurrentQuery}
                    currentQuery={currentQuery}
                    onSelectTag={() => inputFieldRef.current.focus()}
                    builderRef={builderRef}
                  />
                )}
              </section>
            </div>
            <NxButtonBar>
              <NxButton id="advanced-search-button" variant="primary" disabled={!currentQuery}>
                Search
              </NxButton>
            </NxButtonBar>
          </div>
          <fieldset className="nx-fieldset">
            <NxButton
              className="iq-adv-search__query-builder-button"
              onClick={() =>
                selectedCriteriaBuilder === 'queryBuilder'
                  ? setSelectedCriteriaBuilder(null)
                  : setSelectedCriteriaBuilder('queryBuilder')
              }
              type="button"
            >
              Use Query Builder{' '}
              {selectedCriteriaBuilder === 'queryBuilder' ? (
                <NxFontAwesomeIcon icon={faCaretDown} />
              ) : (
                <NxFontAwesomeIcon icon={faCaretRight} />
              )}
            </NxButton>
            <NxButton
              className="iq-adv-search__search-terms-button"
              onClick={() =>
                selectedCriteriaBuilder === 'searchTerms'
                  ? setSelectedCriteriaBuilder(null)
                  : setSelectedCriteriaBuilder('searchTerms')
              }
              type="button"
            >
              Add Search Terms{' '}
              {selectedCriteriaBuilder === 'searchTerms' ? (
                <NxFontAwesomeIcon icon={faCaretDown} />
              ) : (
                <NxFontAwesomeIcon icon={faCaretRight} />
              )}
            </NxButton>
          </fieldset>
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
      </NxTile>
      <AdvancedSearchHelp />
      <NxTile className="iq-adv-search__results-control-tile">
        <div className="nx-tile-content">
          <NxH2 id="advanced-search-result-count">Results: {totalNumberOfHits}</NxH2>
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
      </NxTile>
    </Fragment>
  );
}

AdvancedSearchForm.propTypes = {
  // actions
  setCurrentQuery: PropTypes.func.isRequired,
  setShowAllComponentResults: PropTypes.func.isRequired,
  searchFormSubmit: PropTypes.func.isRequired,
  removeSearchItem: PropTypes.func.isRequired,
  addSearchItem: PropTypes.func.isRequired,
  searchItems: PropTypes.array.isRequired,
  setEasyQueryField: PropTypes.func.isRequired,
  setEasyQueryValue: PropTypes.func.isRequired,
  // formState
  currentQuery: PropTypes.string.isRequired,
  searchResult: PropTypes.object,
  totalNumberOfHits: PropTypes.number,
  isShowingAllComponentResults: PropTypes.bool.isRequired,
  isToggleComponentResultsEnabled: PropTypes.bool.isRequired,
};
