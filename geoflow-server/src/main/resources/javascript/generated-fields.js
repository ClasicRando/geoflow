let generatedFieldSourceTables = [];
let commitgeneratedFieldStOid = null;
let commitGtcOid = null;

const editGeneratedOptions = {
    'name': ValidatorTypes.notEmpty,
    'label': ValidatorTypes.notEmpty,
    'reportGroup': ValidatorTypes.number,
    'expression': ValidatorTypes.notEmpty,
};
const editGeneratedFieldForm = new FormHandler(editGeneratedField.querySelector('form'), editGeneratedOptions);

document.addEventListener('DOMContentLoaded', async () => {
    const generatedFieldsTab = document.querySelector('#generated-fields-tab');
    if (generatedFieldsTab != null) {
        $(generatedFieldsTab).on('show.bs.tab', async (e) => {
            const response = await fetchApi(`/data/source-tables/${runId}`, FetchMethods.GET);
            removeAllChildren(generatedSourceTableSelect);
            if (response.success) {
                generatedFieldSourceTables = response.payload;
                addOptions(generatedSourceTableSelect, generatedFieldSourceTables, 'st_oid', 'table_name');
                const event = new Event('change');
                generatedSourceTableSelect.dispatchEvent(event);
            } else {
                showToast('Error', response.errors);
            }
        });
    }
    generatedSourceTableSelect.addEventListener('change', generatedSourceTableChange);
});

async function generatedSourceTableChange() {
    const stOid = generatedSourceTableSelect.value;
    if (stOid === '') {
        return;
    }
    $generatedFieldsList.bootstrapTable(
        'refreshOptions',
        {
            showHeader: true,
            url: `http://localhost:8080/data/generated-table-columns/${stOid}`,
        }
    ).bootstrapTable('refresh');
    commitgeneratedFieldStOid = parseInt(stOid);
}

function addGeneratedField() {
    commitGtcOid = null;
    editGeneratedFieldForm.resetForm();
    editGeneratedField.querySelector('.modal-title').textContent = 'Create Generated Field';
    $editGeneratedField.modal('show');
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
        st_oid: commitgeneratedFieldStOid,
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
