/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import * as PropTypes from 'prop-types';
import { NxCheckbox, NxTable, NxH2, NxOverflowTooltip } from '@sonatype/react-shared-components';
import { sortItemsByFields } from 'MainRoot/util/sortUtils';

function FirewallRepositoryList({
  title,
  onChange,
  repositories,
  selectedRepositories = [],
  emptyMessage = 'No list available',
  labelPropName = 'publicId',
  checkPropName = 'id',
}) {
  const [selectedItems, setSelectedItems] = useState(selectedRepositories);
  const [items, setItems] = useState([...repositories]);
  const [sortFields, setSortFields] = useState(null);

  const columnSortField = 'publicId';
  const tableAriaLabel = `repository list for ${title}`;
  const isAllItemSelected = selectedItems.length === items.length;

  const handleSelectAll = (event) => {
    let updateSelectedItems = [];

    if (event.target.checked) {
      updateSelectedItems = [...items];
    }

    setSelectedItems(updateSelectedItems);
    onChange(updateSelectedItems);
  };

  const handleSelectItem = (event, item) => {
    let updateSelectedItems;

    if (event.target.checked) {
      updateSelectedItems = [...selectedItems, item];
    } else {
      updateSelectedItems = selectedItems.filter((itemSelected) => itemSelected[checkPropName] !== item[checkPropName]);
    }

    setSelectedItems(updateSelectedItems);
    onChange(updateSelectedItems);
  };

  const getSortDir = () => {
    const isSorted = Boolean(sortFields);
    if (!isSorted) {
      return null;
    }
    const isSortDesc = sortFields.includes('-');
    return isSortDesc ? 'desc' : 'asc';
  };

  const sortField = () => {
    // if sortFields is null starts sorting asc
    const updateSort =
      !sortFields || sortFields === `-${columnSortField}` ? `${columnSortField}` : `-${columnSortField}`;

    setItems(sortItemsByFields([updateSort], items));
    setSortFields(updateSort);
  };

  const headerCheckbox = (
    <NxTable.Cell className="firewall-repository-list__check-all">
      <NxCheckbox
        name="firewall-repository-list__check-all"
        aria-label="firewall repository list check all"
        isChecked={isAllItemSelected}
        onChange={handleSelectAll}
        disabled={!items.length}
      />
    </NxTable.Cell>
  );

  const renderItemRow = (item) => {
    const isChecked = selectedItems.some((selectedItem) => selectedItem[checkPropName] === item[checkPropName]);
    const ariaLabel = `firewall ${item[labelPropName]} repository item`;

    return (
      <NxTable.Row key={item[checkPropName]}>
        <NxTable.Cell className="firewall-repository-list__item">
          <NxCheckbox
            name={item[labelPropName]}
            aria-label={ariaLabel}
            isChecked={isChecked}
            onChange={(event) => handleSelectItem(event, item)}
          ></NxCheckbox>
        </NxTable.Cell>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <div className="nx-truncate-ellipsis">{item[labelPropName]}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
      </NxTable.Row>
    );
  };

  return (
    <div className="firewall-repository-list">
      <NxH2 className="firewall-repository-list__title">
        {title}
        <span className="firewall-repository-list__total-repos nx-counter nx-counter--active">
          {`${selectedItems.length} of ${items.length}`}
        </span>
      </NxH2>
      <div className="firewall-repository-list__table-container nx-scrollable nx-table-container nx-viewport-sized__scrollable">
        <NxTable className="nx-table--fixed-layout" aria-label={tableAriaLabel}>
          <NxTable.Head>
            <NxTable.Row>
              {headerCheckbox}
              <NxTable.Cell isSortable={items.length > 1} sortDir={getSortDir()} onClick={() => sortField()}>
                <span>name</span>
              </NxTable.Cell>
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body className="firewall-repositories-entries" isLoading={false} emptyMessage={emptyMessage}>
            {items.map(renderItemRow)}
          </NxTable.Body>
        </NxTable>
      </div>
    </div>
  );
}

FirewallRepositoryList.propTypes = {
  title: PropTypes.string.isRequired,
  repositories: PropTypes.array.isRequired,
  selectedRepositories: PropTypes.array,
  onChange: PropTypes.func.isRequired,
  checkPropName: PropTypes.string,
  labelPropName: PropTypes.string,
  emptyMessage: PropTypes.string,
};

export default React.memo(FirewallRepositoryList);
