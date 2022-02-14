let sourceTables = [];
let commitStcOid = null;
const editSourceOptions = {
    'label': ValidatorTypes.notEmpty,
    'reportGroup': ValidatorTypes.number,
};
const editSourceFieldForm = new FormHandler(editSourceField.querySelector('form'), editSourceOptions);
const editGeneratedOptions = {
    'label': ValidatorTypes.notEmpty,
    'reportGroup': ValidatorTypes.number,
    'expression': ValidatorTypes.notEmpty,
};
const editGeneratedFieldForm = new FormHandler(editGeneratedField.querySelector('form'), editGeneratedOptions);

document.addEventListener('DOMContentLoaded', async () => {
    const loadingLogicTab = document.querySelector('#loading-logic-tab');
    if (loadingLogicTab != null) {
        $(loadingLogicTab).on('show.bs.tab', async (e) => {
            const response = await fetchApi(`/data/source-tables/${runId}`, FetchMethods.GET);
            if (response.success) {
                sourceTables = response.payload;
            } else {
                showToast('Error', response.errors);
            }
            removeAllChildren(sourceTableSelector);
            removeAllChildren(parentTableSelector);
            addOptions(sourceTableSelector, sourceTables, 'st_oid', 'table_name');
            const event = new Event('change');
            sourceTableSelector.dispatchEvent(event);
            addOptions(parentTableSelector, [{st_oid: null, table_name: ''}, ...sourceTables], 'st_oid', 'table_name');
        });
    }
    sourceTableSelector.addEventListener('change', sourceTableChange);
    parentTableSelector.addEventListener('change', parentTableChange);
});

async function sourceTableChange() {
    removeAllChildren(sourceFieldsList);
    removeAllChildren(linkingKey);
    const stOid = sourceTableSelector.value;
    const response = await fetchApi(`/data/source-table-columns/${stOid}`, FetchMethods.GET);
    if (response.success) {
        $sourceFieldsList.bootstrapTable(
            'refreshOptions',
            {
                showHeader: true,
                url: `http://localhost:8080/data/source-table-columns/${stOid}`,
            }
        ).bootstrapTable('refresh');
        addOptions(linkingKey, response.payload, 'stc_oid', 'name');
    } else {
        showToast('Error', response.errors);
    }
}

async function parentTableChange() {
    removeAllChildren(parentLinkingKey);
    const stOid = parentTableSelector.value;
    if (stOid === 'null') {
        parentLinkingKeyRow.classList.add('hidden');
        return;
    }
    parentLinkingKeyRow.classList.remove('hidden');
    const response = await fetchApi(`/data/source-table-columns/${stOid}`, FetchMethods.GET);
    if (response.success) {
        addOptions(parentLinkingKey, response.payload, 'stc_oid', 'name');
    } else {
        showToast('Error', response.errors);
    }
}

function sourceFieldListEdit(value, row) {
    return `
        <a class="p-2" href="javascript:void(0)" onclick="editSourceFieldLogic(${row.stc_oid})">
            <i class="fas fa-edit" ></i>
        </a>
    `;
}

function editSourceFieldLogic(stcOid) {
    commitStcOid = stcOid;
    const data = $sourceFieldsList.bootstrapTable('getData').find(row => row.stc_oid === stcOid);
    if (typeof data !== 'undefined') {
        editSourceFieldForm.populateForm({
            name: data.name,
            label: data.label,
            reportGroup: data.report_group,
        });
        $editSourceField.modal('show');
    } else {
        showToast('Error', `Could not find a row for stc_oid = '${stcOid}'`);
    }
}

async function commitSourceField() {
    if (!editSourceFieldForm.validate()) {
        return;
    }
    const formData = editSourceFieldForm.formData;
    const body = {
        stc_oid: commitStcOid,
        label: formData.get('label'),
        report_group: formData.get('reportGroup'),
    }
    const response = await fetchApi('/data/source-table-columns', FetchMethods.POST, body);
    if (response.success) {
        $editSourceField.modal('hide');
        showToast('Updated Source Field', `Updated source field for stc_oid = '${commitStcOid}'`);
        $sourceFieldsList.bootstrapTable('refresh');
        commitStcOid = null;
    } else {
        editSourceFieldForm.form.querySelector('p.invalidInput').textContent(formatErrors(json.errors));
    }
}
