/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useMemo, memo, useCallback } from 'react';
import * as PropTypes from 'prop-types';
import { NxTransferList } from '@sonatype/react-shared-components';

const availableItemsCountFormatter = (number) => `${number} Licenses available`;
const selectedItemsCountFormatter = (number) => `${number} Licenses transferred`;

const LtgTransferList = ({ licenseIds, setSelectedLicenses, allLicenses }) => {
  const [availableItemsFilter, setAvailableItemsFilter] = useState('');
  const [selectedItemsFilter, setSelectedItemsFilter] = useState('');

  const selectedItems = useMemo(() => new Set(licenseIds), [licenseIds]);

  const onSelectTransferItem = useCallback((selectedTransferItemSet) => {
    setSelectedLicenses(Array.from(selectedTransferItemSet));
  }, []);

  return (
    <NxTransferList
      id="editor-ltg-included-licenses"
      availableItemsLabel="Available Licenses"
      selectedItemsLabel="Included Licenses"
      allItems={allLicenses}
      selectedItems={selectedItems}
      onChange={onSelectTransferItem}
      availableItemsFilter={availableItemsFilter}
      selectedItemsFilter={selectedItemsFilter}
      onAvailableItemsFilterChange={setAvailableItemsFilter}
      onSelectedItemsFilterChange={setSelectedItemsFilter}
      availableItemsCountFormatter={availableItemsCountFormatter}
      selectedItemsCountFormatter={selectedItemsCountFormatter}
    />
  );
};

LtgTransferList.displayName = 'LtgTransferList';
LtgTransferList.propTypes = {
  allLicenses: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      displayName: PropTypes.string.isRequired,
    })
  ),
  licenseIds: PropTypes.arrayOf(PropTypes.string),
  setSelectedLicenses: PropTypes.func.isRequired,
};

export default memo(LtgTransferList);
