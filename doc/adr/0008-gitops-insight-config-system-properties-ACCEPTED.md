# ADR 8. GitOps Insight Config System Properties
0008-gitops-insight-config-system-properties.md
Date: 2023-11-16

## Status

Accepted

## Context

The change here would be to allow all system configuration properties to be set via the REST API or ENV variable where 
possible. This approach will be supported for both self-hosted and multi-tenant instances.

As part of the [MTIQ SCM Configurable Thread Pool Size](https://sonatype.atlassian.net/browse/CLM-28215) we are making 
the SCM thread pool size configurable. In multi-tenant IQ instances the thread pools are shared between tenants so 
these properties must only be set for the global tenant, however these properties can also be used for self-hosted IQ 
instances. 

Ideally we would like to configure these global properties using a GitOps approach setting configuration via the 
environment (`ENV`) variables or the `config.yml` (K8s `ConfigMap`), however IQ currently supports configuration via 
REST API.

When new configuration properties are added care should be taken to consider multi-tenant use and properties that must 
be set globally vs specifically for tenants must be differentiated.

Important notes about current implementation:
* For multi-tenant IQ, properties that are set for the Global tenant are used as the default value for all tenants
* For multi-tenant IQ, tenant-specific configuration properties must be set via the Admin REST API
* When used in a clustered environment the REST API will propagate changes to other nodes 

## Decision

* All the system configuration properties (where technically possible) can be set via, and will be read in order of precedence as follows
  1. REST API: The properties set via the REST API will be used first
  2. ENV variable: If the property is not set in the REST API and the ENV variable is set the ENV property will be used
  3. Default value: if no properties have been set a default value should be used
* When a configuration value must only be set once for the system globally it must be tagged with a @GlobalProperty annotation, this will then be used to block setting the property for individual tenants

## Consequences

* The system configuration properties can be set externally via ENV as well as REST API, allowing a GitOps approach to configuration
* IQ self-hosted documentation should be updated to reflect the ability to set configuration via ENV as well as REST API
* Properties with the @GlobalProperty annotation cannot be set for individual tenants, but can be set for self-hosted


#### Config YAML File
The `config.yml` file is not being added as a supported method for configuring IQ server. This is to avoid impact of
changing the customer documentation for the deprecated config.yml properties, and reduces the risk of configuration
files not being reloaded in clustered environments.
* ~~`config.yml`: When the property has not been set via REST API or ENV variable then the property from config.yml will be used~~


___

Reference:
* _[Dropwizard Configuration Reference](https://www.dropwizard.io/en/latest/manual/configuration.html)_
* _[Sonatype IQ Config YAML](https://help.sonatype.com/iqserver/configuring/config-yaml)_
* _[Configuration REST API - v2](https://help.sonatype.com/iqserver/automating/rest-apis/configuration-rest-api---v2)_
* _[What is GitOps?](https://www.redhat.com/en/topics/devops/what-is-gitops)_
* _[MTIQ GitOps Config Map](https://github.com/sonatype/mtiq-cmcf-deploy/blob/main/deployment/k8s/base/config-map.yaml)_
