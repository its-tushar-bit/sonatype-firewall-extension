/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useRef, useState } from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { isEmpty, map } from 'ramda';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons/index';
import { NxDropdown, NxFontAwesomeIcon, NxButton } from '@sonatype/react-shared-components';

import { DEFAULT_FILTER_NAME } from '../defaultFilter';
import useClickAway from '../../../react/useClickAway';
import useEscapeKeyStack from '../../../react/useEscapeKeyStack';

export default function ManageFiltersDropdown(props) {
  const { showDirtyAsterisk, applyDefaultFilter, applySavedFilter, selectFilterToDelete, DeleteFilterModal } = props;

  const ref = useRef(null);

  const [filtersDropdownOpen, toggleFiltersDropdown] = useState(false);

  useClickAway(ref, () => toggleFiltersDropdown(false));
  useEscapeKeyStack(filtersDropdownOpen, () => toggleFiltersDropdown(false));

  const savedFilters = props.savedFilters || [],
    appliedFilterName = props.appliedFilterName || DEFAULT_FILTER_NAME;

  const handleDropdownToggle = () => {
    toggleFiltersDropdown(!filtersDropdownOpen);
  };

  const handleDeleteFilter = ({ name }) => {
    selectFilterToDelete(name);
  };

  const handleSelectDefaultFilter = () => {
    applyDefaultFilter();
    toggleFiltersDropdown(false);
  };

  const handleSelectSavedFilter = (filter) => {
    applySavedFilter(filter);
    toggleFiltersDropdown(false);
  };

  function getOptionClassNames(isSelected) {
    return classnames('iq-manage-filters-dropdown__option', {
      'iq-manage-filters-dropdown__option--selected': isSelected,
    });
  }

  function getFilterOption(filter) {
    const isSelected = filter.name === appliedFilterName;

    return (
      <div key={filter.name} className={getOptionClassNames(isSelected)}>
        <button
          onClick={() => handleSelectSavedFilter(filter)}
          className="nx-dropdown-button nx-dropdown-button--select-filter"
        >
          <span>{filter.name}</span>
        </button>
        <NxButton
          onClick={() => handleDeleteFilter(filter)}
          variant="icon-only"
          className="nx-btn--delete-filter"
          title="Delete"
        >
          <NxFontAwesomeIcon icon={faTrashAlt} />
        </NxButton>
      </div>
    );
  }

  function preventDefault(event) {
    if (!event.target.classList.contains('nx-dropdown__toggle-label')) {
      event.preventDefault();
    }
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
        <div className="nx-list__item nx-list__item--empty">No saved filters</div>
      </div>
    );

  return (
    <div ref={ref}>
      <DeleteFilterModal />
      <NxDropdown
        className="iq-manage-filters-dropdown"
        isOpen={filtersDropdownOpen}
        onToggleCollapse={handleDropdownToggle}
        label={dropdownLabel}
        onCloseClick={preventDefault}
        onCloseKeyDown={preventDefault}
        tabIndex={0}
      >
        <div key="Default" className={getOptionClassNames(DEFAULT_FILTER_NAME === appliedFilterName)}>
          <button onClick={handleSelectDefaultFilter} className="nx-dropdown-button nx-dropdown-button--select-filter">
            <span>Default</span>
          </button>
        </div>
        <Fragment>{isEmpty(options) ? emptyListMessage : options}</Fragment>
      </NxDropdown>
    </div>
  );
}

ManageFiltersDropdown.propTypes = {
  appliedFilterName: PropTypes.string,
  showDirtyAsterisk: PropTypes.bool,
  savedFilters: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
    })
  ),
  applyDefaultFilter: PropTypes.func.isRequired,
  applySavedFilter: PropTypes.func.isRequired,
  selectFilterToDelete: PropTypes.func.isRequired,
  DeleteFilterModal: PropTypes.elementType,
};
