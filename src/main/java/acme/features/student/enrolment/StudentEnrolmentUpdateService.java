
package acme.features.student.enrolment;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import acme.entities.course.Course;
import acme.entities.enrolment.Enrolment;
import acme.framework.components.jsp.SelectChoices;
import acme.framework.components.models.Tuple;
import acme.framework.services.AbstractService;
import acme.roles.Student;

@Service
public class StudentEnrolmentUpdateService extends AbstractService<Student, Enrolment> {

	// Internal state ---------------------------------------------------------

	@Autowired
	protected StudentEnrolmentRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void check() {
		boolean status;

		status = super.getRequest().hasData("id", int.class);

		super.getResponse().setChecked(status);
	}

	@Override
	public void authorise() {
		boolean status;
		boolean finalised;
		int masterId;
		Enrolment enrolment;
		Student student;

		masterId = super.getRequest().getData("id", int.class);
		enrolment = this.repository.findOneEnrolmentById(masterId);

		if (enrolment != null) {
			student = enrolment.getStudent();
			finalised = enrolment.getCardHolder() != null && enrolment.getCardNibble() != null ? true : false;
			status = super.getRequest().getPrincipal().hasRole(student);
			super.getResponse().setAuthorised(status && !finalised);
		} else
			super.getResponse().setAuthorised(false);

	}

	@Override
	public void load() {
		Enrolment object;
		int id;

		id = super.getRequest().getData("id", int.class);
		object = this.repository.findOneEnrolmentById(id);

		super.getBuffer().setData(object);
	}

	@Override
	public void bind(final Enrolment object) {
		assert object != null;

		int courseId;
		Course course;

		courseId = super.getRequest().getData("course", int.class);
		course = this.repository.findOneCourseById(courseId);

		super.bind(object, "code", "motivation", "goals", "workTime", "cardHolder", "cardNibble");
		object.setCourse(course);
	}

	@Override
	public void validate(final Enrolment object) {
		assert object != null;
	}

	@Override
	public void perform(final Enrolment object) {
		assert object != null;

		this.repository.save(object);
	}

	@Override
	public void unbind(final Enrolment object) {
		assert object != null;

		boolean finalised;
		Collection<Course> courses;
		SelectChoices choices;
		Tuple tuple;

		courses = this.repository.findAllCourses();
		choices = SelectChoices.from(courses, "title", object.getCourse());

		finalised = object.getCardHolder() != null && object.getCardNibble() != null ? true : false;

		tuple = super.unbind(object, "code", "motivation", "goals", "workTime", "cardHolder", "cardNibble");
		tuple.put("finalised", finalised);
		tuple.put("course", choices.getSelected().getKey());
		tuple.put("courses", choices);
		super.getResponse().setData(tuple);

	}
}