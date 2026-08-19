/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxSelectableTag, NxFieldset } from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';

import { rscColorMap } from '../../react/tag/ComponentLabelTag';

export default function TransferListHalf({ isInAvailableItems, items, title, onItemChange }) {
  return (
    <NxFieldset className="iq-transfer-list__half" label={title}>
      <div className="iq-transfer-list__control-box">
        <div className="iq-transfer-list__item-list">
          {items.map(({ color, description, id, label, ownerType, ownerId }) => (
            <div key={id}>
              <NxSelectableTag
                id={id}
                onSelect={() => {
                  onItemChange({ color, description, id, label, ownerId }, ownerType);
                }}
                selected={!isInAvailableItems}
                color={rscColorMap[color]}
              >
                {label}
              </NxSelectableTag>
            </div>
          ))}
        </div>
      </div>
    </NxFieldset>
  );
}

TransferListHalf.propTypes = {
  isInAvailableItems: PropTypes.bool.isRequired,
  items: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      color: PropTypes.string,
      label: PropTypes.string,
    })
  ).isRequired,
  onItemChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
};
