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
package org.trustedanalytics.user.invite;

import com.google.common.base.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EmailInvitationsService implements InvitationsService {

    private final SpringTemplateEngine templateEngine;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SecurityCodeService securityCodeService;

    @Autowired
    private AccessInvitationsService accessInvitationsService;

    @Autowired
    private UaaOperations uaaPrivilegedClient;

    @Autowired
    private CcOperations ccPrivilegedClient;

    public EmailInvitationsService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String sendInviteEmail(String email, String currentUser,
        InvitationLinkGenerator invitationLinkGenerator) {

        String subject = "Invitation to join Trusted Analytics platform";
        String invitationLink =
                invitationLinkGenerator.getLink(securityCodeService.generateCode(email).getCode());
        String htmlContent = getEmailHtml(email, currentUser, invitationLink);
        messageService.sendMimeMessage(email, subject, htmlContent);
        return invitationLink;

    }

    private String getEmailHtml(String email, String currentUser, String invitationLink) {
        final Context ctx = new Context();
        ctx.setVariable("serviceName", "Trusted Analytics");
        ctx.setVariable("email", email);
        ctx.setVariable("currentUser", currentUser);
        ctx.setVariable("accountsUrl", invitationLink);
        return templateEngine.process("invite", ctx);
    }

    @Override
    public Optional<String> createUser(String username, String password, String orgName) {
        return accessInvitationsService.getAccessInvitations(username)
            .map(invitations -> {
                final ScimUser user = uaaPrivilegedClient.createUser(username, password);
                final UUID userGuid = UUID.fromString(user.getId());
                ccPrivilegedClient.createUser(userGuid);
                if (!Strings.isNullOrEmpty(orgName)) {
                    createOrganizationAndSpace(userGuid, orgName);
                }
                retrieveAndAssignAccessInvitations(userGuid, invitations);
                return user.getId();
            });
    }

    private void createOrganizationAndSpace(UUID userGuid, String orgName) {
        final String defaultSpaceName = "default";
        final UUID orgGuid = ccPrivilegedClient.createOrganization(orgName);
        ccPrivilegedClient.assignUserToOrganization(userGuid, orgGuid);
        final UUID spaceGuid = ccPrivilegedClient.createSpace(orgGuid, defaultSpaceName);
        ccPrivilegedClient.assignUserToSpace(userGuid, spaceGuid);
    }

    private void retrieveAndAssignAccessInvitations(UUID userGuid, AccessInvitations invtiations) {
        getFlatOrgRoleMap(invtiations.getOrgAccessInvitations())
                .forEach(pair -> ccPrivilegedClient.assignOrgRole(userGuid, pair.getKey(), pair.getValue()));
        getFlatOrgRoleMap(invtiations.getSpaceAccessInvitations())
                .forEach(pair -> ccPrivilegedClient.assignSpaceRole(userGuid, pair.getKey(), pair.getValue()));
    }

    private List<Pair<UUID, Role>> getFlatOrgRoleMap(Map<UUID, Set<Role>> orgRoleMap) {
        return orgRoleMap.entrySet()
                .stream()
                .flatMap(orgRoles -> orgRoles.getValue().stream().map(role -> Pair.of(orgRoles.getKey(), role)))
                .collect(Collectors.toList());
    }

}
