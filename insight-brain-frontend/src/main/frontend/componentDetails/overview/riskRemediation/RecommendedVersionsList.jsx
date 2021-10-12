/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxButton, NxList } from '@sonatype/react-shared-components';
import { VersionChangePropTypes } from '../overviewTypes';

export const RecommendedVersionsList = ({ versionChanges, actualVersion, handleCompare }) => {
  return (
    <NxList>
      {versionChanges.map((versionChange) => {
        if (!versionChange.version || actualVersion === versionChange.version) {
          return (
            <NxList.Item key={versionChange.id}>
              <NxList.Subtext className="iq-current-version-recommendation">{versionChange.text}</NxList.Subtext>
            </NxList.Item>
          );
        }
        return (
          <NxList.Item key={versionChange.id}>
            <NxList.Text>Upgrade to {versionChange.version}</NxList.Text>
            <NxList.Subtext>{versionChange.text}</NxList.Subtext>
            <NxList.Actions>
              <NxButton title="Compare" variant="tertiary" onClick={() => handleCompare(versionChange.version)}>
                Compare
              </NxButton>
            </NxList.Actions>
          </NxList.Item>
        );
      })}
    </NxList>
  );
};

RecommendedVersionsList.propTypes = {
  versionChanges: PropTypes.arrayOf(VersionChangePropTypes).isRequired,
  actualVersion: PropTypes.string.isRequired,
  handleCompare: PropTypes.func.isRequired,
};
