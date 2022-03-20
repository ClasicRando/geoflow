let relationshipsSourceTables = [];
let linkingFields = [];
let parentLinkingFields = [];

document.addEventListener('DOMContentLoaded', async () => {
    sourceTableSelector.addEventListener('change', sourceTableChange);
    parentTableSelector.addEventListener('change', parentTableChange);
});

function isGenerated(value) {
    return value ? '<i class="fas fa-check"></i>' : '';
}

async function addRelationship() {
    const response = await fetchApi(`/data/source-tables/${runId}`, FetchMethods.GET);
    removeAllChildren(sourceTableSelector);
    removeAllChildren(parentTableSelector);
    if (response.success) {
        linkingFields = [];
        parentLinkingFields = [];
        relationshipsSourceTables = response.payload;
        addOptions(sourceTableSelector, relationshipsSourceTables, 'st_oid', 'table_name');
        const event = new Event('change');
        sourceTableSelector.dispatchEvent(event);
        $relationshipsModal.modal('show');
    } else {
        showToast('Error', response.errors);
    }
}

async function sourceTableChange() {
    const stOid = sourceTableSelector.value;
    const response = await fetchApi(`/data/all-fields/${stOid}`, FetchMethods.GET);
    if (response.success) {
        linkingFields = response.payload.map(c=>{return {field_id: `${c.field_id}-${c.is_generated}`, name: c.name}});
    } else {
        linkingFields = [];
        showToast('Error', response.errors);
    }
    removeAllChildren(parentTableSelector);
    const parentTables = relationshipsSourceTables.filter(st=>st.st_oid != sourceTableSelector.value);
    addOptions(parentTableSelector, parentTables, 'st_oid', 'table_name');
    const event = new Event('change');
    parentTableSelector.dispatchEvent(event);
}

async function parentTableChange() {
    const stOid = parentTableSelector.value;
    const response = await fetchApi(`/data/all-fields/${stOid}`, FetchMethods.GET);
    if (response.success) {
        parentLinkingFields = response.payload.map(c=>{return {field_id: `${c.field_id}-${c.is_generated}`, name: c.name}});
    } else {
        parentLinkingFields = [];
        showToast('Error', response.errors);
    }
}

async function addLinkingKeyField() {
    const linkingFieldsList = relationshipsModal.querySelector('ol');
    const index = linkingFieldsList.children.length + 1;
    const listItem = document.createElement('li');
    listItem.classList.add('list-group-item');
    listItem.id = `link${index}`;

    const row = document.createElement('div');
    row.classList.add('row', 'py-1');

    const childFieldCol = document.createElement('div');
    childFieldCol.classList.add('col-5');

    const childFieldSelect = document.createElement('select');
    childFieldSelect.classList.add('custom-select');
    childFieldSelect.name = 'childField';
    addOptions(childFieldSelect, linkingFields, 'field_id', 'name');

    childFieldCol.appendChild(childFieldSelect);
    row.appendChild(childFieldCol);

    const parentFieldCol = document.createElement('div');
    parentFieldCol.classList.add('col-5');

    const parentFieldSelect = document.createElement('select');
    parentFieldSelect.classList.add('custom-select');
    parentFieldSelect.name = 'parentField';
    addOptions(parentFieldSelect, parentLinkingFields, 'field_id', 'name');

    parentFieldCol.appendChild(parentFieldSelect);
    row.appendChild(parentFieldCol);

    const removeFieldCol = document.createElement('div');
    removeFieldCol.classList.add('col-2');

    const linkRemoveButton = document.createElement('button');
    linkRemoveButton.classList.add('btn', 'btn-secondary');
    linkRemoveButton.textContent = 'Remove'
    linkRemoveButton.addEventListener('click', (e) => {
        e.target.parentElement.parentElement.parentElement.remove();
    });

    removeFieldCol.appendChild(linkRemoveButton);
    row.appendChild(removeFieldCol);
    listItem.appendChild(row);
    linkingFieldsList.appendChild(listItem);
}

async function commitRelationship() {
    const stOid = sourceTableSelector.value;
    const parentStOid = parentTableSelector.value;
    const links = [];
    forEach(relationshipsModal.querySelectorAll('li'), (item) => {
        const [linkingField,linkingFieldIsGenerated] = item.querySelector('select[name=childField]').value.split('-');
        const [parentLinkingField,parentFieldIsGenerated] = item.querySelector('select[name=parentField]').value.split('-');
        const entry = {
            field_id: linkingField,
            field_is_generated: linkingFieldIsGenerated == 'true',
            parent_field_id: parentLinkingField,
            parent_field_is_generated: parentFieldIsGenerated == 'true',
            st_oid: stOid,
        };
        links.push(entry);
    });
    const body = {
        st_oid: stOid,
        parent_st_oid: parentStOid,
        linking_fields: links,
    };
    const response = await fetchApi('/data/pipeline-relationships', FetchMethods.POST, body);
    if (response.success) {
        $relationshipsModal.modal('hide');
        showToast('Success', response.payload);
        $relationshipsTable.bootstrapTable('refresh');
    } else {
        relationshipsModal.querySelector('p.invalidInput').textContent = formatErrors(response.errors);
    }
}
