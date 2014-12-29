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
package pt.ist.fenix.domain.accessControl.arguments;

import org.fenixedu.academic.domain.accessControl.arguments.DomainObjectArgumentParser;
import org.fenixedu.bennu.core.annotation.GroupArgumentParser;

import pt.ist.fenix.domain.accessControl.PersistentGroupMembers;

@GroupArgumentParser
public class PersistentGroupMembersArgument extends DomainObjectArgumentParser<PersistentGroupMembers> {
    @Override
    public Class<PersistentGroupMembers> type() {
        return PersistentGroupMembers.class;
    }
}
