/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { isEmpty, map } from 'ramda';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons/index';
import { NxDropdown, NxFontAwesomeIcon, NxButton } from '@sonatype/react-shared-components';

import DocumentClickListenerWrapper from '../../../react/DocumentClickListenerWrapper';

export default function ManageFiltersDropdown(props) {
  const {
    showDirtyAsterisk,
    applyDefaultFilter,
    applySavedFilter,
    filtersDropdownOpen,
    toggleFiltersDropdown,
    selectFilterToDelete,
    handleDocumentClick
  } = props;

  const savedFilters = props.savedFilters || [],
      appliedFilterName = props.appliedFilterName || 'Default';

  const handleKeyPress = event => {
    if ((event.key === 'Escape' || event.key === 'Esc') && filtersDropdownOpen) {
      toggleFiltersDropdown(false);
    }
  };

  const handleDropdownToggle = () => {
    toggleFiltersDropdown(!filtersDropdownOpen);
  };

  const handleDeleteFilter = ({ name }) => {
    selectFilterToDelete(name);
  };

  function getOptionClassNames(isSelected) {
    return classnames('iq-manage-filters-dropdown__option', {
      'iq-manage-filters-dropdown__option--selected': isSelected
    });
  }

  function getFilterOption(filter) {
    const isSelected = filter.name === appliedFilterName;

    return (
      <div key={filter.name} className={getOptionClassNames(isSelected)}>
        <button onClick={() => applySavedFilter(filter)}
                className="nx-dropdown-button nx-dropdown-button--select-filter">
          <span>{filter.name}</span>
        </button>
        <NxButton onClick={() => handleDeleteFilter(filter)} variant="tertiary" className="nx-btn--delete-filter">
          <NxFontAwesomeIcon icon={faTrashAlt}/>
        </NxButton>
      </div>
    );
  }

  const options = map(getFilterOption, savedFilters),
      dropdownLabel = (
        <Fragment>
          {showDirtyAsterisk && <span className="iq-manage-filters-dropdown__dirty-asterisk">*</span>}
          <span className="iq-manage-filters-dropdown__label">{appliedFilterName}</span>
        </Fragment>
      ),
      emptyListMessage = (
        <div className="nx-list">
          <div className="nx-list__item nx-list__item--empty">
            No saved filters
          </div>
        </div>
      );

  return (
    <NxDropdown className="iq-manage-filters-dropdown"
                isOpen={filtersDropdownOpen}
                onKeyDown={handleKeyPress}
                onToggleCollapse={handleDropdownToggle}
                label={dropdownLabel}
                tabIndex={0}
                variant="secondary">
      <DocumentClickListenerWrapper onDocumentClick={handleDocumentClick}>
        <div key='Default' className={getOptionClassNames('Default' === appliedFilterName)}>
          <button onClick={applyDefaultFilter}
                  className="nx-dropdown-button nx-dropdown-button--select-filter">
            <span>Default</span>
          </button>
        </div>
        {isEmpty(options) ? emptyListMessage : options}
      </DocumentClickListenerWrapper>
    </NxDropdown>
  );
}

ManageFiltersDropdown.propTypes = {
  appliedFilterName: PropTypes.string,
  showDirtyAsterisk: PropTypes.bool,
  savedFilters: PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string.isRequired
  })),
  applyDefaultFilter: PropTypes.func.isRequired,
  applySavedFilter: PropTypes.func.isRequired,
  filtersDropdownOpen: PropTypes.bool.isRequired,
  toggleFiltersDropdown: PropTypes.func.isRequired,
  selectFilterToDelete: PropTypes.func.isRequired,
  handleDocumentClick: PropTypes.func.isRequired
};
