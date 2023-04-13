/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTableCell, NxTableRow, useToggle, NxFontAwesomeIcon, NxH3 } from '@sonatype/react-shared-components';
import { faCaretDown, faCaretRight } from '@fortawesome/pro-solid-svg-icons';

function CollapsibleRow({ headerTitle, noItemsMessage, isCollapsible = true, children }) {
  const [open, toggleOpen] = useToggle(true);
  const iconCollapse = open ? faCaretDown : faCaretRight;
  const isClickable = Boolean(children) && isCollapsible;

  const EmptyMessage = () => (
    <NxTableRow className="iq-collapsible-row__empty-message">
      <NxTableCell colSpan="100%">{noItemsMessage}</NxTableCell>
    </NxTableRow>
  );

  return (
    <>
      <NxTableRow
        className="iq-collapsible-row iq-collapsible-row__title-row"
        isClickable={isClickable}
        onClick={() => isClickable && toggleOpen()}
      >
        <NxTableCell colSpan="100%" className="iq-collapsible-row__header">
          <span>
            {isClickable && (
              <NxFontAwesomeIcon icon={iconCollapse} className="iq-collapsible-row__header-icon" color="black" />
            )}
            <NxH3 className="iq-collapsible-row__header-title">{headerTitle}</NxH3>
          </span>
        </NxTableCell>
      </NxTableRow>
      {open && (children || <EmptyMessage />)}
    </>
  );
}

CollapsibleRow.propTypes = {
  headerTitle: PropTypes.string.isRequired,
  noItemsMessage: PropTypes.string.isRequired,
  isCollapsible: PropTypes.bool,
  children: PropTypes.node,
};

export default React.memo(CollapsibleRow);
