package org.pageseeder.aspose.ant;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Properties;

public class AsposeWordsTaskTest {

  private static final File CASES = new File("src/test/aspose/cases");

  private static final File RESULTS = new File("test/aspose/results");

  private static final File CLIENT = new File("aspose.properties");

  @Test
  public void testAppend() throws IOException {
    testIndividual("append", true, "A heading 2.docx|Another heading 2.docx", false);
  }

  @Test
  public void testAppend2() throws IOException {
    testIndividual("append2", true, "A heading 2.docx|Another heading 2.docx", true);
  }

  @Test
  public void testBasic() throws IOException {
    testIndividual("basic", false, null,  false);
  }

  @Test
  public void testPsGenerate() throws IOException {
    testIndividual("ps-generated", false, null, false);
  }

  @Test
  public void testUpdateFields() throws IOException {
    testIndividual("update-fields", true, null, false);
  }

  @Test
  public void testUpdateFields2() throws IOException {
    testIndividual("update-fields2", true, null, true);
  }

  public void testIndividual(String folderName, boolean updateFields, String appendDocs, boolean outputDocx) throws IOException {
    File dir = new File(CASES, folderName);
    if (dir.isDirectory()) {

      if (new File(dir, dir.getName() + ".docx").exists()) {
        System.out.println(dir.getName());
        File resultDir = new File(RESULTS, dir.getName());
        if (resultDir.exists()) {
          FileUtils.deleteDirectory(resultDir);
        }
        Assert.assertTrue(resultDir.mkdirs());
        File result = new File(resultDir, dir.getName() + (outputDocx ? ".docx" : ".pdf"));
        File actual = process(dir, result, updateFields, appendDocs);
        File expected = new File(dir, "expected" + (outputDocx ? ".docx" : ".pdf"));

        // Check that the files exist
        Assert.assertTrue(actual.exists());
        Assert.assertTrue(expected.exists());

        Assert.assertTrue(actual.length() > 0);
        Assert.assertTrue(expected.length() > 0);
        Assert.assertTrue("Expected result file size:" + expected.length() + " but was " + actual.length(),
            Math.abs(expected.length() - actual.length()) <= 30); // might be slight variation
      } else {
        throw new IOException("Unable to find DOCX file for test:" + dir.getName());
      }
    }
  }

  private File process(File test, File result, boolean updateFields, String appendDocs) throws IOException {
    AsposeWordsTask task = new AsposeWordsTask();
    task.setSrc(new File(test, test.getName() + ".docx"));
    task.setDest(result);
    try (InputStream input = new FileInputStream(CLIENT)) {
      Properties props = new Properties();
      props.load(input);
      String clientId = props.getProperty("clientid");
      String clientSecret = props.getProperty("clientsecret");
      String url = props.getProperty("baseurl");
      if (clientId == null || clientSecret == null) {
        throw new IOException("The clientid and clientsecret must be defined in pso-aspose-ant/aspose.properties");
      }
      task.setClientId(clientId);
      task.setClientSecret(clientSecret);
      task.setBaseUrl(url);
      if (updateFields) {
        task.setUpdateFields(true);
      }
      task.setAppendDocs(appendDocs);
    } catch (FileNotFoundException ex) {
      throw new IOException("The clientid and clientsecret must be defined in pso-aspose-ant/aspose.properties");
    }
    task.execute();

    return result;
  }

}
