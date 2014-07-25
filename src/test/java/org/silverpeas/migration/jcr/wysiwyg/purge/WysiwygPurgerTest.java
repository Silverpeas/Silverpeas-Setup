/*
 * Copyright (C) 2000 - 2014 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception. You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.migration.jcr.wysiwyg.purge;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.model.SimpleAttachmentBuilder;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.test.jcr.JcrTest;
import org.silverpeas.util.Console;
import org.silverpeas.util.StringUtil;
import org.silverpeas.util.file.FileUtil;

import java.io.File;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.silverpeas.migration.jcr.service.model.DocumentType.wysiwyg;

public class WysiwygPurgerTest {

  private final static String instanceId_A = "kmelia1";
  private final static String foreignId_1 = "foreignId_1";
  private final static String foreignId_2 = "foreignId_2";
  private final static String instanceId_B = "kmelia2";
  private final static String foreignId_11 = "foreignId_11";
  private final static String foreignId_12 = "foreignId_12";
  private final static String instanceId_C = "kmelia3";
  private final static String foreignId_21 = "foreignId_21";
  private final static String foreignId_22 = "foreignId_22";
  private final static String instanceId_D = "kmelia4";
  private final static String foreignId_31 = "foreignId_31";
  private final static String foreignId_32 = "foreignId_32";
  private final static String instanceId_E = "kmelia5";
  private final static String foreignId_41 = "foreignId_41";
  private final static String foreignId_42 = "foreignId_42";
  private final static String foreignId_43 = "foreignId_43";
  private final static String instanceId_F = "kmelia6";
  private final static String foreignId_51 = "foreignId_51";
  private final static String instanceId_G = "kmelia7";
  private final static String foreignId_61 = "foreignId_61";
  private final static String foreignId_62 = "foreignId_62";

  String id_att_A_1;
  String id_att_A_2;
  String id_att_B_11;
  String id_att_B_12;
  String id_att_C_21;
  String id_att_C_22;

  @Test
  public void testPurgeWithOtherExistingAttachment() throws Exception {
    new JcrWysiwygPurgerTest() {
      @Override
      public void run() throws Exception {
        /*
        Preparing
         */

        // Attachment that will be kept.
        createAttachmentsThatWillNotBeModified(this);

        SimpleDocument emptyFr = createEmptyFrWysiwygForTest(instanceId_A, foreignId_1);
        SimpleDocument emptyEn = createEmptyEnWysiwygForTest(instanceId_A, foreignId_2);

        assertContent(emptyFr.getId(), "fr", "");
        assertContent(emptyFr.getId(), "en", null);
        assertContent(emptyEn.getId(), "fr", null);
        assertContent(emptyEn.getId(), "en", "");

        SimpleDocument filledFr = createFrWysiwygForTest(instanceId_B, foreignId_11);
        SimpleDocument filledEn = createEnWysiwygForTest(instanceId_B, foreignId_12);

        assertContent(filledFr.getId(), "fr", "fr_content_kmelia2_foreignId_11");
        assertContent(filledFr.getId(), "en", null);
        assertContent(filledEn.getId(), "fr", null);
        assertContent(filledEn.getId(), "en", "en_content_kmelia2_foreignId_12");

        SimpleDocument emptyFrFilledEn = createEmptyFrWysiwygForTest(instanceId_C, foreignId_21);
        addEnWysiwygForTest(emptyFrFilledEn);
        SimpleDocument filledFrEmptyEn = createFrWysiwygForTest(instanceId_C, foreignId_22);
        addEmptyEnWysiwygForTest(filledFrEmptyEn);

        assertContent(emptyFrFilledEn.getId(), "fr", "");
        assertContent(emptyFrFilledEn.getId(), "en", "en_content_kmelia3_foreignId_21");
        assertContent(filledFrEmptyEn.getId(), "fr", "fr_content_kmelia3_foreignId_22");
        assertContent(filledFrEmptyEn.getId(), "en", "");

        SimpleDocument emptyFrEmptyEn = createEmptyFrWysiwygForTest(instanceId_D, foreignId_31);
        addEmptyEnWysiwygForTest(emptyFrEmptyEn);
        SimpleDocument FilledEnfilledFr = createEnWysiwygForTest(instanceId_D, foreignId_32);
        addFrWysiwygForTest(FilledEnfilledFr);

        assertContent(emptyFrEmptyEn.getId(), "fr", "");
        assertContent(emptyFrEmptyEn.getId(), "en", "");
        assertContent(FilledEnfilledFr.getId(), "fr", "fr_content_kmelia4_foreignId_32");
        assertContent(FilledEnfilledFr.getId(), "en", "en_content_kmelia4_foreignId_32");

        SimpleDocument filledFrEmptyEnEmptyDe = createFrWysiwygForTest(instanceId_E, foreignId_41);
        addEmptyEnWysiwygForTest(filledFrEmptyEnEmptyDe);
        addEmptyDeWysiwygForTest(filledFrEmptyEnEmptyDe);
        SimpleDocument sameFilledFrDeEmptyEn =
            createFreeFrWysiwygForTest(instanceId_E, foreignId_42,
                "same_fr-de_kmelia5_foreignId_42");
        addEmptyEnWysiwygForTest(sameFilledFrDeEmptyEn);
        addFreeDeWysiwygForTest(sameFilledFrDeEmptyEn, "same_fr-de_kmelia5_foreignId_42");
        SimpleDocument sameFilledFrDeFilledEn =
            createFreeFrWysiwygForTest(instanceId_E, foreignId_43,
                "same_fr-de_kmelia5_foreignId_43");
        addEnWysiwygForTest(sameFilledFrDeFilledEn);
        addFreeDeWysiwygForTest(sameFilledFrDeFilledEn, "same_fr-de_kmelia5_foreignId_43");

        assertContent(filledFrEmptyEnEmptyDe.getId(), "fr", "fr_content_kmelia5_foreignId_41");
        assertContent(filledFrEmptyEnEmptyDe.getId(), "en", "");
        assertContent(filledFrEmptyEnEmptyDe.getId(), "de", "");
        assertContent(sameFilledFrDeEmptyEn.getId(), "fr", "same_fr-de_kmelia5_foreignId_42");
        assertContent(sameFilledFrDeEmptyEn.getId(), "en", "");
        assertContent(sameFilledFrDeEmptyEn.getId(), "de", "same_fr-de_kmelia5_foreignId_42");
        assertContent(sameFilledFrDeFilledEn.getId(), "fr", "same_fr-de_kmelia5_foreignId_43");
        assertContent(sameFilledFrDeFilledEn.getId(), "en", "en_content_kmelia5_foreignId_43");
        assertContent(sameFilledFrDeFilledEn.getId(), "de", "same_fr-de_kmelia5_foreignId_43");

        SimpleDocument sameFilledFrEn = createFreeFrWysiwygForTest(instanceId_F, foreignId_51,
            "same_fr-en_kmelia6_foreignId_51");
        addFreeEnWysiwygForTest(sameFilledFrEn, "same_fr-en_kmelia6_foreignId_51");

        assertContent(sameFilledFrEn.getId(), "fr", "same_fr-en_kmelia6_foreignId_51");
        assertContent(sameFilledFrEn.getId(), "en", "same_fr-en_kmelia6_foreignId_51");


        Date aDate = java.sql.Date.valueOf("2014-06-24");
        Date aBeforeDate = DateUtils.addDays(aDate, -4);
        Date aAfterDate = DateUtils.addDays(aDate, 4);

        SimpleDocument severalSameFilledFrFilledEn_1 =
            createFrWysiwygForTest(instanceId_G, foreignId_61);
        severalSameFilledFrFilledEn_1.setCreated(aDate);
        severalSameFilledFrFilledEn_1.setUpdated(null);
        updateAttachmentForTest(severalSameFilledFrFilledEn_1);
        SimpleDocument severalSameFilledFrFilledEn_1_en =
            addEnWysiwygForTest(severalSameFilledFrFilledEn_1);
        severalSameFilledFrFilledEn_1_en.setCreated(aDate);
        severalSameFilledFrFilledEn_1_en.setUpdated(aAfterDate);
        updateAttachmentForTest(severalSameFilledFrFilledEn_1_en);
        SimpleDocument severalSameFilledFrFilledEn_2 =
            createFrWysiwygForTest(instanceId_G, foreignId_61);
        severalSameFilledFrFilledEn_2.setCreated(aBeforeDate);
        severalSameFilledFrFilledEn_2.setUpdated(aAfterDate);
        updateAttachmentForTest(severalSameFilledFrFilledEn_2);
        SimpleDocument severalSameFilledFrFilledEn_2_en =
            addEnWysiwygForTest(severalSameFilledFrFilledEn_2);
        severalSameFilledFrFilledEn_2_en.setCreated(aBeforeDate);
        severalSameFilledFrFilledEn_2_en.setUpdated(aDate);
        updateAttachmentForTest(severalSameFilledFrFilledEn_2_en);

        assertThat(severalSameFilledFrFilledEn_1.getId(),
            not(is(severalSameFilledFrFilledEn_2.getId())));
        assertContent(severalSameFilledFrFilledEn_1.getId(), "fr",
            "fr_content_kmelia7_foreignId_61");
        assertContent(severalSameFilledFrFilledEn_1.getId(), "en",
            "en_content_kmelia7_foreignId_61");
        assertContent(severalSameFilledFrFilledEn_2.getId(), "fr",
            "fr_content_kmelia7_foreignId_61");
        assertContent(severalSameFilledFrFilledEn_2.getId(), "en",
            "en_content_kmelia7_foreignId_61");

        SimpleDocument severalFilledFrFilledEnFilledDe_1 =
            createFreeFrWysiwygForTest(instanceId_G, foreignId_62, "G_62_A");
        severalFilledFrFilledEnFilledDe_1.setCreated(aDate);
        severalFilledFrFilledEnFilledDe_1.setUpdated(null);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_1);
        SimpleDocument severalFilledFrFilledEnFilled_1_en =
            addFreeEnWysiwygForTest(severalFilledFrFilledEnFilledDe_1, "G_62_B");
        severalFilledFrFilledEnFilled_1_en.setCreated(aDate);
        severalFilledFrFilledEnFilled_1_en.setUpdated(aAfterDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilled_1_en);
        SimpleDocument severalFilledFrFilledEnFilledDe_2 =
            createFreeFrWysiwygForTest(instanceId_G, foreignId_62, "G_62_C");
        severalFilledFrFilledEnFilledDe_2.setCreated(aBeforeDate);
        severalFilledFrFilledEnFilledDe_2.setUpdated(aAfterDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_2);
        SimpleDocument severalFilledFrFilledEnFilledDe_2_en =
            addFreeEnWysiwygForTest(severalFilledFrFilledEnFilledDe_2, "G_62_D");
        severalFilledFrFilledEnFilledDe_2_en.setCreated(aBeforeDate);
        severalFilledFrFilledEnFilledDe_2_en.setUpdated(aDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_2_en);
        SimpleDocument severalFilledFrFilledEnFilledDe_2_de =
            addFreeDeWysiwygForTest(severalFilledFrFilledEnFilledDe_2, "G_62_E");
        severalFilledFrFilledEnFilledDe_2_de.setCreated(aBeforeDate);
        severalFilledFrFilledEnFilledDe_2_de.setUpdated(aDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_2_de);
        SimpleDocument severalFilledFrFilledEnFilledDe_3 =
            createFreeDeWysiwygForTest(instanceId_G, foreignId_62, "G_62_E");
        severalFilledFrFilledEnFilledDe_3.setCreated(aBeforeDate);
        severalFilledFrFilledEnFilledDe_3.setUpdated(aAfterDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_3);

        assertThat(severalFilledFrFilledEnFilledDe_1.getId(),
            not(is(severalFilledFrFilledEnFilledDe_2.getId())));
        assertThat(severalFilledFrFilledEnFilledDe_1.getId(),
            not(is(severalFilledFrFilledEnFilledDe_3.getId())));
        assertThat(severalFilledFrFilledEnFilledDe_2.getId(),
            not(is(severalFilledFrFilledEnFilledDe_3.getId())));
        assertContent(severalFilledFrFilledEnFilledDe_1.getId(), "fr", "G_62_A");
        assertContent(severalFilledFrFilledEnFilledDe_1.getId(), "en", "G_62_B");
        assertContent(severalFilledFrFilledEnFilledDe_1.getId(), "de", null);
        assertContent(severalFilledFrFilledEnFilledDe_2.getId(), "fr", "G_62_C");
        assertContent(severalFilledFrFilledEnFilledDe_2.getId(), "en", "G_62_D");
        assertContent(severalFilledFrFilledEnFilledDe_2.getId(), "de", "G_62_E");
        assertContent(severalFilledFrFilledEnFilledDe_3.getId(), "fr", null);
        assertContent(severalFilledFrFilledEnFilledDe_3.getId(), "en", null);
        assertContent(severalFilledFrFilledEnFilledDe_3.getId(), "de", "G_62_E");

        /*
        The test
         */
        WysiwygPurger purger = new WysiwygPurger(getSimpleDocumentService(), 1);
        purger.setConsole(new Console().setEchoAsDotEnabled(false));
        purger.purgeDocuments();

        /*
        Assertions
         */

        assertDocumentDoesNotExist(emptyFr.getId());
        assertDocumentDoesNotExist(emptyEn.getId());
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_A)).exists(), is(true));

        assertContentAndFilename(filledFr.getId(), "fr", "fr_content_kmelia2_foreignId_11");
        assertContentAndFilename(filledFr.getId(), "en", null);
        assertContentAndFilename(filledEn.getId(), "fr", null);
        assertContentAndFilename(filledEn.getId(), "en", "en_content_kmelia2_foreignId_12");
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_B)).exists(), is(true));

        assertContentAndFilename(emptyFrFilledEn.getId(), "fr", null);
        assertContentAndFilename(emptyFrFilledEn.getId(), "en", "en_content_kmelia3_foreignId_21");
        assertContentAndFilename(filledFrEmptyEn.getId(), "fr", "fr_content_kmelia3_foreignId_22");
        assertContentAndFilename(filledFrEmptyEn.getId(), "en", null);
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_C)).exists(), is(true));

        assertDocumentDoesNotExist(emptyFrEmptyEn.getId());
        assertContentAndFilename(FilledEnfilledFr.getId(), "fr", "fr_content_kmelia4_foreignId_32");
        assertContentAndFilename(FilledEnfilledFr.getId(), "en", "en_content_kmelia4_foreignId_32");
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_D)).exists(), is(true));

        assertContentAndFilename(filledFrEmptyEnEmptyDe.getId(), "fr",
            "fr_content_kmelia5_foreignId_41");
        assertContentAndFilename(filledFrEmptyEnEmptyDe.getId(), "en", null);
        assertContentAndFilename(filledFrEmptyEnEmptyDe.getId(), "de", null);
        assertContentAndFilename(sameFilledFrDeEmptyEn.getId(), "fr",
            "same_fr-de_kmelia5_foreignId_42");
        assertContentAndFilename(sameFilledFrDeEmptyEn.getId(), "en", null);
        assertContentAndFilename(sameFilledFrDeEmptyEn.getId(), "de", null);
        assertContentAndFilename(sameFilledFrDeFilledEn.getId(), "fr",
            "same_fr-de_kmelia5_foreignId_43");
        assertContentAndFilename(sameFilledFrDeFilledEn.getId(), "en",
            "en_content_kmelia5_foreignId_43");
        assertContentAndFilename(sameFilledFrDeFilledEn.getId(), "de",
            "same_fr-de_kmelia5_foreignId_43");
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_E)).exists(), is(true));

        assertContentAndFilename(sameFilledFrEn.getId(), "fr", "same_fr-en_kmelia6_foreignId_51");
        assertContentAndFilename(sameFilledFrEn.getId(), "en", null);
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_F)).exists(), is(true));

        assertContentAndFilename(severalSameFilledFrFilledEn_1.getId(), "fr", null);
        assertContentAndFilename(severalSameFilledFrFilledEn_1.getId(), "en",
            "en_content_kmelia7_foreignId_61");
        assertContentAndFilename(severalSameFilledFrFilledEn_2.getId(), "fr",
            "fr_content_kmelia7_foreignId_61");
        assertContentAndFilename(severalSameFilledFrFilledEn_2.getId(), "en", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_1.getId(), "fr", "G_62_A");
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_1.getId(), "en", "G_62_B");
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_1.getId(), "de", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_2.getId(), "fr", "G_62_C");
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_2.getId(), "en", "G_62_D");
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_2.getId(), "de", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_3.getId(), "fr", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_3.getId(), "en", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_3.getId(), "de", "G_62_E");
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_G)).exists(), is(true));

        // Attachments that have not been modified.
        assertAttachementsThatShouldBeNotModified(this);
      }
    }.execute();
  }

  @Test
  public void testPurgeWhenOnlyWysiwygExists() throws Exception {
    new JcrWysiwygPurgerTest() {
      @Override
      public void run() throws Exception {
        /*
        Preparing
         */

        SimpleDocument emptyFr = createEmptyFrWysiwygForTest(instanceId_A, foreignId_1);
        SimpleDocument emptyEn = createEmptyEnWysiwygForTest(instanceId_A, foreignId_2);

        assertContent(emptyFr.getId(), "fr", "");
        assertContent(emptyFr.getId(), "en", null);
        assertContent(emptyEn.getId(), "fr", null);
        assertContent(emptyEn.getId(), "en", "");

        SimpleDocument filledFr = createFrWysiwygForTest(instanceId_B, foreignId_11);
        SimpleDocument filledEn = createEnWysiwygForTest(instanceId_B, foreignId_12);

        assertContent(filledFr.getId(), "fr", "fr_content_kmelia2_foreignId_11");
        assertContent(filledFr.getId(), "en", null);
        assertContent(filledEn.getId(), "fr", null);
        assertContent(filledEn.getId(), "en", "en_content_kmelia2_foreignId_12");

        SimpleDocument emptyFrFilledEn = createEmptyFrWysiwygForTest(instanceId_C, foreignId_21);
        addEnWysiwygForTest(emptyFrFilledEn);
        SimpleDocument filledFrEmptyEn = createFrWysiwygForTest(instanceId_C, foreignId_22);
        addEmptyEnWysiwygForTest(filledFrEmptyEn);

        assertContent(emptyFrFilledEn.getId(), "fr", "");
        assertContent(emptyFrFilledEn.getId(), "en", "en_content_kmelia3_foreignId_21");
        assertContent(filledFrEmptyEn.getId(), "fr", "fr_content_kmelia3_foreignId_22");
        assertContent(filledFrEmptyEn.getId(), "en", "");

        SimpleDocument emptyFrEmptyEn = createEmptyFrWysiwygForTest(instanceId_D, foreignId_31);
        addEmptyEnWysiwygForTest(emptyFrEmptyEn);
        SimpleDocument FilledEnfilledFr = createEnWysiwygForTest(instanceId_D, foreignId_32);
        addFrWysiwygForTest(FilledEnfilledFr);

        assertContent(emptyFrEmptyEn.getId(), "fr", "");
        assertContent(emptyFrEmptyEn.getId(), "en", "");
        assertContent(FilledEnfilledFr.getId(), "fr", "fr_content_kmelia4_foreignId_32");
        assertContent(FilledEnfilledFr.getId(), "en", "en_content_kmelia4_foreignId_32");

        SimpleDocument filledFrEmptyEnEmptyDe = createFrWysiwygForTest(instanceId_E, foreignId_41);
        addEmptyEnWysiwygForTest(filledFrEmptyEnEmptyDe);
        addEmptyDeWysiwygForTest(filledFrEmptyEnEmptyDe);
        SimpleDocument sameFilledFrDeEmptyEn =
            createFreeFrWysiwygForTest(instanceId_E, foreignId_42,
                "same_fr-de_kmelia5_foreignId_42");
        addEmptyEnWysiwygForTest(sameFilledFrDeEmptyEn);
        addFreeDeWysiwygForTest(sameFilledFrDeEmptyEn, "same_fr-de_kmelia5_foreignId_42");
        SimpleDocument sameFilledFrDeFilledEn =
            createFreeFrWysiwygForTest(instanceId_E, foreignId_43,
                "same_fr-de_kmelia5_foreignId_43");
        addEnWysiwygForTest(sameFilledFrDeFilledEn);
        addFreeDeWysiwygForTest(sameFilledFrDeFilledEn, "same_fr-de_kmelia5_foreignId_43");

        assertContent(filledFrEmptyEnEmptyDe.getId(), "fr", "fr_content_kmelia5_foreignId_41");
        assertContent(filledFrEmptyEnEmptyDe.getId(), "en", "");
        assertContent(filledFrEmptyEnEmptyDe.getId(), "de", "");
        assertContent(sameFilledFrDeEmptyEn.getId(), "fr", "same_fr-de_kmelia5_foreignId_42");
        assertContent(sameFilledFrDeEmptyEn.getId(), "en", "");
        assertContent(sameFilledFrDeEmptyEn.getId(), "de", "same_fr-de_kmelia5_foreignId_42");
        assertContent(sameFilledFrDeFilledEn.getId(), "fr", "same_fr-de_kmelia5_foreignId_43");
        assertContent(sameFilledFrDeFilledEn.getId(), "en", "en_content_kmelia5_foreignId_43");
        assertContent(sameFilledFrDeFilledEn.getId(), "de", "same_fr-de_kmelia5_foreignId_43");

        SimpleDocument sameFilledFrEn = createFreeFrWysiwygForTest(instanceId_F, foreignId_51,
            "same_fr-en_kmelia6_foreignId_51");
        addFreeEnWysiwygForTest(sameFilledFrEn, "same_fr-en_kmelia6_foreignId_51");

        assertContent(sameFilledFrEn.getId(), "fr", "same_fr-en_kmelia6_foreignId_51");
        assertContent(sameFilledFrEn.getId(), "en", "same_fr-en_kmelia6_foreignId_51");


        Date aDate = java.sql.Date.valueOf("2014-06-24");
        Date aBeforeDate = DateUtils.addDays(aDate, -4);
        Date aAfterDate = DateUtils.addDays(aDate, 4);

        SimpleDocument severalSameFilledFrFilledEn_1 =
            createFrWysiwygForTest(instanceId_G, foreignId_61);
        severalSameFilledFrFilledEn_1.setCreated(aDate);
        severalSameFilledFrFilledEn_1.setUpdated(null);
        updateAttachmentForTest(severalSameFilledFrFilledEn_1);
        SimpleDocument severalSameFilledFrFilledEn_1_en =
            addEnWysiwygForTest(severalSameFilledFrFilledEn_1);
        severalSameFilledFrFilledEn_1_en.setCreated(aDate);
        severalSameFilledFrFilledEn_1_en.setUpdated(aAfterDate);
        updateAttachmentForTest(severalSameFilledFrFilledEn_1_en);
        SimpleDocument severalSameFilledFrFilledEn_2 =
            createFrWysiwygForTest(instanceId_G, foreignId_61);
        severalSameFilledFrFilledEn_2.setCreated(aBeforeDate);
        severalSameFilledFrFilledEn_2.setUpdated(aAfterDate);
        updateAttachmentForTest(severalSameFilledFrFilledEn_2);
        SimpleDocument severalSameFilledFrFilledEn_2_en =
            addEnWysiwygForTest(severalSameFilledFrFilledEn_2);
        severalSameFilledFrFilledEn_2_en.setCreated(aBeforeDate);
        severalSameFilledFrFilledEn_2_en.setUpdated(aDate);
        updateAttachmentForTest(severalSameFilledFrFilledEn_2_en);

        assertThat(severalSameFilledFrFilledEn_1.getId(),
            not(is(severalSameFilledFrFilledEn_2.getId())));
        assertContent(severalSameFilledFrFilledEn_1.getId(), "fr",
            "fr_content_kmelia7_foreignId_61");
        assertContent(severalSameFilledFrFilledEn_1.getId(), "en",
            "en_content_kmelia7_foreignId_61");
        assertContent(severalSameFilledFrFilledEn_2.getId(), "fr",
            "fr_content_kmelia7_foreignId_61");
        assertContent(severalSameFilledFrFilledEn_2.getId(), "en",
            "en_content_kmelia7_foreignId_61");

        SimpleDocument severalFilledFrFilledEnFilledDe_1 =
            createFreeFrWysiwygForTest(instanceId_G, foreignId_62, "G_62_A");
        severalFilledFrFilledEnFilledDe_1.setCreated(aDate);
        severalFilledFrFilledEnFilledDe_1.setUpdated(null);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_1);
        SimpleDocument severalFilledFrFilledEnFilled_1_en =
            addFreeEnWysiwygForTest(severalFilledFrFilledEnFilledDe_1, "G_62_B");
        severalFilledFrFilledEnFilled_1_en.setCreated(aDate);
        severalFilledFrFilledEnFilled_1_en.setUpdated(aAfterDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilled_1_en);
        SimpleDocument severalFilledFrFilledEnFilledDe_2 =
            createFreeFrWysiwygForTest(instanceId_G, foreignId_62, "G_62_C");
        severalFilledFrFilledEnFilledDe_2.setCreated(aBeforeDate);
        severalFilledFrFilledEnFilledDe_2.setUpdated(aAfterDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_2);
        SimpleDocument severalFilledFrFilledEnFilledDe_2_en =
            addFreeEnWysiwygForTest(severalFilledFrFilledEnFilledDe_2, "G_62_D");
        severalFilledFrFilledEnFilledDe_2_en.setCreated(aBeforeDate);
        severalFilledFrFilledEnFilledDe_2_en.setUpdated(aDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_2_en);
        SimpleDocument severalFilledFrFilledEnFilledDe_2_de =
            addFreeDeWysiwygForTest(severalFilledFrFilledEnFilledDe_2, "G_62_E");
        severalFilledFrFilledEnFilledDe_2_de.setCreated(aBeforeDate);
        severalFilledFrFilledEnFilledDe_2_de.setUpdated(aDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_2_de);
        SimpleDocument severalFilledFrFilledEnFilledDe_3 =
            createFreeDeWysiwygForTest(instanceId_G, foreignId_62, "G_62_E");
        severalFilledFrFilledEnFilledDe_3.setCreated(aBeforeDate);
        severalFilledFrFilledEnFilledDe_3.setUpdated(aAfterDate);
        updateAttachmentForTest(severalFilledFrFilledEnFilledDe_3);

        assertThat(severalFilledFrFilledEnFilledDe_1.getId(),
            not(is(severalFilledFrFilledEnFilledDe_2.getId())));
        assertThat(severalFilledFrFilledEnFilledDe_1.getId(),
            not(is(severalFilledFrFilledEnFilledDe_3.getId())));
        assertThat(severalFilledFrFilledEnFilledDe_2.getId(),
            not(is(severalFilledFrFilledEnFilledDe_3.getId())));
        assertContent(severalFilledFrFilledEnFilledDe_1.getId(), "fr", "G_62_A");
        assertContent(severalFilledFrFilledEnFilledDe_1.getId(), "en", "G_62_B");
        assertContent(severalFilledFrFilledEnFilledDe_1.getId(), "de", null);
        assertContent(severalFilledFrFilledEnFilledDe_2.getId(), "fr", "G_62_C");
        assertContent(severalFilledFrFilledEnFilledDe_2.getId(), "en", "G_62_D");
        assertContent(severalFilledFrFilledEnFilledDe_2.getId(), "de", "G_62_E");
        assertContent(severalFilledFrFilledEnFilledDe_3.getId(), "fr", null);
        assertContent(severalFilledFrFilledEnFilledDe_3.getId(), "en", null);
        assertContent(severalFilledFrFilledEnFilledDe_3.getId(), "de", "G_62_E");

        /*
        The test
         */
        WysiwygPurger purger = new WysiwygPurger(getSimpleDocumentService(), 1);
        purger.setConsole(new Console().setEchoAsDotEnabled(false));
        purger.purgeDocuments();

        /*
        Assertions
         */

        assertDocumentDoesNotExist(emptyFr.getId());
        assertDocumentDoesNotExist(emptyEn.getId());
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_A)).exists(), is(false));

        assertContentAndFilename(filledFr.getId(), "fr", "fr_content_kmelia2_foreignId_11");
        assertContentAndFilename(filledFr.getId(), "en", null);
        assertContentAndFilename(filledEn.getId(), "fr", null);
        assertContentAndFilename(filledEn.getId(), "en", "en_content_kmelia2_foreignId_12");
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_B)).exists(), is(true));

        assertContentAndFilename(emptyFrFilledEn.getId(), "fr", null);
        assertContentAndFilename(emptyFrFilledEn.getId(), "en", "en_content_kmelia3_foreignId_21");
        assertContentAndFilename(filledFrEmptyEn.getId(), "fr", "fr_content_kmelia3_foreignId_22");
        assertContentAndFilename(filledFrEmptyEn.getId(), "en", null);
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_C)).exists(), is(true));

        assertDocumentDoesNotExist(emptyFrEmptyEn.getId());
        assertContentAndFilename(FilledEnfilledFr.getId(), "fr", "fr_content_kmelia4_foreignId_32");
        assertContentAndFilename(FilledEnfilledFr.getId(), "en", "en_content_kmelia4_foreignId_32");
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_D)).exists(), is(true));

        assertContentAndFilename(filledFrEmptyEnEmptyDe.getId(), "fr",
            "fr_content_kmelia5_foreignId_41");
        assertContentAndFilename(filledFrEmptyEnEmptyDe.getId(), "en", null);
        assertContentAndFilename(filledFrEmptyEnEmptyDe.getId(), "de", null);
        assertContentAndFilename(sameFilledFrDeEmptyEn.getId(), "fr",
            "same_fr-de_kmelia5_foreignId_42");
        assertContentAndFilename(sameFilledFrDeEmptyEn.getId(), "en", null);
        assertContentAndFilename(sameFilledFrDeEmptyEn.getId(), "de", null);
        assertContentAndFilename(sameFilledFrDeFilledEn.getId(), "fr",
            "same_fr-de_kmelia5_foreignId_43");
        assertContentAndFilename(sameFilledFrDeFilledEn.getId(), "en",
            "en_content_kmelia5_foreignId_43");
        assertContentAndFilename(sameFilledFrDeFilledEn.getId(), "de",
            "same_fr-de_kmelia5_foreignId_43");
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_E)).exists(), is(true));

        assertContentAndFilename(sameFilledFrEn.getId(), "fr", "same_fr-en_kmelia6_foreignId_51");
        assertContentAndFilename(sameFilledFrEn.getId(), "en", null);
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_F)).exists(), is(true));

        assertContentAndFilename(severalSameFilledFrFilledEn_1.getId(), "fr", null);
        assertContentAndFilename(severalSameFilledFrFilledEn_1.getId(), "en",
            "en_content_kmelia7_foreignId_61");
        assertContentAndFilename(severalSameFilledFrFilledEn_2.getId(), "fr",
            "fr_content_kmelia7_foreignId_61");
        assertContentAndFilename(severalSameFilledFrFilledEn_2.getId(), "en", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_1.getId(), "fr", "G_62_A");
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_1.getId(), "en", "G_62_B");
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_1.getId(), "de", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_2.getId(), "fr", "G_62_C");
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_2.getId(), "en", "G_62_D");
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_2.getId(), "de", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_3.getId(), "fr", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_3.getId(), "en", null);
        assertContentAndFilename(severalFilledFrFilledEnFilledDe_3.getId(), "de", "G_62_E");
        assertThat(new File(FileUtil.getAbsolutePath(instanceId_G)).exists(), is(true));
      }
    }.execute();
  }

  @Test
  public void testPurgeWithInconsistentData() throws Exception {
    new JcrWysiwygPurgerTest() {
      @Override
      public void run() throws Exception {
        /*
        Preparing
         */

        SimpleDocument fr = createFrWysiwygForTest(instanceId_A, foreignId_1);
        addEnWysiwygForTest(fr);
        addFreeDeWysiwygForTest(fr, "de_content");
        SimpleDocument en = createEnWysiwygForTest(instanceId_A, foreignId_2);
        addFrWysiwygForTest(en);

        assertContent(fr.getId(), "fr", "fr_content_kmelia1_foreignId_1");
        assertContent(fr.getId(), "en", "en_content_kmelia1_foreignId_1");
        assertContent(fr.getId(), "de", "de_content");
        assertContent(en.getId(), "fr", "fr_content_kmelia1_foreignId_2");
        assertContent(en.getId(), "en", "en_content_kmelia1_foreignId_2");
        assertContent(en.getId(), "de", null);

        // Inconsistent JCR data
        new File(fr.getAttachmentPath()).delete();

        /*
        The test
         */
        WysiwygPurger purger = new WysiwygPurger(getSimpleDocumentService(), 1);
        purger.setConsole(new Console().setEchoAsDotEnabled(false));
        purger.purgeDocuments();

        /*
        No exception
         */
      }
    }.execute();
  }

  /**
   * Centralization for purge tests.
   * @param test
   * @throws Exception
   */
  private void createAttachmentsThatWillNotBeModified(JcrWysiwygPurgerTest test) throws Exception {
    id_att_A_1 =
        test.createAttachmentForTest(test.defaultDocumentBuilder(instanceId_A, foreignId_1),
            test.defaultFRContentBuilder(), "").getId();
    id_att_A_2 =
        test.createAttachmentForTest(test.defaultDocumentBuilder(instanceId_A, foreignId_2),
            test.defaultFRContentBuilder(), "").getId();
    SimpleDocument simpleDocument_B =
        test.createAttachmentForTest(test.defaultDocumentBuilder(instanceId_B, foreignId_11),
            test.defaultFRContentBuilder(), "dummyContent");
    id_att_B_11 = simpleDocument_B.getId();
    test.updateAttachmentForTest(simpleDocument_B, "en", "dummyContent");
    id_att_B_12 =
        test.createAttachmentForTest(test.defaultDocumentBuilder(instanceId_B, foreignId_12),
            test.defaultFRContentBuilder(), "").getId();
    id_att_C_21 =
        test.createAttachmentForTest(test.defaultDocumentBuilder(instanceId_C, foreignId_21),
            test.defaultFRContentBuilder(), "").getId();
    id_att_C_22 =
        test.createAttachmentForTest(test.defaultDocumentBuilder(instanceId_C, foreignId_22),
            test.defaultFRContentBuilder(), "").getId();
  }

  /**
   * Centralization for purge tests.
   * @param test
   * @throws Exception
   */
  private void assertAttachementsThatShouldBeNotModified(JcrWysiwygPurgerTest test)
      throws Exception {
    SimpleDocument document_att_A_1_fr = test.getDocumentById(id_att_A_1, "fr");
    assertThat(document_att_A_1_fr, notNullValue());
    assertThat(document_att_A_1_fr.getAttachment(), notNullValue());
    SimpleDocument document_att_A_1_en = test.getDocumentById(id_att_A_1, "en");
    assertThat(document_att_A_1_en, notNullValue());
    assertThat(document_att_A_1_en.getAttachment(), nullValue());
    SimpleDocument document_att_A_2_fr = test.getDocumentById(id_att_A_2, "fr");
    assertThat(document_att_A_2_fr, notNullValue());
    assertThat(document_att_A_2_fr.getAttachment(), notNullValue());
    SimpleDocument document_att_A_2_en = test.getDocumentById(id_att_A_2, "en");
    assertThat(document_att_A_2_en, notNullValue());
    assertThat(document_att_A_2_en.getAttachment(), nullValue());
    //---
    SimpleDocument document_att_B_11_fr = test.getDocumentById(id_att_B_11, "fr");
    assertThat(document_att_B_11_fr, notNullValue());
    assertThat(document_att_B_11_fr.getAttachment(), notNullValue());
    SimpleDocument document_att_B_11_en = test.getDocumentById(id_att_B_11, "en");
    assertThat(document_att_B_11_en, notNullValue());
    assertThat(document_att_B_11_en.getAttachment(), notNullValue());
    SimpleDocument document_att_B_12_fr = test.getDocumentById(id_att_B_12, "fr");
    assertThat(document_att_B_12_fr, notNullValue());
    assertThat(document_att_B_12_fr.getAttachment(), notNullValue());
    SimpleDocument document_att_B_12_en = test.getDocumentById(id_att_B_12, "en");
    assertThat(document_att_B_12_en, notNullValue());
    assertThat(document_att_B_12_en.getAttachment(), nullValue());
    //---
    SimpleDocument document_att_C_21_fr = test.getDocumentById(id_att_C_21, "fr");
    assertThat(document_att_C_21_fr, notNullValue());
    assertThat(document_att_C_21_fr.getAttachment(), notNullValue());
    SimpleDocument document_att_C_21_en = test.getDocumentById(id_att_C_21, "en");
    assertThat(document_att_C_21_en, notNullValue());
    assertThat(document_att_C_21_en.getAttachment(), nullValue());
    SimpleDocument document_att_C_22_fr = test.getDocumentById(id_att_C_22, "fr");
    assertThat(document_att_C_22_fr, notNullValue());
    assertThat(document_att_C_22_fr.getAttachment(), notNullValue());
    SimpleDocument document_att_C_22_en = test.getDocumentById(id_att_C_22, "en");
    assertThat(document_att_C_22_en, notNullValue());
    assertThat(document_att_C_22_en.getAttachment(), nullValue());
  }

  /**
   * Executing an adapted JcrTest
   */
  private abstract class JcrWysiwygPurgerTest extends JcrTest {

    public SimpleDocument assertContentAndFilename(final String uuId, final String language,
        final String expectedContent) throws Exception {
      SimpleDocument document = super.assertContent(uuId, language, expectedContent);
      if (document != null && document.getAttachment() != null) {
        String fileLanguage =
            ConverterUtil.checkLanguage(ConverterUtil.extractLanguage(document.getFilename()));
        assertThat(document.getFilename(), fileLanguage, is(language));
      }
      return document;
    }

    protected SimpleDocument createEmptyEnWysiwygForTest(String instanceId, String foreignId)
        throws Exception {
      return createWysiwygForTest(instanceId, foreignId, defaultENContentBuilder(), "en", "");
    }

    protected SimpleDocument createEmptyFrWysiwygForTest(String instanceId, String foreignId)
        throws Exception {
      return createWysiwygForTest(instanceId, foreignId, defaultFRContentBuilder(), "fr", "");
    }

    protected SimpleDocument createEnWysiwygForTest(String instanceId, String foreignId)
        throws Exception {
      return createWysiwygForTest(instanceId, foreignId, defaultENContentBuilder(), "en", null);
    }

    protected SimpleDocument createFrWysiwygForTest(String instanceId, String foreignId)
        throws Exception {
      return createWysiwygForTest(instanceId, foreignId, defaultFRContentBuilder(), "fr", null);
    }

    protected SimpleDocument createFreeFrWysiwygForTest(String instanceId, String foreignId,
        String content) throws Exception {
      return createWysiwygForTest(instanceId, foreignId, defaultFRContentBuilder(), "fr", content);
    }

    protected SimpleDocument createFreeDeWysiwygForTest(String instanceId, String foreignId,
        String content) throws Exception {
      return createWysiwygForTest(instanceId, foreignId,
          defaultFRContentBuilder().setLanguage("de"), "de", content);
    }

    protected SimpleDocument addEmptyEnWysiwygForTest(SimpleDocument document) throws Exception {
      return addWysiwygForTest(document, "en", "");
    }

    protected SimpleDocument addFrWysiwygForTest(SimpleDocument document) throws Exception {
      return addWysiwygForTest(document, "fr", null);
    }

    protected SimpleDocument addEnWysiwygForTest(SimpleDocument document) throws Exception {
      return addWysiwygForTest(document, "en", null);
    }

    protected SimpleDocument addFreeEnWysiwygForTest(SimpleDocument document, String content)
        throws Exception {
      return addWysiwygForTest(document, "en", content);
    }

    protected SimpleDocument addEmptyDeWysiwygForTest(SimpleDocument document) throws Exception {
      return addWysiwygForTest(document, "de", "");
    }

    protected SimpleDocument addFreeDeWysiwygForTest(SimpleDocument document, String content)
        throws Exception {
      return addWysiwygForTest(document, "de", content);
    }

    private SimpleDocument addWysiwygForTest(SimpleDocument document, String language,
        final String emptyContent) throws Exception {
      String content = emptyContent;
      if (!StringUtil.isDefined(content)) {
        if (content != null && content.isEmpty()) {
          content = "";
        } else {
          content =
              language + "_content_" + document.getInstanceId() + "_" + document.getForeignId();
        }
      }
      return updateAttachmentForTest(document, language, content);
    }

    private SimpleDocument createWysiwygForTest(String instanceId, String foreignId,
        SimpleAttachmentBuilder attachmentBuilder, String language, final String emptyContent)
        throws Exception {
      String content = emptyContent;
      if (!StringUtil.isDefined(content)) {
        if (content != null && content.isEmpty()) {
          content = "";
        } else {
          content = language + "_content_" + instanceId + "_" + foreignId;
        }
      }
      String languageSuffix = "_" + language;
      if (language.equals("fr") && (System.currentTimeMillis() % 10) == 0) {
        languageSuffix = "";
      }
      return createAttachmentForTest(
          defaultDocumentBuilder(instanceId, foreignId).setDocumentType(wysiwyg),
          attachmentBuilder.setFilename(foreignId + "wysiwyg" + languageSuffix + ".txt")
              .setSize(content.length()), content
      );
    }
  }
}