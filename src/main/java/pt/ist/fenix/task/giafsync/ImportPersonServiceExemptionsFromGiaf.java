package pt.ist.fenix.task.giafsync;

import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;

import pt.ist.fenix.task.giafsync.GiafSync.ImportProcessor;
import pt.ist.fenix.task.giafsync.GiafSync.Modification;
import pt.ist.fenix.util.oracle.PersistentSuportGiaf;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.GiafProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonProfessionalExemption;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonServiceExemption;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.ServiceExemption;

class ImportPersonServiceExemptionsFromGiaf extends ImportProcessor {
    final static DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");

    final static DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    public ImportPersonServiceExemptionsFromGiaf() {

    }

    @Override
    public List<Modification> processChanges(GiafMetadata metadata, PrintWriter log, Logger logger) throws Exception {
        List<Modification> modifications = new ArrayList<>();
        PersistentSuportGiaf oracleConnection = PersistentSuportGiaf.getInstance();
        PreparedStatement preparedStatement = oracleConnection.prepareStatement(getQuery());
        ResultSet result = preparedStatement.executeQuery();
        int count = 0;
        int news = 0;
        int notImported = 0;
        int dontExist = 0;
        Set<Person> importedButInvalid = new HashSet<Person>();
        while (result.next()) {
            count++;
            String numberString = result.getString("emp_num");
            Person person = metadata.getPerson(numberString, logger);
            if (person == null) {
                logger.debug("Invalid person with number: " + numberString);
                dontExist++;
                continue;
            }
            PersonProfessionalData personProfessionalData = person.getPersonProfessionalData();
            if (personProfessionalData == null) {
                logger.debug("Empty personProfessionalData: " + numberString);
                dontExist++;
                continue;
            }
            final GiafProfessionalData giafProfessionalData =
                    personProfessionalData.getGiafProfessionalDataByGiafPersonIdentification(numberString);
            if (giafProfessionalData == null) {
                logger.debug("Empty giafProfessionalData: " + numberString);
                dontExist++;
                continue;
            }
            final String serviceExemptionGiafId = result.getString("tip_disp");
            final ServiceExemption serviceExemption = metadata.exemption(serviceExemptionGiafId);
            if (serviceExemption == null) {
                logger.debug("Empty serviceExemption: " + serviceExemptionGiafId + " for person number: " + numberString);
                importedButInvalid.add(person);

            }
            String beginDateString = result.getString("DATA_INICIO");
            final LocalDate beginDate =
                    StringUtils.isEmpty(beginDateString) ? null : new LocalDate(Timestamp.valueOf(beginDateString));
            if (beginDate == null) {
                logger.debug("Empty beginDate. Person number: " + numberString + " ServiceExemption: " + serviceExemptionGiafId);
                importedButInvalid.add(person);
            }
            String endDateString = result.getString("DATA_FIM");
            final LocalDate endDate = StringUtils.isEmpty(endDateString) ? null : new LocalDate(Timestamp.valueOf(endDateString));
            if (beginDate != null && endDate != null) {
                if (beginDate.isAfter(endDate)) {
                    logger.debug("BeginDate after endDate. Person number: " + numberString + " begin: " + beginDate + " end: "
                            + endDate);
                    importedButInvalid.add(person);
                }
            }
            String creationDateString = result.getString("data_criacao");
            if (StringUtils.isEmpty(creationDateString)) {
                logger.debug("Empty creationDate. Person number: " + numberString + " ServiceExemption: "
                        + serviceExemptionGiafId);
                notImported++;
                continue;
            }
            final DateTime creationDate = new DateTime(Timestamp.valueOf(creationDateString));

            String modifiedDateString = result.getString("data_alteracao");
            final DateTime modifiedDate =
                    StringUtils.isEmpty(modifiedDateString) ? null : new DateTime(Timestamp.valueOf(modifiedDateString));

            if (!hasPersonServiceExemption(giafProfessionalData, beginDate, endDate, serviceExemption, serviceExemptionGiafId,
                    creationDate, modifiedDate)) {
                modifications.add(new Modification() {
                    @Override
                    public void execute() {
                        new PersonServiceExemption(giafProfessionalData, beginDate, endDate, serviceExemption,
                                serviceExemptionGiafId, creationDate, modifiedDate);
                    }
                });
                news++;
            }
        }
        result.close();
        preparedStatement.close();

        int deleted = 0;
        int totalInFenix = 0;
        int repeted = 0;
        for (GiafProfessionalData giafProfessionalData : Bennu.getInstance().getGiafProfessionalDataSet()) {
            for (PersonProfessionalExemption personProfessionalExemption : giafProfessionalData
                    .getPersonProfessionalExemptionsSet()) {
                if (personProfessionalExemption instanceof PersonServiceExemption
                        && personProfessionalExemption.getAnulationDate() == null) {
                    final PersonServiceExemption personServiceExemption = (PersonServiceExemption) personProfessionalExemption;
                    int countThisPersonServiceExemptionOnGiaf =
                            countThisPersonServiceExemptionOnGiaf(oracleConnection, personServiceExemption, logger);
                    if (countThisPersonServiceExemptionOnGiaf == 0) {
                        modifications.add(new Modification() {
                            @Override
                            public void execute() {
                                personServiceExemption.setAnulationDate(new DateTime());
                            }
                        });
                        deleted++;
                    } else {
                        totalInFenix++;
                        if (countThisPersonServiceExemptionOnGiaf > 1) {
                            repeted += countThisPersonServiceExemptionOnGiaf - 1;
                        }
                    }
                }
            }
        }

        oracleConnection.closeConnection();
        log.println("-- Service Exemptions --");
        log.println("Total GIAF: " + count);
        log.println("New: " + news);
        log.println("Deleted: " + deleted);
        log.println("Not imported: " + notImported);
        log.println("Imported with errors: " + importedButInvalid.size());
        log.println("Repeted: " + repeted);
        log.println("Invalid persons: " + dontExist);
        log.println("Total Fénix: " + totalInFenix);
        log.println("Total Fénix without errors: " + (totalInFenix - importedButInvalid.size()));
        log.println("Missing in Fénix: " + (count - totalInFenix));
        return modifications;
    }

