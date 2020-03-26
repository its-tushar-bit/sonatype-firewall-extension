/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {Fragment, useEffect} from 'react';
import LoadWrapper from '../react/LoadWrapper';
import * as PropTypes from 'prop-types';
import MaximizedContainer from '../react/MaximizedContainer';
import AdvancedSearchForm from './AdvancedSearchForm';
import AdvancedSearchResultCard from './AdvancedSearchResultCard';
import {Messages} from '../util/CommonServices';
import {NxInfoAlert, NxErrorAlert} from '@sonatype/react-shared-components';

export default function AdvancedSearch(props) {
  // Actions
  const {
    load
  } = props;

  // viewState
  const {
    loading,
    error,
    waitingSearchResponse
  } = props;

  // formState
  const {
    searchResult,
    queryError
  } = props;

  // configurationState
  const {
    isEnabled
  } = props;

  const {
    $state
  } = props;

  useEffect(load, []);

  return (
    <LoadWrapper loading={loading} error={error}>
      <MaximizedContainer id="advanced-search-page" className="nx-root-container">
        <div className="nx-page-main">
          {
            !isEnabled &&
              <NxInfoAlert id="advanced-search-disabled-error">Advanced Search is not turned on!</NxInfoAlert>
          }
          {
            isEnabled &&
              <Fragment>
                <div className="nx-page-title">
                  <h1 className="nx-h1" id="advanced-search-page-title">Advanced Search</h1>
                </div>
                <AdvancedSearchForm {...props} />
                {
                  queryError &&
                    <NxErrorAlert id="advanced-search-query-error">
                      {Messages.getHttpErrorMessage(queryError)}
                    </NxErrorAlert>
                }
                {
                  <LoadWrapper loading={waitingSearchResponse}>
                    {
                      searchResult.groupingByDTOS.map(groupingByDTO => advancedSearchResultsGroupedBy(groupingByDTO))
                    }
                  </LoadWrapper>
                }
              </Fragment>
          }
        </div>
      </MaximizedContainer>
    </LoadWrapper>
  );

  function advancedSearchResultsGroupedBy(groupingByDTO) {
    return (
      <Fragment key={groupingByDTO.groupBy}>
        <div className="nx-tile">
          <div className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">{groupingByDTO.groupBy}</h2>
            </div>
            <div className="nx-tile-header__subtitle">{groupingByDTO.additionalInfo}</div>
            {
              (groupingByDTO.groupIdentifier === 'VULNERABILITY_ID' ||
              groupingByDTO.groupIdentifier === 'VULNERABILITY_DESCRIPTION') &&
              <div className="nx-tile-header__subtitle">
                Click <a href={$state.href($state.get('vulnerabilitySearchDetail'), {id: groupingByDTO.groupBy})}>
                here</a> for detailed information.
              </div>
            }
          </div>
          {groupingByDTO.searchResultItemDTOS.map(searchResultItem => {
            return (
              <div className="nx-tile-content nx-tile-content--adv-search-results" key={searchResultItem.resultIndex}>
                <AdvancedSearchResultCard searchResultItem={searchResultItem}
                                          groupIdentifier={groupingByDTO.groupIdentifier}
                                          $state={$state}/>
              </div>
            );
          })}
        </div>
      </Fragment>
    );
  }
}

AdvancedSearch.propTypes = {
  load: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  waitingSearchResponse: PropTypes.bool.isRequired,
  error: PropTypes.object,
  isEnabled: PropTypes.bool.isRequired,
  searchResult: PropTypes.object,
  queryError: PropTypes.object,
  $state: PropTypes.object.isRequired
};
