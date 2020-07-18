# Upgrade Coding Challenge

## Introduction
My name is Vahe Chahinian, and I haven't coded in Java since university (2012). However, I was able to pick things up quickly and here is what I came up. It was quite challenging due to my time constraints with work and family.

## Objective
Design a campsite reservation system using a REST API design.

### Constraints
* The campsite will be free for all.
* The campsite can be reserved for max 3 days.
* The campsite can be reserved minimum 1 day(s) ahead of arrival and up to 1 month in advance. Reservations can be cancelled anytime.
* For sake of simplicity assume the check-in & check-out time is 12:00 AM

### Requirements
* The users will need to find out when the campsite is available. So the system should expose an API to provide information of the availability of the campsite for a given date range with the default being 1 month.
* Provide an end point for reserving the campsite. The user will provide his/her email & full name at the time of reserving the campsite along with intended arrival date and departure date. Return a unique booking identifier back to the caller if the reservation is successful. The unique booking identifier can be used to modify or cancel the reservation later on. Provide appropriate end point(s) to allow modification/cancellation of an existing reservation
* Due to the popularity of the island, there is a high likelihood of multiple users attempting to reserve the campsite for the same/overlapping date(s). Demonstrate with appropriate test cases that the system can gracefully handle concurrent requests to reserve the campsite. Provide appropriate error messages to the caller to indicate the error cases.
* In general, the system should be able to handle large volume of requests for getting the campsite availability.
* There are no restrictions on how reservations are stored as as long as system constraints are not violated.

## Design
### Technologies Used
* https://start.spring.io/ for setting up the project
* Maven
* Java 14
* Junit
* Mysql
* tomcat 9

### Assumptions
* The dates are strings that are properly formatted like so YYYY-MM-DD
* The same customer cannot create 2 reservations on the same dates
* Customer emails are unique to the customer. In other words 2 different customers cannot have the same email.

### Design
I have created a CampsiteReservation class that stores all the information that we need for a reservation: startDate, endDate, reference, fullName.
The save() function will check if the reservation is valid and then it will insert it into the database. I have put in place a pessimistic write lock in the MySQL database to handle concurrency.
The update() function will update the data in the database. I had first thought about using an optimistic write lock for this however it will not work since we do not know if the customer will change to a date that will coincide with someone else's update or creation of a Campsite Reservation. 
Reading the database will not be affected since we are only locking for writes.

Requests and responses of the endpoints are in json format

I have added an index on the reference column so that we can handle a high volume of reads. This is mainly for when a customer would like to see their reservations or when they would like to make an update.
I have also created a multi column index on the start_date and end_date column in order to handle high volume on the get availability endpoint.
The mysql dump is in the main folder and it's called challenge-db.sql.

### Testing
Currently, I only have unit basic tests to handle the base cases of the exercises. There is also unit tests that will handle concurrency issues.
I would have added more unit tests for a better test coverage, but I was short on time.
There should also be user acceptance testing and system testing to ensure that we are truly delivering what the client wants.

### Additional Handled Edge Cases
* Reservation date must be in the future
* Reservation date must be at least 1 day long

## Enhancements
I have added this section since I would add these improvements if I had more time

### General
* Need to use an ORM, it has down sides however it will speed up development and optimize queries behind the scenes.
    * One downside is that an ORM will abstract out logic which can cause unexpected behaviour 
* A better solution would be using a queueing system. That way we can queue up all the requests in the order they came in and then handle them 1 by 1. This way we won’t need to block other write operations on the database table. This solution might slow things down when it comes to the creation of new reservations however it will speed up write operations overall.
* Should validate date formats and check for nulls.
* Could use a Singleton thread pooling design for getting the db connection. Since closing and opening the db connection all the time is inefficient.

### Tests
* Use a more proper JUnit test, an in memory db would be better since it would be faster and it would get erased after each run of tests
* Furthermore, I would add integration tests to test the endpoints themselves as opposed to testing all the functions that they use.

### DB design
It would be better to store the customer information in a separate table with a foreign key to associate the customer table to the schedule table. That way the customer can update their email or name if they want without messing up the reservation.

### Security
* Encrypt all credentials in the GIT repository. We can also not store the credentials in GIT and store it in a more secure location, which would be a better solution.
* Implement an authentication method
    * I would use a shared token strategy where we would give a secret token to our client so that they can use that to encrypt the data coming to us in the endpoint. We would be able to decrypt the data to ensure that we have a real customer attempting to create or modify the data. This strategy would require a customer to have created an account before creating a reservation.
    * Another strategy would be a simpler token based approach where the toke needs to be sent in the request header however this is less secure since that token can get stolen and compromise the whole system.