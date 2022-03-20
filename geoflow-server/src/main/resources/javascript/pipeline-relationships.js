let relationshipsSourceTables = [];
let linkingFields = [];
let parentLinkingFields = [];
let commitRelationshipStOid = null;

document.addEventListener('DOMContentLoaded', async () => {
    sourceTableSelector.addEventListener('change', sourceTableChange);
    parentTableSelector.addEventListener('change', parentTableChange);
});

function isGenerated(value) {
    return value ? '<i class="fas fa-check"></i>' : '';
}

function relationshipActions(value, row) {
    return `
        <a class="p-2" href="javascript:void(0)" onclick="editRelationship(${row.st_oid})">
            <i class="fas fa-edit"></i>
        </a>
        <a class="p-2" href="javascript:void(0)" onclick="deleteRelationship(${row.st_oid})">
            <i class="fas fa-trash"></i>
        </a>
    `;
}

async function addRelationship() {
    const response = await fetchApi(`/data/source-tables/${runId}`, FetchMethods.GET);
    sourceTableSelector.disabled = false;
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

async function editRelationship(stOid) {
    commitRelationshipStOid = stOid;
    const response = await fetchApi(`/data/source-tables/${runId}`, FetchMethods.GET);
    sourceTableSelector.disabled = false;
    removeAllChildren(sourceTableSelector);
    removeAllChildren(parentTableSelector);
    removeAllChildren(relationshipsModal.querySelector('ol'));
    if (response.success) {
        linkingFields = [];
        parentLinkingFields = [];
        relationshipsSourceTables = response.payload;
        addOptions(sourceTableSelector, relationshipsSourceTables, 'st_oid', 'table_name');
        const data = $relationshipsTable.bootstrapTable('getData').find(row => row.st_oid == stOid);
        sourceTableSelector.value = data.st_oid;
        sourceTableSelector.disabled = true;
        populateParentTables();
        parentTableSelector.value = data.parent_st_oid;
        const fieldResponse = await fetchApi(`/data/pipeline-relationship-fields/${data.st_oid}`, FetchMethods.GET);
        if (fieldResponse.success) {
            await getLinkingFields(data.st_oid);
            await getParentLinkingFields(data.parent_st_oid);
            forEach(fieldResponse.payload, (link) => {
                addLinkingKeyField(
                    `${link.field_id}-${link.field_is_generated}`,
                    `${link.parent_field_id}-${link.parent_field_is_generated}`,
                );
            });
        }
        $relationshipsModal.modal('show');
    } else {
        showToast('Error', response.errors);
    }
}

async function deleteRelationship(stOid) {
    commitRelationshipStOid = stOid;
    $relationshipDeleteModal.modal('show');
}

async function sourceTableChange() {
    const stOid = sourceTableSelector.value;
    await getLinkingFields(stOid);
    removeAllChildren(relationshipsModal.querySelector('ol'));
    populateParentTables();
    const event = new Event('change');
    parentTableSelector.dispatchEvent(event);
}

function populateParentTables() {
    removeAllChildren(parentTableSelector);
    const parentTables = relationshipsSourceTables.filter(st => st.st_oid != sourceTableSelector.value);
    addOptions(parentTableSelector, parentTables, 'st_oid', 'table_name');
}

async function getLinkingFields(stOid) {
    const response = await fetchApi(`/data/all-fields/${stOid}`, FetchMethods.GET);
    if (response.success) {
        linkingFields = response.payload.map(c=>{return {field_id: `${c.field_id}-${c.is_generated}`, name: c.name}});
    } else {
        linkingFields = [];
        showToast('Error', response.errors);
    }
}

async function parentTableChange() {
    const stOid = parentTableSelector.value;
    await getParentLinkingFields(stOid);
}

async function getParentLinkingFields(stOid) {
    const response = await fetchApi(`/data/all-fields/${stOid}`, FetchMethods.GET);
    if (response.success) {
        parentLinkingFields = response.payload.map(c=>{return {field_id: `${c.field_id}-${c.is_generated}`, name: c.name}});
    } else {
        parentLinkingFields = [];
        showToast('Error', response.errors);
    }
}

/**
 * 
 * @param {string} id 
 * @param {string} parent_id 
 */
function addLinkingKeyField(id=null,parent_id=null) {
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
    if (id !== null) {
        childFieldSelect.value = id;
    }

    childFieldCol.appendChild(childFieldSelect);
    row.appendChild(childFieldCol);

    const parentFieldCol = document.createElement('div');
    parentFieldCol.classList.add('col-5');

    const parentFieldSelect = document.createElement('select');
    parentFieldSelect.classList.add('custom-select');
    parentFieldSelect.name = 'parentField';
    addOptions(parentFieldSelect, parentLinkingFields, 'field_id', 'name');
    if (parent_id !== null) {
        parentFieldSelect.value = parent_id;
    }

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

async function commitDeleteRelationship() {
    const response = await fetchApi(`/data/pipeline-relationships/${commitRelationshipStOid}`, FetchMethods.DELETE);
    if (response.success) {
        $relationshipDeleteModal.modal('hide');
        showToast('Deleted Relationship', `Deleted relationship for st_oid = ${commitRelationshipStOid}`);
        $relationshipsTable.bootstrapTable('refresh');
        commitRelationshipStOid = null;
    } else {
        showToast('Error', response.errors);
    }
}
