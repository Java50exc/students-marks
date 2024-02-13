package telran.students.service;

import java.util.List;

import telran.students.dto.*;

public interface StudentsService {
	Student addStudent(Student student);
	Mark addMark(long id, Mark mark);
	Student updatePhoneNumber(long id, String phoneNumber);
	Student removeStudent(long id);
	Student getStudent(long id);
	List<Mark> getMarks(long id);
	/**
	 * 
	 * @param markThreshold
	 * @return list of students having all marks greater than markThreshold
	 */
	List<Student> getStudentsAllGoodMarks(int markThreshold);
	/****************************/
	/**
	 * 
	 * @param nMarks
	 * @return list of students having amount of marks less than nMarks
	 */
	List<Student> getStudentsFewMarks(int nMarks);
	/***************************/
	Student getStudentByPhoneNumber(String phoneNumber);
	List<Student> getStudentsByPhonePrefix(String prefix);
}
