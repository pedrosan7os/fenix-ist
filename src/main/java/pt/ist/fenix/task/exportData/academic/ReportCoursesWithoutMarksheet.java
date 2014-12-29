package pt.ist.fenix.task.exportData.academic;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.MarkSheet;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.utl.ist.fenix.tools.util.excel.Spreadsheet;
import pt.utl.ist.fenix.tools.util.excel.Spreadsheet.Row;

public class ReportCoursesWithoutMarksheet extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("DisciplinasSemPauta");
        spreadsheet.setHeader("Plano Curricular\t");
        spreadsheet.setHeader("Unidade Curricular\t");
        spreadsheet.setHeader("Número Responsável\t");
        spreadsheet.setHeader("Responsável\n");

        final Set<CurricularCourse> curricularCourses = new HashSet<CurricularCourse>();
        for (final ExecutionSemester executionSemester : ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet()) {
            for (final Enrolment enrolment : executionSemester.getEnrolmentsSet()) {
                final CurricularCourse curricularCourse = enrolment.getCurricularCourse();
                if (!enrolment.isAnnulled() && !hasCurricularCourseMarkSheet(curricularCourse, enrolment.getExecutionPeriod())) {
                    if (!curricularCourses.contains(curricularCourse)) {
                        curricularCourses.add(curricularCourse);
                        final Person responsible = findResponsible(enrolment);
                        final Row row = spreadsheet.addRow();
                        row.setCell(curricularCourse.getDegreeCurricularPlan().getName());
                        row.setCell(curricularCourse.getName());
                        row.setCell(responsible == null ? "" : responsible.getEmployee().getEmployeeNumber().toString());
                        row.setCell(responsible == null ? "" : responsible.getName());
                    }
                }
            }
        }

        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(byteArrayOS);

        output("disciplinas_sem_pauta.xls", byteArrayOS.toByteArray());
        taskLog("Done.");
    }

    private Person findResponsible(final Enrolment enrolment) {
        for (final Attends attends : enrolment.getAttendsSet()) {
            final ExecutionCourse executionCourse = attends.getExecutionCourse();
            for (final Professorship professorship : executionCourse.getProfessorshipsSet()) {
                if (professorship.isResponsibleFor()) {
                    return professorship.getPerson();
                }
            }
            for (final Professorship professorship : executionCourse.getProfessorshipsSet()) {
                return professorship.getPerson();
            }
        }
        return null;
    }

    private boolean hasCurricularCourseMarkSheet(final CurricularCourse curricularCourse, final ExecutionSemester executionPeriod) {
        for (final MarkSheet markSheet : curricularCourse.getMarkSheetsSet()) {
            if (markSheet.getExecutionPeriod() == executionPeriod) {
                return true;
            }
        }
        return false;
    }
}
