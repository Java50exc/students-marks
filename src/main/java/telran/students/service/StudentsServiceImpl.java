package telran.students.service;

import java.time.*;
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
	public List<Student> getStudentsAllGoodMarksSubject(String subject, int thresholdScore) {
		List<IdPhone> idPhones = studentRepo.findAllGoodMarksBySubject(subject, thresholdScore);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students with all marks scores greater than {} by subject {} are {}", thresholdScore, subject, res);
		return res;
	}

	@Override
	public List<Student> getStudentsMarksAmountBetween(int min, int max) {
		List<IdPhone> idPhones = studentRepo.findCountMarksInRange(min, max);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students with count of marks between {} and {} are {}", min, max, res);
		return res;
	}
	
	private List<Document> getDocumentsByCriteria(AggregationOperation... operations) {
		Aggregation pipeline = Aggregation.newAggregation(operations);
		
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> documents = aggregationResult.getMappedResults();
		log.debug("received {} documents", documents.size());
		return documents;	
	}
	
	private List<Mark> getMarksByCriteria(AggregationOperation... operations) {
		List<Document> docs = getDocumentsByCriteria(operations);
		return docs.stream().map(d -> new Mark(d.getString("subject"), d.getInteger("score"),
				d.getDate("date").toInstant().atZone(ZoneId.systemDefault()).toLocalDate())).toList();
	}
	
	@Override
	public List<Mark> getStudentMarksSubject(long id, String subject) {
		if (!studentRepo.existsById(id)) {
			throw new StudentNotFoundException();
		}
		
		List<Mark> marks = getMarksByCriteria(
				Aggregation.match(Criteria.where("id").is(id)),
				Aggregation.unwind("marks"),
				Aggregation.match(Criteria.where("marks.subject").is(subject)),
				Aggregation.project("marks.subject", "marks.score", "marks.date"));
		
		log.debug("marks of subject {} of student {} are {}", subject, id, marks);
		return marks;
	}
	
	@Override
	public List<Mark> getStudentMarksAtDates(long id, LocalDate from, LocalDate to) {
		if (!studentRepo.existsById(id)) {
			throw new StudentNotFoundException();
		}
		List<Mark> marks = getMarksByCriteria( 
				Aggregation.match(Criteria.where("id").is(id)),
				Aggregation.unwind("marks"),
				Aggregation.match(Criteria.where("marks.date").gte(from).lte(to)),
				Aggregation.project("marks.subject", "marks.score", "marks.date"));

		log.debug("marks between dates {} and {} of student {} are {}", from, to, id, marks);
		return marks;
	}
	
	@Override
	public List<StudentAvgScore> getStudentsAvgScoreGreater(int avgThreshold) {
		List<Document> docs = getDocumentsByCriteria(
				Aggregation.unwind("marks"),
				Aggregation.group("id").avg("marks.score").as("avgScore"),
				Aggregation.match(Criteria.where("avgScore").gt(avgThreshold)),
				Aggregation.sort(Direction.DESC, "avgScore"));
		
		List<StudentAvgScore> res = docs.stream()
				.map(d -> new StudentAvgScore(d.getLong("_id"), d.getDouble("avgScore").intValue())).toList();
		log.debug("students with avg scores greater than {} are {}", avgThreshold, res);
		return res;
	}

	@Override
	public List<Long> getBestStudents(int nStudents) {
		List<Document> docs = getDocumentsByCriteria(
				Aggregation.unwind("marks"),
				Aggregation.match(Criteria.where("marks.score").gt(80)),
				Aggregation.group("id").count().as("count"),
				Aggregation.sort(Direction.DESC, "count"),
				Aggregation.limit(nStudents));
		
		List<Long> res = docs.stream().map(d -> d.getLong("_id")).toList();
		log.debug("best {} students are {}", nStudents, res);
		return res;
	}

	@Override
	public List<Long> getWorstStudents(int nStudents) {
		List<Document> docs = getDocumentsByCriteria(
				Aggregation.project("id").and(AccumulatorOperators.Sum.sumOf("marks.score")).as("sumScores"),
				Aggregation.sort(Direction.ASC, "sumScores"),
				Aggregation.limit(nStudents),
				Aggregation.project("id"));

		List<Long> res = docs.stream().map(d -> d.getLong("_id")).toList();
		log.debug("worst {} students are {}", nStudents, res);
		return res;
	}
	


}
