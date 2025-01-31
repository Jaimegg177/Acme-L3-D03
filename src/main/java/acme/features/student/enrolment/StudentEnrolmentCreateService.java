
package acme.features.student.enrolment;

import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import acme.entities.course.Course;
import acme.entities.enrolment.Enrolment;
import acme.framework.components.jsp.SelectChoices;
import acme.framework.components.models.Tuple;
import acme.framework.services.AbstractService;
import acme.roles.Student;

@Service
public class StudentEnrolmentCreateService extends AbstractService<Student, Enrolment> {

	// Internal state ---------------------------------------------------------

	@Autowired
	protected StudentEnrolmentRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void check() {
		super.getResponse().setChecked(true);
	}

	@Override
	public void authorise() {
		super.getResponse().setAuthorised(true);
	}

	@Override
	public void load() {
		Enrolment object;

		int studentId;
		final Student student;

		object = new Enrolment();

		studentId = super.getRequest().getPrincipal().getActiveRoleId();
		student = this.repository.findOneStudentById(studentId);

		object.setStudent(student);
		object.setFinalised(false);

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

		if (object.getCardHolder().trim() == "")
			object.setCardHolder(null);

		if (object.getCardNibble().trim() == "")
			object.setCardNibble(null);

		object.setFinalised(object.getCardNibble() != null && object.getCardHolder() != null);
	}

	@Override
	public void validate(final Enrolment object) {
		assert object != null;

		if (!super.getBuffer().getErrors().hasErrors("code")) {
			Optional<Enrolment> existing;

			existing = this.repository.findOneEnrolmentByCode(object.getCode());
			if (existing.isPresent())
				super.state(existing == null, "code", "student.enrolment.form.error.code");

		}

		if (!super.getBuffer().getErrors().hasErrors("cardHolder"))
			super.state(object.getCardHolder() == null || object.getCardHolder().trim().length() > 0, "cardHolder", "student.enrolment.form.error.cardHolder");

		if (!super.getBuffer().getErrors().hasErrors("cardNibble"))
			super.state(object.getCardNibble() == null || object.getCardNibble().matches("^[0-9]{4}$"), "cardNibble", "student.enrolment.form.error.cardNibble");

	}

	@Override
	public void perform(final Enrolment object) {
		assert object != null;

		object.setFinalised(object.getCardHolder() != null && object.getCardNibble() != null);

		this.repository.save(object);
	}

	@Override
	public void unbind(final Enrolment object) {
		assert object != null;

		Collection<Course> courses;
		SelectChoices choices;
		Tuple tuple;

		courses = this.repository.findAllPublishedCourses();
		choices = SelectChoices.from(courses, "title", object.getCourse());

		tuple = super.unbind(object, "code", "motivation", "goals", "workTime", "cardHolder", "cardNibble");
		tuple.put("finalised", false);
		tuple.put("course", choices.getSelected().getKey());
		tuple.put("courses", choices);

		super.getResponse().setData(tuple);
	}

}
