import React, { Component } from "react";
import { Button, Card } from "react-bootstrap";
import Appupload from '../Appupload';
import AppuploadNesy from '../AppuploadNesy'; // Import if needed for future use

export default class VersionPicker extends Component {
    state = {
        showVersionPicker: true, // State to control the visibility of VersionPicker
        showAppupload1: false,
        showAppupload2: false,
    };

    toggleView(val) {
        if (val === 1) {
            this.setState({
                showVersionPicker: false, // Hide the VersionPicker panel
                showAppupload1: true,
                showAppupload2: false,
            });
        } else if (val === 2) {
            this.setState({
                showVersionPicker: false, // Hide the VersionPicker panel
                showAppupload1: false,
                showAppupload2: true,
            });
        }
    }

    render() {
        const cardStyle = {
            width: '30rem', // Fixed width
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
                {this.state.showVersionPicker ? (
                    <div className="container">
                        <div className="row mt-5">
                            <div className="col-6">
                                <Card style={cardStyle}>
                                    <Card.Header className="bg-cyan"><h5>OntoGen 1.0</h5></Card.Header>
                                    <Card.Body style={cardBodyStyle}>
                                        <div>
                                            <Card.Text>
                                                Description for the first Appupload component.
                                            </Card.Text>
                                        </div>
                                        <Button onClick={() => this.toggleView(1)} variant="info" style={{ alignSelf: 'flex-start' }}>
                                            Generate
                                        </Button>
                                    </Card.Body>
                                </Card>
                            </div>
                            <div className="col-6">
                                <Card style={cardStyle}>
                                    <Card.Header className="bg-cyan"><h5>OntoGen 2.0</h5></Card.Header>
                                    <Card.Body style={cardBodyStyle}>
                                        <div>
                                            <Card.Text>
                                                Description for the second Appupload component.
                                            </Card.Text>
                                        </div>
                                        <Button onClick={() => this.toggleView(2)} variant="info" style={{ alignSelf: 'flex-start' }}>
                                            Generate
                                        </Button>
                                    </Card.Body>
                                </Card>
                            </div>
                        </div>
                    </div>
                ) : (
                    <>
                        {this.state.showAppupload1 && <Appupload />}
                        {this.state.showAppupload2 && <AppuploadNesy />} {/* If needed */}
                    </>
                )}
            </div>
        );
    }
}
