let sourceTables = [];
let commitStOid = null;
let commitStcOid = null;
let commitGtcOid = null;
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
            removeAllChildren(sourceTableSelector);
            removeAllChildren(parentTableSelector);
            if (response.success) {
                sourceTables = response.payload;
                addOptions(sourceTableSelector, sourceTables, 'st_oid', 'table_name');
                const event = new Event('change');
                sourceTableSelector.dispatchEvent(event);
                addOptions(parentTableSelector, [{st_oid: null, table_name: ''}, ...sourceTables], 'st_oid', 'table_name');
            } else {
                showToast('Error', response.errors);
            }
        });
    }
    sourceTableSelector.addEventListener('change', sourceTableChange);
    parentTableSelector.addEventListener('change', parentTableChange);
});

async function sourceTableChange() {
    removeAllChildren(sourceFieldsList);
    removeAllChildren(linkingKey);
    const stOid = sourceTableSelector.value;
    if (stOid === '') {
        return;
    }
    commitStOid = parseInt(stOid);
    const response = await fetchApi(`/data/source-table-columns/${stOid}`, FetchMethods.GET);
    if (response.success) {
        $sourceFieldsList.bootstrapTable(
            'refreshOptions',
            {
                showHeader: true,
                url: `http://localhost:8080/data/source-table-columns/${stOid}`,
            }
        ).bootstrapTable('refresh');
        $generatedFieldsList.bootstrapTable(
            'refreshOptions',
            {
                showHeader: true,
                url: `http://localhost:8080/data/generated-table-columns/${stOid}`,
            }
        ).bootstrapTable('refresh');
        addOptions(linkingKey, response.payload, 'stc_oid', 'name');
    } else {
        $sourceFieldsList.bootstrapTable(
            'refreshOptions',
            {
                showHeader: true,
            }
        ).bootstrapTable('refresh');
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
        editSourceFieldForm.resetForm();
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
    };
    const response = await fetchApi('/data/source-table-columns', FetchMethods.PUT, body);
    if (response.success) {
        $editSourceField.modal('hide');
        showToast('Updated Source Field', `Updated source field for stc_oid = '${commitStcOid}'`);
        $sourceFieldsList.bootstrapTable('refresh');
        commitStcOid = null;
    } else {
        editSourceFieldForm.form.querySelector('p.invalidInput').textContent = formatErrors(response.errors);
    }
}

function addGeneratedField() {
    commitGtcOid = null;
    editGeneratedFieldForm.resetForm();
    editGeneratedField.querySelector('.modal-title').textContent = 'Create Generated Field';
    $editGeneratedField.modal('show');
}

async function commitGeneratedField() {

}

function generatedFieldAction(value, row) {
    return `
        <a class="p-2" href="javascript:void(0)" onclick="editGeneratedFieldLogic(${row.gtc_oid})">
            <i class="fas fa-edit" ></i>
        </a>
        <a class="p-2" href="javascript:void(0)" onclick="deleteGeneratedFieldLogic(${row.gtc_oid})">
            <i class="fas fa-trash" ></i>
        </a>
    `;
}

function editGeneratedFieldLogic(gtcOid) {
    commitGtcOid = gtcOid;
    const data = $generatedFieldsList.bootstrapTable('getData').find(row => row.gtc_oid === gtcOid);
    if (typeof data !== 'undefined') {
        editGeneratedField.querySelector('.modal-title').textContent = 'Edit Generated Field';
        editGeneratedFieldForm.resetForm();
        editGeneratedFieldForm.populateForm({
            name: data.name,
            label: data.label,
            reportGroup: data.report_group,
            expression: data.generation_expression,
        });
        $editGeneratedField.modal('show');
    } else {
        showToast('Error', `Could not find a row for gtc_oid = '${stcOid}'`);
    }
}

async function commitGeneratedField() {
    if (!editGeneratedFieldForm.validate()) {
        return;
    }
    const formData = editGeneratedFieldForm.formData;
    const body = {
        gtc_oid: commitGtcOid,
        st_oid: commitStOid,
        name: formData.get('name'),
        label: formData.get('label'),
        report_group: formData.get('reportGroup'),
        generation_expression: formData.get('expression'),
    };
    const response = await fetchApi('/data/generated-table-columns', commitGtcOid === null ? FetchMethods.POST : FetchMethods.PUT, body);
    if (response.success) {
        $editGeneratedField.modal('hide');
        if (commitGtcOid === null) {
            showToast('Inserted Generated Field', `Inserted generated field for gtc_oid = '${response.payload}'`);
        } else {
            showToast('Updated Generated Field', `Updated generated field for gtc_oid = '${commitGtcOid}'`);
        }
        $generatedFieldsList.bootstrapTable('refresh');
        commitGtcOid = null;
    } else {
        editGeneratedFieldForm.form.querySelector('p.invalidInput').textContent = formatErrors(response.errors);
    }
}

function deleteGeneratedFieldLogic(gtcOid) {
    commitGtcOid = gtcOid;
    $generatedFieldDeleteModal.modal('show');
}

async function commitGeneratedFieldDelete() {
    const response = await fetchApi(`/data/generated-table-columns/${commitGtcOid}`, FetchMethods.DELETE);
    if (response.success) {
        $generatedFieldDeleteModal.modal('hide');
        showToast('Deleted Generated Field', `Deleted generated field for gtc_oid = '${commitGtcOid}'`);
        $generatedFieldsList.bootstrapTable('refresh');
        commitGtcOid = null;
    } else {
        showToast('Error', response.errors);
    }
}
