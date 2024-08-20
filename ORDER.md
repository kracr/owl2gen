#Documentation to understand the code and design choices:-

1) - Inside Package owlapi -> app.java
    Order in which to read functions :-
        1) - generate
        2) - generateOntology
        3) - loadMainNecessaryThings

2) - Inside Package owlapi.Features.BoilerplateCode -> Util.java
    Order in which to read functions :-
        1) - initializeGlobalHashMaps
        2) - initialUserInputCount
        3) - initialAxiomsCount
        4) - randomizeMapping
        IGNORE - readUserInputFile

3) - Inside Package owlapi.Features.BoilerplateCode -> TextFileProcessor.java
    Order in which to read functions :-
        1) - readTxtFile
        2) - rankAxioms
        3) - loadGlobalCommons
        4) - saveGlobalCommons
        5) - insertIntoGlobalCommons
        IGNORE insertIntoGlobalHashMaps for NOW.

4) - Inside Package owlapi.Features.BoilerplateCode -> CommonFramework.java
    Order in which to read functions :-
        1) - addToOntology
        2) - insertAxiomIntoOntology
        IGNORE - filterGlobalHashMapConcepts

    NOTE :- 
		  * CommonFramework.java's "addToOntology" function corresponds to TextFileProcessor.java's "insertIntoGlobalCommons". They are functioning similar.
		  * CommonFramework.java's "insertAxiomIntoOntology" function corresponds to TextFileProcessor.java's "insertIntoGlobalHashMaps". They are functioning similar.

5) - Inside Package owlapi.Features.BoilerplateCode -> LastDance.java
    Order in which to read functions :-
        1) - oneLastTime
        2) - constructAxiomsHashMap
        3) - addGlobalToOntologyDomainRange
        4) - addGlobalToOntologySubOf ( especially ONLY this function )

6) - FilenameConstructMapping.java -> For each category of constructs, we have defined its corresponding axiom 'txt' file and 'separator' used to PARSE the 		Axioms. Because ALL Axioms 'txt' file is IN FORMAT :- Term1 'SEPARATOR' Terms......... ( Could be from emty to 5 terms ).
		 There we are making these two Hashmaps
		1) - fileMap ( key - constructName ( without Owl/Feature Term in them) , value - fileName to read axioms from )
		2) - separatorMap ( key - constructName ( without Owl/Feature Term in them) , value - 'SEPARATOR' to parse the axiom in their 'txt' file )

7) - Reasoner.java -> This File has owl api code for checking consistency of the generated ontology. 

8) - owlapi.Features.Features -> This is the PACKAGE NAME, under which we have written Category JAVA Files. 
	For example :- ClassEnumerationcategory.java, ClassExpressionAxiomsCategory.java, DataRangesCategory.java like total eight files.
    List of constructs UNDER EACH category java file is mentioned below. 
    In each method, we have written logic TO INSERT that CONSTRUCT into ONTOLOGY. In each method , we are taking ArrayList Of Terms ( Part Of Single Axiom ) As Argument.

	1) - ClassEnumerationcategory = {"OwlClass","ObjectComplement","ObjectIntersection","ObjectOneOf","ObjectUnionOf"};
	2) - ClassExpressionAxiomsCategory = {"AllDisjointClasses","DisjointUnion","DisjointWith","EquivalentClass","RdfsSubClassOf"};
	3) - ObjectPropertyAxiomCategory = {"AsymmetricProperty","EquivalentObjectProperty","FunctionalObjectProperty","InverseFunctionalProperty","InverseOfProperty","IrreflexiveProperty","ObjectPropertyDisjointWith","ReflexiveProperty","SymmetricProperty","TransitiveProperty","RdfsObjectDomain","RdfsObjectRange","PropertyChainAxiom","RdfsObjectSubPropertyOf","AllDisjointObjectProperties","ObjectProperty"}; 
	4) - ObjectPropertyRestrictinoCategory = {"ObjectAllValuesFrom","ObjectHasSelf","ObjectHasValue","ObjectSomeValuesFrom","ObjectQualifiedCardinality","ObjectMaxQualifiedCardinality","ObjectMinQualifiedCardinality"};
	5) - DataPropertyAxiomsCategory = {"AllDisjointDataProperties","EquivalentDataProperty","FunctionalDataProperty","DataPropertyDisjointWith","RdfsDataDomain","RdfsDataRange","RdfsDataSubPropertyOf"}; 
	6) - DataPropertyRestrictionCategory = {"DataAllValues","DataHasValue", "DataMaxQualifiedCardinality","DataMinQualifiedCardinality","DataQualifiedCardinality","DataSomeValuesFrom"};
	7) - DataRangesCategory = {"DataComplementOf","DataIntersectionOf", "DataOneOf","DataUnionOf"};
	8) - Assertions: HasKey, ClassAssertionAxioms, ObjectPropertyAssertionAxioms, DataPropertyAssertionAxioms
