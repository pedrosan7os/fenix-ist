package pt.ist.fenix.task.updateData.enrolment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class InvertFirstYearShiftsCapacity extends CustomTask {

    static private final Integer FIRST_CURRICULAR_YEAR = Integer.valueOf(1);

    @Override
    public void runTask() throws Exception {

        final ExecutionSemester executionSemester = ExecutionYear.readCurrentExecutionYear().getFirstExecutionPeriod();
        final List<Degree> degrees = readDegrees();
        taskLog("Degrees: " + degrees.size());
        taskLog("Period: " + executionSemester.getQualifiedName());

        final Set<Shift> shifts = new HashSet<Shift>();
        for (final Degree degree : readDegrees()) {
            for (final DegreeCurricularPlan degreeCurricularPlan : degree.getActiveDegreeCurricularPlans()) {
                final ExecutionDegree executionDegree =
                        degreeCurricularPlan.getExecutionDegreeByAcademicInterval(executionSemester.getExecutionYear()
                                .getAcademicInterval());

                if (executionDegree != null) {
                    for (final SchoolClass schoolClass : executionDegree.getSchoolClassesSet()) {
                        if (schoolClass.getAnoCurricular().equals(FIRST_CURRICULAR_YEAR)
                                && schoolClass.getExecutionPeriod() == executionSemester) {
                            for (final Shift shift : schoolClass.getAssociatedShiftsSet()) {
                                shifts.add(shift);
                            }
                        }
                    }
                }
            }
        }
        taskLog("Found: " + shifts.size() + " shifts");

        int modified = 0;
        for (final Shift shift : shifts) {
            int capacity = shift.getLotacao().intValue();

            taskLog(String.format("For shift[%s] Capacity is %s: ", shift.getNome(), capacity));
            if (capacity > 0) {
                shift.setLotacao(capacity * -1);
                modified++;
            }
        }
        taskLog("Modified: " + modified);
    }

    private List<Degree> readDegrees() {
        return Degree.readAllByDegreeType(DegreeType.BOLONHA_DEGREE, DegreeType.BOLONHA_INTEGRATED_MASTER_DEGREE);
    }
}
