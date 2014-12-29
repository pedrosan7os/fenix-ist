/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenix.ui.struts.action.manager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.ui.struts.action.manager.ManagerApplications.ManagerSystemManagementApp;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenix.domain.accessControl.PersistentGroupMembers;
import pt.ist.fenix.service.services.manager.DeletePersistentGroup;
import pt.ist.fenix.service.services.manager.RemovePersistentGroupMember;
import pt.ist.fenixframework.FenixFramework;

@StrutsFunctionality(app = ManagerSystemManagementApp.class, path = "groups-management",
        titleKey = "label.access.control.persistent.groups.management")
@Mapping(module = "manager", path = "/accessControlPersistentGroupsManagement")
@Forwards({ @Forward(name = "prepareCreateNewPersistentGroup", path = "/manager/persistentGroups/createNewPersistentGroup.jsp"),
        @Forward(name = "seeAllPersistentGroups", path = "/manager/persistentGroups/seeAllPersistentGroups.jsp") })
public class AccessControlPersistentGroupsManagementDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward listAllGroups(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        request.setAttribute("persistentGroups", rootDomainObject.getPersistentGroupMembersSet());
        return mapping.findForward("seeAllPersistentGroups");
    }

    public ActionForward prepareCreateNewGroup(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        return mapping.findForward("prepareCreateNewPersistentGroup");
    }

    public ActionForward prepareEditPersistentGroup(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        PersistentGroupMembers persistentGroup = getPersistentGroupFromParameter(request);
        request.setAttribute("persistentGroup", persistentGroup);
        return mapping.findForward("prepareCreateNewPersistentGroup");
    }

    public ActionForward deletePersistentGroup(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        PersistentGroupMembers persistentGroup = getPersistentGroupFromParameter(request);
        DeletePersistentGroup.run(persistentGroup);
        return listAllGroups(mapping, form, request, response);
    }

    public ActionForward removePersistentGroupMember(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        PersistentGroupMembers persistentGroup = getPersistentGroupFromParameter(request);
        Person person = getPersonFromParameter(request);
        RemovePersistentGroupMember.run(person, persistentGroup);
        return prepareEditPersistentGroup(mapping, form, request, response);
    }

    protected PersistentGroupMembers getPersistentGroupFromParameter(final HttpServletRequest request) {
        final String persistentGroupIDString = request.getParameter("persistentGroupID");
        return FenixFramework.getDomainObject(persistentGroupIDString);
    }

    protected Person getPersonFromParameter(final HttpServletRequest request) {
        final String personIDString = request.getParameter("personID");
        return (Person) FenixFramework.getDomainObject(personIDString);
    }
}