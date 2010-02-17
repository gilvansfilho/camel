/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor.aggregator;

import org.apache.camel.CamelExchangeException;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.BodyInAggregatingStrategy;

/**
 * @version $Revision$
 */
public class AggregateIgnoreBadCorrelationKeysTest extends ContextTestSupport {

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    public void testAggregateIgnoreBadCorrelationKeys() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .aggregate(header("id"), new BodyInAggregatingStrategy())
                        .completionSize(2).ignoreBadCorrelationKeys()
                        .to("mock:result");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedBodiesReceived("A+C");

        template.sendBodyAndHeader("direct:start", "A", "id", 1);

        // B should be ignored
        template.sendBodyAndHeader("direct:start", "B", "id", null);

        template.sendBodyAndHeader("direct:start", "C", "id", 1);

        assertMockEndpointsSatisfied();
    }

    public void testAggregateNotIgnoreBadCorrelationKeys() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .aggregate(header("id"), new BodyInAggregatingStrategy())
                        .completionSize(2)
                        .to("mock:result");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedBodiesReceived("A+C");

        template.sendBodyAndHeader("direct:start", "A", "id", 1);

        try {
            template.sendBodyAndHeader("direct:start", "B", "id", null);
            fail("Should throw an exception");
        } catch (CamelExecutionException e) {
            CamelExchangeException cause = assertIsInstanceOf(CamelExchangeException.class, e.getCause());
            assertEquals("Correlation key could not be evaluated to a value. Exchange[Message: B]", cause.getMessage());
        }

        template.sendBodyAndHeader("direct:start", "C", "id", 1);

        assertMockEndpointsSatisfied();
    }

}