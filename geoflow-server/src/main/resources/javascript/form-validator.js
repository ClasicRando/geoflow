class ValidatorTypes {
    static notEmpty = '!empty';
    static noWhitespace = '!whitespace';
    static matches = '=';
    static depends = 'depends';
    static oneOrMoreSelected = '>0';
}

class FormValidator {

    constructor(form, options={}) {
        if (typeof form === 'string') {
            this.form = document.querySelector(form);
        } else {
            this.form = form;
        }
        this.options = options;
        this.fields = {};
        for (const field of this.form.querySelectorAll('input[type=text]')) {
            if (field.name in this.options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'textInput',
                    validator: this.options[field.name],
                }
            }
        }
        for (const field of this.form.querySelectorAll('input[type=password]')) {
            if (field.name in this.options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'passwordInput',
                    validator: this.options[field.name],
                }
            }
        }
        for (const field of this.form.querySelectorAll('select:not([multiple=multiple])')) {
            if (field.name in this.options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'multiSelect',
                    validator: this.options[field.name],
                }
            }
        }
        for (const field of this.form.querySelectorAll('select[multiple=multiple]')) {
            if (field.name in this.options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'singleSelect',
                    validator: this.options[field.name],
                }
            }
        }
        for (const field of this.form.querySelectorAll('input[type=checkbox]')) {
            if (field.name in this.options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'checkbox',
                    validator: this.options[field.name],
                }
            }
        }
        for (const field of this.form.querySelectorAll('textArea')) {
            if (field.name in this.options) {
                this.fields[field.name] = {
                    element: field,
                    type: 'textArea',
                    validator: this.options[field.name],
                }
            }
        }
    }

    validate() {
        const formData = new FormData(this.form);
        const handleValidator = (element, value, validator) => {
            const isMultipleValues = value.length > 1;
            const validatorType = typeof validator === 'string' ? validator : validator.type||'';
            switch(validatorType) {
                case ValidatorTypes.notEmpty:
                    if (isMultipleValues) {
                        throw new TypeError('"notEmpty" validator specified for a field with multiple values');
                    }
                    return FormValidator.setFieldState(element, value[0].trim() !== '', validator.message||'Value cannot be empty');
                case ValidatorTypes.noWhitespace:
                    if (isMultipleValues) {
                        throw new TypeError('"noWhitespace" validator specified for a field with multiple values');
                    }
                    const invalidText = /\s/g;
                    return FormValidator.setFieldState(element, !invalidText.test(value[0]), validator.message||'Value cannot contain whitespace');
                case ValidatorTypes.oneOrMoreSelected:
                    return FormValidator.setFieldState(element, isMultipleValues, validator.message||'1 or more values must be selected');
                case ValidatorTypes.matches:
                    if (isMultipleValues) {
                        throw new TypeError('"matches" validator specified for a field with multiple values');
                    }
                    const otherValue = formData.getAll(validator.match);
                    if (otherValue.length > 1) {
                        throw new TypeError('Field specified by "matches" validator has multiple values');
                    }
                    return FormValidator.setFieldState(element, value[0] === otherValue[0], validator.message||`Value from ${validator.match} does not match`);
                case ValidatorTypes.depends:
                    if (typeof validator.method !== 'function') {
                        throw new TypeError('"depends" validator.method must be function');
                    }
                    return FormValidator.setFieldState(element, validator.method(formData), validator.message||'Depends expression was false');
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
}