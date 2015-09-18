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
package org.trustedanalytics.user.invite.rest;

import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.securitycode.InvalidSecurityCodeException;
import org.trustedanalytics.user.invite.securitycode.SecurityCode;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("/rest/registrations")
public class RegistrationsController {

    private final SecurityCodeService securityCodeService;
    private final InvitationsService invitationsService;
    private final AccessInvitationsService accessInvitationsService;

    @Autowired
    public RegistrationsController(SecurityCodeService securityCodeService,
                            InvitationsService invitationsService,
                            AccessInvitationsService accessInvitationsService) {
        this.securityCodeService = securityCodeService;
        this.invitationsService = invitationsService;
        this.accessInvitationsService = accessInvitationsService;
    }

    @RequestMapping(method = RequestMethod.POST)
    public RegistrationModel addUser(@RequestBody RegistrationModel newUser, @RequestParam(value = "code", required = false) String code) {
        if(Strings.isNullOrEmpty(code)) {
            throw new InvalidSecurityCodeException("Security code empty or null");
        }

        SecurityCode sc = securityCodeService.verify(code);
        validate(newUser);

        String email = sc.getEmail();

        try {
            invitationsService.createUser(email, newUser.getPassword(), newUser.getOrg());
            securityCodeService.use(sc);
            accessInvitationsService.useAccessInvitations(email);

        } catch(HttpClientErrorException e) {
            if(e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new EntityAlreadyExists();
            }
            throw e;
        }
        return newUser;
    }

    @RequestMapping(value = "/{code}", method = RequestMethod.GET)
    public InvitationModel getInvitation(@PathVariable("code") String code) {
        try {
            final SecurityCode sc = securityCodeService.verify(code);
            return InvitationModel.of(sc.getEmail(), accessInvitationsService.getOrgCreationEligibility(sc.getEmail()));
        } catch(InvalidSecurityCodeException e) {
            throw new EntityNotFoundException("", e);
        }
    }

    private void validate(RegistrationModel user) {
        if(Strings.nullToEmpty(user.getPassword()).length() < 6) {
            throw new IllegalArgumentException("Password is too short");
        }
    }
}
