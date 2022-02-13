/**
 * @typedef {Object} JQuery
 * @method bootstrapTable
 */

class TableSubscriber {

    /**
     * 
     * @param {string} url 
     * @param {string|HTMLTableElement} table 
     */
    constructor(url, table) {
        /**
         * @type {string}
         */
        this.url = url;
        /**
         * @type {HTMLTableElement}
         */
        this.table = typeof table === 'string' ? document.querySelector(table) : table;
        /**
         * @type {JQuery}
         */
        this.$table = $(table);
        /**
         * @type {WebSocket}
         */
        this.socket = new WebSocket(url);
        /**
         * 
         * @type {HTMLDivElement?}
         */
        this.tableDiv = null;
        const handler = (e) => { this.handleEvent(e); }
        this.socket.addEventListener('message', handler);
        this.socket.addEventListener('open', handler);
        this.socket.addEventListener('error', handler);
        this.socket.addEventListener('close', handler);
        const postHeaderHandler = (e) => {
            this.tableDiv = this.table.closest('div.bootstrap-table');
            this.tableDiv.querySelector('button[name=btnConnected]').addEventListener('click', async (e) => {
                if (tasksSubscriber.isActive) {
                    tasksSubscriber.handleEvent('open');
                } else {
                    const isActive = await tasksSubscriber.attemptRestart();
                    if (isActive) {
                        showToast('Reconnected', 'Connected to subscriber!');
                    } else {
                        showToast('Error', 'Attempted restart of subscriber failed');
                    }
                }
            });
        };
        this.$table.on('post-header.bs.table', postHeaderHandler);
    }

    /**
     * @returns {boolean}
     */
    get isActive() {
        return this.socket.readyState === WebSocket.prototype.OPEN;
    }

    /**
     * @returns {{}[]}
     */
    get tableData() {
        return this.$table.bootstrapTable('getData');
    }

    reloadData() {
        const data = this.tableData;
        this.$table.bootstrapTable('load', data);
    }

    /**
     * @returns {Promise<boolean>}
     */
    attemptRestart() {
        try {
            this.socket.close();
        } catch (ex) {
            console.log(ex);
        }
        const button = this.tableDiv.querySelector('button[name="btnConnected"]');
        button.querySelector('.fa-layers').classList.add('hidden');
        button.disabled = true;
        button.appendChild(spinner);
        this.socket = new WebSocket(this.url);
        const handler = (e) => { this.handleEvent(e); };
        const callback = () => {
            this.socket.addEventListener('error', handler);
            this.socket.addEventListener('close', handler);
            this.socket.addEventListener('message', handler);
        }
        return new Promise((resolve) => {
            setTimeout(() => {
                button.disabled = false;
                button.querySelector('.spinner-border').remove();
                button.querySelector('.fa-layers').classList.remove('hidden');
                if (this.socket.readyState === WebSocket.prototype.OPEN) {
                    handler(new Event('open'));
                    callback();
                    resolve(true);
                } else {
                    handler(new Event('close'));
                    resolve(false);
                }
            }, 3000)
        });
    }

    /**
     * 
     * @param {Event} event 
     */
    handleEvent(event) {
        const slashIcon = this.tableDiv.querySelector('.fa-slash');
        const isHidden = slashIcon.classList.value.includes('hidden');
        switch(event.type) {
            case 'open':
                if (!isHidden) {
                    slashIcon.classList.add('hidden');
                }
                break;
            case 'message':
                this.$table.bootstrapTable('load', JSON.parse(event.data));
                break;
            case 'error':
                if (isHidden) {
                    slashIcon.classList.remove('hidden');
                }
                break;
            case 'close':
                if (isHidden) {
                    slashIcon.classList.remove('hidden');
                }
                break;
        }
    }
}

class FetchMethods {
    static GET = 'GET';
    static POST = 'POST';
    static PUT = 'PUT';
    static PATCH = 'PATCH';
    static DELETE = 'DELETE';
}

/** @type {{string: TableSubscriber}} */
var subscriberTables = {};
document.addEventListener('DOMContentLoaded', () => {
    for (const element of document.querySelectorAll('[data-sub=true]')) {
        const wsUrl = element.attributes['data-sub-url'].value;
        subscriberTables[element.id] = new TableSubscriber(wsUrl, element);
    }
});

const apiFetchOptions = {
    headers: {
        'Content-Type': 'application/json',
    },
    credentials: "include",
}

function redirect(route) {
    window.location.assign(route);
}

/**
 * 
 * @param {string} url 
 * @param {string} method
 * @param {{}} body 
 */
async function fetchApi(url, method, body={}) {
    if (!(method in FetchMethods)) {
        throw `Fetch method of "${method}" is not valid`;
    }
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: method === FetchMethods.GET ? null : JSON.stringify(body),
    };
    const response = await fetch(url.startsWith('/data') || url.startsWith('http') ? url : `/data/${url.trimStart('/')}`, options);
    if (response.status !== 200) {
        return {
            success: false,
            status: response.status,
            payload: null,
            object: null,
            errorCode: response.status,
            errors: [{error: response.statusText}],
        };
    }
    try {
        const json = await response.json();
        return {
            success: 'payload' in json,
            status: response.status,
            payload: json.payload||null,
            object: json.object||null,
            errorCode: json.code||null,
            errors: json.errors||null,
        }
    } catch {
        return {
            success: false,
            status: response.status,
            payload: null,
            object: null,
            errorCode: response.status,
            errors: [{error: await response.text()}],
        };
    }
}

