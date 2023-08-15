/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxCheckbox, NxTable, NxH2, NxOverflowTooltip, NxTooltip } from '@sonatype/react-shared-components';
import { sortItemsByFields } from 'MainRoot/util/sortUtils';

/**
 * @typedef {import('MainRoot/firewallOnboarding/types').Repository} Repository
 */

/**
 * @param {object} props
 * @param {string} props.title
 * @param {function} props.onChange
 * @param {Repository[]} props.repositories
 * @param {string[]=} props.supportedFormats
 * @param {string=} props.emptyMessage
 * @param {string=} props.labelItemPropName
 * @param {string=} props.checkItemPropName
 */
function FirewallRepositoryList({
  title,
  onChange,
  repositories,
  supportedFormats,
  emptyMessage = 'No list available',
  labelItemPropName = 'publicId',
  checkItemPropName = 'quarantineEnabled',
}) {
  const [items, setItems] = useState(repositories);
  const [sortFields, setSortFields] = useState(null);

  const columnSortField = 'publicId';
  const tableAriaLabel = `repository list for ${title}`;
  const isSupportedFormat = (format) => (supportedFormats ? supportedFormats.includes(format) : true);
  const isSupportedItem = ({ format }) => isSupportedFormat(format);
  const supportedItems = items.filter(isSupportedItem);
  const isAllItemSelected = supportedItems.length > 0 && supportedItems.every((item) => item[checkItemPropName]);
  const isAllItemsSupportedFormat = items.every((item) => isSupportedFormat(item.format));
  const isAllItemsUnsupportedFormat = items.every((item) => !isSupportedFormat(item.format));
  const isAllItemsSameFormat = items.every((item) => item.format === items[0].format);

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

  const handleSelectAll = (value) => {
    onChange(supportedItems.map(({ id }) => ({ id, key: checkItemPropName, value })));
  };

  const handleSelectItem = (item) => {
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
        disabled={!items.length || isAllItemsUnsupportedFormat}
      />
    </NxTable.Cell>
  );

  const renderItemRow = (/** @type {Repository} */ repo, /** @type {React.Key} */ index) => {
    const isChecked = repo[checkItemPropName];
    const ariaLabel = `firewall ${repo[labelItemPropName]} repository item`;

    return (
      <NxTable.Row key={index}>
        <NxTable.Cell>
          <NxCheckbox
            name={repo[labelItemPropName]}
            aria-label={ariaLabel}
            isChecked={isChecked}
            onChange={() => handleSelectItem(repo)}
            disabled={!isSupportedFormat(repo.format)}
          />
        </NxTable.Cell>
        <NxTable.Cell className="firewall-repository-list__item-name">
          <NxOverflowTooltip>
            <div className="nx-truncate-ellipsis">
              {repo[labelItemPropName]}
              {!isAllItemsSameFormat && <div className="firewall-repository-list__item-disabled">{repo.format}</div>}
            </div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        {!isAllItemsSupportedFormat && (
          <NxTable.Cell className="firewall-repository-list__item-icon">
            {!isSupportedFormat(repo.format) && (
              <NxTooltip
                title={`This repository with format '${repo.format}' is not supported by the Firewall. Please contact your administrator for more information.`}
              >
                <i className="fa fa-ban" data-testid="repo-disabled-icon"></i>
              </NxTooltip>
            )}
          </NxTable.Cell>
        )}
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
              {!isAllItemsSupportedFormat && (
                <NxTable.Cell className="firewall-repository-list__header-disabled"> </NxTable.Cell>
              )}
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
  supportedFormats: PropTypes.arrayOf(PropTypes.string),
  onChange: PropTypes.func.isRequired,
  checkItemPropName: PropTypes.string,
  labelItemPropName: PropTypes.string,
  emptyMessage: PropTypes.string,
};

export default React.memo(FirewallRepositoryList);
