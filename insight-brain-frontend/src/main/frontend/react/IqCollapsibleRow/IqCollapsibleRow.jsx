/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxTableCell,
  NxTableRow,
  useToggle,
  NxFontAwesomeIcon,
  NxH3,
  NxButton,
  NxButtonBar,
} from '@sonatype/react-shared-components';
import { faCaretDown, faCaretRight } from '@fortawesome/pro-solid-svg-icons';
import classnames from 'classnames';

function CollapsibleRow({
  headerTitle,
  noItemsMessage,
  isCollapsible = true,
  colSpan,
  rowBtnIcon,
  rowBtnTitle,
  rowBtnAction,
  textEllipsis = false,
  children,
}) {
  const [open, toggleOpen] = useToggle(true);
  const iconCollapse = open ? faCaretDown : faCaretRight;
  const isClickable = Boolean(children) && isCollapsible;

  const EmptyMessage = () => (
    <NxTableRow className="iq-collapsible-row__empty-message">
      <NxTableCell className="nx-cell--meta-info" colSpan="100%">
        {noItemsMessage}
      </NxTableCell>
    </NxTableRow>
  );

  return (
    <>
      <NxTableRow className="iq-collapsible-row iq-collapsible-row__title-row">
        <NxTableCell
          colSpan={colSpan || '100%'}
          className={classnames('iq-collapsible-row__header', {
            'nx-clickable': isClickable,
          })}
          onClick={() => isClickable && toggleOpen()}
        >
          <span>
            {isClickable && (
              <NxFontAwesomeIcon icon={iconCollapse} className="iq-collapsible-row__header-icon" color="black" />
            )}
            <NxH3 className={`iq-collapsible-row__header-title ${textEllipsis ? 'iq-truncate-ellipsis' : ''}`}>
              {headerTitle}
            </NxH3>
          </span>
        </NxTableCell>
        {rowBtnIcon && rowBtnTitle && rowBtnAction && (
          <NxTableCell>
            <NxButtonBar>
              <NxButton variant="icon-only" title={rowBtnTitle} onClick={rowBtnAction}>
                <NxFontAwesomeIcon icon={rowBtnIcon} />
              </NxButton>
            </NxButtonBar>
          </NxTableCell>
        )}
      </NxTableRow>
      {open && (children || <EmptyMessage />)}
    </>
  );
}

CollapsibleRow.propTypes = {
  headerTitle: PropTypes.string.isRequired,
  noItemsMessage: PropTypes.string.isRequired,
  isCollapsible: PropTypes.bool,
  colSpan: PropTypes.number,
  rowBtnIcon: PropTypes.object,
  rowBtnTitle: PropTypes.string,
  rowBtnAction: PropTypes.func,
  textEllipsis: PropTypes.bool,
  children: PropTypes.node,
};

export default React.memo(CollapsibleRow);
