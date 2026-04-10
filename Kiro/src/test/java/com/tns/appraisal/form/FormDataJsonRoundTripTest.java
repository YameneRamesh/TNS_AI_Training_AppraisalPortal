package com.tns.appraisal.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Dedicated unit tests for FormData JSON round-trips via FormDataConverter.
 * No Spring context required — pure unit tests.
 */
class FormDataJsonRoundTripTest {

    private FormDataConverter converter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        converter = new FormDataConverter();
        objectMapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // 1. Full round-trip with all sections populated
    // -------------------------------------------------------------------------

    @Test
    void convertToDatabaseColumn_fullFormData_allFieldsSurviveRoundTrip() {
        FormData original = buildFullFormData();

        String json = converter.convertToDatabaseColumn(original);
        assertThat(json).isNotBlank();

        FormData restored = converter.convertToEntityAttribute(json);

        // Header
        assertThat(restored.getHeader().getDateOfHire()).isEqualTo("2020-01-15");
        assertThat(restored.getHeader().getDateOfReview()).isEqualTo("2025-04-01");
        assertThat(restored.getHeader().getReviewPeriod()).isEqualTo("2024-25");
        assertThat(restored.getHeader().getTypeOfReview()).isEqualTo("Annual");

        // keyResponsibilities (3 items)
        assertThat(restored.getKeyResponsibilities()).hasSize(3);
        assertThat(restored.getKeyResponsibilities().get(0).getItemId()).isEqualTo("kr_1");
        assertThat(restored.getKeyResponsibilities().get(0).getSelfRating()).isEqualTo(Rating.MEETS);
        assertThat(restored.getKeyResponsibilities().get(0).getManagerRating()).isEqualTo(Rating.EXCEEDS);
        assertThat(restored.getKeyResponsibilities().get(1).getItemId()).isEqualTo("kr_2");
        assertThat(restored.getKeyResponsibilities().get(2).getItemId()).isEqualTo("kr_3");

        // IDP (3 items)
        assertThat(restored.getIdp()).hasSize(3);
        assertThat(restored.getIdp().get(0).getItemId()).isEqualTo("idp_1");
        assertThat(restored.getIdp().get(0).getSelfRating()).isEqualTo(Rating.EXCELS);

        // PolicyAdherence
        assertThat(restored.getPolicyAdherence().getHrPolicy().getManagerRating()).isEqualTo(8);
        assertThat(restored.getPolicyAdherence().getAvailability().getManagerRating()).isEqualTo(9);
        assertThat(restored.getPolicyAdherence().getAdditionalSupport().getManagerRating()).isEqualTo(7);
        assertThat(restored.getPolicyAdherence().getManagerComments()).isEqualTo("Good adherence");

        // Goals (2 items)
        assertThat(restored.getGoals()).hasSize(2);
        assertThat(restored.getGoals().get(0).getItemId()).isEqualTo("goal_1");
        assertThat(restored.getGoals().get(1).getItemId()).isEqualTo("goal_2");

        // nextYearGoals
        assertThat(restored.getNextYearGoals()).isEqualTo("Grow leadership skills");

        // OverallEvaluation
        assertThat(restored.getOverallEvaluation().getManagerComments()).isEqualTo("Strong performer");
        assertThat(restored.getOverallEvaluation().getTeamMemberComments()).isEqualTo("Great year");

        // Signature
        assertThat(restored.getSignature().getPreparedBy()).isEqualTo("Manager Name");
        assertThat(restored.getSignature().getReviewedBy()).isEqualTo("HR Name");
        assertThat(restored.getSignature().getTeamMemberAcknowledgement()).isEqualTo("Employee Name");
    }

    // -------------------------------------------------------------------------
    // 2. Partial/draft round-trip — only header and nextYearGoals set
    // -------------------------------------------------------------------------

