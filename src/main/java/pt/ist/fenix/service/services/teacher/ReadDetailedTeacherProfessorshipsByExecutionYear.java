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
/*
 * Created on Dec 17, 2003 by jpvl
 *  
 */
package pt.ist.fenix.service.services.teacher;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import com.google.common.base.Strings;

public class ReadDetailedTeacherProfessorshipsByExecutionYear extends ReadDetailedTeacherProfessorshipsAbstractService {

    protected List run(String teacherID, String executionYearID) throws FenixServiceException {

        final Teacher teacher = FenixFramework.getDomainObject(teacherID);
        if (teacher == null) {
            throw new DomainException("error.noTeacher");
        }

        final ExecutionYear executionYear;
        if (Strings.isNullOrEmpty(executionYearID)) {
            executionYear = ExecutionYear.readCurrentExecutionYear();
        } else {
            executionYear = FenixFramework.getDomainObject(executionYearID);
        }

        final List<Professorship> responsibleFors = new ArrayList();
        for (final Professorship professorship : teacher.responsibleFors()) {
            if (professorship.getExecutionCourse().getExecutionPeriod().getExecutionYear() == executionYear) {
                responsibleFors.add(professorship);
            }
        }
        return getDetailedProfessorships(teacher.getProfessorships(executionYear), responsibleFors);
    }

    // Service Invokers migrated from Berserk

    private static final ReadDetailedTeacherProfessorshipsByExecutionYear serviceInstance =
            new ReadDetailedTeacherProfessorshipsByExecutionYear();

    @Atomic
    public static List runReadDetailedTeacherProfessorshipsByExecutionYear(String teacherID, String executionYearID)
            throws FenixServiceException {
        return serviceInstance.run(teacherID, executionYearID);
    }

}