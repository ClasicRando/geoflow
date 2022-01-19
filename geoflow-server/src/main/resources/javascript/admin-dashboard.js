let currentUserOid = null;
const validatorOptions = {
    'fullName': {
        'type': ValidatorTypes.notEmpty,
        'message': 'Cannot be empty',
    },
    'username': [
        {
            'type': ValidatorTypes.notEmpty,
            'message': 'Cannot be empty',
        },
        {
            'type': ValidatorTypes.noWhitespace,
            'message': 'Cannot contain whitespace',
        },
    ],
    'password': [
        {
            'type': ValidatorTypes.notEmpty,
            'message': 'Cannot be empty',
        },
        {
            'type': ValidatorTypes.noWhitespace,
            'message': 'Cannot contain whitespace',
        },
    ],
    'repeatPassword': {
        'type': ValidatorTypes.matches,
        'match': 'password',
    },
    'roles': {
        'type': ValidatorTypes.depends,
        'method': (formData) => {
            return (formData.get('isAdmin') === 'on' ? ['admin'] : formData.getAll('roles')).length > 0;
        },
        'message': 'User must have at least 1 role or the admin role',
    },
};
const validator = new FormValidator(`#${userEditForm}`, validatorOptions);

$(document).ready(async () => {
    const response = await fetchApiGET('/data/roles');
    if (response.success) {
        for (const select of document.getElementsByName('roles')) {
            for (const role of response.payload) {
                const option = document.createElement('option');
                option.value = role.name;
                option.innerText = role.description;
                select.appendChild(option);
            };
        }
    } else {
        showToast(`Error: ${response.errorCode}`, response.errors);
    }

    document.getElementById('isAdmin').addEventListener('change', (e) => {
        const form = e.target.parentNode.parentNode;
        const select = form.querySelector('.custom-select');89
        if (e.target.checked) {
            select.disabled = true;
            select.classList.remove('invalidInput');
            select.classList.add('validInput');
            for (const option of select.querySelectorAll('option')) {
                option.selected = false;
            }
            const error = form.querySelector('#roles-error');
            if (error !== null) {
                error.remove();
            }
        } else {
            select.disabled = false;
            select.classList.remove('validInput');
        }
    });
});

function isAdminFormatter(value, row) {
    return row.roles.includes('admin') ? '<i class="fas fa-check"></i>' : '';
}

function editFormatter(value, row) {
    return value ? `<a href="javascript:void(0)" onClick="openUserEditModal(${row.user_oid})"><i class="fas fa-edit"></i></a>` : '';
}

function openUserEditModal(userOid=null) {
    currentUserOid = userOid;
    const row = $(`#${userTable}`).bootstrapTable('getData').find(row => row.user_oid === userOid)||{};
    const roles = row.roles||'';
    const modal = document.querySelector(`#${userEditModal}`);
    const form = modal.querySelector('form');
    form.querySelector('#fullName').value = row.name||'';
    form.querySelector('#username').value = row.username||'';
    for (const option of form.querySelectorAll('#roles option')) {
        option.selected = roles.includes(option.value);
    }
    form.querySelector('#isAdmin').checked = roles.includes('admin');
    const passwordLabel = form.querySelector('label[for=password]');
    const password = form.querySelector('#password');
    const repeatPasswordLabel = form.querySelector('label[for=repeatPassword]');
    const repeatPassword = form.querySelector('#repeatPassword');
    if (currentUserOid === null) {
        modal.querySelector('.modal-title').textContent = 'Create User';
        password.value = '';
        passwordLabel.style.display = '';
        password.style.display = '';
        repeatPassword.value = '';
        repeatPasswordLabel.style.display = '';
        repeatPassword.style.display = '';
    } else {
        modal.querySelector('.modal-title').textContent = 'Edit User';
        password.value = '';
        passwordLabel.style.display = 'none';
        password.style.display = 'none';
        repeatPassword.value = '';
        repeatPasswordLabel.style.display = 'none';
        repeatPassword.style.display = 'none';
    }
    $(modal).modal('show');
}

async function submitEditUser(form) {
    if (!validator.validate()) {
        return;
    }
    const formData = new FormData(form);
    const user = {
        fullName: formData.get('fullName'),
        username: formData.get('username'),
        user_oid: currentUserOid,
        password: formData.get('password'),
        roles: formData.get('isAdmin') === 'on' ? ['admin'] : formData.getAll('roles'),
    };
    const response = user.user_oid === null ? await fetchPOST('/data/users', user) : await fetchPUT('/data/users', user);
    const json = await response.json();
    if ('payload' in json) {
        $(`#${userEditModal}`).modal('hide');
        showToast('Created User', `Created ${user.username} (${json.payload})`);
        $(`#${userTable}`).bootstrapTable('refresh');
        currentUserOid = null;
    } else {
        document.querySelector('div.modal-footer p.invalidInput').textContent(formatErrors(json.errors));
    }
}
