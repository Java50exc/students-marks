package telran.students;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.exceptions.MarkIllegalStateException;
import telran.students.exceptions.StudentIllegalStateException;
import telran.students.exceptions.StudentNotFoundException;
import telran.students.repo.StudentRepo;
import telran.students.service.StudentsService;
import telran.students.model.*;

@SpringBootTest
class StudentsMarksServiceTests {
	String SUBJECT1 = "Calculus";
	String SUBJECT2 = "Statistics";
	String SUBJECT3 = "Probability";
	String SUBJECT4 = "Game theory";
	String SUBJECT5 = "Combinatorics";
	
	int FAILED_SCORE = 0;
	int AWFUL_SCORE = 10;
	int LOUSY_SCORE = 30;
	int AVERAGE_SCORE = 50;
	int SATISFACTORY_SCORE = 75;
	int EXCELLENT_SCORE = 100;
	
	LocalDate CUR_DATE = LocalDate.now();
	LocalDate BEFORE_DAY_DATE = CUR_DATE.minusDays(1);
	LocalDate BEFORE_WEEK_DATE = CUR_DATE.minusWeeks(1);
	LocalDate BEFORE_MONTH_DATE = CUR_DATE.minusMonths(1);
	LocalDate DATE_NOT_EXISTS = CUR_DATE.minusYears(3).minusDays(22);
	
	long STUDENT_ID_NOT_EXISTS = 120;
	long STUDENT_ID1 = 123;
	long STUDENT_ID2 = 124;
	
	String PHONE_NUMBER1 = "051-555-55-55";
	String PHONE_NUMBER2 = "052-555-55-55";
	String PHONE_NUMBER_UPDATED = "050-555-55-55";
	
	Student STUDENT1 = new Student(STUDENT_ID1, PHONE_NUMBER1);
	Student STUDENT2 = new Student(STUDENT_ID2, PHONE_NUMBER2);
	Student STUDENT_PHONE_UPDATED = new Student(STUDENT_ID1, PHONE_NUMBER_UPDATED);
	
	Mark MARK1 = new Mark(SUBJECT1, AVERAGE_SCORE, CUR_DATE);
	Mark MARK2 = new Mark(SUBJECT2, FAILED_SCORE, BEFORE_DAY_DATE);
	Mark MARK3 = new Mark(SUBJECT3, EXCELLENT_SCORE, BEFORE_WEEK_DATE);
	Mark MARK_NOT_EXISTS = new Mark(SUBJECT3, EXCELLENT_SCORE, DATE_NOT_EXISTS);
	
	
	List<Mark> EXISTING_MARKS = List.of(MARK1, MARK2, MARK3);
	
	@Autowired
	StudentRepo studentRepo;
	@Autowired
	StudentsService studentsService;
	
	
	@AfterEach
	void clearingDb() {
		studentRepo.deleteAll();
	}
	
	@BeforeEach
	void populationDb() {
		StudentDoc studentDoc = new StudentDoc(STUDENT1);
		studentDoc.getMarks().addAll(EXISTING_MARKS);
		studentRepo.save(studentDoc);
	}

	@Test
	void addStudent_correctFlow_success() {
		assertNull(studentRepo.findById(STUDENT_ID2).orElse(null));
		
		Student actualStudent = studentsService.addStudent(STUDENT2);
		assertEquals(STUDENT2, actualStudent);
		
		StudentDoc actualStudentDoc = studentRepo.findById(STUDENT_ID2).orElseThrow();
		assertEquals(STUDENT2, actualStudentDoc.build());
	}
	
	@Test
	void addStudent_studentAlreadyExists_throwsException() {
		long count = studentRepo.count();
		assertThrowsExactly(StudentIllegalStateException.class, () -> studentsService.addStudent(STUDENT1));
		
		assertEquals(count, studentRepo.count());
		StudentDoc actualStudentDoc = studentRepo.findAll().get(0);
		assertEquals(STUDENT1, actualStudentDoc.build());
	}
	

	@Test
	void updatePhoneNumber_correctFlow_success() {
		assertEquals(studentRepo.findById(STUDENT_ID1).orElse(null).build(), STUDENT1);
		
		Student updatedStudent = studentsService.updatePhoneNumber(STUDENT_ID1, PHONE_NUMBER_UPDATED);
		assertEquals(STUDENT_PHONE_UPDATED, updatedStudent);
		
		StudentDoc actualStudentDoc = studentRepo.findById(STUDENT_ID1).orElseThrow();
		assertEquals(STUDENT_PHONE_UPDATED, actualStudentDoc.build());
	}
	
	@Test
	void updatePhoneNumber_studentNotExists_throwsException() {
		long countStudents = studentRepo.count();
		
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.updatePhoneNumber(STUDENT_ID_NOT_EXISTS, PHONE_NUMBER1));
		assertEquals(countStudents, studentRepo.count());
		
		StudentDoc actualStudentDoc = studentRepo.findAll().get(0);
		assertEquals(STUDENT1, actualStudentDoc.build());
	}
	
	@Test
	void addMark_correctFlow_success() {
		StudentDoc studentDoc = studentRepo.findById(STUDENT_ID1).orElseThrow();
		assertEquals(EXISTING_MARKS, studentDoc.getMarks());
		
		Mark actualMark = studentsService.addMark(STUDENT_ID1, MARK_NOT_EXISTS);
		assertEquals(MARK_NOT_EXISTS, actualMark);
		
		studentDoc = studentRepo.findById(STUDENT_ID1).orElseThrow();
		List<Mark> expectedMarks = new ArrayList<Mark>(EXISTING_MARKS);
		expectedMarks.add(MARK_NOT_EXISTS);
		assertEquals(expectedMarks, studentDoc.getMarks());
	}
	
	@Test
	void addMark_studentNotExists_throwsException() {
		long countStudents = studentRepo.count();
		
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.addMark(STUDENT_ID_NOT_EXISTS, MARK1));
		assertEquals(countStudents, studentRepo.count());
		
		StudentDoc actualStudentDoc = studentRepo.findAll().get(0);
		assertEquals(STUDENT1, actualStudentDoc.build());
		assertEquals(EXISTING_MARKS, actualStudentDoc.getMarks());
	}
	
	@Test
	void addMark_markAlreadyExists_throwsException() {
		long countStudents = studentRepo.count();
		
		assertThrowsExactly(MarkIllegalStateException.class, () -> studentsService.addMark(STUDENT_ID1, EXISTING_MARKS.get(0)));
		assertEquals(countStudents, studentRepo.count());
		
		StudentDoc actualStudentDoc = studentRepo.findAll().get(0);
		assertEquals(STUDENT1, actualStudentDoc.build());
		assertEquals(EXISTING_MARKS, actualStudentDoc.getMarks());
	}

}
