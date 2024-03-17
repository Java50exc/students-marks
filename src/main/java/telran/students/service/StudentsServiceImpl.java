package telran.students.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import telran.students.dto.*;
import telran.students.exceptions.*;
import telran.students.model.StudentDoc;
import telran.students.repo.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentsServiceImpl implements StudentsService {
	final StudentRepo studentRepo;
	final MongoTemplate mongoTemplate;

	@Override
	@Transactional
	public Student addStudent(Student student) {
		long id = student.id();
		if (studentRepo.existsById(id)) {
			log.error("student with id {} already exists", id);
			throw new StudentIllegalStateException();
		}
		StudentDoc studentDoc = new StudentDoc(student);
		studentRepo.save(studentDoc);
		log.debug("student {} has been saved", student);
		return student;
	}

	@Override
	@Transactional
	public Mark addMark(long id, Mark mark) {
		StudentDoc studentDoc = studentRepo.findById(id).orElseThrow(() -> {
			log.error("student with id {} not found", id);
			return new StudentNotFoundException();
		});
		log.debug("student with id {} has been found", studentDoc.getId());
		List<Mark> marks = studentDoc.getMarks();

		if (marks.contains(mark)) {
			log.error("mark {} already exists in the list of student with id {}", mark, studentDoc.getId());
			throw new MarkIllegalStateException();
		}
		marks.add(mark);
		studentDoc = studentRepo.save(studentDoc);
		log.debug("mark {} succesfully added and the the student with id {} has been saved, list of marks {}", mark,
				studentDoc.getId(), studentDoc.getMarks());
		return mark;
	}

	@Override
	@Transactional
	public Student updatePhoneNumber(long id, String phoneNumber) {
		StudentDoc studentDoc = studentRepo.findById(id).orElseThrow(() -> new StudentNotFoundException());
		log.debug("student with id {}, old phone number {}, new phone number {}", id, studentDoc.getPhone(),
				phoneNumber);
		studentDoc.setPhone(phoneNumber);
		Student res = studentRepo.save(studentDoc).build();
		log.debug("Student {} has been saved ", res);
		return res;
	}

	@Override
	@Transactional
	public Student getStudent(long id) {
		StudentDoc studentDoc = studentRepo.findStudentNoMarks(id);
		if (studentDoc == null) {
			throw new StudentNotFoundException();
		}
		log.debug("marks of found student {}", studentDoc.getMarks());
		Student student = studentDoc.build();
		log.debug("found student {}", student);
		return student;
	}

	@Override
	@Transactional
	public List<Mark> getMarks(long id) {
		StudentDoc studentDoc = studentRepo.findStudentOnlyMarks(id);
		if (studentDoc == null) {
			throw new StudentNotFoundException();
		}
		List<Mark> res = studentDoc.getMarks();
		log.debug("phone: {}, id: {}", studentDoc.getPhone(), studentDoc.getId());
		log.debug("marks of found student {}", studentDoc.getMarks());

		return res;
	}

	@Override
	@Transactional
	public Student getStudentByPhoneNumber(String phoneNumber) {
		IdPhone idPhone = studentRepo.findByPhone(phoneNumber);
		Student res = null;

		if (idPhone != null) {
			res = new Student(idPhone.getId(), idPhone.getPhone());
		}
		log.debug("student {}", res);

		return res;
	}

	@Override
	@Transactional
	public List<Student> getStudentsByPhonePrefix(String prefix) {
		List<IdPhone> idPhones = studentRepo.findByPhoneRegex(prefix + ".+");
		List<Student> res = idPhones.stream().map(ip -> new Student(ip.getId(), ip.getPhone())).toList();
		log.debug("students {}", res);
		return res;
	}

	@Override
	@Transactional
	public List<Student> getStudentsMarksDate(LocalDate date) {
		List<IdPhone> idPhones = studentRepo.findByMarksDate(date);
		List<Student> res = idPhones.stream().map(ip -> new Student(ip.getId(), ip.getPhone())).toList();
		log.debug("students {}", res);
		return res;
	}

	@Override
	@Transactional
	public List<Student> getStudentsMarksMonthYear(int month, int year) {
		LocalDate from = LocalDate.of(year, month, 1);
		LocalDate to = from.plusMonths(1).minusDays(1);
		List<IdPhone> idPhones = studentRepo.findByMarksDateBetween(from, to);
		List<Student> res = idPhones.stream().map(ip -> new Student(ip.getId(), ip.getPhone())).toList();
		log.debug("students {}", res);
		return res;
	}

	@Override
	@Transactional
	public List<Student> getStudentsGoodSubjectMark(String subject, int markThreshold) {
		List<StudentDoc> studentDocs = studentRepo.findBySubjectAndScoreGreaterThan(subject, markThreshold);
		List<Student> res = studentDocs.stream().map(doc -> doc.build()).toList();
		log.debug("students {}", res);
		return res;
	}

	@Override
	public Student removeStudent(long id) {
		// TODO CW72
		return null;
	}

	@Override
	public List<Student> getStudentsAllGoodMarks(int markThreshold) {
		List<IdPhone> idPhones = studentRepo.findAllGoodMarks(markThreshold);
		List<Student> res = idPhones.stream().map(ip -> new Student(ip.getId(), ip.getPhone())).toList();
		log.debug("students having marks greater than {} are {}", markThreshold, res);
		return res;
	}

	@Override
	public List<Student> getStudentsFewMarks(int nMarks) {
		List<IdPhone> idPhones = studentRepo.findFewMarks(nMarks);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students having amount of marks less than {} are {}", nMarks, res);
		return res;
	}

	private List<Student> idPhonesToStudents(List<IdPhone> idPhones) {
		return idPhones.stream().map(ip -> new Student(ip.getId(), ip.getPhone())).toList();
	}

	@Override
	public List<Mark> getStudentMarksSubject(long id, String subject) {
		if (!studentRepo.existsById(id)) {
			throw new StudentNotFoundException();
		}
		MatchOperation matchStudentOperation = Aggregation.match(Criteria.where("id").is(id));
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		MatchOperation matchSubject = Aggregation.match(Criteria.where("marks.subject").is(subject));
		ProjectionOperation projectOperation = Aggregation.project("marks.subject", "marks.score", "marks.date");
		Aggregation pipeline = Aggregation.newAggregation(matchStudentOperation, unwindOperation, matchSubject,
				projectOperation);

		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> documents = aggregationResult.getMappedResults();
		log.debug("received {} documents", documents.size());
		List<Mark> res = documents.stream().map(d -> new Mark(d.getString("subject"), d.getInteger("score"),
				d.getDate("date").toInstant().atZone(ZoneId.systemDefault()).toLocalDate())).toList();
		log.debug("marks of subject {} of student {} are {}", subject, id, res);
		return res;
	}

	@Override
	public List<StudentAvgScore> getStudentsAvgScoreGreater(int avgThreshold) {
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		GroupOperation groupOperation = Aggregation.group("id").avg("marks.score").as("avgScore");
		MatchOperation matchOperation = Aggregation.match(Criteria.where("avgScore").gt(avgThreshold));
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "avgScore");
		Aggregation pipeline = Aggregation.newAggregation(unwindOperation, groupOperation, matchOperation,
				sortOperation);
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> documents = aggregationResult.getMappedResults();
		List<StudentAvgScore> res = documents.stream()
				.map(d -> new StudentAvgScore(d.getLong("_id"), d.getDouble("avgScore").intValue())).toList();
		log.debug("students with avg scores greater than {} are {}", avgThreshold, res);
		return res;
	}
	
	@Override
	public List<Student> getStudentsAllGoodMarksSubject(String subject, int thresholdScore) {
		// TODO the same as the method getStudentsAllGoodMarks but for a given subject
		// consider additional condition for "subject" in the query object
		return null;
	}

	@Override
	public List<Student> getStudentsMarksAmountBetween(int min, int max) {
		// TODO get students having amount of marks in the closed range [min, max]
		// consider using operator $and inside $expr object like $expr:{$and:[{....},{...}]
		//{....} - contains the object similar to the query of repository method List<IdPhone> findFewMarks(int nMarks);
		return null;
	}

	@Override
	public List<Mark> getStudentMarksAtDates(long id, LocalDate from, LocalDate to) {
		// TODO gets only marks on the dates in a closed range [from, to]
		// of a given student (the same as getStudentsMarksSubject just different match operation
		// think of DRY (Don't Repeat Yourself)
		return null;
	}

	@Override
	public List<Long> getBestStudents(int nStudents) {
		//gets list of a given number of the best student id's
		//Best students are the ones who have most scores greater than 80
		//consider aggregation method count() instead of avg() that we have used at CW #72
		// and LimitOperation as additional AggregationOperation
		return null;
	}

	@Override
	public List<Long> getWorstStudents(int nStudents) {
		// TODO gets list of a given number of the worst student id's
		//Worst students are the ones who have least sum's of all scores
		//Students who have no scores at all should be considered as the worst ones
		//instead of GroupOperation to apply AggregationExpression
		// (with AccumulatorOperators.Sum) and
		// ProjectionOperation for adding new fields with computed values from AggregationExpression
		return null;
	}
	


}
