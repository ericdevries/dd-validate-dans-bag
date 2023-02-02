/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.validatedansbag.core.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class SwordAuthenticator implements Authenticator<BasicCredentials, SwordUser> {

    private static final Logger log = LoggerFactory.getLogger(SwordAuthenticator.class);

    private final HttpClient httpClient;
    private final URL passwordDelegate;

    public SwordAuthenticator(URL passwordDelegate, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.passwordDelegate = passwordDelegate;
    }

    @Override
    public Optional<SwordUser> authenticate(BasicCredentials credentials) throws AuthenticationException {
        try {
            log.debug("Authenticating user {}", credentials.getUsername());

            if (this.validatePasswordWithDelegate(credentials, passwordDelegate)) {
                log.info("Authentication for user {} successful", credentials.getUsername());
                return Optional.of(new SwordUser(credentials.getUsername()));
            }
            else {
                log.info("Authentication for user {} failed", credentials.getUsername());
                return Optional.empty();
            }
        }
        catch (URISyntaxException | IOException e) {
            log.error("An error occurred while authenticating user", e);
            throw new AuthenticationException("Unable to perform authentication check with delegate", e);
        }
    }

    boolean validatePasswordWithDelegate(BasicCredentials basicCredentials, URL passwordDelegate) throws IOException, URISyntaxException, AuthenticationException {
        var auth = basicCredentials.getUsername() + ":" + basicCredentials.getPassword();
        var encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
        var header = String.format("Basic %s", new String(encodedAuth, StandardCharsets.UTF_8));

        var post = new HttpPost(passwordDelegate.toURI());
        post.setHeader("Authorization", header);

        var response = httpClient.execute(post);

        switch (response.getStatusLine().getStatusCode()) {
            case 204:
                return true;
            case 401:
                return false;
            default:
                throw new AuthenticationException("Unexpected response from authentication service: " + response.getStatusLine().getStatusCode());
        }
    }
}
