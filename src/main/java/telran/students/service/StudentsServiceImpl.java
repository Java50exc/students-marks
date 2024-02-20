package telran.students.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
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
import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.dto.StudentAvgScore;
import telran.students.exceptions.StudentIllegalStateException;
import telran.students.exceptions.StudentNotFoundException;
import telran.students.model.StudentDoc;
import telran.students.repo.IdPhone;
import telran.students.repo.StudentRepo;
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentsServiceImpl implements StudentsService {
	private static final String MARKS_SCORE_FIELD = "marks.score";
	private static final String ID_FIELD = "id";
	private static final String SUM_SCORES_FIELD = "sumScore";
	private static final String MARKS_FIELD = "marks";
	private static final String COUNT_FIELD = "count";
	private static final String ID_DOCUMENT_FIELD = "_id";
	private static final int BEST_STUDENTS_MARK_THRESHOLD = 80;
	private static final String AVG_SCORE_FIELD = "avgScore";
	private static final String MARKS_SUBJECT_FIELD = "marks.subject";
	private static final String MARKS_DATE_FIELD = "marks.date";
	private static final String SCORE_FIELD = "score";
	private static final String DATE_FIELD = "date";
	final StudentRepo studentRepo;
	final MongoTemplate mongoTemplate;
	@Override
	@Transactional
	public Student addStudent(Student student) {
		long id = student.id();
		if(studentRepo.existsById(id)) {
			log.error("student with id {} already exists", id);
			throw new StudentIllegalStateException();
		}
		StudentDoc studentDoc = new StudentDoc(student);
		studentRepo.save(studentDoc);
		log.debug("student {} has been saved", student);
		return student;
	}

	@Override
	public Mark addMark(long id, Mark mark) {
		StudentDoc studentDoc = studentRepo.findById(id)
				.orElseThrow(() -> new StudentNotFoundException());
		List<Mark> marks = studentDoc.getMarks();
		log.debug("student with id {}, has marks {} before adding new one",
				id, marks);
		marks.add(mark);
		StudentDoc savedStudent = studentRepo.save(studentDoc);
		log.debug("new marks after saving are {}", savedStudent.getMarks());
		return mark;
	}

	@Override
	@Transactional
	public Student updatePhoneNumber(long id, String phoneNumber) {
		StudentDoc studentDoc = studentRepo.findById(id)
				.orElseThrow(() -> new StudentNotFoundException());
		log.debug("student with id {}, old phone number {}, new phone number {}",
				id,studentDoc.getPhone(), phoneNumber);
		studentDoc.setPhone(phoneNumber);
		Student res = studentRepo.save(studentDoc).build();
		log.debug("Student {} has been saved ", res);
		return res;
	}

	@Override
	public Student removeStudent(long id) {
		Student student = getStudent(id);
		studentRepo.deleteById(id);
		log.debug("student {} has been remved", student);
		return student;
	}

	@Override
	public Student getStudent(long id) {
		StudentDoc studentDoc = studentRepo.findStudentNoMarks(id);
		if(studentDoc == null) {
			throw new StudentNotFoundException();
		}
		log.debug("marks of found student {}", studentDoc.getMarks());	
		Student student = studentDoc.build();
		log.debug("found student {}", student);
		return student;
	}

	@Override
	public List<Mark> getMarks(long id) {
		StudentDoc studentDoc = studentRepo.findStudentOnlyMarks(id);
		if(studentDoc == null) {
			throw new StudentNotFoundException();
		}
		List<Mark> res = studentDoc.getMarks();
		log.debug("phone: {}, id: {}", studentDoc.getPhone(), studentDoc.getId());
		log.debug("marks of found student {}", res);	
		
		return res;
	}

	@Override
	public List<Student> getStudentsAllGoodMarks(int markThreshold) {
		List<IdPhone> idPhones = studentRepo.findAllGoodMarks(markThreshold);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students having marks greater than {} are {}", markThreshold, res);
		return res;
	}

	@Override
	public List<Student> getStudentsFewMarks(int nMarks) {
		List<IdPhone> idPhones = studentRepo.findFewMarks(nMarks);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("student having amount of marks less than {} are {}",nMarks, res );
		return res;
	}

	@Override
	public Student getStudentByPhoneNumber(String phoneNumber) {
		IdPhone idPhone = studentRepo.findByPhone(phoneNumber);
		
		Student res = null;
		if(idPhone != null) {
			res = new Student(idPhone.getId(), idPhone.getPhone());
		}
		log.debug("student {}", res);
		return res;
	}

	@Override
	public List<Student> getStudentsByPhonePrefix(String prefix) {
		List<IdPhone> idPhones = studentRepo.findByPhoneRegex(prefix + ".+");
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students {}", res);
		return res;
	}

	private List<Student> idPhonesToStudents(List<IdPhone> idPhones) {
		return idPhones.stream()
				.map(ip -> new Student(ip.getId(), ip.getPhone())).toList();
	}

	@Override
	public List<Student> getStudentsMarksDate(LocalDate date) {
		List<IdPhone> idPhones = studentRepo.findByMarksDate(date);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("Students having a mark on date {} are {}", date, res);
		return res;
	}

	@Override
	public List<Student> getStudentsMarksMonthYear(int month, int year) {
		LocalDate firstDate = LocalDate.of(year, month, 1);
		LocalDate lastDate = firstDate.with(TemporalAdjusters.lastDayOfMonth());
		List<IdPhone> idPhones = studentRepo.findByMarksDateBetween(firstDate, lastDate);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students having marks on month {} of year {} are {}", month, year, res);
		return res;
	}

	@Override
	public List<Student> getStudentsGoodSubjectMark(String subject, int markThreshold) {
		List<IdPhone> idPhones = studentRepo.findByMarksSubjectAndMarksScoreGreaterThan(subject, markThreshold);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students having marks on subject {} better than {} are {}", subject,
				markThreshold);
		return res;
	}

	@Override
	public List<Mark> getStudentMarksSubject(long id, String subject) {
		MatchOperation matchSubject =
				Aggregation.match(Criteria.where(MARKS_SUBJECT_FIELD).is(subject));
		List<Mark> res = getStudentMarks(id, matchSubject);
		log.debug("marks of subject {} of student {} are {}", subject, id, res);
		return res;
	}

	private List<Mark> getStudentMarks(long id, MatchOperation matchMarks) {
		if(!studentRepo.existsById(id)) {
			throw new StudentNotFoundException();
		}
		MatchOperation matchStudentOperation =
				Aggregation.match(Criteria.where(ID_FIELD).is(id));
		UnwindOperation unwindOperation = Aggregation.unwind(MARKS_FIELD);
		
		ProjectionOperation projectOperation = Aggregation.project(MARKS_SUBJECT_FIELD,
				MARKS_SCORE_FIELD, MARKS_DATE_FIELD);
		Aggregation pipeline = Aggregation.newAggregation(matchStudentOperation,
				unwindOperation, matchMarks,projectOperation);
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class,
				Document.class);
		List<Document> documents = aggregationResult.getMappedResults();
		log.debug("received {} documents", documents.size());
		List<Mark> res = documents.stream()
				.map(d -> new Mark(d.getString("subject"), d.getInteger(SCORE_FIELD),
						d.getDate(DATE_FIELD).toInstant()
					      .atZone(ZoneId.systemDefault())
					      .toLocalDate()))
				.toList();
		return res;
	}

	@Override
	public List<StudentAvgScore> getStudentsAvgScoreGreater(int avgThreshold) {
		UnwindOperation unwindOperation = Aggregation.unwind(MARKS_FIELD);
		GroupOperation groupOperation = Aggregation.group(ID_FIELD).avg(MARKS_SCORE_FIELD)
				.as(AVG_SCORE_FIELD);
		MatchOperation matchOperation = Aggregation.match(Criteria.where(AVG_SCORE_FIELD)
				.gt(avgThreshold));
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, AVG_SCORE_FIELD);
		Aggregation pipeline = Aggregation.newAggregation(unwindOperation, groupOperation,
				matchOperation, sortOperation);
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> documents = aggregationResult.getMappedResults();
		List<StudentAvgScore> res =
				documents.stream()
				.map(d -> new StudentAvgScore(d.getLong(ID_DOCUMENT_FIELD), d.getDouble(AVG_SCORE_FIELD).intValue()))
				.toList();
		log.debug("students with avg scores greater than {} are {}", avgThreshold, res);
		return res;
	}

	@Override
	public List<Student> getStudentsAllGoodMarksSubject(String subject, int thresholdScore) {
		// the same as the method getStudentsAllGoodMarks but for a given subject
		// consider additional condition for "subject" in the query object
		List<IdPhone> idPhones = studentRepo.findAllGoodSubjectMarks(thresholdScore, subject);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students having all marks of the subject {} greater than {} are {}", subject, thresholdScore, res);
		return res;
	}

	@Override
	public List<Student> getStudentsMarksAmountBetween(int min, int max) {
		//get students having amount of marks in the closed range [min, max]
		// consider using operator $and inside $expr object like $expr:{$and:[{....},{...}]
		//{....} - contains the object similar to the query of repository method List<IdPhone> findFewMarks(int nMarks);
		List<IdPhone> idPhones = studentRepo.findBetweenMarksAmount(min, max);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students having amount of marks greater than {} but less than {} are {}",min, max, res );
		return res;
	}

	@Override
	public List<Mark> getStudentMarksAtDates(long id, LocalDate from, LocalDate to) {
		// gets only marks on the dates in a closed range [from, to]
		// of a given student (the same as getStudentsMarksSubject just different match operation
		// think of DRY (Don't Repeat Yourself)
		MatchOperation matchDates = Aggregation.match(Criteria.where("marks.date").gte(from)
	.lte(to));
		List<Mark> res = getStudentMarks(id, matchDates);
		log.debug("marks of the student with id {} on dates [{}-{}] are {}", id, from, to, res);
		return res;
	}

	@Override
	public List<Long> getBestStudents(int nStudents) {
		//gets list of a given number of the best student id's
		//Best students are the ones who have most scores greater than 80
		//consider aggregation method count() instead of avg() that we have used at CW #72
		// and LimitOperation as additional AggregationOperation
		UnwindOperation unwindOperation = Aggregation.unwind(MARKS_FIELD);
		MatchOperation matchOperation = Aggregation.match(Criteria.where(MARKS_SCORE_FIELD)
				.gt(BEST_STUDENTS_MARK_THRESHOLD));
		GroupOperation groupOperation = Aggregation.group(ID_FIELD).count()
				.as(COUNT_FIELD);
		
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, COUNT_FIELD);
		LimitOperation limitOperation = Aggregation.limit(nStudents);
		ProjectionOperation projectionOperation = Aggregation.project(ID_FIELD);
		Aggregation pipeline = Aggregation.newAggregation(unwindOperation,
				matchOperation, groupOperation, sortOperation, limitOperation, projectionOperation);
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> documents = aggregationResult.getMappedResults();
		List<Long> res =
				documents.stream()
				.map(d -> d.getLong(ID_DOCUMENT_FIELD))
				.toList();
		log.debug("students with most scoresgreater than {} are {}", BEST_STUDENTS_MARK_THRESHOLD, res);
		return res;
	}

	@Override
	public List<Long> getWorstStudents(int nStudents) {
		// gets list of a given number of the worst student id's
		//Worst students are the ones who have least sum's of all scores
		//Students who have no scores at all should be considered as the worst ones
		//instead of GroupOperation to apply AggregationExpression
		// (with AccumulatorOperators.Sum) and
		// ProjectionOperation for adding new fields with computed values from AggregationExpression
		AggregationExpression expression = AccumulatorOperators.Sum.sumOf(MARKS_SCORE_FIELD);
		ProjectionOperation projectionOperation = Aggregation.project(ID_FIELD).and(expression)
				.as(SUM_SCORES_FIELD);
		SortOperation sortOperation = Aggregation.sort(Direction.ASC, SUM_SCORES_FIELD);
		LimitOperation limitOperation = Aggregation.limit(nStudents);
		ProjectionOperation projectionOperationOnlyId = Aggregation.project(ID_FIELD);
		Aggregation pipeLine = Aggregation.newAggregation
				( projectionOperation,sortOperation, limitOperation, projectionOperationOnlyId);
		List<Long> res = mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class)
				.getMappedResults().stream().map(d -> d.getLong(ID_FIELD)).toList();
		log.debug("{} worst students are {}", nStudents, res);
		return res;
	}
	

}
