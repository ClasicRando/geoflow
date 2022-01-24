const sourceValidatorOptions = {
    'code': ValidatorTypes.notEmpty,
    'radius': [
        ValidatorTypes.notEmpty,
        ValidatorTypes.number,
    ],
    'reportType': ValidatorTypes.notEmpty,
    'fileLocation': ValidatorTypes.notEmpty,
    'description': ValidatorTypes.notEmpty,
};
const sourceFormHandler = new FormHandler(sourceForm, sourceValidatorOptions);

const contactValidatorOptions = {
    'email': ValidatorTypes.email,
};
const contactFormHandler = new FormHandler(contactForm, contactValidatorOptions);

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
const selectPipelines = {
    'collection': document.querySelector('select[name=collectionPipeline]'),
    'load': document.querySelector('select[name=loadPipeline]'),
    'check': document.querySelector('select[name=checkPipeline]'),
    'qa': document.querySelector('select[name=qaPipeline]'),
};

document.addEventListener('DOMContentLoaded', async () => {
    addOptions('select[name=assignedUser]', collectionUsers, 'user_id', 'name');
    const selectCountry = document.querySelector('select[name=country]');
    addOptions(selectCountry, countries, 'code', 'name');
    setProvSelect(selectCountry.value);
    selectCountry.addEventListener('change', (e) => setProvSelect(e.target.value));
    addOptions('select[name=warehouseType]', warehouseTypes, 'id', 'name');
    for (const pipeline of pipelines) {
        const isDefault = pipeline.name.includes('Default');
        const currentSelect = selectPipelines[pipeline.workflow_operation];
        addOption(currentSelect, pipeline.pipeline_id, pipeline.name);
        if (isDefault) {
            currentSelect.value = pipeline.pipeline_id;
        }
    }
    $dataSourceTable.on('expand-row.bs.table', (e, index, row, $detail) => {
        expandedDsId = row.ds_id;
    });
    $confirmDeleteContact.on('hidden.bs.modal', (e) => {
        requestDsId = null;
        requestContactId = null;
    });
});

function setProvSelect(countryCode) {
    const provSelect = document.querySelector('select[name=prov]');
    removeAllChildren(provSelect);
    const filteredProvs = [{prov_code: 'null', name: 'N/A'}, ...provs.filter(prov => prov.country_code === countryCode)];
    addOptions(provSelect, filteredProvs, 'prov_code', 'name');
}

function dataSourceActionsFormatter(value, row) {
    const editButton = `<a class="p-1" href="javascript:void(0)" onclick="editDataSource(${row.ds_id})"><i class="fas fa-edit"></i></a>`;
    const contactsButton = `<a class="p-1" href="javascript:void(0)" onclick="showContacts(${row.ds_id})"><i class="fas fa-address-card"></i></a>`
    return `${editButton}${contactsButton}`;
}

function dataSourceContactActionsFormatter(value, row) {
    const editButton = `<a class="p-1" href="javascript:void(0)" onclick="editContact(${row.contact_id})"><i class="fas fa-edit"></i></a>`;
    const deleteButton = `<a class="p-1" href="javascript:void(0)" onclick="confirmDeleteContact(${row.contact_id})"><i class="fas fa-trash"></i></a>`;
    return `${editButton}${deleteButton}`;
}

function newDataSource() {
    sourceFormHandler.resetForm();
    sourceModal.querySelector('.modal-title').textContent = 'Create Data Source';
    $sourceModal.modal('show');
}

function editDataSource(dsId) {
    const dataSource = $dataSourceTable.bootstrapTable('getData').find(ds => ds.ds_id === dsId);
    if (typeof dataSource === 'undefined') {
        showToast('Error', 'Could not find the data source selected');
        return;
    }
    sourceFormHandler.populateForm({
        code: dataSource.code,
        prov: dataSource.prov||'null',
        country: dataSource.country,
        description: dataSource.description,
        fileLocation: dataSource.files_location,
        comments: dataSource.comments,
        assignedUser: dataSource.assigned_user,
        radius: dataSource.search_radius,
        warehouseType: dataSource.record_warehouse_type,
        reportType: dataSource.reporting_type,
        collectionPipeline: dataSource.collection_pipeline,
        loadPipeline: dataSource.load_pipeline,
        checkPipeline: dataSource.check_pipeline,
        qaPipeline: dataSource.qa_pipeline,
    });
    requestDsId = dsId;
    sourceModal.querySelector('.modal-title').textContent = 'Edit Data Source';
    $sourceModal.modal('show');
}

