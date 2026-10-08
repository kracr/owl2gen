package com.owl2gendl.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.api.dto.GenerationRequestDto;

class RequestValidatorTest {

	private final RequestValidator validator = new RequestValidator();

	private GenerationRequestDto requestWithConstructs(Map<String, Integer> constructs) {
		return new GenerationRequestDto(new EntityCountsDto(5, 5, 5, 5), constructs, null, null, null, null, null, null);
	}

	@Test
	void acceptsAValidRequest() {
		assertDoesNotThrow(() -> validator.validate(requestWithConstructs(Map.of("DISJOINT_WITH", 10, "SUB_CLASS_OF", 20))));
	}

	@Test
	void rejectsAnUnknownConstructId() {
		InvalidRequestException e = assertThrows(InvalidRequestException.class,
				() -> validator.validate(requestWithConstructs(Map.of("NOT_A_REAL_CONSTRUCT", 5))));
		assertThatMessageMentions(e, "NOT_A_REAL_CONSTRUCT");
	}

	@Test
	void rejectsAnUnknownTopologyVariant() {
		GenerationRequestDto request = new GenerationRequestDto(new EntityCountsDto(5, 5, 5, 5),
				Map.of("DISJOINT_WITH", 5), null, null, List.of("NOT_A_REAL_VARIANT"), null, null, null);
		InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> validator.validate(request));
		assertThatMessageMentions(e, "NOT_A_REAL_VARIANT");
	}

	@Test
	void rejectsAnUnknownTargetProfile() {
		GenerationRequestDto request = new GenerationRequestDto(new EntityCountsDto(5, 5, 5, 5),
				Map.of("DISJOINT_WITH", 5), null, null, null, null, "NOT_A_REAL_PROFILE", null);
		assertThrows(InvalidRequestException.class, () -> validator.validate(request));
	}

	@Test
	void acceptsElEligibleConstructsUnderElProfile() {
		GenerationRequestDto request = new GenerationRequestDto(new EntityCountsDto(5, 5, 5, 5),
				Map.of("DISJOINT_WITH", 5, "SUB_CLASS_OF", 5, "OBJECT_SOME_VALUES_FROM", 5), null, null, null, null, "EL", null);
		assertDoesNotThrow(() -> validator.validate(request));
	}

	@Test
	void rejectsElIneligibleConstructsUnderElProfile() {
		// ObjectUnionOf is not legal in OWL 2 EL (see ConstructId.OBJECT_UNION_OF.elEligible()).
		GenerationRequestDto request = new GenerationRequestDto(new EntityCountsDto(5, 5, 5, 5),
				Map.of("DISJOINT_WITH", 5, "OBJECT_UNION_OF", 5), null, null, null, null, "EL", null);
		InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> validator.validate(request));
		assertThatMessageMentions(e, "OBJECT_UNION_OF");
	}

	@Test
	void rejectsTotalRequestedAxiomsOverTheCap() {
		Map<String, Integer> constructs = Map.of("DISJOINT_WITH", RequestValidator.MAX_TOTAL_REQUESTED_AXIOMS + 1);
		assertThrows(InvalidRequestException.class, () -> validator.validate(requestWithConstructs(constructs)));
	}

	@Test
	void acceptsExactlyTheMaximum() {
		Map<String, Integer> constructs = Map.of("DISJOINT_WITH", RequestValidator.MAX_TOTAL_REQUESTED_AXIOMS);
		assertDoesNotThrow(() -> validator.validate(requestWithConstructs(constructs)));
	}

	@Test
	void acceptsNullEntityCounts() {
		// Entity counts are optional - a null EntityCountsDto (or any null field within it) is filled in
		// later by EntityCountsResolver, never rejected here, even for a selection needing several entities.
		GenerationRequestDto request = new GenerationRequestDto(null, Map.of("ALL_DISJOINT_CLASSES", 5), null, null, null, null, null, null);
		assertDoesNotThrow(() -> validator.validate(request));
	}

	@Test
	void acceptsPartiallyNullEntityCounts() {
		GenerationRequestDto request = new GenerationRequestDto(new EntityCountsDto(null, 5, null, null),
				Map.of("ALL_DISJOINT_CLASSES", 5), null, null, null, null, null, null);
		assertDoesNotThrow(() -> validator.validate(request));
	}

	@Test
	void rejectsExplicitClassCountTooLowForAllDisjointClasses() {
		GenerationRequestDto request = new GenerationRequestDto(new EntityCountsDto(2, 5, 5, 5),
				Map.of("ALL_DISJOINT_CLASSES", 5), null, null, null, null, null, null);
		InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> validator.validate(request));
		assertThatMessageMentions(e, "All Disjoint Classes");
		assertThatMessageMentions(e, "at least 3");
	}

	@Test
	void rejectsExplicitObjectPropertyCountTooLowForSubObjectPropertyOf() {
		GenerationRequestDto request = new GenerationRequestDto(new EntityCountsDto(5, 1, 5, 5),
				Map.of("SUB_OBJECT_PROPERTY_OF", 5), null, null, null, null, null, null);
		InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> validator.validate(request));
		assertThatMessageMentions(e, "at least 2");
	}

	@Test
	void acceptsExplicitCountsThatMeetTheMinimum() {
		GenerationRequestDto request = new GenerationRequestDto(new EntityCountsDto(3, 5, 5, 5),
				Map.of("ALL_DISJOINT_CLASSES", 5), null, null, null, null, null, null);
		assertDoesNotThrow(() -> validator.validate(request));
	}

	private void assertThatMessageMentions(Exception e, String text) {
		if (!e.getMessage().contains(text)) {
			throw new AssertionError("Expected message to mention '" + text + "' but was: " + e.getMessage());
		}
	}
}
