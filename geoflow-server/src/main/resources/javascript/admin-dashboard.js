let currentUserOid = null;
const validatorOptions = {
    'fullName': ValidatorTypes.notEmpty,
    'username': [
        ValidatorTypes.notEmpty,
        ValidatorTypes.noWhitespace,
    ],
    'password': ValidatorTypes.password,
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
const userFormHandler = new FormHandler(userForm, validatorOptions);

document.addEventListener('DOMContentLoaded', async () => {
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

/**
 * 
 * @param {Event} event
 * @param {HTMLButtonElement} btnElement
 */
function togglePasswordView(event, btnElement) {
    event.preventDefault();
    const inputField = btnElement.parentNode.parentNode.querySelector('input');
    const svgTag = btnElement.querySelector('svg');
    if (inputField.type === 'text') {
        inputField.type = 'password';
        svgTag.classList.replace('fa-eye','fa-eye-slash');
    } else {
        inputField.type = 'text';
        svgTag.classList.replace('fa-eye-slash','fa-eye');
    }
}

/**
 * 
 * @param {object} value 
 * @param {Object.<string, object>} row 
 * @returns {string}
 */
function isAdminFormatter(value, row) {
    return row.roles.includes('admin') ? '<i class="fas fa-check"></i>' : '';
}

/**
 * 
 * @param {object} value 
 * @param {Object.<string, object>} row 
 * @returns {string}
 */
function editFormatter(value, row) {
    return value ? `<a href="javascript:void(0)" onClick="openUserModal(${row.user_oid})"><i class="fas fa-edit"></i></a>` : '';
}

/**
 * 
 * @param {number?} userOid 
 */
function openUserModal(userOid=null) {
    currentUserOid = userOid;
    userFormHandler.resetForm();
    const passwordGroup = userFormHandler.form.querySelector('label[for=password]').parentNode;
    const repeatPasswordGroup = userFormHandler.form.querySelector('label[for=repeatPassword]').parentNode;
    if (currentUserOid === null) {
        userModal.querySelector('.modal-title').textContent = 'Create User';
        passwordGroup.style.display = '';
        repeatPasswordGroup.style.display = '';
    } else {
        const row = $userTable.bootstrapTable('getData').find(row => row.user_oid === currentUserOid);
        const roles = row.roles;
        userFormHandler.form.querySelector('#fullName').value = row.name;
        userFormHandler.form.querySelector('#username').value = row.username;
        const options = userFormHandler.form.querySelectorAll('#roles option');
        const optionsCount = options.length;
        for (let i = 0; i < optionsCount; i++) {
            options[i].selected = roles.includes(options[i].value);
        }
        userFormHandler.form.querySelector('#isAdmin').checked = roles.includes('admin');
        userModal.querySelector('.modal-title').textContent = 'Edit User';
        passwordGroup.style.display = 'none';
        repeatPasswordGroup.style.display = 'none';
    }
    $userModal.modal('show');
}

/**
 * 
 * @param {HTMLFormElement} form 
 */
async function submitEditUser(form) {
    if (!userFormHandler.validate()) {
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
        $userModal.modal('hide');
        showToast('Created User', `Created ${user.username} (${json.payload})`);
        $userTable.bootstrapTable('refresh');
        currentUserOid = null;
    } else {
        form.querySelector('p.invalidInput').textContent(formatErrors(json.errors));
    }
}
