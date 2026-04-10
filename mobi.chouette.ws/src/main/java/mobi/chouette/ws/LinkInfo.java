package mobi.chouette.ws;

import javax.ws.rs.core.UriInfo;

import lombok.Data;
import lombok.NoArgsConstructor;
import mobi.chouette.model.iev.Link;

@Data
@NoArgsConstructor
public class LinkInfo {

	private String rel;

	private String href;

	private String type;

	private String method;
	
	public LinkInfo(Link link,UriInfo uriInfo)
	{
		rel = link.getRel();
		href = uriInfo.getBaseUri()+link.getHref();
		type = link.getType();
		method = link.getMethod();
	}

}
