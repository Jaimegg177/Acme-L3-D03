
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
			finalised = enrolment.isFinalised();
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

		if (object.getCardHolder().trim() == "")
			object.setCardHolder(null);

		if (object.getCardNibble().trim() == "")
			object.setCardNibble(null);

		object.setFinalised(object.getCardHolder() != null && object.getCardNibble() != null);
	}

	@Override
	public void validate(final Enrolment object) {
		assert object != null;

		if (!super.getBuffer().getErrors().hasErrors("code")) {
			boolean valid;
			Optional<Enrolment> existing;

			existing = this.repository.findOneEnrolmentByCode(object.getCode());
			if (!existing.isPresent())
				valid = true;
			else if (existing.get().getId() == object.getId())
				valid = true;
			else
				valid = false;
			super.state(valid, "code", "student.enrolment.form.error.code");
		}

		if (!super.getBuffer().getErrors().hasErrors("cardHolder"))
			super.state(object.getCardHolder() == null || object.getCardHolder().trim().length() > 0, "cardHolder", "student.enrolment.form.error.cardHolder");

		if (!super.getBuffer().getErrors().hasErrors("cardNibble"))
			super.state(object.getCardNibble() == null || object.getCardNibble().matches("^[0-9]{4}$"), "cardNibble", "student.enrolment.form.error.cardNibble");
	}

	@Override
	public void perform(final Enrolment object) {
		assert object != null;

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
