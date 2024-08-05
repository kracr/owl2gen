# OntoGen

## Introduction

OntoGen is a configurable ontology generator designed to facilitate the creation of OWL 2 ontologies tailored to specific research needs. 
With a user-friendly interface and powerful backend capabilities, OntoGen allows users to generate custom ontologies based on selected OWL 2 
constructs and profiles. It supports flexible configurations and provides options for both static and dynamic ontology generation, making it 
an ideal tool for benchmarking, performance evaluation, and ontology modeling research.


![OntoGen](https://github.com/kracr/ontogen/blob/master/Images/configpanel.png)

## Steps to Run the Code

### Frontend

**Navigate to the Frontend Directory:**
   ```bash
   cd ~/frontend/src
   npm start ```


If it throws some error ETC of packages NOT Installed OR ANYTHING, then

**Navigate to ** "~/frontend/"**

   ```bash
   cd ~/frontend/
   npm install (After Installing Everything)
   cd src
   npm start. ```

Thats all for running FRONTEND

### Backend

   ```
   Go to "src/main/java" --> "owl.cs.myfirst.owlapi" --> Demo2.java 
   Run as "Spring Boot App" ```


Files (axiomCountOntology.owl, userInputCountOntology.owl, randomAxiomCountOntology.owl, randomUserInputCountOntology.owl) will get SAVED in the 
root directory of this project folder
