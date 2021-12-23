const createContactValidationRules = {
    email: {
        email: true,
    },
};
const createContactValidationMessages = {
    email: {
        email: 'Email field must be a valid email address if not empty'
    },
};
let createContactValidator = null;
let provs = [];
let countries = {};

$(document).ready(async () => {
    const response = await fetch('/data/provs');
    if (response.status === 200) {
        const json = await response.json();
        provs = 'payload' in json ? json.payload : [];
        const $selectCountries = $('select[name=country]');
        const temp = new Set(provs.map(prov => prov.country_code));
        countries = Array.from(temp).map(country => {
            return {
                code: country,
                name: provs.find(prov => prov.country_code === country).country_name,
            }
        });
        for (const country of countries) {
            $selectCountries.append(`<option value="${country.code}">${country.name}</option>`);
        }
        setProvSelect($selectCountries.parents('form').find('select[name=prov]'), $selectCountries.val());
    }
    const response2 = await fetch('/data/rec-warehouse-types');
    if (response2.status === 200) {
        const json2 = await response2.json();
        const warehouseTypes = 'payload' in json2 ? json2.payload : [];
        const $selectWarehouseTypes = $('select[name=warehouseType]');
        for (const type of warehouseTypes) {
            $selectWarehouseTypes.append(`<option value="${type.id}">${type.name}</option>`);
        }
    }
    $('select[name=country]').change((e) => {
        const $countrySelect = $(e.target);
        const countryCode = $countrySelect.val();
        const $provSelect = $countrySelect.parents('form').find('select[name=prov]');
        setProvSelect($provSelect, countryCode);
    });
    createContactValidator = $(`#${createContactFormId}`).validate({
        rules: createContactValidationRules,
        messages: createContactValidationMessages,
        errorClass: 'invalidInput',
        validClass: 'validInput',
        submitHandler: () => {},
    });
});

function setProvSelect($provSelect, countryCode) {
    $provSelect.empty();
    $provSelect.append('<option value="null">N/A</option>');
    for (const prov of provs.filter(prov => prov.country_code === countryCode)) {
        $provSelect.append(`<option value="${prov.prov_code}">${prov.name}</option>`);
    }
}

function dataSourceActionsFormatter(value, row) {
    const editButton = `<i class="fas fa-edit p-1 inTableButton" onclick="editDataSource(${row.ds_id})"></i>`;
    const addButton = `<i class="fas fa-plus p-1 inTableButton" onclick="newContact(${row.ds_id})"></i>`;
    return `<span style="display: inline;">${editButton}${addButton}</span>`;
}

function dataSourceContactActionsFormatter(value, row) {
    return `<i class="fas fa-plus p-1 inTableButton" onclick="editContact(${row.contact_id})"></i>`;
}

function newDataSource() {
    $(`#${createSourceModalId}`).modal('show');
}

function editDataSource(dsId) {
    // $(`#${editSourceModal}`).modal('show');
}

function newContact(dsId) {
    $(`#${createContactModal}`).modal('show');
}

function editContact(contactId) {
    $(`#${editContactModalId}`).modal('show');
}

function postNewContact($form) {

}

function putNewContact($form) {

}

function postNewSource($form) {

}

function putNewSource($form) {

}
