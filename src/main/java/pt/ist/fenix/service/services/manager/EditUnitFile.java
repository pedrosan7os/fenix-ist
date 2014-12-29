/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Core.
 *
 * FenixEdu Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Core.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenix.service.services.manager;

import org.fenixedu.bennu.core.groups.Group;

import pt.ist.fenix.domain.UnitFile;
import pt.ist.fenixframework.Atomic;

public class EditUnitFile {
    @Atomic
    public static void run(UnitFile file, String name, String description, String tags, Group group) {
        file.setDisplayName(name);
        file.setDescription(description);
        file.setPermittedGroup(group.grant(file.getUploader().getUser()));
        file.setUnitFileTags(tags);
    }
}