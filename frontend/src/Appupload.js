import React, { useState } from "react";
import axios from "axios";
import "./bootstrap.min.css";
import "./App.css";
import App from './App';

function Appupload() {
    const [files, setFiles] = useState([]);
    const [isProcessing, setIsProcessing] = useState(false);
    const [isProcessed, setIsProcessed] = useState(false);
    const [showUpload, setShowUpload] = useState(false);
    const [choice, setChoice] = useState(null);

    const handleFileChange = (event) => {
        const selectedFiles = Array.from(event.target.files);
        const filePaths = selectedFiles.map(file => file.path || file.name); // Use file.path for Electron or fall back to file.name
        setFiles(prevFiles => [...prevFiles, ...filePaths]);
    };

    const handleRemoveFile = (index) => {
        setFiles(prevFiles => prevFiles.filter((_, i) => i !== index));
    };

    const handleSend = async () => {
        setIsProcessing(true);
        try {
            const response = await axios.post('http://localhost:8080/api/writeAxiomsToFile', files, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            console.log(response.data);
            setIsProcessed(true);
        } catch (error) {
            console.error('Error executing Java method', error);
        } finally {
            setIsProcessing(false);
        }
    };

    const handleChoice = (selectedChoice) => {
        setChoice(selectedChoice);
        if (selectedChoice === 'upload') {
            setShowUpload(true);
        } else if (selectedChoice === 'default') {
            setIsProcessed(true);
        }
    };

    if (isProcessed) {
        return <App />;
    }

    return (
        <div className="App container-fluid">
            <div className="row justify-content-center">
                <h1 className="mt-4">OntoGen 1.0</h1>
            </div>
            <hr />
            {!choice && (
                <div className="row justify-content-center">
                    <button
                        className="btn btn-info m-2" // Match the color of the Generate button
                        onClick={() => handleChoice('upload')}
                    >
                        Upload Your Own Ontology
                    </button>
                    <button
                        className="btn btn-info m-2" // Match the color of the Generate button
                        onClick={() => handleChoice('default')}
                    >
                        Use Default Ontology
                    </button>
                </div>
            )}
            {showUpload && (
                <div className="row">
                    <div className="col-md-3"></div>
                    <div className="col-md-5">
                        <div className="input-group-append">
                            <input
                                type="file"
                                className="form-control-file m-2"
                                id="uploadFiles"
                                multiple
                                onChange={handleFileChange}
                            />
                        </div>
                        {files.length > 0 && (
                            <div className="file-list mt-3">
                                <h5>Selected Files:</h5>
                                <ul className="list-group">
                                    {files.map((file, index) => (
                                        <li key={index} className="list-group-item d-flex justify-content-between align-items-center">
                                            {file}
                                            <button className="btn btn-danger btn-sm" onClick={() => handleRemoveFile(index)}>x</button>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>
                    <div className="col-md-4">
                        <div className="input-group-append fixed-done-button">
                            <button className="btn btn-secondary m-2" onClick={handleSend} disabled={isProcessing}>
                                {isProcessing ? "Running..." : "Done"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
            <hr />
        </div>
    );
}

export default Appupload;