    @Test
    void convertToDatabaseColumn_partialDraft_nullFieldsPreservedWithoutNPE() {
        FormData draft = new FormData();

        FormData.Header header = new FormData.Header();
        header.setDateOfHire("2021-06-01");
        header.setTypeOfReview("Mid-Year");
        draft.setHeader(header);
        draft.setNextYearGoals("Learn new frameworks");

        String json = converter.convertToDatabaseColumn(draft);
        assertThat(json).isNotBlank();

        FormData restored = converter.convertToEntityAttribute(json);

        assertThat(restored.getHeader().getDateOfHire()).isEqualTo("2021-06-01");
        assertThat(restored.getHeader().getTypeOfReview()).isEqualTo("Mid-Year");
        assertThat(restored.getNextYearGoals()).isEqualTo("Learn new frameworks");
        assertThat(restored.getKeyResponsibilities()).isNull();
        assertThat(restored.getIdp()).isNull();
        assertThat(restored.getPolicyAdherence()).isNull();
        assertThat(restored.getGoals()).isNull();
        assertThat(restored.getOverallEvaluation()).isNull();
        assertThat(restored.getSignature()).isNull();
    }

    // -------------------------------------------------------------------------
    // 3. Rating enum serialization — each value serializes to its display name
    // -------------------------------------------------------------------------

    @Test
    void rating_serialization_producesDisplayName() throws Exception {
        assertThat(objectMapper.writeValueAsString(Rating.EXCELS)).isEqualTo("\"Excels\"");
        assertThat(objectMapper.writeValueAsString(Rating.EXCEEDS)).isEqualTo("\"Exceeds\"");
        assertThat(objectMapper.writeValueAsString(Rating.MEETS)).isEqualTo("\"Meets\"");
        assertThat(objectMapper.writeValueAsString(Rating.DEVELOPING)).isEqualTo("\"Developing\"");
    }

    @Test
    void rating_deserialization_fromDisplayName_returnsCorrectEnum() throws Exception {
        assertThat(objectMapper.readValue("\"Excels\"", Rating.class)).isEqualTo(Rating.EXCELS);
        assertThat(objectMapper.readValue("\"Exceeds\"", Rating.class)).isEqualTo(Rating.EXCEEDS);
        assertThat(objectMapper.readValue("\"Meets\"", Rating.class)).isEqualTo(Rating.MEETS);
        assertThat(objectMapper.readValue("\"Developing\"", Rating.class)).isEqualTo(Rating.DEVELOPING);
    }

    // -------------------------------------------------------------------------
    // 4. Rating case-insensitive deserialization
    // -------------------------------------------------------------------------

    @Test
    void rating_deserialization_caseInsensitive_allVariantsMapToExcels() throws Exception {
        assertThat(objectMapper.readValue("\"excels\"", Rating.class)).isEqualTo(Rating.EXCELS);
        assertThat(objectMapper.readValue("\"EXCELS\"", Rating.class)).isEqualTo(Rating.EXCELS);
        assertThat(objectMapper.readValue("\"Excels\"", Rating.class)).isEqualTo(Rating.EXCELS);
    }

    @Test
    void rating_deserialization_enumNameFallback_uppercaseNameDeserializes() throws Exception {
        // @JsonCreator also accepts the enum name (e.g. "MEETS" maps to Rating.MEETS)
        assertThat(objectMapper.readValue("\"MEETS\"", Rating.class)).isEqualTo(Rating.MEETS);
        assertThat(objectMapper.readValue("\"meets\"", Rating.class)).isEqualTo(Rating.MEETS);
    }

    // -------------------------------------------------------------------------
    // 5. PolicyScore 1–10 range round-trip
    // -------------------------------------------------------------------------

