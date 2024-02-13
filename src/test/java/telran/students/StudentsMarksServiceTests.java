package telran.students;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.exceptions.StudentIllegalStateException;
import telran.students.exceptions.StudentNotFoundException;
import telran.students.repo.StudentRepo;
import telran.students.service.StudentsService;

@SpringBootTest
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class StudentsMarksServiceTests {
	@Autowired
	StudentsService studentsService;
	@Autowired
	StudentRepo studentRepo;
private static final long ID1 = 123;
private static final String PHONE1 = "052-2222222";
private static final String PHONE2 = "053-2222222";
private static final String SUBJECT1 = "subject1";
Student student = new Student(ID1, PHONE1);
Student studentUpdated = new Student(ID1, PHONE2);
Mark mark = new Mark(SUBJECT1, 80, LocalDate.now());
	@Test
	@Order(1)
	void addStudentTest() {
		assertEquals(student, studentsService.addStudent(student));
		assertEquals(student, studentRepo.findById(ID1).orElseThrow().build());
		assertThrowsExactly(StudentIllegalStateException.class, ()->studentsService.addStudent(student));
	}
	@Test
	@Order(2)
	void updatePhoneNumberTest() {
		assertEquals(studentUpdated, studentsService.updatePhoneNumber(ID1, PHONE2));
		assertEquals(studentUpdated, studentRepo.findById(ID1).orElseThrow().build());
		assertThrowsExactly(StudentNotFoundException.class,
				()->studentsService.updatePhoneNumber(ID1 + 1000, PHONE2));
	}
	@Test
	@Order(3)
	void addMarkTest() {
		assertFalse(studentRepo.findById(ID1).orElseThrow().getMarks().contains(mark));
		assertEquals(mark, studentsService.addMark(ID1, mark));
		assertTrue(studentRepo.findById(ID1).orElseThrow().getMarks().contains(mark));
		assertThrowsExactly(StudentNotFoundException.class,
				()->studentsService.addMark(ID1 + 1000, mark));
		
	}

}
