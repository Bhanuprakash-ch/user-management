/**
 *  Copyright (c) 2015 Intel Corporation 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.trustedanalytics.user.orgs;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import org.trustedanalytics.cloud.auth.HeaderAddingHttpInterceptor;

import org.mockito.InOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.InterceptingHttpAccessor;
import org.springframework.web.client.RestOperations;

import java.util.Map;

public class PlatformVerifiers {
    public static void verifySetTokenThenExchange(RestOperations restTemplate, String token,
                                                  String url) {

        InOrder verifier = verifyTokenWasSet(restTemplate, token);
        verifier.verify(restTemplate)
            .exchange( eq(url), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class));
    }

    public static void verifySetTokenThenExchange(RestOperations restTemplate, String token, String url, Map<String, ?> vars ) {

        InOrder verifier = verifyTokenWasSet(restTemplate, token);
        verifier.verify(restTemplate)
            .exchange( eq(url), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class), eq(vars));
    }

    public static void verifySetTokenThenGetForEntity(RestOperations restTemplate, String token,
        String url, Class returnClass, Map<String, ?> vars) {

        InOrder verifier = verifyTokenWasSet(restTemplate, token);
        verifier.verify(restTemplate)
            .getForEntity(url, returnClass, vars);
    }

    public static void verifySetTokenThenPostForEntity(RestOperations restTemplate, String token,
        String url, Object objectToSend, Class typeToSend, Map<String, ?> vars) {

        InOrder verifier = verifyTokenWasSet(restTemplate, token);
        verifier.verify(restTemplate).postForEntity(url, objectToSend, typeToSend, vars);
    }

    public static void verifySetTokenThenPostForEntity(RestOperations restTemplate, String token,
        String url, Object objectToSend, Class typeToSend) {

        InOrder verifier = verifyTokenWasSet(restTemplate, token);
        verifier.verify(restTemplate).postForEntity(url, objectToSend, typeToSend);
    }

    public static void verifySetTokenThenDelete(RestOperations restTemplate, String token,
        String url, Map<String, ?> vars) {

        InOrder verifier = verifyTokenWasSet(restTemplate, token);
        verifier.verify(restTemplate).delete(url, vars);
    }

    private static InOrder verifyTokenWasSet(RestOperations restTemplate, String token) {
        InOrder verifier = inOrder(restTemplate, restTemplate);
        ClientHttpRequestInterceptor interceptor =
            new HeaderAddingHttpInterceptor("Authorization", "bearer " + token);
        verifier.verify((InterceptingHttpAccessor) restTemplate)
            .setInterceptors(singletonList(interceptor));
        return verifier;
    }
}
