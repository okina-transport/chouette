package mobi.chouette.service;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import lombok.extern.slf4j.Slf4j;;
import mobi.chouette.dao.DbStatusChecker;

@Singleton(name = HealthService.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Slf4j
public class HealthService {

	public static final String BEAN_NAME = "HealthService";

	@EJB
	private DbStatusChecker dbStatusChecker;

	public boolean isReady() {
		return dbStatusChecker.isDbUp();
	}

}
