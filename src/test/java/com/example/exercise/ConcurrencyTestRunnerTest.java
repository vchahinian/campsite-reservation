package com.example.exercise;


import com.example.errors.InvalidReservation;
import com.google.code.tempusfugit.concurrency.ConcurrentTestRunner;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


@RunWith(ConcurrentTestRunner.class)
class ConcurrentTestRunnerTest {

    @Test
    public void shouldRunInParallel1() {
        try {
            CampsiteReservation reservation1 = new CampsiteReservation("2020-07-17", "2020-07-20", "Sample1 Name", "sample1@email.com");
            reservation1.save();
        } catch (NoSuchAlgorithmException | SQLException | InvalidReservation e) {
            e.printStackTrace();
        }
        System.out.println("I'm running on thread " + Thread.currentThread().getName());
    }

    @Test
    public void shouldRunInParallel2() {
        try {
            CampsiteReservation reservation2 = new CampsiteReservation("2020-07-17", "2020-07-20", "Sample2 Name", "sample2@email.com");
            reservation2.save();

        } catch (NoSuchAlgorithmException | SQLException | InvalidReservation e) {
            e.printStackTrace();
        }
        System.out.println("I'm running on thread " + Thread.currentThread().getName());
    }

    @Test
    public void shouldRunInParallel3() {
        try {
            CampsiteReservation reservation3 = new CampsiteReservation("2020-07-17", "2020-07-20", "Sample3 Name", "sample3@email.com");
            reservation3.save();
        } catch (NoSuchAlgorithmException | SQLException | InvalidReservation e) {
            e.printStackTrace();
        }
        System.out.println("I'm running on thread " + Thread.currentThread().getName());
    }

    @Test
    public void validatedDB() throws TimeoutException, SQLException, InterruptedException {
        Connection conn = CampsiteReservation.getConnection();
        PreparedStatement statement = conn.prepareStatement("SELECT count(*) FROM schedule;");

        ResultSet rs = statement.executeQuery();
        int nbr_rows = 0;
        if (rs.next()) {
            nbr_rows = rs.getInt(1);
        }
        final int test = nbr_rows;
        assertThat(nbr_rows,equalTo(1));
        waitOrTimeout(() -> test == 1, seconds(10));
    }
}
