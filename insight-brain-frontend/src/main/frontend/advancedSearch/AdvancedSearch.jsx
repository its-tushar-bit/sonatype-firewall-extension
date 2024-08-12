/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import LoadWrapper from '../react/LoadWrapper';
import * as PropTypes from 'prop-types';
import { NxButtonBar, NxTextLink } from '@sonatype/react-shared-components';

import AdvancedSearchForm from './AdvancedSearchForm';
import AdvancedSearchResultCard from './AdvancedSearchResultCard';
import AdvancedSearchExportButton from 'MainRoot/advancedSearch/AdvancedSearchExportButton';
import { includes } from 'ramda';
import { selectIsDeveloper } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useSelector } from 'react-redux';

export default function AdvancedSearch(props) {
  // Actions
  const { load, searchFormSubmit } = props;

  // viewState
  const { loading, loadError: loadErrorProp, waitingSearchResponse } = props;

  // formState
  const {
    searchResult: { groupingByDTOS, totalNumberOfHits },
    searchedQuery,
    searchIncludedAllComponents,
    queryError,
    isSbomManager,
    noSbomManagerEnabledError,
    setCurrentQuery,
    routerCurrentParams,
  } = props;

  // configurationState
  const { isEnabled } = props;

  const exportButtonProps = {
    loading,
    waitingSearchResponse,
    totalNumberOfHits,
    searchedQuery,
    searchIncludedAllComponents,
  };

  const { $state } = props;

  const loadError = isEnabled ? loadErrorProp || noSbomManagerEnabledError : 'Advanced Search is not turned on!';

  const isDeveloper = useSelector(selectIsDeveloper);

  useEffect(() => {
    load();

    if (routerCurrentParams?.search) {
      setCurrentQuery(routerCurrentParams.search);
      searchFormSubmit();
    }
  }, []);

  return (
    <main id="advanced-search-page" className="nx-page-main nx-page-main--advanced-search">
      <LoadWrapper loading={loading} error={loadError} retryHandler={load}>
        <div className="nx-page-title">
          <h1 className="nx-h1" id="advanced-search-page-title">
            Advanced Search
          </h1>
          <NxButtonBar>
            <AdvancedSearchExportButton {...exportButtonProps} />
          </NxButtonBar>
        </div>
        <AdvancedSearchForm {...props} />
        <LoadWrapper loading={waitingSearchResponse} error={queryError} retryHandler={() => searchFormSubmit()}>
          {groupingByDTOS.map(advancedSearchResultsGroupedBy)}
        </LoadWrapper>
      </LoadWrapper>
    </main>
  );

  function advancedSearchResultsGroupedBy(groupingByDto, index) {
    const { groupBy, additionalInfo, groupIdentifier, searchResultItemDTOS } = groupingByDto,
      detailedInfoHref = $state.href($state.get('vulnerabilitySearchDetail'), {
        id: groupBy,
      }),
      showVulnLink = !isSbomManager && includes(groupIdentifier, ['VULNERABILITY_ID', 'VULNERABILITY_DESCRIPTION']),
      headerId = `iq-advanced-search-result-group-${index}-header`;

    return (
      <section key={groupBy} aria-labelledby={headerId} className="nx-tile">
        <header className="nx-tile-header">
          <div className="nx-tile-header__title">
            <h2 id={headerId} className="nx-h2">
              {groupBy}
            </h2>
          </div>
        </header>
        <div className="nx-tile-content">
          {additionalInfo && <p className="nx-p">{additionalInfo}</p>}
          {showVulnLink && (
            <p className="nx-p">
              <NxTextLink href={detailedInfoHref} external={isDeveloper}>
                Click here for detailed information.
              </NxTextLink>
            </p>
          )}
          {searchResultItemDTOS.map((searchResultItem) => (
            <AdvancedSearchResultCard
              {...{ searchResultItem, groupIdentifier, isSbomManager, $state }}
              key={searchResultItem.resultIndex}
            />
          ))}
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
  isSbomManager: PropTypes.bool,
  noSbomManagerEnabledError: PropTypes.string,
  queryError: PropTypes.object,
  $state: PropTypes.object.isRequired,
};
