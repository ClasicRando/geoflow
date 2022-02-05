/** @type {HTMLOListElement} */
let plottingMethodsList = null;
/** @type {{id: number, text: string}[]} */
let tableNameOptions = null;
/** @type {{id: number, text: string}} */
let methodTypeOptions = null;
let options = null;

document.addEventListener('DOMContentLoaded', async () => {
    plottingMethodsList = plottingMethodsModal.querySelector('ol');
    const methodTypesResponse = await fetchApi('/data/plotting-method-types', FetchMethods.GET);
    if (methodTypesResponse.success) {
        methodTypeOptions = methodTypesResponse.payload.map(method => {
            return {
                id: method.method_id,
                text: method.name,
            }
        });
    }
});

/**
 * @param {number} i
 * @returns {HTMLLIElement}
 */
 function plottingMethodListItem(i, order, methodType, stOid) {
    const index = i + 1;
    const liElement = document.createElement('li');
    liElement.classList.add('list-group-item');
    liElement.id = index.toString();
    const row = document.createElement('div');
    row.classList.add('row');
    const orderCol = document.createElement('div');
    orderCol.classList.add('col-2');
    const orderLabel = document.createElement('label');
    orderLabel.htmlFor = `order${index}`;
    orderLabel.textContent = 'Order';
    const orderSelect = document.createElement('select');
    orderSelect.id = `order${index}`;
    orderSelect.name = 'order';
    orderSelect.classList.add('custom-select');
    addOptions(orderSelect, options, 'id', 'text');
    orderSelect.value = order;
    orderCol.appendChild(orderLabel);
    orderCol.appendChild(orderSelect);
    row.appendChild(orderCol);

    const sourceTableCol = document.createElement('div');
    sourceTableCol.classList.add('col');
    const sourceTableLabel = document.createElement('label');
    sourceTableLabel.htmlFor = `sourceTable${index}`;
    sourceTableLabel.textContent = 'Source Table';
    const sourceTableSelect = document.createElement('select');
    sourceTableSelect.id = `sourceTable${index}`;
    sourceTableSelect.name = 'sourceTable';
    sourceTableSelect.classList.add('custom-select');
    addOptions(sourceTableSelect, tableNameOptions, 'id', 'text');
    sourceTableSelect.value = stOid;
    sourceTableCol.appendChild(sourceTableLabel);
    sourceTableCol.appendChild(sourceTableSelect);
    row.appendChild(sourceTableCol);

    const methodCol = document.createElement('div');
    methodCol.classList.add('col');
    const methodLabel = document.createElement('label');
    methodLabel.htmlFor = `method${index}`;
    methodLabel.textContent = 'Method';
    const methodSelect = document.createElement('select');
    methodSelect.id = `method${index}`;
    methodSelect.name = 'method';
    methodSelect.classList.add('custom-select');
    addOptions(methodSelect, methodTypeOptions, 'id', 'text');
    methodSelect.value = methodType;
    methodCol.appendChild(methodLabel);
    methodCol.appendChild(methodSelect);
    row.appendChild(methodCol);

    liElement.appendChild(row);
    return liElement;
}

async function editPlottingMethods() {
    const response = await fetchApi(`/plotting-methods/${runId}`, FetchMethods.GET);
    if (!response.success) {
        showToast('Error', response.errors);
        return;
    }
    const methods = response.payload;
    const methodsCount = methods.length;
    const sourceTablesResponse = await fetchApi(`/source-tables/${runId}`, FetchMethods.GET);
    if (!sourceTablesResponse.success) {
        showToast('Error', sourceTablesResponse.errors);
        return;
    }
    options = (new Array(methodsCount)).fill(undefined).map((_, i) => {
        return {
            id: i + 1,
            text: i + 1,
        }
    });
    tableNameOptions = sourceTablesResponse.payload.map(sourceTable => {
        return {
            id: sourceTable.st_oid,
            text: sourceTable.table_name,
        }
    });
    removeAllChildren(plottingMethodsList);
    for(let i = 0; i < methodsCount; i++) {
        const method = methods[i];
        const methodElement = plottingMethodListItem(i, method.order, method.method_type, method.st_oid);
        plottingMethodsList.appendChild(methodElement);
    }
    $plottingMethodsModal.modal('show');
}

function addPlottingMethod() {
    const lastIndex = plottingMethodsList.querySelectorAll('li').length;
    const methodElement = plottingMethodListItem(lastIndex, '', '', '');
    plottingMethodsList.appendChild(methodElement);
    setOrderNumberOptions();
}

function setOrderNumberOptions() {
    const optionCount = plottingMethodsList.querySelectorAll('li').length;
    options = (new Array(optionCount)).fill(undefined).map((_, i) => {
        return {
            id: i + 1,
            text: (i + 1).toString(),
        }
    });
    for (const element of plottingMethodsList.querySelectorAll('select[name=order]')) {
        if (optionCount !== element.querySelectorAll('option').length) {
            const tempValue = element.value;
            removeAllChildren(element);
            addOptions(element, options, 'id', 'text');
            element.value = tempValue;
        }
    }
}
