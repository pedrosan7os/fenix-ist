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
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.person.MaritalStatus;
import org.fenixedu.academic.domain.person.RoleType;
import org.fenixedu.academic.util.StringFormatter;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.StringNormalizer;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;

import pt.ist.fenix.task.giafsync.GiafSync.ImportProcessor;
import pt.ist.fenix.task.giafsync.GiafSync.Modification;
import pt.ist.fenix.task.updateData.fixNames.DBField2Cap;
import pt.ist.fenix.util.oracle.PersistentSuportGiaf;
import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.ContractSituation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.GiafProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonProfessionalData;

public class UpdatePersonsFromGiaf extends ImportProcessor {

    private static final String SEPARATOR = " - ";

    private static final String MALE = "M";

    private static final String FEMALE = "F";

    @Override
    public List<Modification> processChanges(GiafMetadata metadata, PrintWriter log, Logger logger) throws SQLException {
        List<Modification> modifications = new ArrayList<>();
        try {
            Authenticate.mock(User.findByUsername("ist23932"));

            int count = 0, notImported = 0;
            Set<Person> newPersons = new HashSet<Person>();
            Set<Person> editedPersons = new HashSet<Person>();

            PersistentSuportGiaf oracleConnection = PersistentSuportGiaf.getInstance();
            PreparedStatement preparedStatement = oracleConnection.prepareStatement(getQuery());
            ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                count++;
                String personNumberString = result.getString("EMP_NUM");
                final Person personByNumber = metadata.getPerson(personNumberString, logger);
                if (personByNumber == null) {
                    logger.debug("Invalid person with number: " + personNumberString);
                    notImported++;
                    continue;
                }

                String giafName = result.getString("EMP_NOM");
                String IdDocTypeString = result.getString("emp_bi_tp");
                IDDocumentType idDocumentType = metadata.documentType(IdDocTypeString);
                String oldIdDocNumberString = result.getString("emp_bi_num");
                String idDocNumberString = getValidID(idDocumentType, oldIdDocNumberString);
                String contributorNumber = result.getString("emp_num_fisc");

                String countryName = result.getString("nac_dsc");
                Country nationality =
                        StringUtils.isEmpty(countryName) ? null : metadata.country(StringNormalizer.normalize(countryName));
                MaritalStatus maritalStatus = metadata.maritalStatus(result.getString("emp_stcivil"));
                Gender gender = getGender(result.getString("emp_sex"));
                String validateDocuments =
                        validateDocuments(personNumberString, idDocumentType, oldIdDocNumberString, idDocNumberString,
                                contributorNumber, countryName, nationality, maritalStatus, gender);
                if (!StringUtils.isEmpty(validateDocuments)) {
                    logger.debug("---------------------------------\n" + validateDocuments);
                    notImported++;
                    continue;
                }

                Person personById = Person.readByDocumentIdNumberAndIdDocumentType(idDocNumberString, idDocumentType);
                if (personById != null && !personByNumber.equals(personById)) {
                    logger.debug(personNumberString + "-------------------Confito de pessoas ------------------- "
                            + personByNumber.getUsername() + " e " + personById.getUsername());
                    notImported++;
                    continue;
                }
                // String diffs = getDifferences(personById, personNumberString,
                // idDocumentType, idDocNumberString, giafName,
                // contributorNumber);
                // if (!StringUtils.isEmpty(diffs)) {
                // logger.debug(personNumberString +
                // "---------------------------------" + diffs);
                // notImported++;
                // continue;
                // }

                Party party = Person.readByContributorNumber(contributorNumber);
                if (party != null) {
                    if (party instanceof Person) {
                        Person person2 = (Person) party;
                        if (personByNumber == null || personByNumber != person2) {
                            logger.debug("---------------------------------\nJá existe pessoa com o número de contribuinte:"
                                    + getDifferences(person2, personNumberString, idDocumentType, idDocNumberString, giafName,
                                            contributorNumber));
                            notImported++;
                            continue;
                        }
                    } else {
                        logger.debug("---------------------------------\nJá existe UNIDADE com o número de contribuinte: "
                                + contributorNumber + " \n\t Unidade Fénix: " + party.getName() + ". Número Mecanográfico: "
                                + personNumberString);
                        notImported++;
                        continue;
                    }
                }

                YearMonthDay idEmissionDate = getLocalDateFromString(result.getString("emp_bi_dt"));
                if (canEditPersonInfo(personByNumber, idEmissionDate)) {

// Don't care for giaf name changes
//                    final String prettyPrintName = StringFormatter.prettyPrint(giafName);
//                    if (!personByNumber.getName().equals(prettyPrintName)) {
//                        if (namesCorrectlyPartitioned(personByNumber, prettyPrintName)) {
//                            modifications.add(new Modification() {
//                                @Override
//                                public void execute() {
//                                    personByNumber.setName(prettyPrintName);
//                                }
//                            });
//                            if (!newPersons.contains(personByNumber)) {
//                                editedPersons.add(personByNumber);
//                            }
//                        } else {
//                            logger.debug("\nNão pode alterar nome (tem nomes partidos). Número Mecanográfico: "
//                                    + personNumberString + " Nome: " + prettyPrintName);
//                        }
//                    }

                    YearMonthDay idExpirationDate = getLocalDateFromString(result.getString("emp_bi_val_dt"));
                    String idArquive = result.getString("emp_bi_arq");
                    String fiscalNeighborhood = result.getString("emp_bfiscal");

                    if (!hasEqualPersonalInformation(maritalStatus, gender, idEmissionDate, idExpirationDate, idArquive,
                            idDocumentType, idDocNumberString, contributorNumber, fiscalNeighborhood, nationality, personByNumber)) {
                        modifications.add(setPersonalInformation(maritalStatus, gender, idEmissionDate, idExpirationDate,
                                idArquive, idDocumentType, idDocNumberString, contributorNumber, fiscalNeighborhood, nationality,
                                personByNumber));
                        if (!newPersons.contains(personByNumber)) {
                            editedPersons.add(personByNumber);
                        }
                    }

                    YearMonthDay dateOfBirth = getLocalDateFromString(result.getString("emp_nsc_dt"));
                    String parishOfBirth = StringFormatter.prettyPrint(result.getString("emp_nat_frg"));
                    String districtOfBirth = StringFormatter.prettyPrint(result.getString("emp_nat_dst"));
                    String districtSubdivisionOfBirth = StringFormatter.prettyPrint(result.getString("emp_nat_cnc"));
                    String nameOfFatherString = result.getString("father");
                    String nameOfFather = nameOfFatherString == null ? null : DBField2Cap.prettyPrint(nameOfFatherString);
                    String nameOfMotherString = result.getString("mother");
                    String nameOfMother = nameOfMotherString == null ? null : DBField2Cap.prettyPrint(nameOfMotherString);
                    if (!hasEqualBirthInformation(dateOfBirth, parishOfBirth, districtOfBirth, districtSubdivisionOfBirth,
                            nameOfFather, nameOfMother, personByNumber)) {
                        modifications.add(setBirthInformation(dateOfBirth, parishOfBirth, districtOfBirth,
                                districtSubdivisionOfBirth, nameOfFather, nameOfMother, personByNumber));
                        if (!newPersons.contains(personByNumber)) {
                            editedPersons.add(personByNumber);
                        }
                    }

                } else {
                    logger.debug("Não actualiza: " + personNumberString + " - " + giafName + " ->é aluno! ");
                }
            }
            result.close();
            preparedStatement.close();
            oracleConnection.closeConnection();
            log.println("-- Update Persons --");
            log.println("Total GIAF: " + count);
            log.println("Not imported: " + notImported);
            log.println("New: " + newPersons.size());
            log.println("Edited: " + editedPersons.size());
        } finally {
            Authenticate.unmock();
        }
        return modifications;

    }

    private boolean namesCorrectlyPartitioned(Person personByNumber, String prettyPrintName) {
        if (StringUtils.isEmpty(personByNumber.getProfile().getGivenNames())
                && StringUtils.isEmpty(personByNumber.getFamilyNames())) {
            return true;
        }
        return (personByNumber.getProfile().getGivenNames() + " " + personByNumber.getFamilyNames()).equals(prettyPrintName);
    }

    private boolean canEditPersonInfo(Person personByNumber, YearMonthDay idEmissionDate) {
        Boolean isStudent = RoleType.STUDENT.isMember(personByNumber.getUser());
        ContractSituation contractSituation = getCurrentContractSituation(personByNumber.getEmployee());
        if (contractSituation != null && contractSituation.getEndSituation() && isStudent) {
            if (idEmissionDate == null || idEmissionDate.isBefore(personByNumber.getEmissionDateOfDocumentIdYearMonthDay())) {
                return false;
            }
        }
        return true;
    }

    private ContractSituation getCurrentContractSituation(Employee employee) {
        PersonContractSituation currentPersonContractSituation = null;
        if (employee != null) {
            PersonProfessionalData personProfessionalData = employee.getPerson().getPersonProfessionalData();
            if (personProfessionalData != null) {
                LocalDate today = new LocalDate();
                for (GiafProfessionalData giafProfessionalData : personProfessionalData.getGiafProfessionalDatasSet()) {
                    for (final PersonContractSituation situation : giafProfessionalData.getValidPersonContractSituations()) {
                        if (situation.isActive(today)
                                && (currentPersonContractSituation == null || situation.isAfter(currentPersonContractSituation))) {
                            currentPersonContractSituation = situation;
                        }
                    }
                }
            }
        }
        return currentPersonContractSituation != null ? currentPersonContractSituation.getContractSituation() : null;
    }

    private String getDifferences(Person person, String employeeNumberString, IDDocumentType idDocumentType,
            String idDocNumberString, String giafName, String contributorNumber) {
        StringBuilder log = new StringBuilder();
        Integer employeeNumber = null;
        Employee employee = null;
        try {
            employeeNumber = Integer.parseInt(employeeNumberString);
            employee = Employee.readByNumber(employeeNumber);
        } catch (NumberFormatException e) {
        }
        if (person == null) {
            if (employee != null) {
                log.append("\nExiste um funcionário com o número: " + employeeNumber
                        + "\t mas com outra identificação\tFénix ID:" + employee.getPerson().getIdDocumentType().name()
                        + SEPARATOR + employee.getPerson().getDocumentIdNumber() + " !=  Giaf ID:" + idDocumentType + "  - "
                        + idDocNumberString);
            }
        } else {
            if (person.getEmployee() != null) {
                if (!person.getEmployee().getEmployeeNumber().equals(employeeNumber)) {
                    log.append("\nNúmeros diferentes!\t Fénix num:" + person.getEmployee().getEmployeeNumber() + " !=  Giaf num:"
                            + employeeNumberString);
                }
            } else if (employee != null) {
                log.append("\nExiste outro funcionário com o número: " + employeeNumberString + "\tFénix ID:"
                        + employee.getPerson().getIdDocumentType().name() + SEPARATOR
                        + employee.getPerson().getDocumentIdNumber() + " !=  Giaf ID:" + idDocumentType + "  - "
                        + idDocNumberString);
            }
            if ((!person.getDocumentIdNumber().equalsIgnoreCase(idDocNumberString))
                    || (!idDocumentType.equals(person.getIdDocumentType()))) {
                log.append("\nIDs diferentes!\t Fénix ID:" + person.getIdDocumentType().name() + SEPARATOR
                        + person.getDocumentIdNumber() + " !=  Giaf ID:" + idDocumentType + "  - " + idDocNumberString);
            }
            // if (!equalName(giafName, person.getName())) {
            // log.append("\nNomes diferentes!\t Fénix nome: " +
            // StringNormalizer.normalize(person.getName())
            // + " !=  Giaf nome: " + giafName);
            // }

            if (person.getSocialSecurityNumber() != null) {
                if (!person.getSocialSecurityNumber().equals(contributorNumber)) {
                    log.append("\nNIFs diferentes!\t Fénix NIF: " + person.getSocialSecurityNumber() + " !=  Giaf NIF: "
                            + contributorNumber);
                }
            }
        }
        return log.toString();
    }

    private String validateDocuments(String employeeNumber, IDDocumentType idDocumentType, String oldIdDocNumberString,
            String idDocNumberString, String contributorNumber, String countryName, Country nationality,
            MaritalStatus maritalStatus, Gender gender) {
        StringBuilder errors = new StringBuilder();
        if (idDocumentType == null) {
            errors.append("\nVAZIO: Tipo de Documento de Identificação. Número Mecanográfico: " + employeeNumber);
        }
        if (StringUtils.isEmpty(oldIdDocNumberString)) {
            errors.append("\nVAZIO: Número de Documento de Identificação. Número Mecanográfico: " + employeeNumber);
        }

        if (idDocNumberString == null) {
            errors.append("\nINVÁLIDO: Número de Documento de Identificação. Número Mecanográfico: " + employeeNumber
                    + " TIPO DI:" + idDocumentType.toString() + " Número DI:" + oldIdDocNumberString);
        }
        if (StringUtils.isEmpty(contributorNumber)) {
            errors.append("\nVAZIO: Número de contribuinte. Número Mecanográfico: " + employeeNumber);
        }
        if (nationality == null) {
            errors.append("\nINVÁLIDO: Nacionalidade. Número Mecanográfico: " + employeeNumber + " Nacionalidade:" + countryName);
        }
        if (maritalStatus == null) {
            errors.append("\nINVÁLIDO: Estado Civil. Número Mecanográfico: " + employeeNumber);
        }
        if (gender == null) {
            errors.append("\nINVÁLIDO: Sexo. Número Mecanográfico: " + employeeNumber);
        }
        return errors.toString();
    }

    private boolean hasEqualPersonalInformation(MaritalStatus maritalStatus, Gender gender, YearMonthDay idEmissionDate,
            YearMonthDay idExpirationDate, String idArquive, IDDocumentType idDocumentType, String idDocNumberString,
            String socialSecurityNumber, String fiscalNeighborhood, Country nationality, Person person) {
        return Objects.equals(person.getMaritalStatus(), maritalStatus) && Objects.equals(person.getGender(), gender)
                && Objects.equals(person.getEmissionDateOfDocumentIdYearMonthDay(), idEmissionDate)
                && Objects.equals(person.getExpirationDateOfDocumentIdYearMonthDay(), idExpirationDate)
                && Objects.equals(person.getEmissionLocationOfDocumentId(), idArquive)
                && Objects.equals(person.getDocumentIdNumber(), idDocNumberString)
                && Objects.equals(person.getIdDocumentType(), idDocumentType)
                && Objects.equals(person.getSocialSecurityNumber(), socialSecurityNumber)
                && Objects.equals(person.getFiscalCode(), fiscalNeighborhood) && Objects.equals(person.getCountry(), nationality);
    }

    private Modification setPersonalInformation(final MaritalStatus maritalStatus, final Gender gender,
            final YearMonthDay idEmissionDate, final YearMonthDay idExpirationDate, final String idArquive,
            final IDDocumentType idDocumentType, final String idDocNumberString, final String socialSecurityNumber,
            final String fiscalNeighborhood, final Country nationality, final Person person) {
        return new Modification() {
            @Override
            public void execute() {
                person.setMaritalStatus(maritalStatus);
                person.setGender(gender);
                person.setEmissionDateOfDocumentIdYearMonthDay(idEmissionDate);
                person.setExpirationDateOfDocumentIdYearMonthDay(idExpirationDate);
                person.setEmissionLocationOfDocumentId(idArquive);
                person.setIdentification(idDocNumberString, idDocumentType);
                person.setSocialSecurityNumber(socialSecurityNumber); // contribuinte
                person.setFiscalCode(fiscalNeighborhood); // bairro fiscal
                person.setCountry(nationality);
            }
        };
    }

    private boolean hasEqualBirthInformation(YearMonthDay dateOfBirth, String parishOfBirth, String districtOfBirth,
            String districtSubdivisionOfBirth, String nameOfFather, String nameOfMother, Person person) {
        return Objects.equals(person.getDateOfBirthYearMonthDay(), dateOfBirth)
                && Objects.equals(person.getParishOfBirth(), parishOfBirth)
                && Objects.equals(person.getDistrictOfBirth(), districtOfBirth)
                && Objects.equals(person.getDistrictSubdivisionOfBirth(), districtSubdivisionOfBirth)
                && Objects.equals(person.getNameOfFather(), nameOfFather)
                && Objects.equals(person.getNameOfMother(), nameOfMother);
    }

    private Modification setBirthInformation(final YearMonthDay dateOfBirth, final String parishOfBirth,
            final String districtOfBirth, final String districtSubdivisionOfBirth, final String nameOfFather,
            final String nameOfMother, final Person person) {
        return new Modification() {
            @Override
            public void execute() {
                person.setDateOfBirthYearMonthDay(dateOfBirth);
                person.setParishOfBirth(parishOfBirth);
                person.setDistrictOfBirth(districtOfBirth);
                person.setDistrictSubdivisionOfBirth(districtSubdivisionOfBirth);
                person.setNameOfFather(nameOfFather);
                person.setNameOfMother(nameOfMother);
            }
        };
    }

    private Gender getGender(String gender) {
        if (!StringUtils.isEmpty(gender)) {
            if (gender.equalsIgnoreCase(FEMALE)) {
                return Gender.FEMALE;
            } else if (gender.equalsIgnoreCase(MALE)) {
                return Gender.MALE;
            }
        }
        return null;
    }

    private YearMonthDay getLocalDateFromString(String dateString) {
        YearMonthDay date = null;
        if (!StringUtils.isEmpty(dateString)) {
            date = new YearMonthDay(Timestamp.valueOf(dateString));
        }
        return date;
    }

    private String getValidID(IDDocumentType idDocumentType, String idDocNumberString) {
        if (idDocumentType != null && idDocumentType.equals(IDDocumentType.IDENTITY_CARD)) {
            String idDocNumberStringTrimmed = idDocNumberString.replaceAll("\\s*", "");
            try {
                Integer.valueOf(idDocNumberStringTrimmed);
                return idDocNumberStringTrimmed;
            } catch (NumberFormatException e) {
                if (idDocNumberStringTrimmed.matches("\\d{7,8}\\d[a-zA-Z][a-zA-Z]\\d")) {
                    String[] split = idDocNumberStringTrimmed.split("\\d[a-zA-Z][a-zA-Z]\\d", 0);
                    return Integer.valueOf(split[0]).toString();
                }
            }
        } else {
            return idDocNumberString;
        }
        return null;
    }

    protected String getQuery() {
        StringBuilder query = new StringBuilder();
        query.append("SELECT emp.EMP_NUM, emp.EMP_NOM, info.emp_sex,info.emp_stcivil");
        query.append(",info.emp_bi_tp, info.emp_bi_num, info.emp_bi_arq, info.emp_bi_dt, info.emp_bi_val_dt, info.emp_nsc_dt");
        query.append(",info.emp_num_fisc,emp_bfiscal");
        query.append(",(select emp_nom_famil from sldemp06 where emp_grau_parent = 'P' and emp_sex='M' and rownum=1 and emp.EMP_NUM=emp_num) as father");
        query.append(",(select emp_nom_famil from sldemp06 where emp_grau_parent = 'P' and emp_sex='F' and rownum=1 and emp.EMP_NUM=emp_num) as mother");
        query.append(", nac.nac_dsc, info.emp_nat_loc, info.emp_nat_frg, info.emp_nat_cnc, info.emp_nat_dst");
        query.append(" FROM SLDEMP01 emp, SLDEMP03 info, SLTNAC nac");
        query.append(" WHERE emp.EMP_NUM = info.EMP_NUM and info.emp_nac=nac.emp_nac");
        return query.toString();
    }
}
