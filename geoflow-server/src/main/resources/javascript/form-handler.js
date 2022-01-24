class ValidatorTypes {
    static notEmpty = '!empty';
    static noWhitespace = '!whitespace';
    static matches = '=';
    static depends = 'depends';
    static oneOrMoreSelected = '>0';
    static email = '@'
    static length = 'len';
    static password = 'pass';
    static number = '#';
}

/**
 * 
 * @property {HTMLFormElement | string} form
 * @property {Object.<string, Object.<string, string | Function>>} fields
 */
class FormHandler {

    /**
     * 
     * @param {HTMLFormElement | string} form 
     * @param {Object.<string, string | Function>} options 
     */
    constructor(form, options) {
        if (typeof form === 'string') {
            this.form = document.querySelector(form);
        } else {
            this.form = form;
        }
        this.fields = {};
        for (const field of this.form.querySelectorAll('input[type=text]')) {
            if (field.name in options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'textInput',
                    validator: options[field.name],
                }
            }
        }
        for (const field of this.form.querySelectorAll('input[type=password]')) {
            if (field.name in options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'passwordInput',
                    validator: options[field.name],
                }
            } else {
                this.fields[field.name] = {
                    element: field,
                    type: 'passwordInput',
                    validator: ValidatorTypes.password,
                }
            }
        }
        for (const field of this.form.querySelectorAll('select:not([multiple=multiple])')) {
            if (field.name in options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'singleSelect',
                    validator: options[field.name],
                }
            }
        }
        for (const field of this.form.querySelectorAll('select[multiple=multiple]')) {
            if (field.name in options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'multiSelect',
                    validator: options[field.name],
                }
            }
        }
        for (const field of this.form.querySelectorAll('input[type=checkbox]')) {
            if (field.name in options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'checkbox',
                    validator: options[field.name],
                }
            }
        }
        for (const field of this.form.querySelectorAll('textArea')) {
            if (field.name in options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'textArea',
                    validator: options[field.name],
                }
            }
        }
    }

    validate() {
        const formData = new FormData(this.form);
        const handleValidator = (element, value, validator) => {
            const whitespaceCheck = /\s/g;
            const isMultipleValues = value.length > 1;
            const validatorType = typeof validator === 'string' ? validator : validator.type||'';
            switch(validatorType) {
                case ValidatorTypes.notEmpty:
                    if (isMultipleValues) {
                        throw new TypeError('"notEmpty" validator specified for a field with multiple values');
                    }
                    return FormHandler.setFieldState(element, value[0].trim() !== '', validator.message||'Value cannot be empty');
                case ValidatorTypes.noWhitespace:
                    if (isMultipleValues) {
                        throw new TypeError('"noWhitespace" validator specified for a field with multiple values');
                    }
                    return FormHandler.setFieldState(element, !whitespaceCheck.test(value[0]), validator.message||'Value cannot contain whitespace');
                case ValidatorTypes.oneOrMoreSelected:
                    return FormHandler.setFieldState(element, isMultipleValues, validator.message||'1 or more values must be selected');
                case ValidatorTypes.matches:
                    if (isMultipleValues) {
                        throw new TypeError('"matches" validator specified for a field with multiple values');
                    }
                    const otherValue = formData.getAll(validator.match);
                    if (otherValue.length > 1) {
                        throw new TypeError('Field specified by "matches" validator has multiple values');
                    }
                    return FormHandler.setFieldState(element, value[0] === otherValue[0], validator.message||`Value from ${validator.match} does not match`);
                case ValidatorTypes.depends:
                    if (typeof validator.method !== 'function') {
                        throw new TypeError('"depends" validator.method must be function');
                    }
                    return FormHandler.setFieldState(element, validator.method(formData), validator.message||'Depends expression was false');
                case ValidatorTypes.email:
                    if (isMultipleValues) {
                        throw new TypeError('"email" validator specified for a field with multiple values');
                    }
                    const emailCheck = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
                    const isValidEmail = value[0].trim() === '' || emailCheck.test(value[0]);
                    return FormHandler.setFieldState(element, isValidEmail, validator.message||'Must be an email address');
                case ValidatorTypes.number:
                    if (isMultipleValues) {
                        throw new TypeError('"number" validator specified for a field with multiple values');
                    }
                    let isValidNumber = true;
                    try {
                        Number.parseFloat(value[0])
                    } catch {
                        isValidNumber = false;
                    }
                    return FormHandler.setFieldState(element, isValidNumber, validator.message||'Value must be a number');
                case ValidatorTypes.length:
                    if (isMultipleValues) {
                        throw new TypeError('"length" validator specified for a field with multiple values');
                    }
                    const maxLength = validator.max||Number.MAX_VALUE;
                    const minLength = validator.min||0;
                    return FormHandler.setFieldState(element, value[0].length <= maxLength && value[0].length >= minLength, validator.message||`Value must be between ${minLength}-${maxLength}`);
                case ValidatorTypes.password:
                    if (isMultipleValues) {
                        throw new TypeError('"password" validator specified for a field with multiple values');
                    }
                    const password = value[0];
                    const containsChars = /[A-Z]/g.test(password) && /[a-z]/g.test(password) && /\d/g.test(password) && /[^A-Z0-9]/gi.test(password);
                    const isValidPassword = !whitespaceCheck.test(password) && password !== '' && password.length >= 8 && containsChars;
                    const message = 'Passwords must be 8 or more non-whitespace characters, containing at least 1 uppper, lower, number and other character';
                    return FormHandler.setFieldState(element, isValidPassword, message);
            }
        };
        let isValid = true;
        try {
            for (const [key, field] of Object.entries(this.fields)) {
                if (Array.isArray(field.validator)) {
                    const value = formData.getAll(key);
                    for (const validator of field.validator) {
                        if (!handleValidator(field.element, value, validator)) {
                            isValid = false;
                            break;
                        }
                    }
                } else {
                    isValid = isValid && handleValidator(field.element, formData.getAll(key), field.validator);
                }
            }
        } catch (ex) {
            console.log(ex);
            isValid = false;
        }
        return isValid;
    }

    /**
     * 
     * @param {HTMLElement} element 
     * @param {boolean} isValid 
     * @param {string} invalidMessage 
     * @returns 
     */
    static setFieldState(element, isValid, invalidMessage) {
        if (isValid) {
            element.classList.remove('invalidInput');
            element.classList.add('validInput');
            const messageNode = element.parentNode.querySelector('.invalidMessage');
            if (messageNode !== null) {
                messageNode.remove();
            }
        } else {
            element.classList.remove('validInput');
            element.classList.add('invalidInput');
            const messageNode = element.parentNode.querySelector('.invalidMessage');
            if (messageNode === null) {
                const invalidMessageNode = document.createElement('div');
                invalidMessageNode.classList.add('invalidInput', 'invalidMessage');
                invalidMessageNode.textContent = invalidMessage;
                element.parentNode.appendChild(invalidMessageNode);
            }
        }
        return isValid;
    }

    resetForm() {
        const inputs = this.form.querySelectorAll('input');
        const inputsCount = inputs.length;
        for (let i = 0; i < inputsCount; i++) {
            if (inputs[i].type === 'checkbox') {
                inputs[i].checked = false;
            } else {
                inputs[i].value = '';
            }
        }
        const singleSelects = this.form.querySelectorAll('select:not([multiple=multiple])');
        const singleSelectsCount = singleSelects.length;
        for (let j = 0; j < singleSelectsCount; j++) {
            const singleSelect = singleSelects[j];
            const defaultOption = Array.from(singleSelect.options).find(option => option.textContent.includes('Default'));
            if (typeof defaultOption !== 'undefined') {
                singleSelect.value = defaultOption.value;
            } else {
                singleSelect.value = singleSelect.firstChild.value;
            }
        }
        const multiSelects = this.form.querySelectorAll('select[multiple=multiple]');
        const multiSelectsCount = multiSelects.length;
        for (let k = 0; k < multiSelectsCount; k++) {
            multiSelects[k].value = '';
        }
        const textAreas = this.form.querySelectorAll('textArea');
        const textAreasCount = textAreas.length;
        for (let l = 0; l < textAreasCount; l++) {
            textAreas[l].value = '';
        }
        const errorMessage = this.form.querySelector('p.invalidInput');
        if (errorMessage !== null) {
            errorMessage.textContent = '';
        }
    }

    /**
     * 
     * @param {Object.<string, object>} obj 
     */
    populateForm(obj) {
        for (const [key, value] of Object.entries(obj)) {
            const element = this.form.querySelector(`[name=${key}]`);
            if (element === null) {
                console.log(`Cannot find element for "${key}"`);
            } else if (element.type === 'checkbox') {
                element.checked = value;
            } else if (element.nodeName === 'SELECT') {
                element.value = value;
                if (element.value !== '') {
                    continue;
                }
                const option = Array.from(element.options).find(option => option.textContent === value);
                if (typeof option !== 'undefined') {
                    element.value = option.value;
                } else {
                    throw `Cannot find value of "${value}" for field name of "${key}"`;
                }
            } else {
                element.value = value;
            }
        }
    }
}