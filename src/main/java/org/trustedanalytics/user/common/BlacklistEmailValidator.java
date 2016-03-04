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

package org.trustedanalytics.user.common;

import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.google.common.base.CharMatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.trustedanalytics.user.invite.WrongEmailAddressException;

public class BlacklistEmailValidator implements EmailValidator {
    private static final Log LOGGER = LogFactory.getLog(BlacklistEmailValidator.class);

    private final List<String> forbiddenDomains;

    public BlacklistEmailValidator(List<String> forbiddenDomains) {
        this.forbiddenDomains = forbiddenDomains;
    }

    private void validateDomain(String email) {
        String domain = email.substring(email.indexOf("@") + 1);
        domain = domain.toLowerCase();
        if(forbiddenDomains.contains(domain)){
            throw new WrongEmailAddressException("That domain is blocked");
        }
    }

    private void validateEmailAddress(String email) {
        try{
            new InternetAddress(email).validate();
        } catch (AddressException e){
            LOGGER.warn(e);
            throw new WrongEmailAddressException("That email address is not valid");
        }

        if(!CharMatcher.ascii().matchesAllOf(email)) {
            throw new WrongEmailAddressException("Email must not containt NON-ASCII characters");
        }

    }

    /* That method at first checks whether string passed into parameter
     * conforms to the syntax rules of RFC 822. Second method checks
     * whether email address is on a blacklist defined in application.yml
     */
    @Override
    public void validate(String email) {
        validateEmailAddress(email);
        validateDomain(email);
    }

}