package com.demo.oauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Landing page for the authorization_code flow: the authorization server
 * redirects the browser here with ?code=..., and this page shows the code plus
 * the curl command that exchanges it for tokens.
 */
@Controller
public class CallbackController {

  @GetMapping("/callback")
  @ResponseBody
  public String callback(@RequestParam(required = false) String code,
      @RequestParam(required = false) String error,
      @RequestParam(required = false, name = "error_description") String errorDescription) {
    if (error != null) {
      return "authorization failed: " + error
          + (errorDescription == null ? "" : " (" + errorDescription + ")");
    }
    return """
        <h2>Authorization code received</h2>
        <p><code>%s</code></p>
        <p>Exchange it for tokens:</p>
        <pre>curl -u demo-client:demo-secret \\
  -d "grant_type=authorization_code" \\
  -d "code=%s" \\
  -d "redirect_uri=http://127.0.0.1:9000/callback" \\
  http://localhost:9000/oauth2/token</pre>
        """.formatted(code, code);
  }
}
