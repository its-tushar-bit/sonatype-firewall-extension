/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package io.dropwizard.web.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.dropwizard.web.conf.WebConfiguration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class TestConfig extends Configuration {
    @Valid
    @NotNull
    @JsonProperty("web1")
    private WebConfiguration web1Configuration = new WebConfiguration();

    @Valid
    @NotNull
    @JsonProperty("web2")
    private WebConfiguration web2Configuration = new WebConfiguration();

    @Valid
    @NotNull
    @JsonProperty("webAdmin")
    private WebConfiguration adminWebConfiguration = new WebConfiguration();

    public WebConfiguration getWeb1Configuration() {
        return web1Configuration;
    }

    public WebConfiguration getWeb2Configuration() {
        return web2Configuration;
    }

    public WebConfiguration getAdminWebConfiguration() {
        return adminWebConfiguration;
    }

    public void setWeb1Configuration(WebConfiguration webConfiguration) {
        this.web1Configuration = webConfiguration;
    }

    public void setWeb2Configuration(WebConfiguration webConfiguration) {
        this.web2Configuration = webConfiguration;
    }

    public void setAdminWebConfiguration(WebConfiguration adminWebConfiguration) {
        this.adminWebConfiguration = adminWebConfiguration;
    }
}
