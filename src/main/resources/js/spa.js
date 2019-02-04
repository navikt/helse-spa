// attaches an event to all relevant input-fields
function startTheCaseWork() {
    var targets = document.getElementsByClassName("vilkaar_input");

    for (var i = 0; i < targets.length; i++) {
        targets[i].addEventListener("input", applicationUpdated);
    }

}

function applicationUpdated(event) {
    // send that application to get the SPA-treatment

    var inputs = document.getElementsByClassName("vilkaar_input");
    var soknad = {};
    for (var i = 0; i < inputs.length; i++) {
        var value = inputs[i].value;
        var id = inputs[i].id.substr(6);
        soknad[id] = value;
    }

    post(soknad, applyNare)
}

function post(soknad, callback) {
    var request = new XMLHttpRequest();
    request.open("POST", "/soknad", true);
    request.setRequestHeader("Content-Type", "application/json");
    request.send(JSON.stringify(soknad));
}

function applyNare() {
    console.log("applying nare");
}