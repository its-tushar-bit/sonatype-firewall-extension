/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxCheckbox, NxTable, NxH2, NxOverflowTooltip } from '@sonatype/react-shared-components';
import { sortItemsByFields } from 'MainRoot/util/sortUtils';

function FirewallRepositoryList({
  title,
  onChange,
  repositories,
  emptyMessage = 'No list available',
  labelItemPropName = 'publicId',
  checkItemPropName = 'quarantineEnabled',
}) {
  const [items, setItems] = useState([...repositories]);
  const [sortFields, setSortFields] = useState(null);

  const columnSortField = 'publicId';
  const tableAriaLabel = `repository list for ${title}`;
  const isAllItemSelected = items.every((item) => item[checkItemPropName]);

  const totalConfiguredRepositories = () => {
    const totalConfiguredRepos = items.filter((items) => items[checkItemPropName] === true).length;
    return `${totalConfiguredRepos} of ${items.length}`;
  };

  useEffect(() => {
    if (sortFields) {
      setItems(sortItemsByFields([sortFields], repositories));
    } else {
      setItems(repositories);
    }
  }, [repositories]);

  const handleSelectAll = (event) => {
    onChange(items.map(({ id }) => ({ id, key: checkItemPropName, value: event.target.checked })));
  };

  const handleSelectItem = (event, item) => {
    onChange([
      {
        id: item.id,
        key: checkItemPropName,
        value: !item[checkItemPropName],
      },
    ]);
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

  const renderItemRow = (repo, index) => {
    const isChecked = repo[checkItemPropName];
    const ariaLabel = `firewall ${repo[labelItemPropName]} repository item`;

    return (
      <NxTable.Row key={index}>
        <NxTable.Cell className="firewall-repository-list__item-check">
          <NxCheckbox
            name={repo[labelItemPropName]}
            aria-label={ariaLabel}
            isChecked={isChecked}
            onChange={(event) => handleSelectItem(event, repo)}
          ></NxCheckbox>
        </NxTable.Cell>
        <NxTable.Cell className="firewall-repository-list__item-name">
          <NxOverflowTooltip>
            <div className="nx-truncate-ellipsis">{repo[labelItemPropName]}</div>
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
          {totalConfiguredRepositories()}
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
  repositories: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      repositoryManagerId: PropTypes.string,
      publicId: PropTypes.string,
      repositoryType: PropTypes.string,
      enabled: PropTypes.bool,
      quarantineEnabled: PropTypes.bool,
      policyCompliantComponentSelectionEnabled: PropTypes.bool,
      namespaceConfusionProtectionEnabled: PropTypes.bool,
      format: PropTypes.string,
    })
  ).isRequired,
  selectedRepositories: PropTypes.array,
  onChange: PropTypes.func.isRequired,
  checkItemPropName: PropTypes.string,
  labelItemPropName: PropTypes.string,
  emptyMessage: PropTypes.string,
};

export default React.memo(FirewallRepositoryList);
