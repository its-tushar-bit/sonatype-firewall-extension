/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React from 'react';
import {
  NxModal,
  NxH2,
  NxInfoAlert,
  NxH3,
  NxList,
  NxButton,
  NxWarningAlert,
  NxFooter,
  NxButtonBar,
} from '@sonatype/react-shared-components';

const SuccessMoveAppModal = ({ warnings, closeModal }) => {
  return (
    <NxModal id="success-move-application-modal" onCancel={closeModal}>
      <NxModal.Header>
        <NxH2>Application Moved Successfully</NxH2>
      </NxModal.Header>

      <NxModal.Content>
        <NxInfoAlert>
          <NxH3>Please check the following configurations to ensure they do what you expect :</NxH3>
          <NxList bulleted>
            <NxList.Item>
              <NxList.Text>Policy Notifications</NxList.Text>
            </NxList.Item>
            <NxList.Item>
              <NxList.Text>User Access</NxList.Text>
            </NxList.Item>
          </NxList>
        </NxInfoAlert>
        {warnings?.length > 0 && (
          <NxWarningAlert>
            <NxH3>Policy configurations that differ between the old and new parent:</NxH3>
            <NxList bulleted>
              {warnings.map((warning, index) => (
                <NxList.Item key={index}>
                  <NxList.Text>{warning}</NxList.Text>
                </NxList.Item>
              ))}
            </NxList>
          </NxWarningAlert>
        )}
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton variant="primary" onClick={closeModal}>
            OK
          </NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  );
};

SuccessMoveAppModal.propTypes = {
  closeModal: PropTypes.func.isRequired,
  warnings: PropTypes.object,
};

export default SuccessMoveAppModal;
