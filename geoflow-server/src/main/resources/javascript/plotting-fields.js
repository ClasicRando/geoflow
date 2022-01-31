const plottingFieldsFormHandler = typeof plottingFieldsForm !== 'undefined' && plottingFieldsForm !== null ? new FormHandler(plottingFieldsForm) : null;
var plottingFieldsStOid = null;

async function plottingFields(stOid) {
    const fieldsResponse = await fetchApi(`/data/plotting-fields/source-table/${stOid}`, FetchMethods.GET);
    const columnsResponse = await fetchApi(`/data/source-table-columns/${stOid}`, FetchMethods.GET);
    if (!columnsResponse.success) {
        showToast('Error', `Could not find source columns for st_oid = ${stOid}`);
        return;
    }
    const columnsOptions = columnsResponse.payload.map(column => {
        return {
            value: column.name,
            text: column.name,
        }
    })
    plottingFieldsStOid = stOid;
    forEach(plottingFieldsFormHandler.form.querySelectorAll('select:not([multiple=multiple])'), (element) => {
        addOption(element, '', '');
        addOptions(element, columnsOptions, 'value', 'text');
    });
    forEach(plottingFieldsFormHandler.form.querySelectorAll('select[multiple=multiple]'), (element) => {
        addOptions(element, columnsOptions, 'value', 'text');
    });
    if (fieldsResponse.success && (fieldsResponse.payload||[]).length === 1) {
        const payload = fieldsResponse.payload[0];
        plottingFieldsFormHandler.populateForm({
            mergeKey: payload.merge_key,
            companyName: payload.name,
            addressLine1: payload.address_line1,
            addressLine2: payload.address_line2,
            city: payload.city,
            alternateCities: payload.alternate_cities,
            mailCode: payload.mail_code,
            prov: payload.prov,
            latitude: payload.latitude,
            longitude: payload.longitude,
        });
    } else {
        plottingFieldsFormHandler.resetForm();
    }
    $plottingFieldsModal.modal('show');
}

async function submitPlottingFields() {
    const data = plottingFieldsFormHandler.formData;
    const plottingFields = {
        st_oid: plottingFieldsStOid,
        merge_key: data.get('mergeKey'),
        name: data.get('companyName'),
        address_line1: data.get('addressLine1'),
        address_line2: data.get('addressLine2'),
        city: data.get('city'),
        alternate_cities: data.getAll('alternateCities'),
        mail_code: data.get('mailCode'),
        prov: data.get('prov'),
        latitude: data.get('latitude'),
        longitude: data.get('longitude'),
        clean_address: null,
        clean_city: null,
    };
    const response = await fetchApi('/data/plotting-fields', FetchMethods.POST, plottingFields);
    if (response.success) {
        $plottingFieldsTable.bootstrapTable('refresh');
        $plottingFieldsModal.modal('hide');
        showToast('Set Plotting Fields', `Set plotting fields for st_oid = ${plottingFieldsStOid}`);
        plottingFieldsStOid = null;
    } else {
        plottingFieldsForm.querySelector(p.invalidInput).textContent = formatErrors(json.errors);
    }
}

function plottingFieldsActions(value, row) {
    const editButton = `
        <a class="p-2" href="javascript:void(0)" onClick="plottingFields(${row.st_oid})">
            <i class="fas fa-edit"></i>
        </a>
    `;
    const deleteButton = `
        <a class="p-2" href="javascript:void(0)" onClick="confirmDeletePlottingFields(${row.st_oid})">
            <i class="fas fa-trash"></i>
        </a>
    `;
    return `${editButton}${deleteButton}`;
}

function confirmDeletePlottingFields(stOid) {
    deletePlottingFieldsStOid = stOid;
    $confirmDeletePlottingFields.modal('show');
}

async function deletePlottingFields() {
    const response = await fetchDELETE(`/data/plotting-fields/${deletePlottingFieldsStOid}`);
    const json = await response.json();
    if ('errors' in json) {
        showToast('Error', json.errors);
    } else {
        $plottingFieldsTable.bootstrapTable('refresh');
        showToast('Deleted Plotting Fields', `Deleted plotting fields for st_oid = ${deletePlottingFieldsStOid}`);
        deletePlottingFieldsStOid = null;
    }
    $confirmDeletePlottingFields.modal('hide');
}
