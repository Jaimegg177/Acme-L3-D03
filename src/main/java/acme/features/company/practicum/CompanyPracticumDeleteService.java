
package acme.features.company.practicum;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import acme.entities.course.Course;
import acme.entities.practicum.Practicum;
import acme.entities.practicumSession.PracticumSession;
import acme.enums.Indication;
import acme.framework.components.jsp.SelectChoices;
import acme.framework.components.models.Tuple;
import acme.framework.services.AbstractService;
import acme.roles.Company;

@Service
public class CompanyPracticumDeleteService extends AbstractService<Company, Practicum> {

	@Autowired
	protected CompanyPracticumRepository repository;


	@Override
	public void check() {
		boolean status;

		status = super.getRequest().hasData("id", int.class);

		super.getResponse().setChecked(status);
	}

	@Override
	public void authorise() {
		Practicum object;
		int id;
		boolean status;
		Company company;

		company = this.repository.findCompanyByUserId(super.getRequest().getPrincipal().getAccountId());
		id = super.getRequest().getData("id", int.class);
		object = this.repository.findPracticumById(id);
		status = object != null && super.getRequest().getPrincipal().hasRole(Company.class) && object.getCompany().getId() == company.getId();
		super.getResponse().setAuthorised(status && !object.isPublished());
	}

	@Override
	public void load() {
		Practicum object;
		int id;
		final double totalTime;
		final Collection<PracticumSession> sessions;

		id = super.getRequest().getData("id", int.class);
		object = this.repository.findPracticumById(id);

		sessions = this.repository.findAllSessionByPracticumId(id);
		totalTime = sessions.stream().mapToDouble(x -> x.getPeriodEnd().getTime() - x.getPeriodStart().getTime()).sum();

		object.setEstimatedTotalTime(totalTime / (1000 * 60 * 60));

		super.getBuffer().setData(object);
	}

	@Override
	public void bind(final Practicum object) {
		assert object != null;

		super.bind(object, "code", "title", "practicumAbstract", "goals", "estimatedTotalTime", "published");
	}

	@Override
	public void validate(final Practicum object) {
		assert object != null;
	}

	@Override
	public void perform(final Practicum object) {
		assert object != null;

		final Collection<PracticumSession> practicumSession;

		practicumSession = this.repository.findAllSessionByPracticumId(object.getId());
		this.repository.deleteAll(practicumSession);
		this.repository.delete(object);
	}

	@Override
	public void unbind(final Practicum object) {
		assert object != null;

		SelectChoices choice;
		Collection<Course> courses;
		Tuple tuple;

		courses = this.repository.findAllCourses().stream().filter(x -> !x.getIndicator().equals(Indication.THEORETICAL)).collect(Collectors.toList());
		choice = SelectChoices.from(courses, "title", object.getCourse());

		tuple = super.unbind(object, "code", "title", "practicumAbstract", "goals", "estimatedTotalTime", "published", "company", "course");
		tuple.put("courses", choice);
		tuple.put("company", object.getCompany().getName());

		super.getResponse().setData(tuple);
	}
}
