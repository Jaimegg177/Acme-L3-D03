
package acme.features.auditor.audit;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import acme.entities.audit.Audit;
import acme.entities.auditingRecords.AuditingRecords;
import acme.entities.course.Course;
import acme.enums.Indication;
import acme.enums.Mark;
import acme.framework.components.accounts.Principal;
import acme.framework.components.jsp.SelectChoices;
import acme.framework.components.models.Tuple;
import acme.framework.services.AbstractService;
import acme.roles.Auditor;

@Service
public class AuditorAuditPublishService extends AbstractService<Auditor, Audit> {

	// Internal state ---------------------------------------------------------

	@Autowired
	protected AuditorAuditRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void check() {
		boolean status;

		status = super.getRequest().hasData("id", int.class);

		super.getResponse().setChecked(status);
	}

	@Override
	public void authorise() {
		final boolean status;
		int id;
		Audit audit;
		Principal principal;

		id = super.getRequest().getData("id", int.class);
		principal = super.getRequest().getPrincipal();
		audit = this.repository.findOneAuditById(id);
		status = audit != null && !audit.isPublished() && principal.hasRole(audit.getAuditor());

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		Audit object;
		int id;

		id = super.getRequest().getData("id", int.class);
		object = this.repository.findOneAuditById(id);

		super.getBuffer().setData(object);
	}

	@Override
	public void bind(final Audit object) {
		assert object != null;

		super.bind(object, "code", "conclusion", "strongPoints", "weakPoints", "mark", "course", "published");
	}

	@Override
	public void validate(final Audit object) {
		assert object != null;

		if (!super.getBuffer().getErrors().hasErrors("indicator")) {
			int id;
			id = super.getRequest().getData("id", int.class);

			final Collection<AuditingRecords> auditingRecords = this.repository.findAuditingRecordsByAuditId(id);
			final boolean onlyTheoretical = auditingRecords.stream().allMatch(x -> x.getAudit().getCourse().equals(Indication.THEORETICAL));
			super.state(!onlyTheoretical, "indicator", "Auditor.Audit.form.error.onlyTheoretical");
		}
	}

	@Override
	public void perform(final Audit object) {
		assert object != null;

		object.setPublished(true);
		this.repository.save(object);
	}

	@Override
	public void unbind(final Audit object) {
		assert object != null;

		SelectChoices marks;
		SelectChoices courses2;
		Tuple tuple;
		Collection<Course> courses;

		courses = this.repository.findAllPublishedCourses();
		final List<Course> auditsCourse = this.repository.findAllCoursesFromAudit();

		courses.removeAll(auditsCourse);
		courses.add(object.getCourse());

		courses2 = SelectChoices.from(courses, "title", object.getCourse());

		marks = SelectChoices.from(Mark.class, object.getMark());

		tuple = super.unbind(object, "code", "conclusion", "strongPoints", "weakPoints", "mark", "published", "course");
		tuple.put("marks", marks);
		tuple.put("courses", courses2);

		super.getResponse().setData(tuple);
	}

}
