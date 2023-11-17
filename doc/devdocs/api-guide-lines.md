<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# API Guidelines

## Creating New Endpoints

Endpoints are defined inside of classes that end with the word `Resource`. "Resource" classes live under the
`insight-brain-service` module, unless they are specific to MTIQ. MTIQ specific "Resource" classes live under the
`nexus-mtiq-server` module.

When defining new endpoints, keep in mind:

* `/rest`: not intended for direct use by customers but rather by other Sonatype code (usually the IQ frontend but
  sometimes also the integrations)
* `/api/v2`: intended for direct use by customer code. Backwards compatible. May also be used by other Sonatype code
  including the frontend
* `/api/v2/experimental`: open to being used by customer code but is not guaranteed to stay compatible
  (or even continue to exist) in the future
* `:8071/...`: Operational Admin API. In on-prem mainly contains system health check endpoints. In SaaS also contains
  tenant management endpoints
