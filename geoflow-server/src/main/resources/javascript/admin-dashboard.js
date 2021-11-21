$(document).ready(function() {

});

function isAdminFormatter(value, row) {
    return row.roles.includes('admin') ? '<i class="fas fa-check"></i>' : '';
}

function userActionsFormatter(value, row) {
    return row.roles.includes('admin') ? '' : `<span style="display: inline;"><i class="fas fa-edit p-1 inTableButton"></i></span>`;
}

function openNewUserModal() {
    $(`#${userCreateModal}`).modal('show');
}

function postCreateUser() {

}