package mobi.chouette.exchange;

import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.dao.GroupOfLineDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.model.Company;
import mobi.chouette.model.GroupOfLine;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.*;
import java.util.stream.Collectors;

@Stateless 
public class DaoReader {

	@EJB 
	protected LineDAO lineDAO;

	@EJB 
	protected NetworkDAO ptNetworkDAO;

	@EJB 
	protected CompanyDAO companyDAO;

	@EJB 
	protected GroupOfLineDAO groupOfLineDAO;


	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Set<Long> loadLines(String type, List<Long> ids) {
		Set<Line> lines = new HashSet<Line>();
		if (ids == null || ids.isEmpty()) {
			lines.addAll(lineDAO.findAll());
		} else {
			if (type.equals("line")) {
				lines.addAll(lineDAO.findAll(ids));
			} else if (type.equals("network")) {
				List<Network> list = ptNetworkDAO.findAll(ids);
				for (Network ptNetwork : list) {
					lines.addAll(ptNetwork.getLines());
				}
			} else if (type.equals("company")) {
				List<Company> list = companyDAO.findAll(ids);
				for (Company company : list) {
					lines.addAll(company.getLines());
				}
			} else if (type.equals("group_of_line")) {
				List<GroupOfLine> list = groupOfLineDAO.findAll(ids);
				for (GroupOfLine groupOfLine : list) {
					lines.addAll(groupOfLine.getLines());
				}
			}
		}
		// ordonnancement des lignes
		LineComparator lineComp = new LineComparator();

		return lines.stream()
					 .filter(line -> line.getSupprime().equals(false))
				     .sorted(lineComp)
				     .map(Line::getId)
				     .collect(Collectors.toCollection(LinkedHashSet::new));
	}


	public class LineComparator implements Comparator<Line> {

		@Override
		public int compare(Line o1, Line o2) {

			Integer posO1 = null;
			Integer posO2 = null;

			if (o1.getPosition() != null) {
				if (o1.getNetwork() != null && o1.getNetwork().getPosition() != null) {
					posO1 = o1.getPosition() + (o1.getNetwork().getPosition() * 1000);
				} else  {
					posO1 = o1.getPosition();
				}
			}

			if (o2.getPosition() != null) {
				if (o2.getNetwork() != null && o2.getNetwork().getPosition() != null) {
					posO2 = o2.getPosition() + (o2.getNetwork().getPosition() * 1000);
				} else {
					posO2 = o2.getPosition();
				}
			}

			if (posO1 != null && posO2 != null){
				return posO1.compareTo(posO2);
			}

			if (o1.getPublishedName() != null && o2.getPublishedName() != null){
				return o1.getPublishedName().compareTo(o2.getPublishedName());
			}
			return o1.getObjectId().compareTo(o2.getObjectId());
		}
	}


}
