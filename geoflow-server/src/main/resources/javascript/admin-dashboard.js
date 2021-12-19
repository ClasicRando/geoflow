const createValidationRules = {
    fullName: 'required',
    username: 'required',
    password: {
        required: true,
        minlength: 6,
    },
    repeatPassword: {
        required: true,
        equalTo: '#password',
    },
    roles: {
        required: {
            depends: (element) => !($('#isAdmin').prop('checked')),
        },
    },
};
const createValidationMessages = {
    fullName: 'Please specify a name',
    username: 'Please specify a username',
    password: {
        required: 'Please specify a password',
        minlength: jQuery.validator.format('Password must be atleast {0} characters'),
    },
    repeatPassword: {
        required: 'Passwords must match',
        equalTo: 'Passwords must match',
    },
    roles: {
        required: 'Atleast 1 roles must be selected',
    },
};
let createValidator = null;

const editValidationRules = {
    fullName: 'required',
    username: 'required',
    roles: {
        required: {
            depends: (element) => !($('#isAdmin').prop('checked')),
        },
    },
};
const editValidationMessages = {
    fullName: 'Please specify a name',
    username: 'Please specify a username',
    roles: {
        required: 'Atleast 1 roles must be selected',
    },
};
let editValidator = null;
let updateUserOid = -1;

$(document).ready(async () => {
    const response = await fetch('/data/roles');
    if (response.status === 200) {
        const json = await response.json();
        if ('payload' in json) {
            const roles = json.payload;
            for (const select of document.getElementsByName('roles')) {
                roles.forEach(role => {
                    const option = document.createElement('option');
                    option.value = role.name;
                    option.innerText = role.description;
                    select.appendChild(option);
                });
            }
        } else {
            console.log(json);
        }
    } else {
        console.log(response);
    }
    $('#isAdmin').change((e) => {
        const $form = $(e.target).parent().parent();
        const $select = $('.custom-select');
        if (this.checked) {
            $select.prop("disabled", true).removeClass('invalidInput').addClass('validInput');
            $select.find('option').prop('selected', false);
            const $error = $form.find('#roles-error');
            if ($error.length !== 0) {
                $error.remove();
            }
        } else {
            $select.prop("disabled", false).removeClass('validInput');
        }
    });
    createValidator = $(`#${userCreateForm}`).validate({
        rules: createValidationRules,
        messages: createValidationMessages,
        errorClass: 'invalidInput',
        validClass: 'validInput',
        submitHandler: () => {},
    });
    editValidator = $(`#${userEditForm}`).validate({
        rules: editValidationRules,
        messages: editValidationMessages,
        errorClass: 'invalidInput',
        validClass: 'validInput',
        submitHandler: () => {},
    });
    $(`#${userCreateModal}`).on('hidden.bs.modal', (e) => {
        $(e.target).find('option').prop('selected',false);
        createValidator.resetForm();
        $(`#${userTable}`).bootstrapTable('refresh');
    });
    $(`#${userEditModal}`).on('hidden.bs.modal', (e) => {
        $(e.target).find('option').prop('selected',false);
        editValidator.resetForm();
        $(`#${userTable}`).bootstrapTable('refresh');
    });
});

function isAdminFormatter(value, row) {
    return row.roles.includes('admin') ? '<i class="fas fa-check"></i>' : '';
}

function editFormatter(value, row) {
    return value ? `<span style="display: inline;"><i class="fas fa-edit p-1 inTableButton" onClick="openEditUserModal(${row.user_oid})"></i></span>` : '';
}

function openNewUserModal() {
    $(`#${userCreateModal}`).modal('show');
}

async function submitNewUser($form) {
    const $fullName = $form.find('#fullName');
    const $username = $form.find('#username');
    const $password = $form.find('#password');
    const $roles = $form.find('#roles');
    const $isAdmin = $form.find('#isAdmin');
    const user = {
        fullName: $fullName.val(),
        username: $username.val(),
        user_oid: null,
        password: $password.val(),
        roles: $isAdmin.prop('checked') ? ['admin'] : $roles.val(),
    };
    const response = await fetchPOST('/data/users', user);
    const json = await response.json();
    console.log(json);
    if ('errors' in json) {
        $(`#${userCreateModal}ResponseErrorMessage`).text(formatErrors(json.errors));
    } else {
        $(`#${userCreateModal}`).modal('hide');
        showToast('Created User', `Created ${user.username} (${json.payload})`);
    }
}

async function createUser() {
    const $form = $(`#${userCreateForm}`);
    if ($form.valid()) {
        submitNewUser($form);
    }
}

function openEditUserModal(userOid) {
    updateUserOid = userOid;
    const $modal = $(`#${userEditModal}`);
    const row = $(`#${userTable}`).bootstrapTable('getData').find(row => row.user_oid === userOid);
    const $form = $modal.find('form');
    $form.find('#fullName').val(row.name).change();
    $form.find('#username').val(row.username).change();
    $form.find('#roles option').each((i, element) => {
        const $element = $(element);
        if (row.roles.includes($element.val())) {
            $element.prop('selected', 'selected');
        }
    });
    $form.find('#isAdmin').prop('checked', row.roles.includes('admin'));
    $modal.modal('show');
}

async function submitEditUser($form) {
    const $fullName = $form.find('#fullName');
    const $username = $form.find('#username');
    const $roles = $form.find('#roles');
    const $isAdmin = $form.find('#isAdmin');
    const user = {
        fullName: $fullName.val(),
        username: $username.val(),
        user_oid: updateUserOid,
        roles: $isAdmin.prop('checked') ? ['admin'] : $roles.val(),
        password: null,
    };
    const response = await fetchPATCH('/data/users', user);
    const json = await response.json();
    if ('errors' in json) {
        $(`#${userEditModal}ResponseErrorMessage`).text(formatErrors(json.errors));
    } else {
        $(`#${userEditModal}`).modal('hide');
        showToast('Updated User', `Successful update to ${json.payload.username}`);
    }
    updateUserOid = -1;
}

async function editUser() {
    const $form = $(`#${userEditForm}`);
    if ($form.valid()) {
        submitEditUser($form);
    }
}