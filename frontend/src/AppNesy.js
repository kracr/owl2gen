/* eslint-disable */
import React, { useState } from "react";
import Predefined from "./types/Predefined";
import axios from "axios";
import "./bootstrap.min.css";
import "./App.css"

const constructs1 = [
  "OwlClass",
  "ObjectIntersection",
  "EquivalentClass",
  "RdfsSubClassOf",
  "RdfsObjectSubPropertyOf",
  "DataProperty",
  "ObjectProperty",
  "ClassAssertion",
  "ObjectPropertyAssertion",
  "DataPropertyAssertion",
  "TransitiveProperty",
  "PropertyChainAxiom",
];
let initally1 = [];
const indexes1 = new Map();
for (let i = 0; i < constructs1.length; i++) {
  initally1[i] = false;
  indexes1[constructs1[i]] = i;
  //console.log(constructs1[i]+" "+initally1[i]+" "+indexes1[constructs1[i]]);
}
const h1 = "Language Constructs";

const constructs2 = [
  "A⊑C C⊑D ⇒ A⊑D",
  "A⊑C1 A⊑C2 C1⊓C2⊑D ⇒ A⊑D",
  "A⊑C C⊑∃R.D ⇒ A⊑∃R.D",
  "A⊑∃R.B B⊑C ∃R.C⊑D ⇒ A⊑D",
  "A⊑∃S.D S⊑R ⇒ A⊑∃R.D",
  "A⊑∃R1.C C⊑∃R2.D R1oR2⊑R ⇒ A⊑∃R.D",
];
let initally2 = [];
const indexes2 = new Map();
for (let i = 0; i < constructs2.length; i++) {
  initally2[i] = false;
  indexes2[constructs2[i]] = i;
}
const h2 = "Reasoning Patterns";

const constructs3 = [
  "normalized axioms",
  "Train-Test-Valid Splits",

];

let initally3 = [];
const indexes3 = new Map();
for (let i = 0; i < constructs3.length; i++) {
  initally3[i] = false;
  indexes3[constructs3[i]] = i;
}
const h3 = "Preprocessing";

const constructs4 = [
  "TBox Inconsistencies",
  "ABox Inconsistencies",
];
let initally4 = [];
const indexes4 = new Map();
for (let i = 0; i < constructs4.length; i++) {
  initally4[i] = false;
  indexes4[constructs4[i]] = i;
}
const h4 = "Introducting Inconsistencies";



const headers = [h1, h2, h3, h4];
const headingInd = new Map();
for (let i = 0; i < headers.length; i++) {
  headingInd[headers[i]] = i;
}

const overallconstructs = [
  constructs1,
  constructs2,
  constructs3,
  constructs4,
 
];

let total = 0;
for (let i = 0; i < overallconstructs.length; i++) {
  total = total + overallconstructs[i].length;
}