async function fetchApiGET(url) {
    const response = await fetch(url.startsWith('/data') ? url : `/data/${url.trimStart('/')}`);
    if (response.status !== 200) {
        return {
            success: false,
            status: response.status,
            payload: null,
            object: null,
            errorCode: response.status,
            errors: [{error: response.statusText}],
        };
    }
    try {
        const json = await response.json();
        return {
            success: 'payload' in json,
            status: response.status,
            payload: json.payload||null,
            object: json.object||null,
            errorCode: json.code||null,
            errors: json.errors||null,
        }
    } catch {
        return {
            success: false,
            status: response.status,
            payload: null,
            object: null,
            errorCode: response.status,
            errors: [{error: await response.text()}],
        };
    }
}

function fetchJSON(method, url, data) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    };
    return fetch(url, options);
}

function fetchPUT(url, data={}) {
    return fetchJSON('PUT', url, data);
}

function fetchPATCH(url, data={}) {
    return fetchJSON('PATCH', url, data);
}

function fetchPOST(url, data={}) {
    return fetchJSON('POST', url, data);
}

function fetchDELETE(url) {
    return fetchJSON('DELETE', url, {});
}

function clickableTd(value, row, index) {
    return {classes: 'clickCell'};
}

async function setPlottingMethods() {
    return '';
}

function showToast(title, message) {
    const container = document.getElementById('toasts');
    const toastDiv = document.createElement('div');
    const toastBody = Array.isArray(message) ? formatErrors(message) : message;
    toastDiv.innerHTML = `
    <div class="toast hide" role="alert" data-delay="10000" aria-live="assertive" aria-atomic="true">
        <div class="toast-header">
            <img src="http://localhost:8080/favicon.ico" class="rounded mr-2">
            <strong class="mr-2">${title}</strong>
            <small>just now</small>
            <button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">
                <span aria-hidden="true">&times;</span>
            </button>
        </div>
        <div class="toast-body">
            ${toastBody}
        </div>
    </div>
    `;
    container.appendChild(toastDiv);
    const $toast = $(toastDiv.querySelector('.toast'));
    $toast.toast('show');
    $toast.on('hidden.bs.toast', (e) => {
        toastDiv.remove();
    });
}

function formatErrors(errors) {
    return errors.map(error => `${error.error_name} -> ${error.message}`).join('\n');
}

function titleCase(title) {
    return title.replace(
        /\w\S*/g,
        function(txt) {
            return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
        }
    ).replace(
        'Id',
        'ID'
    );
}

function alternateCitiesFormatter(value, row) {
    return value.join(',');
}

/**
 * @param {!HTMLSelectElement} selectElement
 * @param {string} value
 * @param {string} text
 */
function addOption(selectElement, value, text) {
    const optionElement = document.createElement('option');
    optionElement.value = value;
    optionElement.textContent = text;
    selectElement.add(optionElement);
}

/**
 * @param {!HTMLSelectElement|!string} selectElement
 * @param {Array.<Object.<string, string>>} objects
 * @param {string} valueField
 * @param {string} textField
 */
function addOptions(selectElement, objects, valueField, textField) {
    const element = typeof selectElement === 'string' ? document.querySelector(selectElement) : selectElement;
    forEach(objects, (item) => {
        const optionElement = document.createElement('option');
        optionElement.value = item[valueField];
        optionElement.textContent = item[textField];
        element.add(optionElement);
    });
}

/**
 * @param {HTMLElement} element
 */
function removeAllChildren(element) {
    while (element.lastChild) {
        element.removeChild(element.lastChild);
    }
}

/**
 * 
 * @param {Array<T>|NodeList<T>|Iterable<T>} collection 
 * @param {function(T): void|function(number, T): void} func 
 */
function forEach(collection, func) {
    if (collection === null) {
        return;
    }
    if (typeof collection === 'undefined') {
        return;
    }
    if (typeof func !== 'function') {
        throw TypeError(`func: Expected Function, got ${func}`);
    }
    if (func.length > 2) {
        throw TypeError(`func: Function should have 1-2 parameters, got ${func.length}`);
    }
    const isIterator = collection[Symbol.iterator] === 'function';
    if (isIterator) {
        for (const [i, item] of collection.entries()) {
            if (func.length === 1) {
                func(item);
            } else {
                func(i, item);
            }
        }
    } else if (collection instanceof Array || collection instanceof NodeList) {
        const itemCount = collection.length;
        for (let i = 0; i < itemCount; i++) {
            if (func.length === 1) {
                func(collection[i]);
            } else {
                func(i, collection[i]);
            }
        }
    } else {
        throw TypeError(`collection: Expected Array, NodeList or Iterable, got ${collection}`);
    }
}
