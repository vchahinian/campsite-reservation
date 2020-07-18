package com.example.exercise;

import com.example.errors.InvalidReservation;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(SpringRunner.class)
class ExerciseApplicationTests {
	public String testReference = null;

	@Test
	public void campsite_reservation_save() throws InvalidReservation, NoSuchAlgorithmException, SQLException {
		String startDate = "2020-08-19";
		String endDate = "2020-08-21";
		String fullName = "Sample Name";
		String email = "sample@email.com";
		CampsiteReservation testReservation = new CampsiteReservation(startDate, endDate, fullName, email);
		testReference = testReservation.save();
		String seed = email + startDate + endDate;
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		byte[] result =  sha.digest(seed.getBytes());
		String reference = CampsiteReservation.hexEncode(result);
		assertThat(
				reference,
				equalTo(testReference)
		);
	}

	@Test
	public void campsite_reservation_update() throws InvalidReservation, NoSuchAlgorithmException, SQLException {
		String startDate = "2020-07-22";
		String endDate = "2020-07-25";
		String fullName = "Sample Name";
		String email = "sample@email.com";
		CampsiteReservation oldReservation = new CampsiteReservation(startDate, endDate, fullName, email);
		oldReservation.save();

		startDate = "2020-07-25";
		endDate = "2020-07-28";
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate parsedStartDate = LocalDate.parse(startDate, formatter);
		LocalDate parsedEndDate = LocalDate.parse(endDate, formatter);

		oldReservation.setStartDate(parsedStartDate);
		oldReservation.setEndDate(parsedEndDate);
		oldReservation.update();

	}

	@Test
	public void campsite_reservation_save_with_dates_taken_error() throws NoSuchAlgorithmException, InvalidReservation, SQLException {
		CampsiteReservation testReservation = new CampsiteReservation("2020-08-01", "2020-08-03", "Sample Name", "sample@email.com");
		testReservation.save();
		CampsiteReservation duplicate = new CampsiteReservation("2020-08-01", "2020-08-03", "Sample Name2", "sample2@email.com");
		Exception exception = assertThrows(InvalidReservation.class, duplicate::save);
		assertEquals("There is already a reservation on your chosen dates", exception.getMessage());
	}

	@Test
	public void campsite_reservation_save_with_more_than_three_day_error() throws NoSuchAlgorithmException {
		CampsiteReservation testReservation = new CampsiteReservation("2020-08-07", "2020-08-11", "Sample Name", "sample@email.com");
		Exception exception = assertThrows(InvalidReservation.class, testReservation::save);
		assertEquals("You can only reserve a maximum of 3 days", exception.getMessage());
	}

	@Test
	public void campsite_reservation_save_more_than_one_month_error() throws NoSuchAlgorithmException {
		CampsiteReservation testReservation = new CampsiteReservation("2020-12-07", "2020-12-10", "Sample Name", "sample@email.com");
		Exception exception = assertThrows(InvalidReservation.class, testReservation::save);
		assertEquals("Date must be at least 1 day before and a maximum 1 month in advance", exception.getMessage());
	}
}
