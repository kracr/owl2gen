package com.owl2gendl.topology;

public final class VariantStrategyFactory {

	private VariantStrategyFactory() {
	}

	public static AttachmentStrategy attachmentStrategyFor(TopologyVariant variant) {
		return switch (variant) {
			case CHAIN -> new ChainAttachment();
			case BALANCED_TREE -> new BalancedTreeAttachment();
			case UNIFORM_RANDOM -> new UniformRandomAttachment();
			case PREFERENTIAL -> new PreferentialAttachment();
		};
	}
}
