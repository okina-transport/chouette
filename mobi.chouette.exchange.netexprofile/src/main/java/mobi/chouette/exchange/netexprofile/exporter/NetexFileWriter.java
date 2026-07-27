package mobi.chouette.exchange.netexprofile.exporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.exporter.writer.PublicationDeliveryFranceWriter;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;

@Log4j
class NetexFileWriter implements Constant {

	void writeXmlFile(Context context, Path filePath, ExportableData exportableData, ExportableNetexData exportableNetexData, NetexFragmentMode fragmentMode,
			Marshaller marshaller) throws XMLStreamException {

		try (Writer bufferedWriter = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
			IndentingXMLStreamWriter writer = NetexXMLProcessingHelperFactory.createXMLWriter(bufferedWriter);

			try {
				writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");

				// TODO changer d'un data producer à l'autre pour changer de PROFIL IDFM Norvégien
//				PublicationDeliveryWriter.write(context, writer, exportableData, exportableNetexData, fragmentMode, marshaller);
				PublicationDeliveryFranceWriter.write(context, writer, exportableData, exportableNetexData, fragmentMode, marshaller);
				writer.flush();
			} finally {
				try {
					writer.close();
				} catch (XMLStreamException e) {
					log.error("Error flushing and closing Netex Export XML file "+filePath.toString(),e);
					throw e;
				}
			}

		} catch (XMLStreamException | IOException e) {
			log.error("Could not produce XML file", e);
			throw new RuntimeException(e);
		}
	}

}
