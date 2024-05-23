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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
		Set<Line> lines = new HashSet<>();
		if (ids == null || ids.isEmpty()) {
			lines.addAll(lineDAO.findAll());
		} else {
            switch (type) {
                case "line":
                    lines.addAll(lineDAO.findAll(ids));
                    break;
                case "network": {
                    List<Network> list = ptNetworkDAO.findAll(ids);
                    for (Network ptNetwork : list) {
                        lines.addAll(ptNetwork.getLines());
                    }
                    break;
                }
                case "company": {
                    List<Company> list = companyDAO.findAll(ids);
                    for (Company company : list) {
                        lines.addAll(company.getLines());
                    }
                    break;
                }
                case "group_of_line": {
                    List<GroupOfLine> list = groupOfLineDAO.findAll(ids);
                    for (GroupOfLine groupOfLine : list) {
                        lines.addAll(groupOfLine.getLines());
                    }
                    break;
                }
            }
		}
		// ordonnancement des lignes
		return lines.stream()
					 .filter(line -> line.getSupprime().equals(false))
				     .sorted(new LineComparator())
				     .map(Line::getId)
				     .collect(Collectors.toCollection(LinkedHashSet::new));
	}

}
