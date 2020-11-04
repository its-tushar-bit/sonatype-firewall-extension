/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {useEffect} from 'react';
import LoadWrapper from '../react/LoadWrapper';
import * as PropTypes from 'prop-types';
import MaximizedContainer from '../react/MaximizedContainer';
import AdvancedSearchForm from './AdvancedSearchForm';
import AdvancedSearchResultCard from './AdvancedSearchResultCard';

export default function AdvancedSearch(props) {
  // Actions
  const {
    load,
    searchFormSubmit
  } = props;

  // viewState
  const {
    loading,
    loadError: loadErrorProp,
    waitingSearchResponse
  } = props;

  // formState
  const {
    searchResult: {
      groupingByDTOS
    },
    queryError
  } = props;

  // configurationState
  const {
    isEnabled
  } = props;

  const {
    $state
  } = props;

  const loadError = isEnabled ? loadErrorProp : 'Advanced Search is not turned on!';

  useEffect(load, []);

  return (
    <MaximizedContainer id="advanced-search-page" className="nx-page-content">
      <main className="nx-page-main nx-page-main--advanced-search">
        <LoadWrapper loading={loading} error={loadError} retryHandler={load}>
          <div className="nx-page-title">
            <h1 className="nx-h1" id="advanced-search-page-title">Advanced Search</h1>
          </div>
          <AdvancedSearchForm {...props} />
          <LoadWrapper loading={waitingSearchResponse} error={queryError} retryHandler={() =>searchFormSubmit()}>
            {groupingByDTOS.map(advancedSearchResultsGroupedBy)}
          </LoadWrapper>
        </LoadWrapper>
      </main>
    </MaximizedContainer>
  );

  function advancedSearchResultsGroupedBy(groupingByDto) {
    const {
          groupBy,
          additionalInfo,
          groupIdentifier,
          searchResultItemDTOS
        } = groupingByDto,
        detailedInfoHref = $state.href($state.get('vulnerabilitySearchDetail'), {id: groupBy});

    return (
      <section key={groupBy} className="nx-tile">
        <header className="nx-tile-header">
          <div className="nx-tile-header__title">
            <h2 className="nx-h2">{groupBy}</h2>
          </div>
        </header>
        <div className="nx-tile-content">
          { additionalInfo && <p className="nx-p">{additionalInfo}</p> }
          { (groupIdentifier === 'VULNERABILITY_ID' || groupIdentifier === 'VULNERABILITY_DESCRIPTION') &&
            <p className="nx-p">
              <a href={detailedInfoHref}>Click here for detailed information.</a>
            </p>
          }
          {searchResultItemDTOS.map(searchResultItem =>
            <AdvancedSearchResultCard { ...({ searchResultItem, groupIdentifier, $state }) }
                                      key={searchResultItem.resultIndex} />
          )}
        </div>
      </section>
    );
  }
}

AdvancedSearch.propTypes = {
  load: PropTypes.func.isRequired,
  searchFormSubmit: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  waitingSearchResponse: PropTypes.bool.isRequired,
  loadError: PropTypes.object,
  isEnabled: PropTypes.bool.isRequired,
  searchResult: PropTypes.object,
  queryError: PropTypes.object,
  $state: PropTypes.object.isRequired
};
