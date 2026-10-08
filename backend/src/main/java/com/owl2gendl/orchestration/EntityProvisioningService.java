package com.owl2gendl.orchestration;

import org.springframework.stereotype.Service;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.context.GenerationContext;

/** Seeds a {@link GenerationContext}'s entity pools before axiom generation begins. */
@Service
public class EntityProvisioningService {

	public void seed(GenerationContext ctx, EntityCountsDto counts) {
		for (int i = 0; i < counts.classes(); i++) {
			ctx.pools().classes().mintNew();
		}
		for (int i = 0; i < counts.objectProperties(); i++) {
			ctx.pools().objectProperties().mintNew();
		}
		for (int i = 0; i < counts.dataProperties(); i++) {
			ctx.pools().dataProperties().mintNew();
		}
		for (int i = 0; i < counts.individuals(); i++) {
			ctx.pools().individuals().mintNew();
		}
	}
}
