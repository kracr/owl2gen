package com.owl2gendl.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.owl2gendl.api.dto.CatalogDto;
import com.owl2gendl.api.dto.CategoryDto;
import com.owl2gendl.api.dto.ConstructDescriptorDto;
import com.owl2gendl.catalog.ConstructCatalog;
import com.owl2gendl.catalog.ConstructCategory;
import com.owl2gendl.catalog.ConstructId;

@RestController
@CrossOrigin(origins = {"http://localhost:5173"})
public class CatalogController {

	private final ConstructCatalog catalog;

	public CatalogController(ConstructCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/api/catalog")
	public CatalogDto getCatalog() {
		List<CategoryDto> categories = catalog.byCategory().entrySet().stream()
				.map(this::toCategoryDto)
				.toList();
		return new CatalogDto(categories);
	}

	private CategoryDto toCategoryDto(Map.Entry<ConstructCategory, List<ConstructId>> entry) {
		List<ConstructDescriptorDto> constructs = entry.getValue().stream()
				.map(id -> new ConstructDescriptorDto(
						id.name(),
						id.displayName(),
						id.description(),
						id.supportsNesting(),
						id.requiresIndividuals(),
						id.elEligible()))
				.toList();
		return new CategoryDto(entry.getKey().name(), entry.getKey().displayName(), constructs);
	}
}
