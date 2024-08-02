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
                <button className="btn btn-info m-2"> Try OntoGen </button> {/* btn btn-success*/}
              </Nav.Link>
            </Nav>
          </span>
          <hr className="border p-0 m-0 mt-2 mb-2" />

          <h5 className="lead">
            <p>OntoGen is an ontology generation tool designed to create a broad spectrum of ontologies specifically for benchmarking purposes. With OntoGen, users can generate custom ontologies tailored to various requirements and scenarios, facilitating rigorous evaluation and comparison of different ontology-based systems. The tool supports diverse ontology constructs and configurations, making it suitable for benchmarking across multiple domains and use cases. Whether for academic research, industry applications, or technology validation, OntoGen provides a comprehensive and flexible solution for generating high-quality ontologies that meet the needs of different benchmarking tasks.</p>
          </h5>
        </div>
      </div>
    );
  }
}
