import React, { Component } from "react";
import { Nav } from "react-bootstrap";
import { BrowserRouter as Router, Switch, Route, Link } from "react-router-dom";

export default class Home extends Component {
  render() {
    return (
      <div className="container">
        <h1 className="m-4 p-2 text-center">
          OntoGen: A Configurable Ontology Generator for Benchmarking
        </h1>
        <div>
          <span className="row">
            <h3 className="ml-3 pt-2 mb-0">Introduction</h3>
            <Nav className="ml-auto">
              <Nav.Link as={Link} to="/owl2bench">
                <button className="btn btn-success"> Try OntoGen </button>
              </Nav.Link>
            </Nav>
          </span>
          <hr className="border p-0 m-0 mt-2 mb-2" />

          <h5 className="lead">
            <p>In the past decade, there has been remarkable progress towards the
            development of reasoners that involve expressive ontology languages
            such as OWL 2. However, they still do not scale well on expressive
            language profiles (OWL2 DL). To build better quality reasoners,
            developers need to find and improve the performance bottlenecks of
            their existing systems . A reasoner benchmark aids the reasoner
            developers to evaluate their system’s performance and deal with the
            limitations. Furthermore, it paves the way for further research to
            improve performance and functionality. In particular, a reasoner
            needs to be evaluated from several aspects such as support for
            different language constructs and their combinations, their effect
            on reasoning performance, ability to handle large ontologies, and
            capability to handle queries that involve reasoning. Though there
            are some existing ontology benchmarks, they are limited in scope.</p>
            
           
           <p> Tyring to fill the gap, OntoGen is an ontology genetation tool that can generate 
            a wide variety of ontologies for the purpose of benchmarking.</p>
          </h5>
        </div>
      </div>
    );
  }
}
