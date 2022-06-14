/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import { faFile } from '@fortawesome/pro-regular-svg-icons';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';

import { getAdvancedSearchCsvExportUrl } from 'MainRoot/util/CLMLocation';

export default function AdvancedSearchExportButton({
  loading,
  waitingSearchResponse,
  totalNumberOfHits,
  searchedQuery,
  searchIncludedAllComponents,
}) {
  const isDisabled = loading || waitingSearchResponse || totalNumberOfHits === 0;
  const exportUrl = isDisabled ? '#' : getAdvancedSearchCsvExportUrl(searchedQuery, searchIncludedAllComponents);
  const buttonClasses = classNames('nx-btn nx-btn--tertiary', { disabled: isDisabled });

  return (
    <a
      id="iq-advanced-search-export"
      className={buttonClasses}
      href={exportUrl}
      onClick={(event) => (isDisabled ? event.preventDefault() : true)}
      aria-disabled={isDisabled}
      download
    >
      <NxFontAwesomeIcon icon={faFile} />
      <span>Export Results</span>
    </a>
  );
}

AdvancedSearchExportButton.propTypes = {
  loading: PropTypes.bool.isRequired,
  waitingSearchResponse: PropTypes.bool.isRequired,
  totalNumberOfHits: PropTypes.number.isRequired,
  searchedQuery: PropTypes.string,
  searchIncludedAllComponents: PropTypes.bool.isRequired,
};
