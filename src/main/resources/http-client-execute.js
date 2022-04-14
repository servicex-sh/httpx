// noinspection JSUnusedLocalSymbols
const fs = require('fs');
const os = require("os");

class ContentType {
    mimeType = ""
    charset = "utf-8";

    constructor(mimeType) {
        this.mimeType = mimeType;
    }
}

class GlobalVariables {
    store = undefined
    globalVariablesFile = undefined

    set(varName, varValue) {
        this.getStore()[varName] = varValue;
        fs.writeFileSync(this.globalVariablesFile, JSON.stringify(this.store, null, 2));
    }

    get(varName) {
        return this.getStore()[varName]
    }

    isEmpty() {
        return Object.keys(this.getStore()).length === 0;
    }

    clear(varName) {
        delete this.getStore()[varName]
        fs.writeFileSync(this.globalVariablesFile, JSON.stringify(this.store, null, 2));
    }

    clearAll() {
        if (!this.isEmpty()) {
            this.store = {};
            fs.writeFileSync(this.globalVariablesFile, '{}');
        }
    }

    getStore() {
        if (this.store === undefined) {
            this.initStore();
        }
        return this.store;
    }

    initStore() {
        try {
            const userHomeDir = os.homedir();
            const servicexDir = `${userHomeDir}/.servicex`;
            this.globalVariablesFile = `${servicexDir}/global_variables.json`
            if (!fs.existsSync(servicexDir)) {
                fs.mkdirSync(servicexDir);
            }
            if (!fs.existsSync(this.globalVariablesFile)) {
                this.store = {};
                fs.writeFileSync(this.globalVariablesFile, '{}');
            } else {
                // noinspection JSCheckFunctionSignatures
                this.store = JSON.parse(fs.readFileSync(this.globalVariablesFile));
            }
        } catch (err) {
            console.error(err)
        }
    }
}

class ResponseHeaders {
    store = new Map();

    valueOf(headerName) {
        return this.store.get(headerName)
    }

    valuesOf(headerName) {
        let value = this.store.get(headerName);
        if (value == null) {
            return []
        } else if (value && !Array.isArray(value)) {
            return [value];
        } else {
            return value;
        }
    }

    add(name, value) {
        this.store.set(name, value);
    }
}

class HttpResponse {
    /**
     * response body: object if application/json
     *
     * @type {string | object | LineStreamResponse}
     */
    body;
    headers = new ResponseHeaders();
    status = 200;
    /**
     *  content type
     * @type {ContentType}
     */
    contentType;

    constructor(status, contentType) {
        this.status = status;
        if (typeof contentType === "string") {
            let parts = contentType.split(";");
            this.contentType = new ContentType(parts[0]);
            if (parts.length > 1) {
                this.contentType.charset = parts[1];
            }
        }
    }

    setHeaders(headers) {
        for (const [key, value] of Object.entries(headers)) {
            this.headers.add(key, value);
        }
    }

    setBase64Body(base64Text) {
        let bodyText = decodeURIComponent(Buffer.from(base64Text, 'base64').toString('utf-8'))
        if (this.contentType.mimeType.indexOf("json") >= 0) {
            this.body = JSON.parse(bodyText);
        } else {
            this.body = bodyText;
        }
    }

}

class HttpClient {
    global = new GlobalVariables();

    test(testName, func) {
        func();
    }

    log(message) {
        console.log(message);
    }

    assert(condition, message) {
        console.assert(condition, message);
    }

    exit() {
        process.exit(0)
    }
}

function encodeBody(plainText) {
    return Buffer.from(encodeURIComponent(plainText)).toString('base64');
}

const statusCode = 222;
const contentType = 'application/json';
const headers = {'header': 'value'};
const client = new HttpClient();
const response = new HttpResponse(statusCode, contentType);
response.setHeaders(headers);
const base64Body = encodeBody({});
response.setBase64Body(base64Body);

/*
client.test("Request executed successfully", function () {
    client.log(response.status)
    client.log(response.contentType)
    client.log(response.body)
});
*/

