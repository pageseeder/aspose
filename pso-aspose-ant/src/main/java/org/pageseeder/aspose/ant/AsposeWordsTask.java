/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.aspose.ant;

import jakarta.mail.MessagingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.aspose.words.cloud.*;
import com.aspose.words.cloud.api.*;
import com.aspose.words.cloud.model.*;
import com.aspose.words.cloud.model.requests.*;
import com.aspose.words.cloud.model.responses.*;
import java.nio.file.Files;
import java.util.UUID;

/**
 * An ANT task to convert a DOCX file to PDF using Aspose cloud
 *
 * @author Philip Rutherford
 */
public final class AsposeWordsTask extends Task {

  private static final String DOCX_EXTENSION = ".docx";
  /**
   * The Word document to convert.
   */
  private File source;

  /**
   * The PDF file to create.
   */
  private File destination;

  /**
   * The Aspose cloud base URL
   */
  private String baseUrl = null;

  /**
   * The Aspose cloud client ID
   */
  private String clientId;

  /**
   * The Aspose cloud client secret
   */
  private String clientSecret;

  /**
   * Pipe-separated list of document names to append to the source document.
   */
  private String appendDocs = null;

  /**
   * Whether to update DOCX fields
   */
  private boolean updateFields = false;

  // Set properties
  // ----------------------------------------------------------------------------------------------

  /**
   * Set the source file (a DOCX file).
   *
   * @param docx The Word document (DOCX) to import.
   */
  public void setSrc(File docx) {
    if (!(docx.exists())) throw new BuildException("the document " + docx.getName()+ " doesn't exist");
    if (docx.isDirectory()) throw new BuildException("the document " + docx.getName() + " can't be a directory");
    String name = docx.getName();
    if (!name.endsWith(DOCX_EXTENSION)) {
      log("Word document file should generally end with .docx - but was "+name);
    }
    this.source = docx;
  }

  /**
   * Set the destination The PDF file to create.
   *
   * @param destination The destination file.
   */
  public void setDest(File destination) {
    this.destination = destination;
  }

  /**
   * @param url the Aspose cloud base URL.
   */
  public void setBaseUrl(String url) {
    this.baseUrl = url;
  }

  /**
   * @param id the Aspose cloud client ID.
   */
  public void setClientId(String id) {
    this.clientId = id;
  }

  /**
   * @param secret the Aspose cloud client secret.
   */
  public void setClientSecret(String secret) {
    this.clientSecret = secret;
  }

  /**
   *
   * @param appendDocs pipe-separated list of document names to append to the source document.
   */
  public void setAppendDocs(String appendDocs) {
    this.appendDocs = appendDocs;
  }

  /**
   * @param update whether to update DOCX fields
   */
  public void setUpdateFields(boolean update) {
    this.updateFields = update;
  }

  // Execute
  // ----------------------------------------------------------------------------------------------

  @Override
  public void execute() throws BuildException {
    if (this.source == null)
      throw new BuildException("Source document must be specified using 'src' attribute");
    if (this.destination == null)
      throw new BuildException("Destination document must be specified using 'dest' attribute");
    if (this.clientId == null)
      throw new BuildException("Client ID must be specified using 'clientid' attribute");
    if (this.clientSecret == null)
      throw new BuildException("Client secret must be specified using 'clientsecret' attribute");

    log("Converting DOCX " + this.source.getName() + " to PDF " + this.destination.getName());

    ApiClient apiClient = new ApiClient(this.clientId, this.clientSecret, this.baseUrl);
    apiClient.setReadTimeout(1200_000); // 20 minutes
    WordsApi wordsApi = new WordsApi(apiClient);
    if (this.appendDocs != null) {
      // 1. Generate a unique folder path for this specific thread execution
      String uniqueFolder = "conversions/" + UUID.randomUUID().toString() + "/";
      try {
        appendAndConvert(uniqueFolder, wordsApi);
      } catch (Exception ex) {
        throw new BuildException(ex);
      } finally {
        // 7. Wipe the entire unique folder from the cloud to clean up all files at once
        try {
          log("Deleting conversion folder");
          DeleteFolderRequest deleteFolder = new DeleteFolderRequest(uniqueFolder, null, true); // true = recursive delete
          wordsApi.deleteFolder(deleteFolder);
        } catch (Exception ex) {
          log("Unable to delete conversion folder '" + uniqueFolder + "'", ex,
                  org.apache.tools.ant.Project.MSG_WARN);
        }
      }
    } else {
      try {
        if (convert(wordsApi)) return;
      } catch (Exception ex) {
        throw new BuildException(ex);
      }
    }
    log("Conversion complete");
  }

