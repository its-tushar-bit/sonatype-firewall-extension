/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getApplicableWaiversUrl, getViolationDetailsUrl } from 'MainRoot/util/CLMLocation';
import type { ApplicableWaiversDTO, ViolationDetailsDTO } from 'MainRoot/nosc/violations/detail/violationDetailTypes';

export async function fetchCrossStageViolationDetails(id: string): Promise<ViolationDetailsDTO> {
  const { data } = await axios.get<ViolationDetailsDTO>(getViolationDetailsUrl(id));
  return data;
}

export async function fetchApplicableWaivers(id: string): Promise<ApplicableWaiversDTO> {
  const { data } = await axios.get<ApplicableWaiversDTO>(getApplicableWaiversUrl(id));
  return data;
}
