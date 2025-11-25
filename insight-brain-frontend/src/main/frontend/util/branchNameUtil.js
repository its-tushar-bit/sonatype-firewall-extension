/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getCompositeSourceControlUrl } from './CLMLocation';
import { selectApplicationReportMetaData } from '../applicationReport/applicationReportSelectors';

export const fetchDefaultBranchName = async (state) => {
  const applicationReportMetaData = selectApplicationReportMetaData(state);

  if (!applicationReportMetaData) {
    return null;
  }

  const { data } = await axios.get(
    getCompositeSourceControlUrl('application', applicationReportMetaData.application.id)
  );
  return data.baseBranch?.value || data.baseBranch?.parentValue;
};
