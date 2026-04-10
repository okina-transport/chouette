package mobi.chouette.ws.parameter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ParamConverter;

public class DateParameterConverter implements ParamConverter<Date> {

    public static final String FORMAT = "yyyy-MM-dd";

    @Override
    public Date fromString(String string) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT);
        try {
            return simpleDateFormat.parse(string);
        } catch (ParseException ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public String toString(Date t) {
        return new SimpleDateFormat(FORMAT).format(t);
    }

}