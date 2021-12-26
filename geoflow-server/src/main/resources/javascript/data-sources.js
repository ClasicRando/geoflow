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

const editContactValidationRules = {
    email: {
        email: true,
    },
};
const editContactValidationMessages = {
    email: {
        email: 'Email field must be a valid email address if not empty'
    },
};
let editContactValidator = null;

const createSourceValidationRules = {
    editCode: 'required',
    editRadius: {
        required: true,
        number: true,
    },
    editReportType: 'required',
    editFileLocation: 'required',
    editDescription: 'required',
};
const createSourceValidationMessages = {
    editCode: 'Source code cannot be empty',
    editRadius: {
        required: 'Radius value cannot be empty',
        number: 'Radius value has be a number',
    },
    editReportType: 'Report type cannot be empty',
    editFileLocation: 'File location cannot be empty',
    editDescription: 'Description cannot be empty',
};
let createSourceValidator = null;

const editSourceValidationRules = {
    editCode: 'required',
    editRadius: {
        required: true,
        number: true,
    },
    editReportType: 'required',
    editFileLocation: 'required',
    editDescription: 'required',
};
const editSourceValidationMessages = {
    editCode: 'Source code cannot be empty',
    editRadius: {
        required: 'Radius value cannot be empty',
        number: 'Radius value has be a number',
    },
    editReportType: 'Report type cannot be empty',
    editFileLocation: 'File location cannot be empty',
    editDescription: 'Description cannot be empty',
};
let editSourceValidator = null;

let requestDsId = null;
let requestContactId = null;
let expandedDsId = null;
const provs = JSON.parse(provsJson).payload||[];
const countries = Array.from(new Set(provs.map(prov => prov.country_code))).map(country => {
    return {
        code: country,
        name: provs.find(prov => prov.country_code === country).country_name,
    }
});
const collectionUsers = JSON.parse(collectionUsersJson).payload||[];
const warehouseTypes = JSON.parse(warehouseTypesJson).payload||[];
const pipelines = JSON.parse(pipelinesJson).payload||[];

