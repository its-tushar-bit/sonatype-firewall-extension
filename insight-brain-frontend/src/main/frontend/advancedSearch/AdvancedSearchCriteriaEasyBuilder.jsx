/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useMemo } from 'react';
import {
  NxButton,
  NxDivider,
  NxFontAwesomeIcon,
  NxH2,
  NxH3,
  NxStatefulDropdown,
  NxTextInput,
  NxTile,
} from '@sonatype/react-shared-components';
import { faPlus, faSearch, faTrash } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useSelector } from 'react-redux';
import { getQueryBuilderGroups } from './utils';

function SearchRow({ searchItem, index, setField, setValue, removeSearchItem }) {
  const isSbomManager = useSelector(selectIsSbomManager);
  const searchTypes = useMemo(() => getQueryBuilderGroups(isSbomManager), [isSbomManager]);
  const dropdownOptions = searchTypes.map((searchType) => {
    return (
      <Fragment key={searchType.value}>
        {searchType.prefixList?.map((prefix) => (
          <Fragment key={prefix.value}>
            {prefix.show && searchType.show && (
              <button
                className="nx-dropdown-button"
                key={prefix.value}
                title={prefix.label}
                onClick={() => setField({ index, value: prefix })}
              >
                {prefix.label}
              </button>
            )}
          </Fragment>
        ))}
      </Fragment>
    );
  });

  return (
    <div className="iq-adv-search__query-row">
      {index > 0 && (
        <div className="iq-adv-search__operator">
          <NxStatefulDropdown label={searchItem.operator}>
            <button onClick={() => setValue({ index, value: 'OR', key: 'operator' })} className="nx-dropdown-button">
              OR
            </button>
            <button onClick={() => setValue({ index, value: 'AND', key: 'operator' })} className="nx-dropdown-button">
              AND
            </button>
          </NxStatefulDropdown>
        </div>
      )}
      <div className="iq-adv-search__field">
        <NxStatefulDropdown label={searchItem.field.label || 'Select Field'}>{dropdownOptions}</NxStatefulDropdown>
      </div>

      <div className="iq-adv-search__match">
        <NxStatefulDropdown label={searchItem.isExactMatch ? 'Exact Match' : 'Partial Match'}>
          <button onClick={() => setValue({ index, value: false, key: 'isExactMatch' })} className="nx-dropdown-button">
            Partial Match
          </button>
          <button onClick={() => setValue({ index, value: true, key: 'isExactMatch' })} className="nx-dropdown-button">
            Exact Match
          </button>
        </NxStatefulDropdown>
      </div>
      <div className="iq-adv-search__value">
        <NxTextInput
          value={searchItem.value}
          onChange={(value) => setValue({ index, value, key: 'value' })}
          isPristine={false}
          placeholder={searchItem.field.example?.replace(/"/g, '') || 'Enter Value'}
        />
      </div>
      <div className="iq-adv-search__trash">
        <NxButton onClick={() => removeSearchItem(index)} variant="icon-only" type="button" title="Remove">
          <NxFontAwesomeIcon icon={faTrash} title="Remove Search Item" />
        </NxButton>
      </div>
    </div>
  );
}

export default function AdvancedSearchCriteriaEasyBuilder(props) {
  const { searchItems, setField, setValue, removeSearchItem, addSearchItem, builderRef } = props;

  return (
    <div id="iq-adv-search__query-builder-easy" className="iq-adv-search__query-builder" ref={builderRef}>
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Build Query Rules</NxH2>
          </NxTile.HeaderTitle>
          <NxTile.HeaderActions>
            <NxButton onClick={addSearchItem} variant="primary" type="button">
              <NxFontAwesomeIcon icon={faPlus} />
              <span>Add Rule</span>
            </NxButton>
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content>
          <NxDivider />
          <div className="iq-adv-search__query-builder-content">
            {searchItems.length === 0 && (
              <div className="iq-adv-search__query-builder-content-empty" role="status">
                <NxFontAwesomeIcon className="iq-adv-search__query-builder-content-empty-icon" icon={faSearch} />
                <NxH3 className="iq-adv-search__query-builder-content-empty-title">Start Building Your Query</NxH3>
                <div className="iq-adv-search__query-builder-content-empty-description">
                  Add rules to search by specific criteria like application name, component version, or vulnerability
                  ID.
                </div>
              </div>
            )}
            {searchItems.map((searchItem, index) => (
              <SearchRow
                searchItem={searchItem}
                index={index}
                setField={setField}
                setValue={setValue}
                removeSearchItem={removeSearchItem}
                key={`search-item-${index}`}
              />
            ))}
          </div>
        </NxTile.Content>
      </NxTile>
    </div>
  );
}

SearchRow.propTypes = {
  searchItem: PropTypes.object.isRequired,
  index: PropTypes.number.isRequired,
  setField: PropTypes.func.isRequired,
  setValue: PropTypes.func.isRequired,
  removeSearchItem: PropTypes.func.isRequired,
};

AdvancedSearchCriteriaEasyBuilder.propTypes = {
  searchItems: PropTypes.array.isRequired,
  setField: PropTypes.func.isRequired,
  setValue: PropTypes.func.isRequired,
  removeSearchItem: PropTypes.func.isRequired,
  addSearchItem: PropTypes.func.isRequired,
  builderRef: PropTypes.object.isRequired,
};
