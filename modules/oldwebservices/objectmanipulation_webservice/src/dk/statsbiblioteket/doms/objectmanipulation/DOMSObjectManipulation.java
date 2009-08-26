package dk.statsbiblioteket.doms.objectmanipulation;

import javax.jws.WebMethod;
import javax.jws.WebService;

import dk.statsbiblioteket.doms.DomsUserToken;
import dk.statsbiblioteket.doms.bitstorage.BitstorageFile;
import dk.statsbiblioteket.doms.bitstorage.TestBedBitstorage;
import dk.statsbiblioteket.doms.bitstorage.TestBedBitstorageServiceLocator;
import dk.statsbiblioteket.doms.fedora.FedoraUtils;
import dk.statsbiblioteket.doms.namespace.NamespaceConstants;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.axis.types.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Implementation of web service for manipulating existing objects in the DOMS. */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "",
        reviewers = {""})
@WebService
public class DOMSObjectManipulation implements ObjectManipulation {

    private static final Log log
            = LogFactory.getLog(DOMSObjectManipulation.class);

    @WebMethod
    public void addFile(String username, String password,
                        FileInformation fileInformation, URI pid)
            throws Exception {

        log.trace("Entering DOMSObjectManipulation.addFile with parameters:\n" +
                  "username='" + username + "' password='" + password +
                  "' uri: '" + fileInformation.getUri() + "'\npid: '" + pid +
                  "'");

        System.out.println(
                "Entering DOMSObjectManipulation.addFile with parameters:\n" +
                "username='" + username + "' password='" + password +
                "' uri: '" + fileInformation.getUri() + "'\npid: '" + pid +
                "'");

        try {
            DomsUserToken token = new DomsUserToken(username, password);

            //TODO check if object with given pid exists and is reasonable type

            TestBedBitstorageServiceLocator serviceLocator =
                    new TestBedBitstorageServiceLocator();
            TestBedBitstorage bitstorage = serviceLocator.getBitstorage();

            BitstorageFile bitstorageFile = bitstorage.uploadFile(
                    fileInformation.getFileName(), fileInformation.getUri(),
                    fileInformation.getMd5Sum());

            //TODO Not checked that all information is added
            // Set content datastream
            FedoraUtils.addDatastreamByURI(
                    pid, "CONTENT", bitstorageFile.getFileurl(),
                    bitstorageFile.getMd5CheckSum(),
                    new URI("info:pronom/" + bitstorageFile.getPronomID()),
                    token);

            // Build characterization datastream document
            Document characterizationDocument
                    = FedoraUtils.DOCUMENT_BUILDER.newDocument();
            Element characterization = characterizationDocument.createElementNS(
                    NamespaceConstants.NAMESPACE_CHARACTERISATION,
                    "c:characterisation");
            Element characterisationRun = characterizationDocument
                    .createElementNS(
                            NamespaceConstants.NAMESPACE_CHARACTERISATION,
                            "c:characterisationRun");
            characterization.appendChild(characterisationRun);
            Element formatURI = characterizationDocument.createElementNS(
                    NamespaceConstants.NAMESPACE_CHARACTERISATION,
                    "c:formatURI");
            formatURI.setTextContent(
                    "info:pronom/" + bitstorageFile.getPronomID());
            Element valid = characterizationDocument.createElementNS(
                    NamespaceConstants.NAMESPACE_CHARACTERISATION, "c:valid");
            valid.setTextContent((bitstorageFile.getValidationStatus().equals("VALID") ?  "true" : "false"));
            Element output = characterizationDocument.createElementNS(
                    NamespaceConstants.NAMESPACE_CHARACTERISATION, "c:output");
            output.setTextContent(
                    new String(
                            bitstorageFile.getCharacterizationOutput(),
                            "UTF-8"));
            characterisationRun.appendChild(formatURI);
            characterisationRun.appendChild(valid);
            characterisationRun.appendChild(output);
            characterizationDocument.appendChild(characterization);

            // Set characterization datastream
            FedoraUtils.addDatastreamByDocument(
                    pid, "CHARACTERISATION", characterizationDocument, token);

            //FIXME This should really be done on file approval. Should we hook a Fedora method again?
            bitstorage.approveFile(fileInformation.getUri(), fileInformation.getMd5Sum());

        } catch (Exception e) {
            // TODO: Remember exception handling. Don't let any strange exceptions
            //       escape.
            log.error("Failed adding '" + fileInformation.getUri() + "' to '" + pid + "'", e);
            throw new Exception("Failed adding '" + fileInformation.getUri() + "' to '" + pid + "'", e);
        }
    }
}
