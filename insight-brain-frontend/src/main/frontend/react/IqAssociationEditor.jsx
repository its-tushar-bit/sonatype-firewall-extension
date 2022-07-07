/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxCheckbox, getUniqueId, NxFontAwesomeIcon, NxRadio, NxErrorAlert } from '@sonatype/react-shared-components';
import cx from 'classnames';
import { angularToRscColorMap } from 'MainRoot/OrgsAndPolicies/utility/util';

import Hexagon from './Hexagon';

/**
 * Renders a FieldSet with either NxRadios or NxCheckboxes based on the fieldType
 */
export const FieldType = Object.freeze({
  Radio: 'Radio',
  CheckBox: 'CheckBox',
});

export const IqAssociationEditor = ({
  items,
  label,
  fieldType,
  selectedParam,
  icon,
  description,
  disabled,
  onChange,
  isRequired,
  emptyItemsText = 'There are no items configured',
}) => {
  const onToggleSelected = (item) => {
    const updatedItem = {
      ...item,
      [selectedParam]: !item[selectedParam],
    };
    onChange(updatedItem);
  };

  const isFieldTypeCheckbox = fieldType === FieldType.CheckBox;
  const InputField = isFieldTypeCheckbox ? NxCheckbox : NxRadio;
  const associationEditorId = getUniqueId('association-editor-title');
  const hasItems = items?.length;

  return (
    // Using div instead of fieldset due to chromium bug for multi-column layout https://bugs.chromium.org/p/chromium/issues/detail?id=874051
    <div
      className={cx(
        'nx-fieldset iq-association-editor',
        { 'iq-association-editor--multi-column': items?.length >= 10 },
        { 'iq-association-editor--full-width': !hasItems }
      )}
      role="group"
      aria-labelledby={associationEditorId}
    >
      <div className={cx('nx-legend', { 'nx-legend--optional': !isRequired })}>
        <span id={associationEditorId} className="nx-legend__text">
          {label}
        </span>
      </div>
      {hasItems ? (
        items.map((item, index) => {
          const iconIsHexagon = icon === 'hexagon';
          const color = item.color ? `nx-selectable-color--${angularToRscColorMap[item.color]}` : '';

          const renderIcon = () =>
            iconIsHexagon ? <Hexagon className={color} /> : <NxFontAwesomeIcon icon={icon} className={color} />;

          return (
            <InputField
              key={index}
              value={item[description]}
              disabled={disabled}
              isChecked={item[selectedParam]}
              onChange={() => onToggleSelected(item)}
            >
              {renderIcon()}
              <span className="iq-association-editor__description">{item[description]}</span>
            </InputField>
          );
        })
      ) : (
        <NxErrorAlert>{emptyItemsText}</NxErrorAlert>
      )}
    </div>
  );
};

IqAssociationEditor.propTypes = {
  items: PropTypes.arrayOf(PropTypes.shape({ color: PropTypes.string })).isRequired,
  label: PropTypes.string,
  fieldType: PropTypes.oneOf([FieldType.CheckBox, FieldType.Radio]),
  onChange: PropTypes.func,
  selected: PropTypes.bool,
  selectedParam: PropTypes.string,
  description: PropTypes.string,
  icon: PropTypes.oneOfType([PropTypes.shape({}), PropTypes.string]),
  disabled: PropTypes.bool,
  isRequired: PropTypes.bool,
  emptyItemsText: PropTypes.string,
};
