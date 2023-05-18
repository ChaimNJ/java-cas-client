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
package org.apereo.cas.client.validation;

import org.apereo.cas.client.proxy.CleanUpTimerTask;
import org.apereo.cas.client.proxy.ProxyGrantingTicketStorage;
import org.apereo.cas.client.proxy.ProxyGrantingTicketStorageImpl;
import org.apereo.cas.client.util.MethodFlag;

import junit.framework.TestCase;
import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockServletContext;

import jakarta.servlet.FilterConfig;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Unit test for {@link Cas20ProxyReceivingTicketValidationFilter}
 *
 * @author Brad Cupit (brad [at] lsu {dot} edu)
 */
public class Cas20ProxyReceivingTicketValidationFilterTests extends TestCase {

    private final Timer defaultTimer = new Timer(true);

    private final ProxyGrantingTicketStorage storage = new ProxyGrantingTicketStorageImpl();

    private final CleanUpTimerTask defaultTimerTask = new CleanUpTimerTask(storage);

    public void testStartsThreadAtStartup() throws Exception {
        final var scheduleMethodFlag = new MethodFlag();
        final var filter = newCas20ProxyReceivingTicketValidationFilter();

        final var timer = new Timer(true) {
            @Override
            public void schedule(final TimerTask task, final long delay, final long period) {
                scheduleMethodFlag.setCalled();
            }
        };

        filter.setMillisBetweenCleanUps(1);
        filter.setProxyGrantingTicketStorage(storage);
        filter.setTimer(timer);
        filter.setTimerTask(defaultTimerTask);

        filter.init();
        assertTrue(scheduleMethodFlag.wasCalled());
    }

    public void testShutsDownTimerThread() throws Exception {
        final var cancelMethodFlag = new MethodFlag();
        final var filter = newCas20ProxyReceivingTicketValidationFilter();

        final var timer = new Timer(true) {
            @Override
            public void cancel() {
                cancelMethodFlag.setCalled();
                super.cancel();
            }
        };

        filter.setProxyGrantingTicketStorage(storage);
        filter.setMillisBetweenCleanUps(1);
        filter.setTimer(timer);
        filter.setTimerTask(defaultTimerTask);
        filter.init();
        filter.destroy();

        assertTrue(cancelMethodFlag.wasCalled());
    }

    public void testCallsCleanAllOnSchedule() throws Exception {
        final var timerTaskFlag = new MethodFlag();
        final var filter = newCas20ProxyReceivingTicketValidationFilter();

        final var timerTask = new TimerTask() {
            @Override
            public void run() {
                timerTaskFlag.setCalled();
            }
        };

        final var millisBetweenCleanUps = 250;
        filter.setProxyGrantingTicketStorage(storage);
        filter.setTimerTask(timerTask);
        filter.setTimer(defaultTimer);
        filter.setMillisBetweenCleanUps(millisBetweenCleanUps);

        filter.init();

        // wait long enough for the clean up to occur
        Thread.sleep(millisBetweenCleanUps * 2);

        assertTrue(timerTaskFlag.wasCalled());
        filter.destroy();
    }

    public void testDelaysFirstCleanAll() throws Exception {
        final var timerTaskFlag = new MethodFlag();
        final var filter = newCas20ProxyReceivingTicketValidationFilter();

        final var timerTask = new TimerTask() {
            @Override
            public void run() {
                timerTaskFlag.setCalled();
            }
        };

        final var millisBetweenCleanUps = 250;
        filter.setProxyGrantingTicketStorage(storage);
        filter.setMillisBetweenCleanUps(millisBetweenCleanUps);
        filter.setTimer(defaultTimer);
        filter.setTimerTask(timerTask);

        filter.init();

        assertFalse(timerTaskFlag.wasCalled());

        // wait long enough for the clean up to occur
        Thread.sleep(millisBetweenCleanUps * 2);

        assertTrue(timerTaskFlag.wasCalled());

        filter.destroy();
    }

    public void testThrowsForNullStorage() throws Exception {
        final var filter = newCas20ProxyReceivingTicketValidationFilter();
        filter.setProxyGrantingTicketStorage(null);

        try {
            filter.init();
            fail("expected an exception due to null ProxyGrantingTicketStorage");
        } catch (final IllegalArgumentException exception) {
            // test passes
        }
    }

    public void testGetTicketValidator() throws Exception {
        final var filter = newCas20ProxyReceivingTicketValidationFilter();

        // Test case #1
        final var config1 = new MockFilterConfig();
        config1.addInitParameter("allowedProxyChains", "https://a.example.com");
        config1.addInitParameter("casServerUrlPrefix", "https://cas.jasig.org/");
        config1.addInitParameter("service", "http://www.jasig.org");
        filter.init(config1);
        assertNotNull(filter.getTicketValidator(config1));
    }

    @Test
    public void getTicketValidatorWithProxyChains() throws Exception {
        final var filter = newCas20ProxyReceivingTicketValidationFilter();
        // Test case #2
        final var config2 = new MockFilterConfig();
        config2.addInitParameter("allowedProxyChains", "https://a.example.com https://b.example.com");
        config2.addInitParameter("casServerUrlPrefix", "https://cas.jasig.org/");
        config2.addInitParameter("service", "http://www.jasig.org");
        filter.init(config2);
        assertNotNull(filter.getTicketValidator(config2));
    }


    @Test
    public void getTIcketValidatorWithProxyChainsAndLineBreak() throws Exception {
        final var filter = newCas20ProxyReceivingTicketValidationFilter();

        // Test case #3
        final var config3 = new MockFilterConfig();
        config3.addInitParameter("allowedProxyChains",
            "https://a.example.com https://b.example.com\nhttps://c.example.com");
        config3.addInitParameter("casServerUrlPrefix", "https://cas.jasig.org/");
        config3.addInitParameter("service", "http://www.jasig.org");
        filter.init(config3);
        assertNotNull(filter.getTicketValidator(config3));
    }

    public void testRenewInitParamThrows() throws Exception {
        final var f = new Cas20ProxyReceivingTicketValidationFilter();
        final var config = new MockFilterConfig();
        config.addInitParameter("casServerUrlPrefix", "https://cas.example.com");
        config.addInitParameter("renew", "true");
        try {
            f.init(config);
            fail("Should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Renew MUST"));
        }
    }

    public void testAllowsRenewContextParam() throws Exception {
        final var f = new Cas20ProxyReceivingTicketValidationFilter();
        final var context = new MockServletContext();
        context.addInitParameter("casServerUrlPrefix", "https://cas.example.com");
        context.addInitParameter("renew", "true");
        context.addInitParameter("service", "http://www.jasig.org");
        final FilterConfig config = new MockFilterConfig(context);
        f.init(config);
        final var validator = f.getTicketValidator(config);
        assertTrue(validator instanceof AbstractUrlBasedTicketValidator);
        assertTrue(((AbstractUrlBasedTicketValidator) validator).isRenew());
    }

    /**
     * construct a working {@link Cas20ProxyReceivingTicketValidationFilter}
     */
    private static Cas20ProxyReceivingTicketValidationFilter newCas20ProxyReceivingTicketValidationFilter() {
        final var filter = new Cas20ProxyReceivingTicketValidationFilter();
        filter.setServerName("localhost");
        filter.setTicketValidator(new Cas20ProxyTicketValidator(""));

        return filter;
    }
}
