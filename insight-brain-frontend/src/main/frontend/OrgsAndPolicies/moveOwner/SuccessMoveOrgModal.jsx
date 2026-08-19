/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React from 'react';
import {
  NxButton,
  NxButtonBar,
  NxFooter,
  NxH2,
  NxH3,
  NxInfoAlert,
  NxList,
  NxModal,
  NxP,
  NxTextLink,
  NxWarningAlert,
} from '@sonatype/react-shared-components';

const SuccessMoveOrgModal = ({ warnings, closeModal }) => {
  return (
    <NxModal id="success-move-org-modal" onCancel={closeModal}>
      <NxModal.Header>
        <NxH2>Organization Moved Successfully</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        {warnings?.length > 0 && (
          <NxWarningAlert>
            <NxList bulleted>
              {warnings.map((warning, index) => (
                <NxList.Item key={index}>
                  <NxList.Text>{warning.message}</NxList.Text>
                </NxList.Item>
              ))}
            </NxList>
          </NxWarningAlert>
        )}
        <NxInfoAlert>
          <NxP>
            Local configuration settings are unaffected and remain unchanged by a move. Be sure to confirm any
            previously inherited parent-level configurations to ensure the desired behavior.
          </NxP>
          <NxH3 className="move-owner-success-links-header">Additional Resources:</NxH3>
          <div className="move-owner-success-links">
            <NxList bulleted>
              <NxList.Item>
                <NxList.Text>
                  <NxTextLink href="https://links.sonatype.com/products/nxiq/doc/hierarchy-and-inheritance" external>
                    Hierarchy and Inheritance
                  </NxTextLink>
                </NxList.Text>
              </NxList.Item>
              <NxList.Item>
                <NxList.Text>
                  <NxTextLink
                    href="https://links.sonatype.com/products/nxiq/doc/add-edit-and-delete-organizations"
                    external
                  >
                    Moving Organizations
                  </NxTextLink>
                </NxList.Text>
              </NxList.Item>
            </NxList>
            <NxList bulleted>
              <NxList.Item>
                <NxList.Text>
                  <NxTextLink href="https://links.sonatype.com/products/nxiq/doc/policy-management" external>
                    Policy Management
                  </NxTextLink>
                </NxList.Text>
              </NxList.Item>
              <NxList.Item>
                <NxList.Text>
                  <NxTextLink href=" https://links.sonatype.com/products/nxiq/doc/policy-overrides" external>
                    Override Policy Actions
                  </NxTextLink>
                </NxList.Text>
              </NxList.Item>
            </NxList>
          </div>
        </NxInfoAlert>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton variant="primary" onClick={closeModal}>
            Close
          </NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  );
};

SuccessMoveOrgModal.propTypes = {
  closeModal: PropTypes.func.isRequired,
  warnings: PropTypes.object,
};

export default SuccessMoveOrgModal;
