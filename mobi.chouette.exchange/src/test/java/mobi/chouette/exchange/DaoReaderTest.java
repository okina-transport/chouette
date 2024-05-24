package mobi.chouette.exchange;

import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.dao.GroupOfLineDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DaoReaderTest {

    DaoReader tested = new DaoReader();

    @BeforeMethod
    public void beforeMethod() {
        tested.companyDAO = Mockito.mock(CompanyDAO.class);
        tested.lineDAO = Mockito.mock(LineDAO.class);
        tested.groupOfLineDAO = Mockito.mock(GroupOfLineDAO.class);
        tested.ptNetworkDAO = Mockito.mock(NetworkDAO.class);
    }


    @Test
    public void testCompare_whenLinesHavePositionAndHaveObjectIdOrPublishedName_doNotRaiseIAE() {
        // arrange

        // these lines and network were from CAEN PROD STRAN referential
        // they used to raise an illegalArgumentException: "Comparison method violates its general contract!"
        Network n1 = new Network();
        n1.setId(1L);
        n1.setPosition(null);

        Network n12 = new Network();
        n12.setId(12L);
        n12.setPosition(null);

        Network n13 = new Network();
        n13.setId(13L);
        n13.setPosition(null);

        Network n14 = new Network();
        n14.setId(14L);
        n14.setPosition(null);

        Network n15 = new Network();
        n15.setId(15L);
        n15.setPosition(null);

        Line line135 = new Line();
        line135.setId(135L);
        line135.setNetwork(n15);
        line135.setObjectId("SAINT_NAZAIRE:Line:102");
        line135.setPublishedName(null);
        line135.setSupprime(false);

        Line line244 = new Line();
        line244.setId(244L);
        line244.setNetwork(n1);
        line244.setObjectId("SAINT_NAZAIRE:Line:80");
        line244.setPublishedName("Gare / Université (ligne circulaire)");

        Line line245 = new Line();
        line245.setId(245L);
        line245.setNetwork(n1);
        line245.setObjectId("SAINT_NAZAIRE:Line:82");
        line245.setPublishedName("Prézégat / St-Nazaire");
        line245.setSupprime(false);

        Line line133 = new Line();
        line133.setId(133L);
        line133.setNetwork(n15);
        line133.setObjectId("SAINT_NAZAIRE:Line:100");
        line133.setPublishedName(null);
        line133.setSupprime(false);

        Line line270 = new Line();
        line270.setId(270L);
        line270.setNetwork(n12);
        line270.setObjectId("SAINT_NAZAIRE:Line:T5 ESAT");
        line270.setPublishedName("Saint-Nazaire - esat Pontchateau");
        line270.setSupprime(false);

        Line line271 = new Line();
        line271.setId(271L);
        line271.setNetwork(n13);
        line271.setObjectId("SAINT_NAZAIRE:Line:01");
        line271.setPublishedName("Nantes - Saint-Nazaire - Le Croisic");
        line271.setSupprime(false);

        Line line134 = new Line();
        line134.setId(134L);
        line134.setNetwork(n15);
        line134.setObjectId("SAINT_NAZAIRE:Line:101");
        line134.setPublishedName(null);
        line134.setSupprime(false);

        Line line138 = new Line();
        line138.setId(138L);
        line138.setNetwork(n15);
        line138.setObjectId("SAINT_NAZAIRE:Line:105");
        line138.setPublishedName(null);
        line138.setSupprime(false);

        Line line274 = new Line();
        line274.setId(274L);
        line274.setNetwork(n13);
        line274.setObjectId("SAINT_NAZAIRE:Line:28");
        line274.setPublishedName("Nantes - Angers - Laval - Rennes");
        line274.setSupprime(false);

        Line line277 = new Line();
        line277.setId(277L);
        line277.setNetwork(n1);
        line277.setObjectId("SAINT_NAZAIRE:Line:220");
        line277.setPublishedName("Navette Centre");
        line277.setSupprime(false);

        Line line276 = new Line();
        line276.setId(276L);
        line276.setNetwork(n14);
        line276.setObjectId("SAINT_NAZAIRE:Line:11067641");
        line276.setPublishedName("La Baule / Pornichet / Saint-Nazaire");
        line276.setSupprime(false);

        Line line201 = new Line();
        line201.setId(201L);
        line201.setNetwork(n15);
        line201.setObjectId("SAINT_NAZAIRE:Line:170");
        line201.setPublishedName(null);
        line201.setSupprime(false);

        Line line280 = new Line();
        line280.setId(280L);
        line280.setNetwork(n1);
        line280.setObjectId("SAINT_NAZAIRE:Line:89");
        line280.setPublishedName("Navette des Plages");
        line280.setSupprime(false);

        Line line136 = new Line();
        line136.setId(136L);
        line136.setNetwork(n15);
        line136.setObjectId("SAINT_NAZAIRE:Line:103");
        line136.setPublishedName(null);
        line136.setSupprime(false);

        Line line137 = new Line();
        line137.setId(137L);
        line137.setNetwork(n15);
        line137.setObjectId("SAINT_NAZAIRE:Line:104");
        line137.setPublishedName(null);
        line137.setSupprime(false);

        Line line176 = new Line();
        line176.setId(176L);
        line176.setNetwork(n15);
        line176.setObjectId("SAINT_NAZAIRE:Line:144");
        line176.setPublishedName(null);
        line176.setSupprime(false);

        Line line177 = new Line();
        line177.setId(177L);
        line177.setNetwork(n15);
        line177.setObjectId("SAINT_NAZAIRE:Line:145");
        line177.setPublishedName(null);
        line177.setSupprime(false);

        Line line178 = new Line();
        line178.setId(178L);
        line178.setNetwork(n15);
        line178.setObjectId("SAINT_NAZAIRE:Line:146");
        line178.setPublishedName(null);
        line178.setSupprime(false);

        Line line181 = new Line();
        line181.setId(181L);
        line181.setNetwork(n15);
        line181.setObjectId("SAINT_NAZAIRE:Line:149");
        line181.setPublishedName(null);
        line181.setSupprime(false);

        Line line180 = new Line();
        line180.setId(180L);
        line180.setNetwork(n15);
        line180.setObjectId("SAINT_NAZAIRE:Line:148");
        line180.setPublishedName(null);
        line180.setSupprime(false);

        Line line184 = new Line();
        line184.setId(184L);
        line184.setNetwork(n15);
        line184.setObjectId("SAINT_NAZAIRE:Line:152");
        line184.setPublishedName(null);
        line184.setSupprime(false);

        Line line192 = new Line();
        line192.setId(192L);
        line192.setNetwork(n15);
        line192.setObjectId("SAINT_NAZAIRE:Line:160");
        line192.setPublishedName(null);
        line192.setSupprime(false);

        Line line196 = new Line();
        line196.setId(196L);
        line196.setNetwork(n15);
        line196.setObjectId("SAINT_NAZAIRE:Line:164");
        line196.setPublishedName(null);
        line196.setSupprime(false);

        Line line199 = new Line();
        line199.setId(199L);
        line199.setNetwork(n15);
        line199.setObjectId("SAINT_NAZAIRE:Line:168");
        line199.setPublishedName(null);
        line199.setSupprime(false);

        Line line200 = new Line();
        line200.setId(200L);
        line200.setNetwork(n15);
        line200.setObjectId("SAINT_NAZAIRE:Line:169");
        line200.setPublishedName(null);
        line200.setSupprime(false);

        Line line202 = new Line();
        line202.setId(202L);
        line202.setNetwork(n15);
        line202.setObjectId("SAINT_NAZAIRE:Line:171");
        line202.setPublishedName(null);
        line202.setSupprime(false);

        Line line206 = new Line();
        line206.setId(206L);
        line206.setNetwork(n15);
        line206.setObjectId("SAINT_NAZAIRE:Line:175");
        line206.setPublishedName(null);
        line206.setSupprime(false);

        Line line203 = new Line();
        line203.setId(203L);
        line203.setNetwork(n15);
        line203.setObjectId("SAINT_NAZAIRE:Line:172");
        line203.setPublishedName(null);
        line203.setSupprime(false);

        Line line205 = new Line();
        line205.setId(205L);
        line205.setNetwork(n15);
        line205.setObjectId("SAINT_NAZAIRE:Line:174");
        line205.setPublishedName(null);
        line205.setSupprime(false);

        Line line208 = new Line();
        line208.setId(208L);
        line208.setNetwork(n15);
        line208.setObjectId("SAINT_NAZAIRE:Line:177");
        line208.setPublishedName(null);
        line208.setSupprime(false);

        Line line207 = new Line();
        line207.setId(207L);
        line207.setNetwork(n15);
        line207.setObjectId("SAINT_NAZAIRE:Line:176");
        line207.setPublishedName(null);
        line207.setSupprime(false);

        Line line209 = new Line();
        line209.setId(209L);
        line209.setNetwork(n15);
        line209.setObjectId("SAINT_NAZAIRE:Line:179");
        line209.setPublishedName(null);
        line209.setSupprime(false);

        Line line210 = new Line();
        line210.setId(210L);
        line210.setNetwork(n15);
        line210.setObjectId("SAINT_NAZAIRE:Line:180");
        line210.setPublishedName(null);
        line210.setSupprime(false);

        Line line211 = new Line();
        line211.setId(211L);
        line211.setNetwork(n15);
        line211.setObjectId("SAINT_NAZAIRE:Line:181");
        line211.setPublishedName(null);
        line211.setSupprime(false);

        Line line212 = new Line();
        line212.setId(212L);
        line212.setNetwork(n15);
        line212.setObjectId("SAINT_NAZAIRE:Line:182");
        line212.setPublishedName(null);
        line212.setSupprime(false);

        Line line216 = new Line();
        line216.setId(216L);
        line216.setNetwork(n15);
        line216.setObjectId("SAINT_NAZAIRE:Line:187");
        line216.setPublishedName(null);
        line216.setSupprime(false);

        Line line213 = new Line();
        line213.setId(213L);
        line213.setNetwork(n15);
        line213.setObjectId("SAINT_NAZAIRE:Line:184");
        line213.setPublishedName(null);
        line213.setSupprime(false);

        Line line214 = new Line();
        line214.setId(214L);
        line214.setNetwork(n15);
        line214.setObjectId("SAINT_NAZAIRE:Line:185");
        line214.setPublishedName(null);
        line214.setSupprime(false);

        Line line215 = new Line();
        line215.setId(215L);
        line215.setNetwork(n15);
        line215.setObjectId("SAINT_NAZAIRE:Line:186");
        line215.setPublishedName(null);
        line215.setSupprime(false);

        Line line217 = new Line();
        line217.setId(217L);
        line217.setNetwork(n15);
        line217.setObjectId("SAINT_NAZAIRE:Line:188");
        line217.setPublishedName(null);
        line217.setSupprime(false);

        Line line218 = new Line();
        line218.setId(218L);
        line218.setNetwork(n15);
        line218.setObjectId("SAINT_NAZAIRE:Line:189");
        line218.setPublishedName(null);
        line218.setSupprime(false);

        Line line219 = new Line();
        line219.setId(219L);
        line219.setNetwork(n15);
        line219.setObjectId("SAINT_NAZAIRE:Line:190");
        line219.setPublishedName(null);
        line219.setSupprime(false);

        Line line226 = new Line();
        line226.setId(226L);
        line226.setNetwork(n15);
        line226.setObjectId("SAINT_NAZAIRE:Line:199");
        line226.setPublishedName(null);
        line226.setSupprime(false);

        Line line220 = new Line();
        line220.setId(220L);
        line220.setNetwork(n15);
        line220.setObjectId("SAINT_NAZAIRE:Line:191");
        line220.setPublishedName(null);
        line220.setSupprime(false);

        Line line221 = new Line();
        line221.setId(221L);
        line221.setNetwork(n15);
        line221.setObjectId("SAINT_NAZAIRE:Line:192");
        line221.setPublishedName(null);
        line221.setSupprime(false);

        Line line222 = new Line();
        line222.setId(222L);
        line222.setNetwork(n15);
        line222.setObjectId("SAINT_NAZAIRE:Line:193");
        line222.setPublishedName(null);
        line222.setSupprime(false);

        Line line223 = new Line();
        line223.setId(223L);
        line223.setNetwork(n15);
        line223.setObjectId("SAINT_NAZAIRE:Line:196");
        line223.setPublishedName(null);
        line223.setSupprime(false);

        Line line248 = new Line();
        line248.setId(248L);
        line248.setNetwork(n15);
        line248.setObjectId("SAINT_NAZAIRE:Line:93");
        line248.setPublishedName(null);
        line248.setSupprime(false);

        Line line253 = new Line();
        line253.setId(253L);
        line253.setNetwork(n15);
        line253.setObjectId("SAINT_NAZAIRE:Line:98");
        line253.setPublishedName(null);
        line253.setSupprime(false);

        Line line141 = new Line();
        line141.setId(141L);
        line141.setNetwork(n15);
        line141.setObjectId("SAINT_NAZAIRE:Line:108");
        line141.setPublishedName(null);
        line141.setSupprime(false);

        Line line139 = new Line();
        line139.setId(139L);
        line139.setNetwork(n15);
        line139.setObjectId("SAINT_NAZAIRE:Line:106");
        line139.setPublishedName(null);
        line139.setSupprime(false);

        Line line147 = new Line();
        line147.setId(147L);
        line147.setNetwork(n15);
        line147.setObjectId("SAINT_NAZAIRE:Line:114");
        line147.setPublishedName(null);
        line147.setSupprime(false);

        Line line172 = new Line();
        line172.setId(172L);
        line172.setNetwork(n15);
        line172.setObjectId("SAINT_NAZAIRE:Line:140");
        line172.setPublishedName(null);
        line172.setSupprime(false);

        Line line174 = new Line();
        line174.setId(174L);
        line174.setNetwork(n15);
        line174.setObjectId("SAINT_NAZAIRE:Line:142");
        line174.setPublishedName(null);
        line174.setSupprime(false);

        Line line173 = new Line();
        line173.setId(173L);
        line173.setNetwork(n15);
        line173.setObjectId("SAINT_NAZAIRE:Line:141");
        line173.setPublishedName(null);
        line173.setSupprime(false);

        Line line175 = new Line();
        line175.setId(175L);
        line175.setNetwork(n15);
        line175.setObjectId("SAINT_NAZAIRE:Line:143");
        line175.setPublishedName(null);
        line175.setSupprime(false);

        Line line185 = new Line();
        line185.setId(185L);
        line185.setNetwork(n15);
        line185.setObjectId("SAINT_NAZAIRE:Line:153");
        line185.setPublishedName(null);
        line185.setSupprime(false);

        Line line179 = new Line();
        line179.setId(179L);
        line179.setNetwork(n15);
        line179.setObjectId("SAINT_NAZAIRE:Line:147");
        line179.setPublishedName(null);
        line179.setSupprime(false);

        Line line182 = new Line();
        line182.setId(182L);
        line182.setNetwork(n15);
        line182.setObjectId("SAINT_NAZAIRE:Line:150");
        line182.setPublishedName(null);
        line182.setSupprime(false);

        Line line183 = new Line();
        line183.setId(183L);
        line183.setNetwork(n15);
        line183.setObjectId("SAINT_NAZAIRE:Line:151");
        line183.setPublishedName(null);
        line183.setSupprime(false);

        Line line186 = new Line();
        line186.setId(186L);
        line186.setNetwork(n15);
        line186.setObjectId("SAINT_NAZAIRE:Line:154");
        line186.setPublishedName(null);
        line186.setSupprime(false);

        Line line187 = new Line();
        line187.setId(187L);
        line187.setNetwork(n15);
        line187.setObjectId("SAINT_NAZAIRE:Line:155");
        line187.setPublishedName(null);
        line187.setSupprime(false);

        Line line188 = new Line();
        line188.setId(188L);
        line188.setNetwork(n15);
        line188.setObjectId("SAINT_NAZAIRE:Line:156");
        line188.setPublishedName(null);
        line188.setSupprime(false);

        Line line189 = new Line();
        line189.setId(189L);
        line189.setNetwork(n15);
        line189.setObjectId("SAINT_NAZAIRE:Line:157");
        line189.setPublishedName(null);
        line189.setSupprime(false);

        Line line190 = new Line();
        line190.setId(190L);
        line190.setNetwork(n15);
        line190.setObjectId("SAINT_NAZAIRE:Line:158");
        line190.setPublishedName(null);
        line190.setSupprime(false);

        Line line191 = new Line();
        line191.setId(191L);
        line191.setNetwork(n15);
        line191.setObjectId("SAINT_NAZAIRE:Line:159");
        line191.setPublishedName(null);
        line191.setSupprime(false);

        Line line193 = new Line();
        line193.setId(193L);
        line193.setNetwork(n15);
        line193.setObjectId("SAINT_NAZAIRE:Line:161");
        line193.setPublishedName(null);
        line193.setSupprime(false);

        Line line194 = new Line();
        line194.setId(194L);
        line194.setNetwork(n15);
        line194.setObjectId("SAINT_NAZAIRE:Line:162");
        line194.setPublishedName(null);
        line194.setSupprime(false);

        Line line195 = new Line();
        line195.setId(195L);
        line195.setNetwork(n15);
        line195.setObjectId("SAINT_NAZAIRE:Line:163");
        line195.setPublishedName(null);
        line195.setSupprime(false);

        Line line197 = new Line();
        line197.setId(197L);
        line197.setNetwork(n15);
        line197.setObjectId("SAINT_NAZAIRE:Line:165");
        line197.setPublishedName(null);
        line197.setSupprime(false);

        Line line224 = new Line();
        line224.setId(224L);
        line224.setNetwork(n15);
        line224.setObjectId("SAINT_NAZAIRE:Line:197");
        line224.setPublishedName(null);
        line224.setSupprime(false);

        Line line204 = new Line();
        line204.setId(204L);
        line204.setNetwork(n15);
        line204.setObjectId("SAINT_NAZAIRE:Line:173");
        line204.setPublishedName(null);
        line204.setSupprime(false);

        Line line225 = new Line();
        line225.setId(225L);
        line225.setNetwork(n15);
        line225.setObjectId("SAINT_NAZAIRE:Line:198");
        line225.setPublishedName(null);
        line225.setSupprime(false);

        Line line227 = new Line();
        line227.setId(227L);
        line227.setNetwork(n15);
        line227.setObjectId("SAINT_NAZAIRE:Line:200");
        line227.setPublishedName(null);
        line227.setSupprime(false);

        Line line228 = new Line();
        line228.setId(228L);
        line228.setNetwork(n15);
        line228.setObjectId("SAINT_NAZAIRE:Line:201");
        line228.setPublishedName(null);
        line228.setSupprime(false);

        Line line229 = new Line();
        line229.setId(229L);
        line229.setNetwork(n15);
        line229.setObjectId("SAINT_NAZAIRE:Line:202");
        line229.setPublishedName(null);
        line229.setSupprime(false);

        Line line234 = new Line();
        line234.setId(234L);
        line234.setNetwork(n15);
        line234.setObjectId("SAINT_NAZAIRE:Line:219");
        line234.setPublishedName(null);
        line234.setSupprime(false);

        Line line230 = new Line();
        line230.setId(230L);
        line230.setNetwork(n15);
        line230.setObjectId("SAINT_NAZAIRE:Line:203");
        line230.setPublishedName(null);
        line230.setSupprime(false);

        Line line232 = new Line();
        line232.setId(232L);
        line232.setNetwork(n15);
        line232.setObjectId("SAINT_NAZAIRE:Line:207");
        line232.setPublishedName(null);
        line232.setSupprime(false);

        Line line233 = new Line();
        line233.setId(233L);
        line233.setNetwork(n15);
        line233.setObjectId("SAINT_NAZAIRE:Line:218");
        line233.setPublishedName(null);
        line233.setSupprime(false);

        Line line251 = new Line();
        line251.setId(251L);
        line251.setNetwork(n15);
        line251.setObjectId("SAINT_NAZAIRE:Line:96");
        line251.setPublishedName(null);
        line251.setSupprime(false);

        Line line252 = new Line();
        line252.setId(252L);
        line252.setNetwork(n15);
        line252.setObjectId("SAINT_NAZAIRE:Line:97");
        line252.setPublishedName(null);
        line252.setSupprime(false);

        Line line254 = new Line();
        line254.setId(254L);
        line254.setNetwork(n15);
        line254.setObjectId("SAINT_NAZAIRE:Line:99");
        line254.setPublishedName(null);
        line254.setSupprime(false);

        Line line275 = new Line();
        line275.setId(275L);
        line275.setNetwork(n14);
        line275.setObjectId("SAINT_NAZAIRE:Line:10569695");
        line275.setPublishedName("Herbignac / Saint-Lyphard / Guérande / Saint-Nazaire");
        line275.setSupprime(false);

        Line line231 = new Line();
        line231.setId(231L);
        line231.setNetwork(n1);
        line231.setObjectId("SAINT_NAZAIRE:Line:206");
        line231.setPublishedName("Gare SNCF / Trignac Mairie");
        line231.setSupprime(false);

        Line line278 = new Line();
        line278.setId(278L);
        line278.setNetwork(n1);
        line278.setObjectId("SAINT_NAZAIRE:Line:221");
        line278.setPublishedName("Navette Littoral");
        line278.setSupprime(false);

        Line line279 = new Line();
        line279.setId(279L);
        line279.setNetwork(n1);
        line279.setObjectId("SAINT_NAZAIRE:Line:222");
        line279.setPublishedName("Navette Village");
        line279.setSupprime(false);

        Line line140 = new Line();
        line140.setId(140L);
        line140.setNetwork(n15);
        line140.setObjectId("SAINT_NAZAIRE:Line:107");
        line140.setPublishedName(null);
        line140.setSupprime(false);

        Line line235 = new Line();
        line235.setId(235L);
        line235.setNetwork(n1);
        line235.setObjectId("SAINT_NAZAIRE:Line:226");
        line235.setPublishedName("Gare SNCF / Bois de Brais");
        line235.setSupprime(false);

        Line line236 = new Line();
        line236.setId(236L);
        line236.setNetwork(n1);
        line236.setObjectId("SAINT_NAZAIRE:Line:227");
        line236.setPublishedName("Petit Maroc / Papin");
        line236.setSupprime(false);

        Line line237 = new Line();
        line237.setId(237L);
        line237.setNetwork(n1);
        line237.setObjectId("SAINT_NAZAIRE:Line:62");
        line237.setPublishedName("ZeniBus (Navette Centre-Ville)");

        Line line238 = new Line();
        line238.setId(238L);
        line238.setNetwork(n1);
        line238.setObjectId("SAINT_NAZAIRE:Line:65");
        line238.setPublishedName(null);
        line238.setSupprime(false);

        Line line239 = new Line();
        line239.setId(239L);
        line239.setNetwork(n1);
        line239.setObjectId("SAINT_NAZAIRE:Line:67");
        line239.setPublishedName("Gare SNCF / Pré Hembert");
        line239.setSupprime(false);

        Line line240 = new Line();
        line240.setId(240L);
        line240.setNetwork(n1);
        line240.setObjectId("SAINT_NAZAIRE:Line:69");
        line240.setPublishedName("Voltaire-Landettes / Le Grand Pez");
        line240.setSupprime(false);

        Line line241 = new Line();
        line241.setId(241L);
        line241.setNetwork(n1);
        line241.setObjectId("SAINT_NAZAIRE:Line:70");
        line241.setPublishedName("Halles de Penhoët / Chemin de la Source");
        line241.setSupprime(false);

        Line line242 = new Line();
        line242.setId(242L);
        line242.setNetwork(n1);
        line242.setObjectId("SAINT_NAZAIRE:Line:77");
        line242.setPublishedName("Ch.-des-Marais / St-Nazaire");
        line242.setSupprime(false);

        Line line243 = new Line();
        line243.setId(243L);
        line243.setNetwork(n1);
        line243.setObjectId("SAINT_NAZAIRE:Line:78");
        line243.setPublishedName("Donges / St-Nazaire");
        line243.setSupprime(false);

        Line line142 = new Line();
        line142.setId(142L);
        line142.setNetwork(n15);
        line142.setObjectId("SAINT_NAZAIRE:Line:109");
        line142.setPublishedName(null);
        line142.setSupprime(false);

        Line line143 = new Line();
        line143.setId(143L);
        line143.setNetwork(n15);
        line143.setObjectId("SAINT_NAZAIRE:Line:110");
        line143.setPublishedName(null);
        line143.setSupprime(false);

        Line line145 = new Line();
        line145.setId(145L);
        line145.setNetwork(n15);
        line145.setObjectId("SAINT_NAZAIRE:Line:112");
        line145.setPublishedName(null);
        line145.setSupprime(false);

        Line line144 = new Line();
        line144.setId(144L);
        line144.setNetwork(n15);
        line144.setObjectId("SAINT_NAZAIRE:Line:111");
        line144.setPublishedName(null);
        line144.setSupprime(false);

        Line line146 = new Line();
        line146.setId(146L);
        line146.setNetwork(n15);
        line146.setObjectId("SAINT_NAZAIRE:Line:113");
        line146.setPublishedName(null);
        line146.setSupprime(false);

        Line line148 = new Line();
        line148.setId(148L);
        line148.setNetwork(n15);
        line148.setObjectId("SAINT_NAZAIRE:Line:115");
        line148.setPublishedName(null);
        line148.setSupprime(false);

        Line line149 = new Line();
        line149.setId(149L);
        line149.setNetwork(n15);
        line149.setObjectId("SAINT_NAZAIRE:Line:116");
        line149.setPublishedName(null);
        line149.setSupprime(false);

        Line line150 = new Line();
        line150.setId(150L);
        line150.setNetwork(n15);
        line150.setObjectId("SAINT_NAZAIRE:Line:117");
        line150.setPublishedName(null);
        line150.setSupprime(false);

        Line line152 = new Line();
        line152.setId(152L);
        line152.setNetwork(n15);
        line152.setObjectId("SAINT_NAZAIRE:Line:119");
        line152.setPublishedName(null);
        line152.setSupprime(false);

        Line line151 = new Line();
        line151.setId(151L);
        line151.setNetwork(n15);
        line151.setObjectId("SAINT_NAZAIRE:Line:118");
        line151.setPublishedName(null);
        line151.setSupprime(false);

        Line line153 = new Line();
        line153.setId(153L);
        line153.setNetwork(n15);
        line153.setObjectId("SAINT_NAZAIRE:Line:120");
        line153.setPublishedName(null);
        line153.setSupprime(false);

        Line line154 = new Line();
        line154.setId(154L);
        line154.setNetwork(n15);
        line154.setObjectId("SAINT_NAZAIRE:Line:121");
        line154.setPublishedName(null);
        line154.setSupprime(false);

        Line line156 = new Line();
        line156.setId(156L);
        line156.setNetwork(n15);
        line156.setObjectId("SAINT_NAZAIRE:Line:123");
        line156.setPublishedName(null);
        line156.setSupprime(false);

        Line line155 = new Line();
        line155.setId(155L);
        line155.setNetwork(n15);
        line155.setObjectId("SAINT_NAZAIRE:Line:122");
        line155.setPublishedName(null);
        line155.setSupprime(false);

        Line line198 = new Line();
        line198.setId(198L);
        line198.setNetwork(n15);
        line198.setObjectId("SAINT_NAZAIRE:Line:166");
        line198.setPublishedName(null);
        line198.setSupprime(false);

        Line line157 = new Line();
        line157.setId(157L);
        line157.setNetwork(n15);
        line157.setObjectId("SAINT_NAZAIRE:Line:124");
        line157.setPublishedName(null);
        line157.setSupprime(false);

        Line line166 = new Line();
        line166.setId(166L);
        line166.setNetwork(n15);
        line166.setObjectId("SAINT_NAZAIRE:Line:134");
        line166.setPublishedName(null);
        line166.setSupprime(false);

        Line line246 = new Line();
        line246.setId(246L);
        line246.setNetwork(n15);
        line246.setObjectId("SAINT_NAZAIRE:Line:91");
        line246.setPublishedName(null);
        line246.setSupprime(false);

        Line line249 = new Line();
        line249.setId(249L);
        line249.setNetwork(n15);
        line249.setObjectId("SAINT_NAZAIRE:Line:94");
        line249.setPublishedName(null);
        line249.setSupprime(false);

        Line line247 = new Line();
        line247.setId(247L);
        line247.setNetwork(n15);
        line247.setObjectId("SAINT_NAZAIRE:Line:92");
        line247.setPublishedName(null);
        line247.setSupprime(false);

        Line line250 = new Line();
        line250.setId(250L);
        line250.setNetwork(n15);
        line250.setObjectId("SAINT_NAZAIRE:Line:95");
        line250.setPublishedName(null);
        line250.setSupprime(false);

        Line line265 = new Line();
        line265.setId(265L);
        line265.setNetwork(n12);
        line265.setObjectId("SAINT_NAZAIRE:Line:315");
        line265.setPublishedName("Préfailles - Saint Nazaire");
        line265.setSupprime(false);

        Line line266 = new Line();
        line266.setId(266L);
        line266.setNetwork(n12);
        line266.setObjectId("SAINT_NAZAIRE:Line:316");
        line266.setPublishedName("Frossay - Saint Nazaire");
        line266.setSupprime(false);

        Line line267 = new Line();
        line267.setId(267L);
        line267.setNetwork(n12);
        line267.setObjectId("SAINT_NAZAIRE:Line:317");
        line267.setPublishedName("Saint Brévin - Saint Nazaire");
        line267.setSupprime(false);

        Line line268 = new Line();
        line268.setId(268L);
        line268.setNetwork(n12);
        line268.setObjectId("SAINT_NAZAIRE:Line:344");
        line268.setPublishedName("Châteaubriant - Saint Nazaire");
        line268.setSupprime(false);

        Line line269 = new Line();
        line269.setId(269L);
        line269.setNetwork(n12);
        line269.setObjectId("SAINT_NAZAIRE:Line:T5");
        line269.setPublishedName("Saint-Nazaire - Saint-Nicolas-de-Redon");
        line269.setSupprime(false);

        Line line272 = new Line();
        line272.setId(272L);
        line272.setNetwork(n13);
        line272.setObjectId("SAINT_NAZAIRE:Line:19");
        line272.setPublishedName("Nantes - Angers - Saumur");
        line272.setSupprime(false);

        Line line273 = new Line();
        line273.setId(273L);
        line273.setNetwork(n13);
        line273.setObjectId("SAINT_NAZAIRE:Line:21");
        line273.setPublishedName("Nantes - Angers - Le Mans");
        line273.setSupprime(false);

        Line line158 = new Line();
        line158.setId(158L);
        line158.setNetwork(n15);
        line158.setObjectId("SAINT_NAZAIRE:Line:125");
        line158.setPublishedName(null);
        line158.setSupprime(false);

        Line line168 = new Line();
        line168.setId(168L);
        line168.setNetwork(n15);
        line168.setObjectId("SAINT_NAZAIRE:Line:136");
        line168.setPublishedName(null);
        line168.setSupprime(false);

        Line line159 = new Line();
        line159.setId(159L);
        line159.setNetwork(n15);
        line159.setObjectId("SAINT_NAZAIRE:Line:127");
        line159.setPublishedName(null);
        line159.setSupprime(false);

        Line line169 = new Line();
        line169.setId(169L);
        line169.setNetwork(n15);
        line169.setObjectId("SAINT_NAZAIRE:Line:137");
        line169.setPublishedName(null);
        line169.setSupprime(false);

        Line line160 = new Line();
        line160.setId(160L);
        line160.setNetwork(n15);
        line160.setObjectId("SAINT_NAZAIRE:Line:128");
        line160.setPublishedName(null);
        line160.setSupprime(false);

        Line line170 = new Line();
        line170.setId(170L);
        line170.setNetwork(n15);
        line170.setObjectId("SAINT_NAZAIRE:Line:138");
        line170.setPublishedName(null);
        line170.setSupprime(false);

        Line line161 = new Line();
        line161.setId(161L);
        line161.setNetwork(n15);
        line161.setObjectId("SAINT_NAZAIRE:Line:129");
        line161.setPublishedName(null);
        line161.setSupprime(false);

        Line line162 = new Line();
        line162.setId(162L);
        line162.setNetwork(n15);
        line162.setObjectId("SAINT_NAZAIRE:Line:130");
        line162.setPublishedName(null);
        line162.setSupprime(false);

        Line line163 = new Line();
        line163.setId(163L);
        line163.setNetwork(n15);
        line163.setObjectId("SAINT_NAZAIRE:Line:131");
        line163.setPublishedName(null);
        line163.setSupprime(false);

        Line line164 = new Line();
        line164.setId(164L);
        line164.setNetwork(n15);
        line164.setObjectId("SAINT_NAZAIRE:Line:132");
        line164.setPublishedName(null);
        line164.setSupprime(false);

        Line line165 = new Line();
        line165.setId(165L);
        line165.setNetwork(n15);
        line165.setObjectId("SAINT_NAZAIRE:Line:133");
        line165.setPublishedName(null);
        line165.setSupprime(false);

        Line line171 = new Line();
        line171.setId(171L);
        line171.setNetwork(n15);
        line171.setObjectId("SAINT_NAZAIRE:Line:139");
        line171.setPublishedName(null);
        line171.setSupprime(false);

        Line line167 = new Line();
        line167.setId(167L);
        line167.setNetwork(n15);
        line167.setObjectId("SAINT_NAZAIRE:Line:135");
        line167.setPublishedName(null);
        line167.setSupprime(false);

        List<Line> lines = Arrays.asList(line135, line244, line245, line133, line270, line271, line134, line138, line274, line277, line276, line201, line280, line136, line137, line176, line177, line178, line181, line180, line184, line192, line196, line199, line200, line202, line206, line203, line205, line208, line207, line209, line210, line211, line212, line216, line213, line214, line215, line217, line218, line219, line226, line220, line221, line222, line223, line248, line253, line141, line139, line147, line172, line174, line173, line175, line185, line179, line182, line183, line186, line187, line188, line189, line190, line191, line193, line194, line195, line197, line224, line204, line225, line227, line228, line229, line234, line230, line232, line233, line251, line252, line254, line275, line231, line278, line279, line140, line235, line236, line237, line238, line239, line240, line241, line242, line243, line142, line143, line145, line144, line146, line148, line149, line150, line152, line151, line153, line154, line156, line155, line198, line157, line166, line246, line249, line247, line250, line265, line266, line267, line268, line269, line272, line273, line158, line168, line159, line169, line160, line170, line161, line162, line163, line164, line165, line171, line167);

        Mockito.when(tested.lineDAO.findAll()).thenReturn(lines);

        try {
            // act
            Set<Long> output = tested.loadLines("", new ArrayList<>());

            // assert
            List<Long> expectedIds = lines.stream().map(Line::getId).collect(Collectors.toList());
            Assert.assertEqualsNoOrder(output.toArray(), expectedIds.toArray(), "should contain all line ids");
        } catch (IllegalArgumentException e) {
            Assert.fail("should not raise IllegalArgumentException: " + e.getMessage());
        }


    }

}
