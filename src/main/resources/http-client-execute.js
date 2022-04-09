class ContentType {
    mimeType = ""
    charset = "utf-8";

    constructor(mimeType) {
        this.mimeType = mimeType;
    }
}

class Variables {
    store = new Map();

    set(varName, varValue) {
        this.store.set(varName, varValue);
    }


    get(varName) {
        return this.store.get(varName)
    }

    isEmpty() {
        return this.store.size === 0
    }


    clear(varName) {
        this.store.delete(varName)
    }

    clearAll() {
        this.store.clear()
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
    global = new Variables();

    constructor(variables) {
        for (const [key, value] of Object.entries(variables)) {
            this.global.set(key, value)
        }
    }

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
const variables = {id: 111};
const client = new HttpClient(variables);
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

