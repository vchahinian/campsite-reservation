package com.example.exercise;

import com.example.errors.InvalidReservation;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class CampsiteReservation {

    private String reference;
    private LocalDate startDate;
    private LocalDate endDate;
    private String fullName;
    private String email;

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/test_java?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        String user = "test_java";
        String password = "testjava";

        return DriverManager.getConnection(url, user, password);
    }


    public CampsiteReservation(String startDate, String endDate, String fullName, String email) throws NoSuchAlgorithmException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate parsedStartDate = LocalDate.parse(startDate, formatter);
        LocalDate parsedEndDate = LocalDate.parse(endDate, formatter);

        String seed = email + startDate + endDate;
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] result =  sha.digest(seed.getBytes());

        // Need to generate reference
        this.reference = hexEncode(result);
        this.startDate = parsedStartDate;
        this.endDate = parsedEndDate;
        this.fullName = fullName;
        this.email = email;
    }

    // When retrieving from DB
    public CampsiteReservation(String reference, String startDate, String endDate, String fullName, String email) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate parsedStartDate = LocalDate.parse(startDate, formatter);
        LocalDate parsedEndDate = LocalDate.parse(endDate, formatter);
        // Need to generate reference
        this.reference = reference;
        this.startDate = parsedStartDate;
        this.endDate = parsedEndDate;
        this.fullName = fullName;
        this.email = email;
    }

    // Criteria:
    // The campsite can be reserved for max 3 day
    // The campsite can be reserved minimum 1 day(s) ahead of arrival and up to 1 month in advance.
    // Doesn't take into account blackout dates
    public void validateReservation(Connection conn) throws InvalidReservation, SQLException {
        LocalDate currentDate = LocalDate.now();
        long noOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate);

        // Reservation cannot be more than 3 days
        if (noOfDaysBetween > 0) {
            throw new InvalidReservation("You must pick a date that is in the future" );
        }

        // Reservation cannot be more than 3 days
        if (noOfDaysBetween > 3) {
            throw new InvalidReservation("You can only reserve a maximum of 3 days" );
        }



        noOfDaysBetween = ChronoUnit.DAYS.between(currentDate, startDate);
        long noOfMonthsBetween = ChronoUnit.MONTHS.between(currentDate, startDate);
        // Needs to be a minimum of 1 day ahead of time and a maximum of 1 month in advance
        if (noOfDaysBetween < 1 || noOfMonthsBetween > 1) {
            throw new InvalidReservation("Date must be at least 1 day before and a maximum 1 month in advance" );
        }

        // In order for a date range to overlap we need to go back 2 days. Since the constraint is that the maximum
        // reservation duration is 3 days.
        LocalDate startDateRangeStart = startDate.minusDays(2);
        java.sql.Date newEarliestReservationStartDate = java.sql.Date.valueOf(startDateRangeStart);
        java.sql.Date newLatestStartDate = java.sql.Date.valueOf(endDate);

        // Ensuring that no one else has book the same slot this includes overlap

        PreparedStatement statement = conn.prepareStatement("SELECT start_date, end_date FROM schedule WHERE start_date BETWEEN ? AND ?");
        statement.setDate(1, newEarliestReservationStartDate);
        statement.setDate(2, newLatestStartDate);

        ResultSet rs = statement.executeQuery();

        while (rs.next())
        {
            LocalDate dbStartDate = rs.getDate("start_date").toLocalDate();
            LocalDate dbEndDate = rs.getDate("end_date").toLocalDate();

            // If there is any overlap
            if ((startDate.compareTo(dbStartDate) >= 0) || (endDate.compareTo(dbEndDate) <= 0)){
                throw new InvalidReservation("There is already a reservation on your chosen dates" );
            }
        }
        statement.close();
    }

    public static String getAllReservations() throws SQLException {
        JSONArray jsonArray = new JSONArray();

        LocalDate current = LocalDate.now();
        LocalDate localMonthFromNow = current.plusDays(31);

        java.sql.Date monthFromNow = java.sql.Date.valueOf(localMonthFromNow);
        java.sql.Date dateNow = java.sql.Date.valueOf(current);

        try {
            Connection conn = getConnection();
            PreparedStatement statement = conn.prepareStatement("SELECT reference, start_date, end_date, full_name, email from schedule where start_date between ? and ? OR end_date between ? and ?");
            statement.setDate(1, dateNow);
            statement.setDate(2, monthFromNow);
            statement.setDate(3, dateNow);
            statement.setDate(4, monthFromNow);

            ResultSet rs = statement.executeQuery();
            ArrayList<LocalDate[]> availableRange = new ArrayList<LocalDate[]>();
            LocalDate previousEndDate = null;
            while (rs.next())
            {
                LocalDate rsStartDate = rs.getDate("start_date").toLocalDate();
                LocalDate rsEndDate = rs.getDate("end_date").toLocalDate();
                LocalDate[] availableTimeSlot = new LocalDate[2];
                JSONObject jsonObject = new JSONObject();
                if (previousEndDate == null){
                    availableTimeSlot[0] = current;
                }else{
                    availableTimeSlot[0] = previousEndDate;
                }
                availableTimeSlot[1] = rsStartDate;
                availableRange.add(availableTimeSlot);

                jsonObject.put("startDate", availableTimeSlot[0]);
                jsonObject.put("endDate", availableTimeSlot[1]);


                jsonArray.add(jsonObject);

                previousEndDate = rsEndDate;
            }
            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new SQLException(throwables.getMessage());
        }

        return jsonArray.toString();
    }

    public static String getAllReservationsByDateRange(String startDate, String endDate) throws SQLException {
        JSONArray jsonArray = new JSONArray();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate parsedStartDate = LocalDate.parse(startDate, formatter);
        LocalDate parsedEndDate = LocalDate.parse(endDate, formatter);

        java.sql.Date startSqlDate = java.sql.Date.valueOf(parsedStartDate);
        java.sql.Date endSqlDate = java.sql.Date.valueOf(parsedEndDate);

        try {
            Connection conn = getConnection();
            PreparedStatement statement = conn.prepareStatement("SELECT reference, start_date, end_date, full_name, email from schedule where start_date between ? and ? OR end_date between ? and ? ORDER BY start_date ASC");
            statement.setDate(1, startSqlDate);
            statement.setDate(2, endSqlDate);
            statement.setDate(3, startSqlDate);
            statement.setDate(4, endSqlDate);

            ResultSet rs = statement.executeQuery();

            ArrayList<LocalDate[]> availableRange = new ArrayList<LocalDate[]>();
            LocalDate previousEndDate = null;
            while (rs.next())
            {
                LocalDate rsStartDate = rs.getDate("start_date").toLocalDate();
                LocalDate rsEndDate = rs.getDate("end_date").toLocalDate();
                LocalDate[] availableTimeSlot = new LocalDate[2];
                JSONObject jsonObject = new JSONObject();
                if (previousEndDate == null){
                    availableTimeSlot[0] = parsedStartDate;
                }else{
                    availableTimeSlot[0] = previousEndDate;
                }
                availableTimeSlot[1] = rsStartDate;
                availableRange.add(availableTimeSlot);

                jsonObject.put("startDate", availableTimeSlot[0]);
                jsonObject.put("endDate", availableTimeSlot[1]);


                jsonArray.add(jsonObject);

                previousEndDate = rsEndDate;
            }
            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new SQLException(throwables.getMessage());
        }

        return jsonArray.toString();
    }

    public String save() throws InvalidReservation, SQLException {
        java.sql.Date sqlStartDate = java.sql.Date.valueOf(startDate);
        java.sql.Date sqlEndDate = java.sql.Date.valueOf(endDate);


        Connection conn = null;
        PreparedStatement statement = null;

        try {
            conn = getConnection();
            PreparedStatement lockStatement = conn.prepareStatement("LOCK TABLES schedule WRITE");
            lockStatement.execute();
            lockStatement.close();

            validateReservation(conn);
            // Lock table

            statement = conn.prepareStatement("insert into schedule(reference, start_date, end_date, full_name, email) value(?,?,?,?,?)");
            statement.setString(1, reference);
            statement.setDate(2, sqlStartDate);
            statement.setDate(3, sqlEndDate);
            statement.setString(4, fullName);
            statement.setString(5, email);

            statement.execute();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new SQLException(throwables.getMessage());
        } finally {
            // Unlock table
            assert conn != null;
            PreparedStatement unlockStatement = conn.prepareStatement("UNLOCK TABLES;");
            unlockStatement.execute();
            unlockStatement.close();

            if(statement != null){
                statement.close();
            }

            conn.close();

        }
        return reference;
    }

    public void update() throws SQLException, InvalidReservation {

        java.sql.Date sqlStartDate = java.sql.Date.valueOf(startDate);
        java.sql.Date sqlEndDate = java.sql.Date.valueOf(endDate);
        Connection conn = null;
        PreparedStatement statement = null;

        try {
            conn = getConnection();
            // Lock table
            PreparedStatement lockStatement = conn.prepareStatement("LOCK TABLES schedule WRITE");
            lockStatement.execute();
            lockStatement.close();

            validateReservation(conn);

            statement = conn.prepareStatement("UPDATE schedule SET start_date = ?, end_date = ?, full_name = ?, email = ? WHERE reference = ?");
            statement.setDate(1, sqlStartDate);
            statement.setDate(2, sqlEndDate);
            statement.setString(3, fullName);
            statement.setString(4, email);
            statement.setString(5, reference);

            statement.execute();
            // Unlock table
            PreparedStatement unlockStatement = conn.prepareStatement("UNLOCK TABLES;");
            unlockStatement.execute();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new SQLException(throwables.getMessage());
        } finally {
            // Unlock table
            if(conn != null){
                PreparedStatement unlockStatement = conn.prepareStatement("UNLOCK TABLES;");
                unlockStatement.execute();
                unlockStatement.close();

                conn.close();
            }
            if(statement != null){
                statement.close();
            }
        }
    }

    public static void delete(String reference) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement statement = conn.prepareStatement("Delete from schedule where reference = ?");
        statement.setString(1, reference);
        statement.execute();
    }

    public static CampsiteReservation getReservationByReference(String reference) throws SQLException {
        Connection conn = getConnection();

        PreparedStatement statement = conn.prepareStatement("SELECT id, reference, start_date, end_date, full_name, email from schedule where reference = ?");
        statement.setString(1, reference);

        ResultSet rs = statement.executeQuery();
        String startDate = null;
        String endDate = null;
        String fullName = null;
        String email = null;

        while (rs.next())
        {
            startDate = rs.getDate("start_date").toString();
            endDate = rs.getDate("end_date").toString();
            fullName = rs.getString("full_name");
            email = rs.getString("email");

        }
        statement.close();
        conn.close();

        return new CampsiteReservation(reference, startDate, endDate, fullName, email);
    }

    public String toString(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("reference", reference);
        jsonObject.put("startDate", startDate);
        jsonObject.put("endDate", endDate);
        jsonObject.put("fullName", fullName);
        jsonObject.put("email", email);

        return jsonObject.toString();
    }

    static public String hexEncode(byte[] input){
        StringBuilder result = new StringBuilder();
        char[] digits = {'0', '1', '2', '3', '4','5','6','7','8','9','a','b','c','d','e','f'};
        for (int idx = 0; idx < input.length; ++idx) {
            byte b = input[idx];
            result.append(digits[ (b&0xf0) >> 4 ]);
            result.append(digits[ b&0x0f]);
        }
        return result.toString();
    }
}
