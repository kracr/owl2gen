package owl.cs.myfirst.owlapi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@SpringBootApplication
@RequestMapping("/api")
public class Demo2 {
	public static void main(String[] args) {
		SpringApplication.run(Demo2.class, args);
	}

	@RequestMapping(value = "/send", method = RequestMethod.POST)
	// public void frontendValue(@RequestBody UserData params) {
	public void frontendValue(@RequestBody com.fasterxml.jackson.databind.JsonNode params)
			throws OWLOntologyStorageException, OWLOntologyCreationException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException,
			ClassNotFoundException {
		JsonNode arrNode = params.get("userValues");
		int[] userval = new int[arrNode.size()];
		int i = 0;
		if (arrNode.isArray()) {
			for (JsonNode objNode : arrNode) {
				userval[i] = objNode.asInt();
				i++;
			}
		}
		String format = params.get("format").asText();
		boolean extraAxioms = params.get("extra").asBoolean();

		System.out.println(" Generating ontologies in " + format + " format.");
		app App = new app();
		App.generate(userval, format, extraAxioms);
	}
	@RequestMapping(value = "/nesy", method = RequestMethod.POST)
	// public void frontendValue(@RequestBody UserData params) {
	public void frontendValueforNesy(@RequestBody com.fasterxml.jackson.databind.JsonNode params)
			throws OWLOntologyStorageException, OWLOntologyCreationException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException,
			ClassNotFoundException {
		JsonNode arrNode = params.get("userValues");
		int[] userval = new int[arrNode.size()];
		int i = 0;
		if (arrNode.isArray()) {
			for (JsonNode objNode : arrNode) {
				userval[i] = objNode.asInt();
				i++;
			}
		}
		String format = params.get("format").asText();
		boolean extraAxioms = params.get("extra").asBoolean();

		System.out.println(" Generating ontologies in " + format + " format.");
		app App = new app();
		App.generate(userval, format, extraAxioms);
	}

	  @RequestMapping(value = "/writeAxiomsToFile", method = RequestMethod.POST)
	  public void writeToAxiom(@RequestBody List<String> filePaths) {
	        System.out.println("Received file paths: " + filePaths);

	        // Example: Directly call the method to process file paths
	        WriteAxiomsToTextFiles axiomToText = new WriteAxiomsToTextFiles();
	        try {
	            axiomToText.writeAxioms(filePaths);
	            System.out.println("Axioms added to files successfully.");
	        } catch (OWLOntologyCreationException | IOException e) {
	            System.err.println("Error writing axioms to files: " + e.getMessage());
	            e.printStackTrace();
	        }
	    }
}