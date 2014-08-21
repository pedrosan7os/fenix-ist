package pt.ist.fenix.user.management;

import java.util.SortedSet;
import java.util.TreeSet;

import net.sourceforge.fenixedu.domain.Employee;
import net.sourceforge.fenixedu.domain.student.Student;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.Atomic.TxMode;

public class NumberRangeCheck extends CustomTask {
    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        SortedSet<Integer> usernames = new TreeSet<>();
        for (User user : Bennu.getInstance().getUserSet()) {
            try {
                usernames.add(Integer.valueOf(user.getUsername().replaceAll("ist", "")));
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        printRanges("Usersnames: ", usernames);
        SortedSet<Integer> students = new TreeSet<>();
        for (Student student : Bennu.getInstance().getStudentsSet()) {
            students.add(student.getNumber());
        }
        printRanges("Students: ", students);
        SortedSet<Integer> employees = new TreeSet<>();
        for (Employee employee : Bennu.getInstance().getEmployeesSet()) {
            employees.add(employee.getEmployeeNumber());
        }
        printRanges("Employees: ", employees);
    }

    private void printRanges(String prefix, SortedSet<Integer> codes) {
        Integer start = null;
        Integer current = null;
        for (Integer code : codes) {
            if (start == null) {
                start = code;
                current = code;
            } else if (code >= current + 1 && code < current + 100) {
                current = code;
            } else {
                getTaskLogWriter().printf("%s%d-%d%n", prefix, start, current);
                start = code;
                current = code;
            }
        }
        getTaskLogWriter().printf("%s%d-%d%n", prefix, start, current);
    }
}
