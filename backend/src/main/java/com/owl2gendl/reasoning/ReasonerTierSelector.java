package com.owl2gendl.reasoning;

import org.springframework.stereotype.Component;

/**
 * Picks a reasoner tier: an explicit override always wins, otherwise the default (Openllet — the only tier
 * for now). Kept as its own component rather than inlined so a future second tier (see {@link ReasonerTier})
 * has a natural place to add real profile-based selection logic.
 */
@Component
public class ReasonerTierSelector {

	public ReasonerTier select(ReasonerTier override) {
		return override != null ? override : ReasonerTier.OPENLLET;
	}
}
