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
package org.apereo.cas.client.session;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * HashMap backed implementation of SessionMappingStorage.
 *
 * @author Scott Battaglia
 * @version $Revision$ $Date$
 * @since 3.1
 *
 */
public final class HashMapBackedSessionMappingStorage implements SessionMappingStorage {

    /**
     * Maps the ID from the CAS server to the Session.
     */
    private final Map<String, HttpSession> MANAGED_SESSIONS = new HashMap<>();

    /**
     * Maps the Session ID to the key from the CAS Server.
     */
    private final Map<String, String> ID_TO_SESSION_KEY_MAPPING = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public synchronized HttpSession removeSessionByMappingId(final String mappingId) {
        final var session = MANAGED_SESSIONS.get(mappingId);

        if (session != null) {
            removeBySessionById(session.getId());
        }

        return session;
    }

    @Override
    public synchronized void removeBySessionById(final String sessionId) {
        logger.debug("Attempting to remove Session=[{}]", sessionId);

        final var key = ID_TO_SESSION_KEY_MAPPING.get(sessionId);

        if (logger.isDebugEnabled()) {
            if (key != null) {
                logger.debug("Found mapping for session.  Session Removed.");
            } else {
                logger.debug("No mapping for session found.  Ignoring.");
            }
        }
        MANAGED_SESSIONS.remove(key);
        ID_TO_SESSION_KEY_MAPPING.remove(sessionId);
    }

    @Override
    public synchronized void addSessionById(final String mappingId, final HttpSession session) {
        ID_TO_SESSION_KEY_MAPPING.put(session.getId(), mappingId);
        MANAGED_SESSIONS.put(mappingId, session);

    }
}
