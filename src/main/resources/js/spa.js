// attaches an event to all relevant input-fields
function startTheCaseWork() {
    var targets = document.getElementsByClassName("vilkaar_input");

    for (var i = 0; i < targets.length; i++) {
        targets[i].addEventListener("input", applicationUpdated);
    }

}

function applicationUpdated(event) {
    // send that application to get the SPA-treatment
}