    @Test
    void convertToDatabaseColumn_policyScoreRange_integerRatingsSurviveRoundTrip() {
        for (int rating = 1; rating <= 10; rating++) {
            FormData formData = new FormData();
            FormData.PolicyAdherence pa = new FormData.PolicyAdherence();
            FormData.PolicyScore score = new FormData.PolicyScore();
            score.setManagerRating(rating);
            pa.setHrPolicy(score);
            formData.setPolicyAdherence(pa);

            String json = converter.convertToDatabaseColumn(formData);
            FormData restored = converter.convertToEntityAttribute(json);

            assertThat(restored.getPolicyAdherence().getHrPolicy().getManagerRating())
                    .as("PolicyScore managerRating=%d should survive round-trip", rating)
                    .isEqualTo(rating);
        }
    }

    // -------------------------------------------------------------------------
    // 6. Empty lists round-trip — serialize/deserialize as empty lists, not null
    // -------------------------------------------------------------------------

    @Test
    void convertToDatabaseColumn_emptyLists_deserializeAsEmptyListsNotNull() {
        FormData formData = new FormData();
        formData.setKeyResponsibilities(new ArrayList<>());
        formData.setIdp(new ArrayList<>());
        formData.setGoals(new ArrayList<>());

        String json = converter.convertToDatabaseColumn(formData);
        FormData restored = converter.convertToEntityAttribute(json);

        assertThat(restored.getKeyResponsibilities()).isNotNull().isEmpty();
        assertThat(restored.getIdp()).isNotNull().isEmpty();
        assertThat(restored.getGoals()).isNotNull().isEmpty();
    }

    // -------------------------------------------------------------------------
    // 7. Unknown JSON fields are ignored
    // -------------------------------------------------------------------------

    @Test
    void convertToEntityAttribute_unknownJsonFields_deserializesWithoutError() {
        String jsonWithUnknownFields = """
                {
                  "unknownField": "value",
                  "nextYearGoals": "Improve skills",
                  "anotherUnknown": 42,
                  "header": {
                    "typeOfReview": "Annual",
                    "legacyField": "ignored"
                  }
                }
                """;

        FormData restored = converter.convertToEntityAttribute(jsonWithUnknownFields);

        assertThat(restored).isNotNull();
        assertThat(restored.getNextYearGoals()).isEqualTo("Improve skills");
        assertThat(restored.getHeader().getTypeOfReview()).isEqualTo("Annual");
    }

    // -------------------------------------------------------------------------
    // 8. Null converter inputs
    // -------------------------------------------------------------------------

    @Test
    void convertToDatabaseColumn_nullInput_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_nullInput_returnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_emptyString_returnsNull() {
        assertThat(converter.convertToEntityAttribute("")).isNull();
    }

    @Test
    void convertToEntityAttribute_blankString_returnsNull() {
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }

    // -------------------------------------------------------------------------
    // 9. Invalid JSON throws IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void convertToEntityAttribute_invalidJson_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("not-valid-json{{{"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // 10. Multiple RatedItems preserve insertion order
    // -------------------------------------------------------------------------

    @Test
    void convertToDatabaseColumn_multipleRatedItems_preservesInsertionOrder() {
        FormData formData = new FormData();
        List<FormData.RatedItem> items = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            FormData.RatedItem item = new FormData.RatedItem();
            item.setItemId("item_" + i);
            items.add(item);
        }
        formData.setKeyResponsibilities(items);

        String json = converter.convertToDatabaseColumn(formData);
        FormData restored = converter.convertToEntityAttribute(json);