function showContacts(dsId) {
    $contactTable.bootstrapTable('refreshOptions', {url: `/data/data-source-contacts/${dsId}`}).bootstrapTable('refresh');
    requestDsId = dsId;
    contactFormHandler.form.style = 'display: none;';
    $contactModal.modal('show');
}

/**
 * 
 * @param {Event} event 
 */
function exitContacts(event) {
    event.preventDefault();
    contactFormHandler.form.style = 'display: none;';
}

function newContact() {
    requestContactId = null;
    contactFormHandler.resetForm();
    contactFormHandler.form.style = '';
    contactFormHandler.form.querySelector('h3').textContent = 'Create Contact';
}

/**
 * 
 * @param {string} contactString 
 */
function editContact(contactId) {
    const contact = $contactTable.bootstrapTable('getData').find(row => row.contact_id === contactId);
    requestContactId = contact.contact_id;
    contactFormHandler.populateForm(contact);
    contactFormHandler.form.style = '';
    contactFormHandler.form.querySelector('h3').textContent = 'Edit Contact';
}

/**
 * 
 * @param {Event} event 
 */
async function submitContact(event) {
    event.preventDefault();
    if (!contactFormHandler.validate()) {
        return;
    }
    const formData = new FormData(contactFormHandler.form);
    const contact = {
        contact_id: requestContactId,
        ds_id: requestDsId,
        name: formData.get('name'),
        email: formData.get('email'),
        website: formData.get('website'),
        type: formData.get('type'),
        notes: formData.get('notes'),
    };
    const response = contact.contact_id === null ? await fetchPOST('/data/data-source-contacts', contact) : await fetchPATCH('/data/data-source-contacts', contact);
    const json = await response.json();
    if ('payload' in json) {
        $contactTable.bootstrapTable('refresh');
        contactFormHandler.form.style = 'display: none;';
        showToast('Created Contact', `Successful created contact_id = ${json.payload}`);
        requestContactId = null;
    } else {
        contactFormHandler.form.querySelector('p.invalidInput').textContent = formatErrors(json.errors);
    }
}

async function deleteContact() {
    const response = await fetchDELETE(`/data/data-source-contacts/${requestContactId}`);
    const json = await response.json();
    if ('payload' in json) {
        $contactTable.bootstrapTable('refresh');
        showToast('Updated Contact', json.payload);
    } else {
        showToast('Error Updating Contact', json.errors);
    }
    requestContactId = null;
    $confirmDeleteContact.modal('hide');
}

function confirmDeleteContact(contactId) {
    requestContactId = contactId;
    $confirmDeleteContact.modal('show');
}

async function submitSource() {
    if (!sourceFormHandler.validate()) {
        return;
    }
    const prov = formData.get('prov');
    const formData = new FormData(sourceFormHandler.form);
    const source = {
        ds_id: requestDsId,
        code: formData.get('code'),
        prov: prov === 'null' ? null : prov,
        country: formData.get('country'),
        description: formData.get('description'),
        files_location: formData.get('fileLocation'),
        prov_level: prov !== 'null',
        comments: formData.get('comments'),
        assigned_user: formData.get('assignedUser'),
        search_radius: formData.get('radius'),
        record_warehouse_type: formData.get('warehouseType'),
        reporting_type: formData.get('reportType'),
        collection_pipeline: formData.get('collectionPipeline'),
        load_pipeline: formData.get('loadPipeline'),
        check_pipeline: formData.get('checkPipeline'),
        qa_pipeline: formData.get('qaPipeline'),
    };
    const response = source.ds_id === null ? await fetchPOST('/data/data-sources', source) : await fetchPATCH('/data/data-sources', source);
    const json = await response.json();
    if ('payload' in json) {
        $dataSourceTable.bootstrapTable('refresh');
        $sourceModal.modal('hide');
        showToast('Updated User', `Successful created ds_id = ${json.payload}`);
        requestContactId = null;
    } else {
        sourceFormHandler.form.querySelector('p.invalidInput').textContent = formatErrors(json.errors);
    }
}
