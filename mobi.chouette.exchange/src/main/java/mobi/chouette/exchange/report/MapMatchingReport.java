package mobi.chouette.exchange.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mobi.chouette.common.Constant;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@XmlRootElement(name = "map_matching_report")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"routesOk", "routesError"})
@Data
@EqualsAndHashCode(callSuper = false)
public class MapMatchingReport extends AbstractReport implements Constant, Report {

    @XmlElement(name = "routesOk")
    private Map<String, String> routesOk = new HashMap<>();

    @XmlElement(name = "routesError")
    private Map<String, String> routesError = new HashMap<>();

    @XmlTransient
    private Date date = new Date(0);

    @Override
    public boolean isEmpty() {
        // used to know if report has to be saved
        // Map matching Report has to be saved any time
        return false;
    }


    @Override
    public void print(PrintStream out, StringBuilder ret, int level, boolean first) {
        ret.setLength(0);
        out.print("{\"map_matching_report\": {\n");
        out.print("\"routesOk\": {\n");
        for (Map.Entry<String, String> entry : routesOk.entrySet()){
            out.print(entry.getKey() +  " - " + entry.getValue() + "\n");
        }
        out.print("}\n");
        out.print("\"routesError\": " + routesError + "\n");
        for (Map.Entry<String, String> entry : routesError.entrySet()){
            out.print(entry.getKey() +  " - " + entry.getValue());
        }
        out.print("}\n");
        out.println("}\n}");
    }


    @Override
    public void print(PrintStream stream) {
        print(stream, new StringBuilder(), 1, true);
    }
}