        List<FormData.RatedItem> restoredItems = restored.getKeyResponsibilities();
        assertThat(restoredItems).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(restoredItems.get(i).getItemId()).isEqualTo("item_" + (i + 1));
        }
    }

    // -------------------------------------------------------------------------
    // 11. Special characters in text fields
    // -------------------------------------------------------------------------

    @Test
    void convertToDatabaseColumn_specialCharactersInTextFields_surviveRoundTripWithoutCorruption() {
        FormData formData = new FormData();
        String specialText = "Unicode: \u00e9\u00e0\u00fc | Quotes: \"hello\" | Newline:\nLine2 | HTML: <b>bold</b> & more";
        formData.setNextYearGoals(specialText);

        FormData.OverallEvaluation eval = new FormData.OverallEvaluation();
        eval.setManagerComments("Tab:\there | Backslash: C:\\path\\file");
        formData.setOverallEvaluation(eval);

        String json = converter.convertToDatabaseColumn(formData);
        FormData restored = converter.convertToEntityAttribute(json);

        assertThat(restored.getNextYearGoals()).isEqualTo(specialText);
        assertThat(restored.getOverallEvaluation().getManagerComments())
                .isEqualTo("Tab:\there | Backslash: C:\\path\\file");
    }

    // -------------------------------------------------------------------------
    // 12. Idempotent serialization — same JSON produced on repeated calls
    // -------------------------------------------------------------------------

    @Test
    void convertToDatabaseColumn_sameInput_producesIdenticalJsonOnRepeatedCalls() {
        FormData formData = buildFullFormData();

        String json1 = converter.convertToDatabaseColumn(formData);
        String json2 = converter.convertToDatabaseColumn(formData);

        assertThat(json1).isEqualTo(json2);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private FormData buildFullFormData() {
        FormData formData = new FormData();

        FormData.Header header = new FormData.Header();
        header.setDateOfHire("2020-01-15");
        header.setDateOfReview("2025-04-01");
        header.setReviewPeriod("2024-25");
        header.setTypeOfReview("Annual");
        formData.setHeader(header);

        formData.setKeyResponsibilities(List.of(
                ratedItem("kr_1", "Did well on KR1", Rating.MEETS, "Agreed", Rating.EXCEEDS),
                ratedItem("kr_2", "Solid delivery", Rating.EXCEEDS, "Excellent", Rating.EXCELS),
                ratedItem("kr_3", "Needs improvement", Rating.DEVELOPING, "Work on this", Rating.DEVELOPING)
        ));

        formData.setIdp(List.of(
                ratedItem("idp_1", "Completed training", Rating.EXCELS, "Great", Rating.EXCELS),
                ratedItem("idp_2", "Partial completion", Rating.MEETS, "OK", Rating.MEETS),
                ratedItem("idp_3", "Started course", Rating.DEVELOPING, "Continue", Rating.DEVELOPING)
        ));

        FormData.PolicyAdherence pa = new FormData.PolicyAdherence();
        pa.setHrPolicy(policyScore(8));
        pa.setAvailability(policyScore(9));
        pa.setAdditionalSupport(policyScore(7));
        pa.setManagerComments("Good adherence");
        formData.setPolicyAdherence(pa);

        formData.setGoals(List.of(
                ratedItem("goal_1", "Achieved goal 1", Rating.MEETS, "On track", Rating.MEETS),
                ratedItem("goal_2", "Exceeded goal 2", Rating.EXCEEDS, "Impressive", Rating.EXCEEDS)
        ));

        formData.setNextYearGoals("Grow leadership skills");

        FormData.OverallEvaluation eval = new FormData.OverallEvaluation();
        eval.setManagerComments("Strong performer");
        eval.setTeamMemberComments("Great year");
        formData.setOverallEvaluation(eval);

        FormData.Signature sig = new FormData.Signature();
        sig.setPreparedBy("Manager Name");
        sig.setReviewedBy("HR Name");
        sig.setTeamMemberAcknowledgement("Employee Name");
        formData.setSignature(sig);

        return formData;
    }

    private FormData.RatedItem ratedItem(String itemId, String selfComment, Rating selfRating,
                                          String managerComment, Rating managerRating) {
        FormData.RatedItem item = new FormData.RatedItem();
        item.setItemId(itemId);
        item.setSelfComment(selfComment);
        item.setSelfRating(selfRating);
        item.setManagerComment(managerComment);
        item.setManagerRating(managerRating);
        return item;
    }

    private FormData.PolicyScore policyScore(int rating) {
        FormData.PolicyScore score = new FormData.PolicyScore();
        score.setManagerRating(rating);
        return score;
    }
}