$(document).ready(async () => {
    const $assingedUserSelect = $('select[name=assignedUser]');
    for (const user of collectionUsers) {
        $assingedUserSelect.append(`<option value="${user.user_id}">${user.name}</option>`)
    }
    const $selectCountries = $('select[name=country]');
    for (const country of countries) {
        $selectCountries.append(`<option value="${country.code}">${country.name}</option>`);
    }
    setProvSelect($selectCountries.parents('form').find('select[name=prov]'), $selectCountries.val());
    const $selectWarehouseTypes = $('select[name=warehouseType]');
    for (const type of warehouseTypes) {
        $selectWarehouseTypes.append(`<option value="${type.id}">${type.name}</option>`);
    }
    const $selectCollectionPipelines = $('select[name=collectionPipeline]');
    const $selectLoadPipelines = $('select[name=loadPipeline]');
    const $selectCheckPipelines = $('select[name=checkPipeline]');
    const $selectQaPipelines = $('select[name=qaPipeline]');
    for (const pipeline of pipelines) {
        const isDefault = pipeline.name.includes('Default');
        switch (pipeline.workflow_operation) {
            case 'collection':
                $selectCollectionPipelines.append(`<option value="${pipeline.pipeline_id}">${pipeline.name}</option>`);
                if (isDefault) {
                    $selectCollectionPipelines.val(pipeline.pipeline_id)
                }
                break;
            case 'load':
                $selectLoadPipelines.append(`<option value="${pipeline.pipeline_id}">${pipeline.name}</option>`);
                if (isDefault) {
                    $selectLoadPipelines.val(pipeline.pipeline_id)
                }
                break;
            case 'check':
                $selectCheckPipelines.append(`<option value="${pipeline.pipeline_id}">${pipeline.name}</option>`);
                if (isDefault) {
                    $selectCheckPipelines.val(pipeline.pipeline_id)
                }
                break;
            case 'qa':
                $selectQaPipelines.append(`<option value="${pipeline.pipeline_id}">${pipeline.name}</option>`);
                if (isDefault) {
                    $selectQaPipelines.val(pipeline.pipeline_id)
                }
                break;
        }
    }
    $('select[name=country]').change((e) => {
        const $countrySelect = $(e.target);
        const countryCode = $countrySelect.val();
        const $provSelect = $countrySelect.parents('form').find('select[name=prov]');
        setProvSelect($provSelect, countryCode);
    });
    $(`#${dataSourceTableId}`).on('expand-row.bs.table', (e, index, row, $detail) => {
        expandedDsId = row.ds_id;
    });
    $(`#${confirmDeleteContactId}`).on('hidden.bs.modal', (e) => {
        requestDsId = null;
        requestContactId = null;
    });
    $(`#${createSourceModalId}`).on('hidden.bs.modal', (e) => {
        requestDsId = null;
        requestContactId = null;
    });
    $(`#${createContactModalId}`).on('hidden.bs.modal', (e) => {
        requestDsId = null;
        requestContactId = null;
    });
    createContactValidator = $(`#${createContactFormId}`).validate({
        rules: createContactValidationRules,
        messages: createContactValidationMessages,
        errorClass: 'invalidInput',
        validClass: 'validInput',
        submitHandler: () => {},
    });
    editContactValidator = $(`#${editContactFormId}`).validate({
        rules: editContactValidationRules,
        messages: editContactValidationMessages,
        errorClass: 'invalidInput',
        validClass: 'validInput',
        submitHandler: () => {},
    });
    createSourceValidator = $(`#${createSourceFormId}`).validate({
        rules: createSourceValidationRules,
        messages: createSourceValidationMessages,
        errorClass: 'invalidInput',
        validClass: 'validInput',
        submitHandler: () => {},
    });
    editSourceValidator = $(`#${editSourceFormId}`).validate({
        rules: editSourceValidationRules,
        messages: editSourceValidationMessages,
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
    const editButton = `<i class="fas fa-edit p-1 inTableButton" onclick="editContact('${JSON.stringify(row, null, null)}')"></i>`
    const deleteButton = `<i class="fas fa-trash p-1 inTableButton" onclick="confirmDeleteContact(${row.contact_id})"></i>`;
    return `<span style="display: inline;">${editButton}${deleteButton}</span>`;
}

function newDataSource() {
    $(`#${createSourceFormId}ResponseErrorMessage`).text('');
    $('#createCode').val('');
    $('#createRadius').val('');
    $('#createReportType').val('');
    $('#createFileLocation').val('');
    $('#createDescription').val('');
    $('#createComments').val('');
    setDefaultOption('#createCountry');
    setDefaultOption('#createProv');
    setDefaultOption('#createCollectionPipeline');
    setDefaultOption('#createLoadPipeline');
    setDefaultOption('#createCheckPipeline');
    setDefaultOption('#createQaPipeline');
    setDefaultOption('#createWarehouseType');
    setDefaultOption('#createAssignedUser');
    $(`#${createSourceModalId}`).modal('show');
}

function editDataSource(dsId) {
    const dataSource = $(`#${dataSourceTableId}`).bootstrapTable('getData').find(ds => ds.ds_id === dsId);
    if (typeof dataSource === "undefined") {
        showToast('Error', 'Could not find the data source selected');
        return;
    }
    setOptionByText('#editCollectionPipeline', dataSource.collection_pipeline);
    setOptionByText('#editLoadPipeline', dataSource.load_pipeline);
    setOptionByText('#editCheckPipeline', dataSource.check_pipeline);
    setOptionByText('#editQaPipeline', dataSource.qa_pipeline);
    setOptionByText('#editWarehouseType', dataSource.record_warehouse_type);
    setOptionByText('#editAssignedUser', dataSource.assigned_user);
    $('#editCode').val(dataSource.code);
    $('#editRadius').val(dataSource.search_radius);
    $('#editCountry').val(dataSource.country);
    $('#editReportType').val(dataSource.reporting_type);
    $('#editFileLocation').val(dataSource.files_location);
    $('#editDescription').val(dataSource.description);
    $('#editComments').val(dataSource.comments);
    $('#editProv').val(dataSource.prov||'null');
    requestDsId = dsId;
    $(`#${editSourceModalId}`).modal('show');
}

function setDefaultOption(selector) {
    const $el = $(selector);
    $el.val($el.find(selector.includes('Pipeline') ? 'option:contains(Default)' : ':first-child').val());
}

function setOptionByText(selector, text) {
    const $el = $(selector);
    $el.val($el.find(`option:contains(${text})`).val());
}

function newContact(dsId) {
    requestDsId = dsId;
    requestContactId = null;
    $(`#${createContactFormId}ResponseErrorMessage`).text('');
    $('#createName').val('');
    $('#createEmail').val('');
    $('#createWebsite').val('');
    $('#createType').val('');
    $('#createNotes').val('');
    $(`#${createContactModalId}`).modal('show');
}

function editContact(contactString) {
    const contact = JSON.parse(contactString);
    requestContactId = contact.contact_id;
    requestDsId = expandedDsId;
    $('#editName').val(contact.name);
    $('#editEmail').val(contact.email);
    $('#editWebsite').val(contact.country);
    $('#editType').val(contact.type);
    $('#editNotes').val(contact.notes);
    $(`#${editContactModalId}`).modal('show');
}

function getContact($form) {
    return {
        contact_id: requestContactId,
        ds_id: requestDsId,
        name: $form.find('input[name=name]').val(),
        email: $form.find('input[name=email]').val(),
        website: $form.find('input[name=website]').val(),
        type: $form.find('input[name=type]').val(),
        notes: $form.find('textarea[name=notes]').val(),
    }
}

async function postContact($form) {
    if ($form.valid()) {
        const contact = getContact($form);
        const response = await fetchPOST('/data/data-source-contacts', contact);
        const json = await response.json();
        if ('payload' in json) {
            $(`#${dataSourceTableId}`).bootstrapTable('refresh');
            $(`#${createContactModalId}`).modal('hide');
            showToast('Created Contact', `Successful created contact_id = ${json.payload}`);
        } else {
            $(`#${createContactModalId}ResponseErrorMessage`).text(formatErrors(json.errors));
        }
    }
}

async function putContact($form) {
    if ($form.valid()) {
        const contact = getContact($form);
        const response = await fetchPATCH('/data/data-source-contacts', contact);
        const json = await response.json();
        if ('payload' in json) {
            $(`#${dataSourceTableId}`).bootstrapTable('refresh');
            $(`#${editContactModalId}`).modal('hide');
            showToast('Updated Contact', `Successful updated contact_id = ${json.payload.contact_id}`);
        } else {
            $(`#${editContactModalId}ResponseErrorMessage`).text(formatErrors(json.errors));
        }
    }
}

async function deleteContact() {
    const response = await fetchDELETE(`/data/data-source-contacts/${requestContactId}`);
    const json = await response.json();
    if ('payload' in json) {
        $(`#${dataSourceTableId}`).bootstrapTable('refresh');
        showToast('Updated Contact', json.payload);
    } else {
        showToast('Error Updating Contact', formatErrors(json.errors));
    }
    $(`#${confirmDeleteContactId}`).modal('hide');
}

function confirmDeleteContact(contactId) {
    requestContactId = contactId;
    $(`#${confirmDeleteContactId}`).modal('show');
}

function getDataSource($form) {
    const prov = $form.find('select[name=prov]').val();
    return {
        ds_id: requestDsId,
        code: $form.find('input[name=code]').val(),
        prov: prov === 'null' ? null : prov,
        country: $form.find('select[name=country]').val(),
        description: $form.find('textarea[name=description]').val(),
        files_location: $form.find('input[name=fileLocation]').val(),
        prov_level: prov !== 'null',
        comments: $form.find('textarea[name=comments]').val(),
        assigned_user: $form.find('select[name=assignedUser]').val(),
        search_radius: $form.find('input[name=radius]').val(),
        record_warehouse_type: $form.find('select[name=warehouseType]').val(),
        reporting_type: $form.find('input[name=reportType]').val(),
        collection_pipeline: $form.find('select[name=collectionPipeline]').val(),
        load_pipeline: $form.find('select[name=loadPipeline]').val(),
        check_pipeline: $form.find('select[name=checkPipeline]').val(),
        qa_pipeline: $form.find('select[name=qaPipeline]').val(),
    }
}

async function postDataSource($form) {
    if ($form.valid()) {
        const dataSource = getDataSource($form);
        const response = await fetchPOST('/data/data-sources', dataSource);
        const json = await response.json();
        if ('payload' in json) {
            $(`#${dataSourceTableId}`).bootstrapTable('refresh');
            $(`#${editSourceModalId}`).modal('hide');
            showToast('Updated User', `Successful created ds_id = ${json.payload}`);
        } else {
            $(`#${editSourceModalId}ResponseErrorMessage`).text(formatErrors(json.errors));
        }
    }
}

async function patchDataSource($form) {
    if ($form.valid()) {
        const dataSource = getDataSource($form);
        const response = await fetchPATCH('/data/data-sources', dataSource);
        const json = await response.json();
        if ('payload' in json) {
            $(`#${dataSourceTableId}`).bootstrapTable('refresh');
            $(`#${editSourceModalId}`).modal('hide');
            showToast('Updated User', `Successful updated ds_id = ${json.payload.ds_id}`);
        } else {
            $(`#${editSourceModalId}ResponseErrorMessage`).text(formatErrors(json.errors));
        }
    }
}
