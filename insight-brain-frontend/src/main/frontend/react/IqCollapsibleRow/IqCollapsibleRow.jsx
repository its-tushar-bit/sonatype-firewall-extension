/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTableCell, NxTableRow, useToggle, NxFontAwesomeIcon, NxH3 } from '@sonatype/react-shared-components';
import { faCaretDown, faCaretRight } from '@fortawesome/pro-solid-svg-icons';
import cx from 'classnames';

function CollapsibleRow({ headerTitle = 'My title', noItemsMessage = '', children }) {
  const [open, toggleOpen] = useToggle(true);
  const iconCollapse = open ? faCaretDown : faCaretRight;
  const isClickable = Boolean(children);
  const headerTitleClass = cx('iq-collasible-row__header-title', {
    'empty-message': !isClickable,
  });

  const EmptyMessage = () => (
    <NxTableRow className="iq-collasible-row__empty-message">
      <NxTableCell colspan="100%">{noItemsMessage}</NxTableCell>
    </NxTableRow>
  );

  return (
    <>
      <NxTableRow className="iq-collasible-row" isClickable={isClickable} onClick={() => isClickable && toggleOpen()}>
        <NxTableCell colspan="100%" className="iq-collasible-row__header">
          <span>
            {isClickable && (
              <NxFontAwesomeIcon icon={iconCollapse} className="iq-collasible-row__header-icon" color="black" />
            )}
            <NxH3 className={headerTitleClass}>{headerTitle}</NxH3>
          </span>
        </NxTableCell>
      </NxTableRow>
      {open && (children || <EmptyMessage />)}
    </>
  );
}

CollapsibleRow.propTypes = {
  headerTitle: PropTypes.string.isRequired,
  noItemsMessage: PropTypes.string,
  children: PropTypes.node,
};

export default React.memo(CollapsibleRow);
