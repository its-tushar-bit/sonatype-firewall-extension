package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @since 1.21
 */
public class HttpHeaderValidatorFilterChainTest
    extends AbstractBrainServiceTest
{

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserSessionResource.RESOURCE_PATH).auth(User.ADMIN_USERNAME, "admin123");
  }

  @Test
  public void testValidHeader() throws Exception {
    assertResponseStatus(204, restRequest().header("Host", "localhost").post());
  }

  @Test
  public void testInvalidHeader() throws Exception {
    // Using X-Forwarded-Proto instead of Host since Host seems to be overridden by the client
    HttpResponse response = restRequest().header("X-Forwarded-Proto", "http\"><script>alert(document.domain)</script>")
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("Illegal header value detected in 'X-Forwarded-Proto'"));
  }

}
