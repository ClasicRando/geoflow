$(document).ready(function() {
    $(`#${isAdmin}`).change(function() {
        if (this.checked) {
            $(`#${rolesSelect}`).prop("disabled", true);
        } else {
            $(`#${rolesSelect}`).prop("disabled", false);
        }
    });
});