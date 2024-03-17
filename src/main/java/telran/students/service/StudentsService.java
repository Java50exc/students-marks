package telran.students.service;

import java.time.LocalDate;
import java.util.List;

import telran.students.dto.*;

public interface StudentsService {
	Student addStudent(Student student);
	Mark addMark(long id, Mark mark);
	Student updatePhoneNumber(long id, String phoneNumber);
	Student removeStudent(long id);
	Student getStudent(long id);
	List<Mark> getMarks(long id);
	Student getStudentByPhoneNumber(String phoneNumber);
	List<Student> getStudentsByPhonePrefix(String prefix);
	List<Student> getStudentsMarksDate(LocalDate date);
	List<Student> getStudentsMarksMonthYear(int month, int year);
	List<Student> getStudentsGoodSubjectMark(String subject, int markThreshold);
	List<Student> getStudentsAllGoodMarks(int markThreshold);
	List<Student> getStudentsFewMarks(int nMarks);
	List<Mark> getStudentMarksSubject(long id, String subject);
	List<StudentAvgScore> getStudentsAvgScoreGreater(int avgThreshold); 
	
	List<Student> getStudentsAllGoodMarksSubject(String subject, int thresholdScore);
	List<Student> getStudentsMarksAmountBetween(int min, int max);
	List<Mark> getStudentMarksAtDates(long id, LocalDate from, LocalDate to);
	List<Long> getBestStudents(int nStudents);
	List<Long> getWorstStudents(int nStudents);
}
