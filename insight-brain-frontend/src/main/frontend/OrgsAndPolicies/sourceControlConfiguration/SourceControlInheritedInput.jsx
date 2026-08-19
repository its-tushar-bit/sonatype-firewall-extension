/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React from 'react';
import { NxFieldset, NxFontAwesomeIcon, NxRadio, NxTooltip } from '@sonatype/react-shared-components';
import { faQuestionCircle } from '@fortawesome/pro-solid-svg-icons';
import { SCM_FEATURE_UNSUPPORTED_MESSAGE } from './utils';
import RenderMarkdown from 'MainRoot/react/RenderMarkdown';

const SourceControlInheritedInput = ({
  title,
  description,
  sourceControl,
  optionName,
  onChange,
  isDisabled,
  ...props
}) => {
  const label = (
    <>
      {title}{' '}
      <NxTooltip title={isDisabled ? '' : <RenderMarkdown>{description}</RenderMarkdown>}>
        <NxFontAwesomeIcon icon={faQuestionCircle} />
      </NxTooltip>
    </>
  );

  const disabled =
    (!sourceControl?.provider.rscValue.value && !sourceControl?.provider.isInherited) ||
    (sourceControl?.provider.isInherited && !sourceControl?.provider.parentValue.value) ||
    isDisabled;
  const { parentName, parentValue, value } = sourceControl ? sourceControl[optionName] : {};
  const inheritText = parentName
    ? `Inherit from ${parentName} (${parentValue ? 'Enabled' : 'Disabled'})`
    : 'Inherit (Not Configured)';

  return (
    <>
      <NxTooltip title={isDisabled ? SCM_FEATURE_UNSUPPORTED_MESSAGE : ''}>
        <NxFieldset label={label} disabled={disabled} {...props}>
          <NxRadio name={title} value={null} onChange={onChange} isChecked={value === null} disabled={disabled}>
            {inheritText}
          </NxRadio>

          <NxRadio name={title} value={'Enabled'} onChange={onChange} isChecked={value === true} disabled={disabled}>
            Enabled
          </NxRadio>

          <NxRadio name={title} value={'Disabled'} onChange={onChange} isChecked={value === false} disabled={disabled}>
            Disabled
          </NxRadio>
        </NxFieldset>
      </NxTooltip>
    </>
  );
};

SourceControlInheritedInput.propTypes = {
  description: PropTypes.string,
  onChange: PropTypes.func,
  optionName: PropTypes.string,
  sourceControl: PropTypes.object,
  title: PropTypes.string.isRequired,
  isDisabled: PropTypes.bool.isRequired,
};

export default SourceControlInheritedInput;
