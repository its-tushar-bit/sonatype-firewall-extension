/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { faCopy } from '@fortawesome/free-solid-svg-icons';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxTextInput,
  NxTooltip,
  NxButtonBar,
  NxFormGroup,
} from '@sonatype/react-shared-components';

export default function UserTokenDisplay({ userToken }) {
  const { userCode, passCode } = userToken;

  const onSubmit = (evt) => {
    evt.preventDefault();
  };

  const copyToClipboard = (textToCopy) => navigator.clipboard.writeText(textToCopy);

  return (
    <form onSubmit={onSubmit}>
      <div className="nx-form-row">
        <NxFormGroup label="User Code" isRequired>
          <NxTextInput
            id="user-token-usercode"
            isPristine={true}
            value={userCode}
            className="user-token-modal__input visual-testing-ignore"
          />
        </NxFormGroup>
        <NxButtonBar>
          <NxTooltip title="Copy to clipboard">
            <NxButton id="user-token-copy-usercode" variant="tertiary" onClick={() => copyToClipboard(userCode)}>
              <NxFontAwesomeIcon icon={faCopy} />
            </NxButton>
          </NxTooltip>
        </NxButtonBar>
      </div>
      <div className="nx-form-row">
        <NxFormGroup label="Passcode" isRequired>
          <NxTextInput
            id="user-token-passcode"
            isPristine={true}
            value={passCode}
            className="user-token-modal__input visual-testing-ignore"
          />
        </NxFormGroup>
        <NxButtonBar>
          <NxTooltip title="Copy to clipboard">
            <NxButton id="user-token-copy-passcode" variant="tertiary" onClick={() => copyToClipboard(passCode)}>
              <NxFontAwesomeIcon icon={faCopy} />
            </NxButton>
          </NxTooltip>
        </NxButtonBar>
      </div>
    </form>
  );
}

export const userTokenType = {
  userCode: PropTypes.string.isRequired,
  passCode: PropTypes.string.isRequired,
};

UserTokenDisplay.propTypes = {
  userToken: PropTypes.shape(userTokenType).isRequired,
};
