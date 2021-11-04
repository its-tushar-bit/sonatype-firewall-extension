/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';

import TransferListHalf from './TransferListHalf';

export default function TransferList({ available, selected, onAddItem, onRemoveItem }) {
  return (
    <div className="iq-transfer-list">
      <TransferListHalf title="Available Labels" isInAvailableItems={true} items={available} onItemChange={onAddItem} />
      <TransferListHalf
        title="Applied Labels"
        isInAvailableItems={false}
        items={selected}
        onItemChange={onRemoveItem}
      />
    </div>
  );
}

TransferList.propTypes = {
  selected: PropTypes.array.isRequired,
  available: PropTypes.array.isRequired,
  onAddItem: PropTypes.func.isRequired,
  onRemoveItem: PropTypes.func.isRequired,
};
