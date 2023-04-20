
package acme.features.company.practicumSession;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import acme.entities.practicum.Practicum;
import acme.entities.practicumSession.PracticumSession;
import acme.framework.components.models.Tuple;
import acme.framework.services.AbstractService;
import acme.roles.Company;

@Service
public class CompanyPracticumSessionListService extends AbstractService<Company, PracticumSession> {

	@Autowired
	protected CompanyPracticumSessionRepository repository;


	@Override
	public void check() {
		super.getResponse().setChecked(true);
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
		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		Collection<PracticumSession> objects;
		int id;

		id = super.getRequest().getData("id", int.class);
		objects = this.repository.findAllSessionByPracticumId(id);

		super.getBuffer().setData(objects);
	}

	@Override
	public void unbind(final PracticumSession object) {
		assert object != null;
		int masterId;
		masterId = super.getRequest().getData("masterId", int.class);

		Tuple tuple;

		tuple = super.unbind(object, "title", "periodStart", "periodEnd", "published");
		super.getResponse().setGlobal("masterId", masterId);
		super.getResponse().setData(tuple);
	}
}