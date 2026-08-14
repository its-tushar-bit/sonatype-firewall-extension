/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Owner type identifiers used to switch between application-scoped and HRC-scoped code paths.
//
// The backend serializes com.sonatype.insight.brain.report.OwnerType via @JsonValue which
// lowercases the enum name — that is what the REST layer sends and receives on the wire.
// Redux carries an uppercase form internally (kept human-readable and matching the Java enum
// name). The selector layer (see selectOwnerType) normalizes both to the uppercase form before
// comparison.
//
// Use API_* when talking to the backend (query params, body fields), and use the uppercase
// constants for Redux state or in-app comparisons.
export const OWNER_TYPE_APPLICATION = 'APPLICATION';
export const OWNER_TYPE_HRC = 'HOSTED_REPOSITORY_COMPONENT';

export const API_OWNER_TYPE_APPLICATION = 'application';
export const API_OWNER_TYPE_HRC = 'hosted_repository_component';
