/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apereo.cas.client.authentication;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;

public class FacesCompatibleAuthenticationRedirectStrategyTests {

    private FacesCompatibleAuthenticationRedirectStrategy strategy;

    @Before
    public void setUp() throws Exception {
        this.strategy = new FacesCompatibleAuthenticationRedirectStrategy();
    }

    @Test
    public void didWeRedirect() throws Exception {
        final var redirectUrl = "http://www.apereo.org";
        final HttpServletRequest request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();

        this.strategy.redirect(request, response, redirectUrl);
        assertEquals(redirectUrl, response.getRedirectedUrl());
    }

    @Test
    public void facesPartialResponse() throws Exception {
        final var redirectUrl = "http://www.apereo.org";
        final var request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();
        request.setParameter("javax.faces.partial.ajax", "true");
        this.strategy.redirect(request, response, redirectUrl);
        assertNull(response.getRedirectedUrl());
        assertTrue(response.getContentAsString().contains(redirectUrl));
    }
}
