package mobi.chouette.exchange.gtfs.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.model.FeedInfo;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static mobi.chouette.common.TimeUtil.toLocalDate;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsFeedInfo extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    @Getter
    @Setter
    private String feedPublisherName;

    @Getter
    @Setter
    private URL feedPublisherUrl;

    @Getter
    @Setter
    private String feedLang;

    @Getter
    @Setter
    private LocalDate feedStartDate;

    @Getter
    @Setter
    private LocalDate feedEndDate;

    @Getter
    @Setter
    private String feedVersion;

    @Getter
    @Setter
    private String feedContactEmail;

    @Getter
    @Setter
    private URL feedContactUrl;

    public GtfsFeedInfo(GtfsFeedInfo bean) {
        this(bean.getFeedPublisherName(), bean.getFeedPublisherUrl(), bean.getFeedLang(), bean.getFeedStartDate(), bean.getFeedEndDate(), bean.getFeedVersion(), bean.getFeedContactEmail(), bean.getFeedContactUrl());
        this.setId(bean.getId());
    }

    public GtfsFeedInfo(FeedInfo feedInfo) throws MalformedURLException {
        this(feedInfo.getPublisherName(), !StringUtils.isEmpty(feedInfo.getPublisherUrl()) ? new URL(feedInfo.getPublisherUrl()) : null,
                feedInfo.getLang(), toLocalDate(feedInfo.getStartDate()),
                toLocalDate(feedInfo.getEndDate()),
                LocalDateTime.now().format(formatter), feedInfo.getContactEmail(), !StringUtils.isEmpty(feedInfo.getContactUrl()) ? new URL(feedInfo.getContactUrl()) : null);
        this.setId(feedInfo.getId() != null ? feedInfo.getId().intValue() : null);
    }

}
