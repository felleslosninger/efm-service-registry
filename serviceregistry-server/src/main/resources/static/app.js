var React = require('react');
var ReactDOM = require('react-dom');
var $ = require('jquery');

class App extends React.Component {
    render() {
        return (
            <div>
                <h1>Legg til enhet</h1>
                <EnhetsForm/>
            </div>
        )
    }
}

class EnhetsForm extends React.Component {

    constructor(props) {
        super(props);

        this.state= {
            organisasjonsnummer: "",
            navn: "",
            organisasjonsform: "",
            retval: ""
        };

        this.orgnrHandler= this.orgnrHandler.bind(this);
        this.navnHandler= this.navnHandler.bind(this);
        this.organisasjonsformHandler= this.organisasjonsformHandler.bind(this);
        this.sendForm= this.sendForm.bind(this);
        this.resetForm= this.resetForm.bind(this);
    }


    orgnrHandler(e) {
        this.setState({organisasjonsnummer: e.target.value});
    }

    navnHandler(e) {
        this.setState({navn: e.target.value});
    }

    organisasjonsformHandler(e) {
        this.setState({organisasjonsform: e.target.value});
    }

    sendForm() {
        var msg= {
            organisasjonsnummer: this.state.organisasjonsnummer,
            navn: this.state.navn,
            organisasjonsform: this.state.organisasjonsform
        }
        console.log(JSON.stringify(msg));
        $.ajax({
            url: '/addmockenhet',
            type: 'POST',
            contentType: 'application/json',
            dataType: 'html',
            processData: false,
            cache: false,
            data: JSON.stringify(msg),
            success: function(ret) {
                this.setState({retval: `>> ${ret}`});
            }.bind(this),
            error: function(xhr, status, err) {
                console.error(this.props.url, status, err.toString());
                this.setState({retval: `>> Feil: ${err.toString()}`});
            }.bind(this)
        });
    }

    resetForm() {
        this.setState({
            organisasjonsnummer: "",
            navn: "",
            organisasjonsform: "",
            retval: ""
        });
    }

    render() {
        return (
            <div>
                <div className="well well-lg">
                    <form>
                        <div className="form-group form-group-lg">
                            <input type="text" name="organisasjonsnummer" className="form-control input-lg" value={this.state.organisasjonsnummer} onChange={this.orgnrHandler} placeholder="Organisasjonsnummer"/>
                        </div>
                        <div className="form-group form-group-lg">
                            <input type="text" name="navn" className="form-control input-lg" value={this.state.navn} onChange={this.navnHandler} placeholder="Navn"/>
                        </div>
                        <div className="form-group form-group-lg">
                            <input type="text" name="organisasjonsform" className="form-control input-lg" value={this.state.organisasjonsform} onChange={this.organisasjonsformHandler} placeholder="Organisasjonsform"/>
                        </div>
                        <button type="button" onClick={this.sendForm} className="btn btn-primary btn-lg">Legg til</button>
                        &nbsp;
                        <button type="reset" onClick={this.resetForm} className="btn btn-primary btn-lg">Nullstill</button>
                    </form>
                </div>
                <h2>{this.state.retval}</h2>
            </div>
        )
    }
}

ReactDOM.render(
    <App/>,
    document.getElementById('react')
);