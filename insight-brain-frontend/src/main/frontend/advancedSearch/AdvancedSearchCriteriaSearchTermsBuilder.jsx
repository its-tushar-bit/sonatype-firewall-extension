/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import { NxCode, NxDivider, NxH2, NxH4, NxSelectableTag, NxTile } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useSelector } from 'react-redux';
import { getQueryBuilderGroups } from './utils';

function PrefixTag({ prefix, currentQuery, setCurrentQuery, onClick }) {
  const prefixTagOnClickHandler = () => {
    setCurrentQuery(currentQuery.trim() + (currentQuery.trim() !== '' ? ' ' : '') + prefix + ':');
    onClick();
  };

  return (
    <NxSelectableTag
      id={'advanced-search-query-builder-tag-' + prefix}
      onSelect={prefixTagOnClickHandler}
      selected={currentQuery.indexOf(prefix) !== -1}
      role="button"
      aria-label={`Add ${prefix} to search query`}
    >
      {prefix}
    </NxSelectableTag>
  );
}

PrefixTag.propTypes = {
  prefix: PropTypes.string.isRequired,
  currentQuery: PropTypes.string.isRequired,
  setCurrentQuery: PropTypes.func.isRequired,
  onClick: PropTypes.func.isRequired,
};

function PrefixRow({ prefix, currentQuery, setCurrentQuery, onSelectTag }) {
  return (
    <div className="iq-adv-search__query-builder-prefix-row">
      <PrefixTag
        prefix={prefix.value}
        currentQuery={currentQuery}
        setCurrentQuery={setCurrentQuery}
        onClick={onSelectTag}
      />
      <div className="iq-adv-search__query-builder-prefix-row-content">
        <p className="iq-adv-search__query-builder-prefix-row-content-label">Filter by {prefix.label}</p>
        <p className="iq-adv-search__query-builder-prefix-row-content-example">
          e.g{' '}
          <NxCode>
            {prefix.value}:{prefix.example}
          </NxCode>
        </p>
      </div>
    </div>
  );
}

PrefixRow.propTypes = {
  prefix: PropTypes.object.isRequired,
  currentQuery: PropTypes.string.isRequired,
  setCurrentQuery: PropTypes.func.isRequired,
  onSelectTag: PropTypes.func.isRequired,
};

export default function AdvancedSearchCriteriaSearchTermsBuilder(props) {
  const { setCurrentQuery, currentQuery, onSelectTag, builderRef } = props;
  const isSbomManager = useSelector(selectIsSbomManager);

  const queryBuilderGroups = useMemo(() => getQueryBuilderGroups(isSbomManager), [isSbomManager]);

  return (
    <div id="iq-adv-search__query-builder-search-terms" className="iq-adv-search__query-builder" ref={builderRef}>
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Search Terms</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxDivider />
          <div className="iq-adv-search__query-builder-content">
            {queryBuilderGroups.map((group) => {
              return (
                <div key={group.value}>
                  <NxH4>{group.label}</NxH4>
                  {group.prefixList.map((prefix) => (
                    <PrefixRow
                      prefix={prefix}
                      key={prefix.value}
                      currentQuery={currentQuery}
                      setCurrentQuery={setCurrentQuery}
                      onSelectTag={onSelectTag}
                    />
                  ))}
                </div>
              );
            })}
          </div>
        </NxTile.Content>
      </NxTile>
    </div>
  );
}

AdvancedSearchCriteriaSearchTermsBuilder.propTypes = {
  setCurrentQuery: PropTypes.func.isRequired,
  currentQuery: PropTypes.string.isRequired,
  onSelectTag: PropTypes.func.isRequired,
  builderRef: PropTypes.object.isRequired,
};
