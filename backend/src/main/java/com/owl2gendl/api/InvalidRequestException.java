package com.owl2gendl.api;

/** Thrown for request problems Bean Validation annotations can't express (e.g. unknown enum names, cross-field limits). */
public class InvalidRequestException extends RuntimeException {

	public InvalidRequestException(String message) {
		super(message);
	}
}
