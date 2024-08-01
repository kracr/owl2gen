import React, { Component } from "react";
import { Button, Card } from "react-bootstrap";
import Appupload from '../Appupload';
import FixedTBox from "./FixedTBox";
import GenACT from "./GenACT";
import AppuploadNesy from '../AppuploadNesy';
export default class VersionPicker extends Component {
    state = {
        showv1: false,
        showv2: false,
        showv3: false,
        showv4: false, // New state variable for the fourth project
    };

    toggleView(val) {
        if (val === 1) {
            this.setState({
                showv1: !this.state.showv1,
                showv2: false,
                showv3: false,
                showv4: false,
            });
        } else if (val === 2) {
            this.setState({
                showv1: false,
                showv2: !this.state.showv2,
                showv3: false,
                showv4: false,
            });
        } else if (val === 3) {
            this.setState({
                showv1: false,
                showv2: false,
                showv3: !this.state.showv3,
                showv4: false,
            });
        } else if (val === 4) {
            this.setState({
                showv1: false,
                showv2: false,
                showv3: false,
                showv4: !this.state.showv4,
            });
        }
    }

    render() {
        const cardStyle = {
            width: '15rem', // Fixed width
            height: '20rem', // Fixed height
            margin: '10px', // Space between cards
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
        };

        const cardBodyStyle = {
            flexGrow: 1,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
        };

        return (
            <div>
                <div className="container">
                    <div className="row mt-5">
                        <div className="col-3">
                            <Card style={cardStyle}>
                                <Card.Header className="bg-cyan"><h5>OWL2Bench</h5></Card.Header>
                                <Card.Body style={cardBodyStyle}>
                                    <div>
                                        { /* <Card.Title>About</Card.Title> */}
                                        <Card.Text>
                                            Fixed TBox for each OWL 2 EL, QL, RL, and DL
                                        </Card.Text>
                                    </div>
                                    <Button onClick={() => this.toggleView(1)} variant="info" style={{ alignSelf: 'flex-start' }}>
                                        Generate
                                    </Button>
                                </Card.Body>
                            </Card>
                        </div>
                        <div className="col-3">
                            <Card style={cardStyle}>
                                <Card.Header className="bg-cyan"><h5>CustomOWL2Bench</h5></Card.Header>
                                <Card.Body style={cardBodyStyle}>
                                    <div>
                                        { /* <Card.Title>About</Card.Title> */}
                                        <Card.Text>
                                            Customizable based on OWL 2 Language constructs
                                        </Card.Text>
                                    </div>
                                    <Button onClick={() => this.toggleView(2)} variant="info" style={{ alignSelf: 'flex-start' }}>
                                        Generate
                                    </Button>
                                </Card.Body>
                            </Card>
                        </div>
                        <div className="col-3">
                            <Card style={cardStyle}>
                                <Card.Header className="bg-cyan"><h5>GenACT</h5></Card.Header>
                                <Card.Body style={cardBodyStyle}>
                                    <div>
                                        { /* <Card.Title>About</Card.Title> */}
                                        <Card.Text>
                                            With supporting text below as a natural lead-in to additional content.
                                        </Card.Text>
                                    </div>
                                    <Button onClick={() => this.toggleView(3)} variant="info" style={{ alignSelf: 'flex-start' }}>
                                        Generate
                                    </Button>
                                </Card.Body>
                            </Card>
                        </div>
                        <div className="col-3">
                            <Card style={cardStyle}>
                                <Card.Header className="bg-cyan"><h5>NeSyBench</h5></Card.Header>
                                <Card.Body style={cardBodyStyle}>
                                    <div>
                                         { /* <Card.Title>About</Card.Title> */}
                                        <Card.Text>
                                            With supporting text below as a natural lead-in to additional content.
                                        </Card.Text>
                                    </div>
                                    <Button onClick={() => this.toggleView(4)} variant="info" style={{ alignSelf: 'flex-start' }}>
                                        Generate
                                    </Button>
                                </Card.Body>
                            </Card>
                        </div>
                    </div>
                </div>
                {(this.state.showv1) ? <FixedTBox /> : ' '}
                {(this.state.showv2) ? <Appupload /> : ' '}
                {(this.state.showv3) ? <GenACT /> : ' '}
                {(this.state.showv4) ? <AppuploadNesy /> : ' '} {/* Placeholder for the fourth component */}
            </div>
        );
    }
}
