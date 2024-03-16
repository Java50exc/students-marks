package telran.students;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.exceptions.StudentNotFoundException;
import telran.students.repo.StudentRepo;
import telran.students.service.StudentsService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static telran.students.TestDb.*;

import java.util.List;

@SpringBootTest
class StudentsMarksServiceTests {
	
	@Autowired
	StudentRepo studentRepo;
	@Autowired
	StudentsService studentsService;
	@Autowired
	TestDb testDb;
	
	
	@BeforeEach
	void setUp() {
		testDb.createDb();
	}

	@Test
	void addStudent_correctFlow_success() {

	}
	
	@Test
	void addStudent_studentAlreadyExists_throwsException() {

	}
	
	@Test
	void updatePhoneNumber_correctFlow_success() {

	}
	
	@Test
	void updatePhoneNumber_studentNotExists_throwsException() {

	}
	
	@Test
	void addMark_correctFlow_success() {

	}
	
	@Test
	void addMark_studentNotExists_throwsException() {

	}
	
	@Test
	void addMark_markAlreadyExists_throwsException() {

	}
	
	@Test
	void getStudent_correctFlow_success() {
		//FIXME 
		assertEquals(students[0], studentsService.getStudent(ID1));
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.getStudent(10000));
	}
	
	@Test
	void getMarks_correctFlow_success() {
		//FIXME 
		assertArrayEquals(marks[0], studentsService.getMarks(ID1).toArray(Mark[]::new));
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.getMarks(10000));

	}
	
	@Test
	void getStudentByPhoneNumber_correctFlow_success() {
		//FIXME
		assertEquals(students[0], studentsService.getStudentByPhoneNumber(PHONE1));
	}
	
	@Test
	void getStudentsByPhonePrefix_correctFlow_success() {
		//FIXME
		List<Student> expected = List.of(students[0], students[6]);
		assertIterableEquals(expected, studentsService.getStudentsByPhonePrefix("051"));
	}

}
