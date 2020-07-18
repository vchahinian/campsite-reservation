package com.example.exercise;

import com.example.errors.InvalidReservation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping(value = "/campsite-reservation", produces = "application/json")
public class CampsiteReservationController {

    @RequestMapping(value = "/get_by_date_range", method = RequestMethod.GET)
    public ResponseEntity getAllCampsiteReservations(@RequestParam(name="startDate", required=false) String startDate, @RequestParam(name="endDate", required=false) String endDate) {
        if (startDate != null){
            try {
                return ResponseEntity.status(HttpStatus.OK).body(CampsiteReservation.getAllAvailabilitiesByDateRange(startDate, endDate));
            } catch (SQLException throwables) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected Error. Please contact the Administrator.");
            }
        }else{
            try {
                return ResponseEntity.status(HttpStatus.OK).body(CampsiteReservation.getAllAvailabilities());
            } catch (SQLException throwables) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected Error. Please contact the Administrator.");
            }
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity createCampsiteReservation(@RequestParam(name="startDate", required=true) String startDate, String endDate, String fullName, String email ) {
        String reference;

        try {
            CampsiteReservation newReservation = new CampsiteReservation(startDate, endDate, fullName, email);
            reference = newReservation.save();
        } catch (InvalidReservation invalidReservation) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(invalidReservation.getMessage());
        } catch(SQLIntegrityConstraintViolationException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Reservation already exists for that timeslot using email "+email);
        }catch (NoSuchAlgorithmException | SQLException unexpectedError){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected Error. Please contact the Administrator.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(reference);
    }

    @RequestMapping(value = "/{reference}", method = RequestMethod.PUT)
    public ResponseEntity updateCampsiteReservation(@RequestParam(name = "reference", required=false) @PathVariable String reference, @RequestParam(name="startDate", required=false) String startDate, @RequestParam(name="endDate", required=false) String endDate, @RequestParam(name="fullName", required=false) String fullName, @RequestParam(name="email", required=false) String email ) {

        CampsiteReservation reservation;
        try {
            reservation = CampsiteReservation.getReservationByReference(reference);
        } catch (SQLException throwables) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected Error. Please contact the Administrator.");
        }

        if (startDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate parsedStartDate = LocalDate.parse(startDate, formatter);
            reservation.setStartDate(parsedStartDate);
        }

        if (endDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate parsedEndDate = LocalDate.parse(endDate, formatter);
            reservation.setEndDate(parsedEndDate);
        }

        if (endDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate parsedEndDate = LocalDate.parse(endDate, formatter);
            reservation.setEndDate(parsedEndDate);
        }

        if (fullName != null) {
            reservation.setFullName(fullName);
        }

        if (email != null) {
            reservation.setEmail(email);
        }

        try {
            reservation.update();
        } catch (InvalidReservation invalidReservation) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(invalidReservation.getMessage());
        } catch (SQLException throwables) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected Error. Please contact the Administrator.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(reference);
    }

    @RequestMapping(value = "/{reference}", method = RequestMethod.GET)
    public ResponseEntity getCampsiteReservation( @PathVariable String reference) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(CampsiteReservation.getReservationByReference(reference).toString());
        } catch (SQLException throwables) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected Error. Please contact the Administrator.");
        }
    }

    @RequestMapping(value = "/{reference}", method = RequestMethod.DELETE)
    public ResponseEntity deleteCampsiteReservation(@RequestParam(name = "reference", required = false) @PathVariable String reference) {
        try {
            CampsiteReservation.delete(reference);
            return ResponseEntity.status(HttpStatus.OK).body("Deleted reference " + reference);
        } catch (SQLException throwables) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected Error. Please contact the Administrator.");
        }
    }

}
