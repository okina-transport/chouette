package mobi.chouette.ws;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.parameters.AbstractParameter;
import mobi.chouette.model.iev.Job;
import mobi.chouette.model.iev.Link;
import mobi.chouette.service.JobService;
import mobi.chouette.service.ServiceConstants;
import mobi.chouette.service.ServiceException;

@Data
@NoArgsConstructor
public class JobInfo implements ServiceConstants {

	private Long id;

	private String referential;

	private String action;

	private String type;

	private Date created;

	private Date started;

	private Date updated;

	private STATUS status;

	@JsonProperty("links")
	private List<LinkInfo> linkInfos;

	@JsonProperty("action_parameters")
	private AbstractParameter actionParameters;

	public JobInfo(JobService job, boolean addLink, UriInfo uriInfo) throws ServiceException {
		this(job, addLink, true, uriInfo);
	}

	public JobInfo(JobService job, boolean addLink, boolean addActionParameters, UriInfo uriInfo) throws ServiceException {
		id = job.getId();
		referential = job.getReferential();
		action = job.getAction();
		type = job.getType();
		created = job.getCreated() == null ? null : TimeUtil.toDate(job.getCreated());
		started = job.getStarted() == null ? null : TimeUtil.toDate(job.getStarted());
		updated = job.getUpdated() == null ? null : TimeUtil.toDate(job.getUpdated());
		status = STATUS.valueOf(job.getStatus().name());

		if (addActionParameters) {
			actionParameters = job.getActionParameter();
		}
		if (addLink) {
			linkInfos = new ArrayList<>();
			for (Link link : job.getJob().getLinks()) {
				link.setHref(getRelHref(link.getRel(), job));
				link.setMethod(getMethod(link.getRel(), job));
				linkInfos.add(new LinkInfo(link, uriInfo));
			}
		}
	}

	private String getFileBaseHref() {
		return MessageFormat.format("{0}/{1}/data/{2,number,#}", ROOT_PATH, referential, id);
	}

	private String getScheduledJobHref() {
		return MessageFormat.format("{0}/{1}/scheduled_jobs/{2,number,#}", ROOT_PATH, referential, id);
	}

	private String getTerminatedJobHref() {
		return MessageFormat.format("{0}/{1}/terminated_jobs/{2,number,#}", ROOT_PATH, referential, id);
	}

	private String getRelHref(String rel, JobService jobService) {
		if (rel.equals(Link.PARAMETERS_REL)) {
			return getFileBaseHref() + "/" + PARAMETERS_FILE;
		} else if (rel.equals(Link.ACTION_PARAMETERS_REL)) {
			return getFileBaseHref() + "/" + ACTION_PARAMETERS_FILE;
		} else if (rel.equals(Link.VALIDATION_PARAMETERS_REL)) {
			return getFileBaseHref() + "/" + VALIDATION_PARAMETERS_FILE;
		} else if (rel.equals(Link.DATA_REL) && (action.equals("exporter") || action.equals("globalExport"))) {
			return getFileBaseHref() + "/" + jobService.getOutputFilename();
		} else if (rel.equals(Link.DATA_REL) && !action.equals("exporter")) {
			return getFileBaseHref() + "/" + jobService.getInputFilename();
		} else if (rel.equals(Link.INPUT_REL)) {
			return getFileBaseHref() + "/" + jobService.getInputFilename();
		} else if (rel.equals(Link.OUTPUT_REL)) {
			return getFileBaseHref() + "/" + jobService.getOutputFilename();
		} else if (rel.equals(Link.VALIDATION_REL)) {
			return getFileBaseHref() + "/" + VALIDATION_FILE;
		} else if (rel.equals(Link.REPORT_REL)) {
			return getFileBaseHref() + "/" + REPORT_FILE;
		} else if (rel.equals(Link.CANCEL_REL)) {
			return getScheduledJobHref();
		} else if (rel.equals(Link.DELETE_REL)) {
			return getTerminatedJobHref();
		} else if (rel.equals(Link.LOCATION_REL) && hasTerminatedState(jobService)) {
			return getTerminatedJobHref();
		} else if (rel.equals(Link.LOCATION_REL) && !hasTerminatedState(jobService)) {
			return getScheduledJobHref();
		}
		return null;
	}

	private boolean hasTerminatedState(JobService jobService) {
		return terminatedStates().contains(jobService.getStatus());
	}

	private Set<Job.STATUS> terminatedStates() {
		Set<Job.STATUS> set = new HashSet<Job.STATUS>();
		set.add(Job.STATUS.TERMINATED);
		set.add(Job.STATUS.DELETED);
		return set;
	}

	private String getMethod(String rel, JobService jobService) {
		if (rel.equals(Link.PARAMETERS_REL)) {
			return Link.GET_METHOD;
		} else if (rel.equals(Link.ACTION_PARAMETERS_REL)) {
			return Link.GET_METHOD;
		} else if (rel.equals(Link.VALIDATION_PARAMETERS_REL)) {
			return Link.GET_METHOD;
		} else if (rel.equals(Link.DATA_REL)) {
			return Link.GET_METHOD;
		} else if (rel.equals(Link.INPUT_REL)) {
			return Link.GET_METHOD;
		} else if (rel.equals(Link.OUTPUT_REL)) {
			return Link.GET_METHOD;
		} else if (rel.equals(Link.VALIDATION_REL)) {
			return Link.GET_METHOD;
		} else if (rel.equals(Link.REPORT_REL)) {
			return Link.GET_METHOD;
		} else if (rel.equals(Link.CANCEL_REL)) {
			return Link.DELETE_METHOD;
		} else if (rel.equals(Link.DELETE_REL)) {
			return Link.DELETE_METHOD;
		} else if (rel.equals(Link.LOCATION_REL)) {
			return Link.GET_METHOD;
		}
		return null;
	}

	public enum STATUS implements java.io.Serializable {
		RESCHEDULED, SCHEDULED, STARTED, TERMINATED, CANCELED, ABORTED
	}

}
