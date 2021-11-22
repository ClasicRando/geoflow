const validationRules = {
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
            depends: function (element) { return !($('#isAdmin').prop('checked')); },
        },
    },
};
const validationMessages = {
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
let validator = null;

$(document).ready(function() {
    $(`#${isAdmin}`).change(function() {
        const $select = $(`#${rolesSelect}`);
        if (this.checked) {
            $select.prop("disabled", true).removeClass('invalidInput').addClass('validInput');
            $select.find('option').prop('selected', false);
            const $error = $('#roles-error');
            if ($error.length !== 0) {
                $error.remove();
            }
        } else {
            $select.prop("disabled", false).removeClass('validInput');
        }
    });
    const $form = $(`#${createUserForm}`);
    validator = $form.validate({
        rules: validationRules,
        messages: validationMessages,
        errorClass: 'invalidInput',
        validClass: 'validInput',
        submitHandler: () => {},
    });
});

function isAdminFormatter(value, row) {
    return row.roles.includes('admin') ? '<i class="fas fa-check"></i>' : '';
}

function editFormatter(value, row) {
    return value ? `<span style="display: inline;"><i class="fas fa-edit p-1 inTableButton"></i></span>` : '';
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
    const response = await postJSON('/api/users', user);
    const json = await response.json();
    console.log(json);
    if ('success' in json) {
        $(`#${userCreateModal}`).modal('hide');
        showToast('Created User', json.success);
    } else {
        $(`#${userCreateModal}ResponseErrorMessage`).text(json.error);
    }
}

async function postCreateUser() {
    const $form = $(`#${createUserForm}`);
    if ($form.valid()) {
        submitNewUser($form);
    }
}