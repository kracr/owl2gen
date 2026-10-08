package com.owl2gendl.catalog;

/** How many distinct entities of each type a construct (or a whole selection) needs at minimum. */
public record EntityRequirement(int classes, int objectProperties, int dataProperties, int individuals) {

	public static EntityRequirement none() {
		return new EntityRequirement(0, 0, 0, 0);
	}

	public EntityRequirement max(EntityRequirement other) {
		return new EntityRequirement(
				Math.max(classes, other.classes),
				Math.max(objectProperties, other.objectProperties),
				Math.max(dataProperties, other.dataProperties),
				Math.max(individuals, other.individuals));
	}
}
