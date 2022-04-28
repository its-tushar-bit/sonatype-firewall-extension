/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect, memo } from 'react';
import * as PropTypes from 'prop-types';
import { eqProps } from 'ramda';
import { NxTransferList } from '@sonatype/react-shared-components';

const availableItemsCountFormatter = (number) => `${number} Licenses Available`;
const selectedItemsCountFormatter = (number) => `${number} Licenses Selected`;

const OverriddenField = (props) => {
  const { licenseIds, setSelectedLicenses, onUnmount, allLicenses } = props,
    [availableItemsFilter, setAvailableItemsFilter] = useState(''),
    [selectedItemsFilter, setSelectedItemsFilter] = useState('');

  useEffect(() => {
    return () => {
      onUnmount();
    };
  }, []);

  const allLicensesFiltered = allLicenses.filter((license) => license.id !== 'Disabled');
  const selectedTransferItems = new Set(licenseIds);
  const onSelectTransferItem = (selectedTransferItemSet) => {
    setSelectedLicenses(Array.from(selectedTransferItemSet));
  };

  return (
    <NxTransferList
      className="nx-transfer-list--full-width iq-edit-licenses-form__overridden"
      availableItemsLabel="Available Licenses"
      selectedItemsLabel="Selected Licenses"
      availableItemsCountFormatter={availableItemsCountFormatter}
      selectedItemsCountFormatter={selectedItemsCountFormatter}
      allItems={allLicensesFiltered}
      selectedItems={selectedTransferItems}
      availableItemsFilter={availableItemsFilter}
      selectedItemsFilter={selectedItemsFilter}
      onAvailableItemsFilterChange={setAvailableItemsFilter}
      onSelectedItemsFilterChange={setSelectedItemsFilter}
      onChange={onSelectTransferItem}
    />
  );
};

OverriddenField.displayName = 'OverriddenField';
OverriddenField.propTypes = {
  allLicenses: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      displayName: PropTypes.string.isRequired,
    })
  ),
  licenseIds: PropTypes.arrayOf(PropTypes.string),
  setSelectedLicenses: PropTypes.func.isRequired,
  onUnmount: PropTypes.func.isRequired,
};

const areEqual = eqProps('licenseIds');

export default memo(OverriddenField, areEqual);