  private boolean convert(WordsApi wordsApi) throws IOException, ApiException, MessagingException {
    byte[] requestDocument = Files.readAllBytes(this.source.toPath());
    long tempFolder = System.nanoTime();
    if (this.updateFields) {
      log("Updating fields");
      String tempResult = tempFolder + "/" + this.source.getName();
      UpdateFieldsOnlineRequest request = new UpdateFieldsOnlineRequest(requestDocument,
              null, null, null, false, tempResult);
      UpdateFieldsOnlineResponse result = wordsApi.updateFieldsOnline(request);
      requestDocument = result.getDocument().get(tempResult);
    }
    if (this.destination.getName().toLowerCase().endsWith(DOCX_EXTENSION)) {
      Files.write(this.destination.toPath(), requestDocument);
      return true;
    }
    PdfSaveOptionsData requestSaveOptionsData = new PdfSaveOptionsData();
    String tempResult = tempFolder + "/" + this.destination.getName();
    requestSaveOptionsData.setFileName(tempResult);
    OutlineOptionsData outlineOptions = new OutlineOptionsData();
    outlineOptions.headingsOutlineLevels(6);
    requestSaveOptionsData.outlineOptions(outlineOptions);
    SaveAsOnlineRequest request = new SaveAsOnlineRequest(requestDocument,
            requestSaveOptionsData, null, null, null, false, null);
    SaveAsOnlineResponse result = wordsApi.saveAsOnline(request);
    Files.write(this.destination.toPath(), result.getDocument().get(tempResult));
    return false;
  }

  private void appendAndConvert(String uniqueFolder, WordsApi wordsApi) throws IOException, ApiException, MessagingException {
    log("Loading source document");
    String masterName = this.source.getName();
    byte[] masterBytes = Files.readAllBytes(this.source.toPath());
    UploadFileRequest uploadMaster = new UploadFileRequest(masterBytes, uniqueFolder + masterName, null);
    wordsApi.uploadFile(uploadMaster);

    log("Loading append documents");
    String[] appendFiles = this.appendDocs.split("\\|");
    for (String appendFile : appendFiles) {
      byte[] subBytes = Files.readAllBytes(new File(this.source.getParentFile(), appendFile).toPath());
      // The second parameter specifies the path target inside the cloud
      UploadFileRequest uploadSub = new UploadFileRequest(subBytes, uniqueFolder + appendFile, null);
      wordsApi.uploadFile(uploadSub);
    }

    log("Appending documents");
    ArrayList<DocumentEntry> entries = new ArrayList<>();
    for (String subFile : appendFiles) {
      DocumentEntry entry = new DocumentEntry();
      // Crucial: Point to the file inside the unique folder
      entry.setFileReference(new FileReference(uniqueFolder + subFile));
      entry.setImportFormatMode(DocumentEntry.ImportFormatModeEnum.USEDESTINATIONSTYLES);
      entries.add(entry);
    }
    DocumentEntryList documentList = new DocumentEntryList();
    documentList.setDocumentEntries(entries);
    AppendDocumentRequest appendRequest = new AppendDocumentRequest(
             masterName, documentList, uniqueFolder, null, null,
            null, null, false, null,
            null, null);
    wordsApi.appendDocument(appendRequest);

    if (this.updateFields) {
      log("Updating fields");
      UpdateFieldsRequest request = new UpdateFieldsRequest(
              masterName,
              uniqueFolder,
              null, null, null, null,
              false, null);
      wordsApi.updateFields(request);
    }

    String downloadPath = uniqueFolder + masterName;
    if (!this.destination.getName().toLowerCase().endsWith(DOCX_EXTENSION)) {
      log("Converting to PDF");
      PdfSaveOptionsData saveOptions = new PdfSaveOptionsData();
      saveOptions.setFileName("FinalCompiledBook.pdf");
      OutlineOptionsData outlineOptions = new OutlineOptionsData();
      outlineOptions.headingsOutlineLevels(6);
      saveOptions.outlineOptions(outlineOptions);
      SaveAsRequest saveAsRequest = new SaveAsRequest(
              masterName, saveOptions, uniqueFolder,
              null, null, null, null,
              false, null);
      wordsApi.saveAs(saveAsRequest);
      downloadPath = uniqueFolder + "FinalCompiledBook.pdf";
    }

    log("Downloading result");
    DownloadFileRequest downloadRequest = new DownloadFileRequest(
            downloadPath,null,null);
    byte[] resultBytes = wordsApi.downloadFile(downloadRequest);
    Files.write(this.destination.toPath(), resultBytes);
  }
}