    private int countThisPersonServiceExemptionOnGiaf(PersistentSuportGiaf oracleConnection,
            PersonServiceExemption personServiceExemption, Logger logger) throws SQLException {
        String query = getServiceExemptionQuery(personServiceExemption);
        PreparedStatement preparedStatement = null;
        ResultSet result = null;
        try {
            preparedStatement = oracleConnection.prepareStatement(query);
            result = preparedStatement.executeQuery();
            if (result.next()) {
                int count = result.getInt("cont");
                if (count > 0) {
                    if (count > 1) {
                        logger.debug("---> " + count + " ---> "
                                + personServiceExemption.getGiafProfessionalData().getGiafPersonIdentification() + " FA: "
                                + personServiceExemption.getServiceExemption().getGiafId());
                    }
                    return count;
                }
            }
            return 0;
        } finally {
            if (result != null) {
                result.close();
            }
            preparedStatement.close();
        }
    }

    private String getServiceExemptionQuery(PersonServiceExemption personServiceExemption) {
        StringBuilder query = new StringBuilder();
        query.append("select count(*) as cont from SLDEQUIDISP where emp_num=");
        query.append(personServiceExemption.getGiafProfessionalData().getGiafPersonIdentification());
        if (personServiceExemption.getBeginDate() != null) {
            query.append(" and DATA_INICIO=to_date('");
            query.append(dateFormat.print(personServiceExemption.getBeginDate()));
            query.append("','YYYY-MM-DD')");
        } else {
            query.append(" and DATA_INICIO is null");
        }
        if (personServiceExemption.getEndDate() != null) {
            query.append(" and DATA_FIM=to_date('");
            query.append(dateFormat.print(personServiceExemption.getEndDate()));
            query.append("','YYYY-MM-DD')");
        } else {
            query.append(" and DATA_FIM is null");
        }
        if (personServiceExemption.getServiceExemption() != null) {
            query.append(" and tip_disp=");
            query.append(personServiceExemption.getServiceExemptionGiafId());
        } else {
            query.append(" and tip_disp is null");
        }

        query.append(" and data_criacao=to_date('");
        query.append(dateTimeFormat.print(personServiceExemption.getCreationDate()));
        query.append("','YYYY-MM-DD HH24:mi:ss')");
        if (personServiceExemption.getModifiedDate() != null) {
            query.append(" and data_alteracao=to_date('");
            query.append(dateTimeFormat.print(personServiceExemption.getModifiedDate()));
            query.append("','YYYY-MM-DD HH24:mi:ss')");
        } else {
            query.append("and data_alteracao is null");
        }
        return query.toString();

    }

    private boolean hasPersonServiceExemption(GiafProfessionalData giafProfessionalData, LocalDate beginDate, LocalDate endDate,
            ServiceExemption serviceExemption, String serviceExemptionGiafId, DateTime creationDate, DateTime modifiedDate) {
        for (PersonProfessionalExemption personProfessionalExemption : giafProfessionalData.getPersonProfessionalExemptionsSet()) {
            if (personProfessionalExemption instanceof PersonServiceExemption) {
                PersonServiceExemption personServiceExemption = (PersonServiceExemption) personProfessionalExemption;
                if (personServiceExemption.getAnulationDate() == null
                        && Objects.equals(personServiceExemption.getBeginDate(), beginDate)
                        && Objects.equals(endDate, personServiceExemption.getEndDate())
                        && Objects.equals(serviceExemption, personServiceExemption.getServiceExemption())
                        && Objects.equals(serviceExemptionGiafId, personServiceExemption.getServiceExemptionGiafId())
                        && Objects.equals(creationDate, personServiceExemption.getCreationDate())
                        && Objects.equals(modifiedDate, personServiceExemption.getModifiedDate())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected String getQuery() {
        return "SELECT dispensas.DATA_FIM, dispensas.DATA_INICIO, dispensas.EMP_NUM, dispensas.tip_disp, dispensas.data_criacao, dispensas.data_alteracao FROM SLDEQUIDISP dispensas WHERE dispensas.TIPO(+) = 'D'";
    }
}