function App() {
  const [checkbox1, setCheck1] = useState(initally1);
  const [checkbox2, setCheck2] = useState(initally2);
  const [checkbox3, setCheck3] = useState(initally3);
  const [checkbox4, setCheck4] = useState(initally4);
  const [extra, setExtra] = useState(true);
  const [format, setFormat] = useState("XML");

  const fxns = [
    setCheck1,
    setCheck2,
    setCheck3,
    setCheck4,
  ];
  const boxes = [
    checkbox1,
    checkbox2,
    checkbox3,
    checkbox4,
  ];
  const index = [
    indexes1,
    indexes2,
    indexes3,
    indexes4,
  ];

  let generate = () => {
    let userInput = [];
    let flag = false;
    let ifany = false;
    for (let i = 0; i < boxes.length; i++) {
      for (let j = 0; j < boxes[i].length; j++) {
        if (boxes[i][j] == true) {
          let construct = overallconstructs[i][j];
          let userVal = document.getElementById(construct + "Text").value;
          ifany = true;
          if (userVal >= 1) {
            userInput.push(userVal);
          } else {
            flag = true;
            userVal = 1;
            userInput.push(1);
          }
          // console.log(construct + " | " + userVal);
        } else {
          userInput.push(0);
        }
      }
    }
    if (ifany == true) {
      console.log(extra + " |" + format);
      axios
        .post("http://localhost:8080/api/send", {
          userValues: userInput,
          format: format,
          extra: extra,
        })
        .then((response) => {
          alert(
         //   response.status +
            //  " All Non - User Input Checkboxes have been set 1 value by default "
          
           // "Response status " + response.status + 
            "\n \nGenerating"
          );
        });
    } else {
      alert(" Nothing Checked , Cant Generate ");
    }
  };

  let invertToggle = () => {
    for (let j = 0; j < boxes.length; j++) {
      let allCheck = [];
      for (let i = 0; i < boxes[j].length; i++) {
        allCheck[i] = !boxes[j][i];
      }
      fxns[j](allCheck);
    }
    // console.log(headingInd[ind]+" toggle "+ind);
  };
  let toggle = (boolVal) => {
    for (let j = 0; j < boxes.length; j++) {
      let allCheck = [];
      let contructNames = overallconstructs[j];
      for (let i = 0; i < boxes[j].length; i++) {
        allCheck[i] = boolVal;
        if (boolVal == false) {
          document.getElementById(contructNames[i] + "Text").value = "";
          document.getElementById("selectAllInput").value = "";
          // console.log(
          //   contructNames[i] +
          //     " = " +
          //     document.getElementById(contructNames[i] + "Text").value
          // );
        }
        if (boolVal == true) {
          document.getElementById(contructNames[i] + "Text").value =
            document.getElementById("selectAllInput").value.length != 0
              ? document.getElementById("selectAllInput").value
              : 1;
          // console.log(
          //   document.getElementById("selectAllInput").value +
          //     " =  dekho left main "
          // );
        }
      }
      fxns[j](allCheck);
    }
  };
  let selectAll = () => toggle(true);
  let selectNone = () => toggle(false);

  let toggleChildren = (ind, boolVal) => {
    let allCheck = [];
    let contructNames = overallconstructs[headingInd[ind]];
    for (let i = 0; i < boxes[headingInd[ind]].length; i++) {
      // console.log(
      //   contructNames[i] +
      //     " = " +
      //     document.getElementById(contructNames[i] + "Text").value +
      //     " = " +
      //     ind +
      //     " || " +
      //     document.getElementById("selectAllInput" + ind).value
      // );
      if (boolVal == false) {
        document.getElementById("selectAllInput" + ind).value = "";
        document.getElementById(contructNames[i] + "Text").value = "";
      } else if (boolVal == true) {
        document.getElementById(contructNames[i] + "Text").value =
          document.getElementById("selectAllInput" + ind).value.length != 0
            ? document.getElementById("selectAllInput" + ind).value
            : 1;
      }
      allCheck[i] = boolVal;
    }
    fxns[headingInd[ind]](allCheck);
  };
  let invertToggleChildren = (e) => {
    let ind = e.target.id;
    //console.log(headingInd[ind]+" toggle "+ind);
    let allCheck = [];
    for (let i = 0; i < boxes[headingInd[ind]].length; i++) {
      allCheck[i] = !boxes[headingInd[ind]][i];
    }
    fxns[headingInd[ind]](allCheck);
    //     allCheck[i] = !checked[i];
  };
  //index - array of hashmaps
  //boxes - array of checkboxes
  //headingInd - hashmap of heading's index
  let handleCheckboxOne = (e) => {
    const nameclicked = e.target.name;
    let ind = e.target.parentElement.id;
    if (e.target.checked == false) {
      document.getElementById(nameclicked + "Text").value = "";
    }
    let allCheck = [...boxes[headingInd[ind]]];
    allCheck[index[headingInd[ind]][nameclicked]] =
      !boxes[headingInd[ind]][index[headingInd[ind]][nameclicked]];
    fxns[headingInd[ind]](allCheck);
    //console.log(ind+ " | "+nameclicked+"  "+e.target.checked+" "+allCheck[index[headingInd[ind]][nameclicked]]+" "+!boxes[headingInd[ind]][index[headingInd[ind]][nameclicked]]);
  };

  let extraHierarchyDomainRange = (e) => {
    const target = e.target;
    const value = target.type === "checkbox" ? target.checked : target.value;
    setExtra(value);
  };

  let selectOutputFormat = (e) => {
    const target = e.target;
    setFormat(target.value);
  };

  return (
    
    <div className="App container-fluid">
                            <hr/>

      <div className="row justify-content-center">
        <h1 className="mt-4">NeSyBench : Benchmarking EL++ Reasoners</h1>
      </div>   
      
      <hr></hr>
      <hr></hr>
      <div className="row">
        <div className="container col-6">
          <Predefined
            invertToggle={invertToggleChildren}
            handleCheckboxChange={handleCheckboxOne}
            toggleParent={toggleChildren}
            constructs={constructs1}
            initally={checkbox1}
            indexes={indexes1}
            heading={h1}
          />
        </div>
        <div className="container col-6">
          <Predefined
            invertToggle={invertToggleChildren}
            handleCheckboxChange={handleCheckboxOne}
            toggleParent={toggleChildren}
            constructs={constructs2}
            initally={checkbox2}
            indexes={indexes2}
            heading={h2}
          />
        </div>
      </div>
      <br></br>
      <div className="row">
        <div className="container col-6">
          <Predefined
            invertToggle={invertToggleChildren}
            handleCheckboxChange={handleCheckboxOne}
            toggleParent={toggleChildren}
            constructs={constructs3}
            initally={checkbox3}
            indexes={indexes3}
            heading={h3}
          />
        </div>
        <div className="container col-6">
          <Predefined
            invertToggle={invertToggleChildren}
            handleCheckboxChange={handleCheckboxOne}
            toggleParent={toggleChildren}
            constructs={constructs4}
            initally={checkbox4}
            indexes={indexes4}
            heading={h4}
          />
        </div>
      </div>
    
      
      <br></br>
      <hr></hr>
      <hr></hr>
      <div className="row justify-content-around">
        <select onChange={selectOutputFormat}>
          <option value="XML">OWL/XML</option>
          <option value="RDF">RDF/XML</option>
          <option value="Manchester">Manchester</option>
          <option value="Functional">Functional</option>
          <option value="Turtle">Turtle</option>
        </select>
        <div className="form-check">
          <input
            class="form-check-input"
            type="checkbox"
            value={extra}
            id="flexCheckChecked"
            onChange={extraHierarchyDomainRange}
            // checked
          ></input>
          <label class="form-check-label" for="flexCheckChecked">
            Domain Agnostic
          </label>
        </div>
        <button className="btn btn-primary" onClick={(event) => generate()}>
          {" "}
          Generate{" "}
        </button>
      </div>
      <hr></hr>
      <div className="row justify-content-center">
        © &nbsp;
        <a href="https://kracr.iiitd.edu.in/" target="_blank">
          KRaCR Lab
        </a>
        , 2024
      </div>
      <hr></hr>
    </div>
  );
}

export default App;
