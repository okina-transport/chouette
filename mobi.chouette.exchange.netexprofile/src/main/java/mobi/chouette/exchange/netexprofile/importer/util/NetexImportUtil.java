package mobi.chouette.exchange.netexprofile.importer.util;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;

import java.util.Arrays;
import java.util.List;

import static mobi.chouette.common.Constant.COLON_REPLACEMENT_CODE;
import static mobi.chouette.common.Constant.CONFIGURATION;

public class NetexImportUtil {

    /**
     * Define if a file is a common file (calendar/stops/common) or a line file.
     * (currently defined by name but later, replaced by an intelligent process that will read the file to understand its type)
     * @param fileName
     *     The name of the file
     * @return
     *     true : the file is common
     *     false : the file is not common (it is a line file)
     */
    public static boolean isCommonFile(String fileName){

        String fileNameLowerCase = fileName.toLowerCase();
        List<String> commonFiles = Arrays.asList("calendriers.xml", "commun.xml","arrets.xml","reseaux.xml");

        //mobiiti stops file has a custom file name starting with "ARRET_"
        return fileNameLowerCase.startsWith("arret_") || commonFiles.stream()
                .anyMatch(fileNameLowerCase::contains) ;
    }

    public static String composeObjectId( String type,String prefix, String id) {
        if (id == null || id.isEmpty() ) return "";
        return prefix + ":" + type + ":" + replaceColons(id.trim());
    }

    public static String composeObjectIdFromNetexId(Context context, String type, String id) {
        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
        return composeObjectIdFromNetexId(type,parameters.getObjectIdPrefix(),id);
    }

    public static String composeObjectIdFromNetexId(String type, String prefix, String netexId){
        String[] tokens = netexId.split(":");
        if (tokens.length == 3 || ((tokens.length == 4) && "LOC".equals(tokens[3]))){
            return composeObjectId(type,prefix,tokens[2]);
        }
        return composeObjectId(type,prefix,netexId);
    }

    public static String composeOperatorIdFromNetexId( String prefix, String netexId){
        String[] tokens = netexId.split(":");
        if (tokens.length != 4){
            throw new IllegalArgumentException("Netex Id should always have 4 parts. id in error:" + netexId);
        }
        String organisationCode = tokens[2].endsWith("o") ? tokens[2] : tokens[2] + "o";
        return composeObjectId("Operator",prefix,organisationCode);
    }

    /**
     * Replace colons(:) in input string by a special code handled by application (##3A##)
     * @param inputString
     * @return the string with colons encoded
     */

    private static String replaceColons(String inputString){
        return inputString.replace(":",COLON_REPLACEMENT_CODE);
    }


}
