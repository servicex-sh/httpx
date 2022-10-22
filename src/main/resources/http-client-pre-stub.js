const nodeCrypto = require("crypto");

class HttpClientRequest {
    variables = new RequestVariables();
}

class RequestVariables {
    set(varName, varValue) {
        console.log("__variable:" + varName + "," + varValue);
    }
}

class DigestBuilder {

    constructor(hashOrHmac) {
        this.hashOrHmac = hashOrHmac;
    }

    updateWithText(textInput, encoding) {
        this.hash = this.hashOrHmac.update(Buffer.from(textInput, encoding || "utf-8"));
        return this;
    }

    updateWithHex(hexInput) {
        this.hash = this.hashOrHmac.update(Buffer.from(hexInput, "hex"))
        return this;
    }

    updateWithBase64(base64Input, urlSafe) {
        if (urlSafe) {
            this.hash = this.hashOrHmac.update(Buffer.from(base64Input, "base64url"));
        } else {
            this.hash = this.hashOrHmac.update(Buffer.from(base64Input, "base64"));
        }
        return this;
    }

    digest() {
        return new Digest(this.hash);
    }
}

class HmacInitializer {
    constructor(name) {
        this.name = name;
    }

    withTextSecret(textSecret, encoding) {
        var hmac = nodeCrypto.createHmac(this.name, Buffer.from(textSecret, encoding || "utf-8"))
        return new DigestBuilder(hmac);
    }

    withHexSecret(hexSecret) {
        var hmac = nodeCrypto.createHmac(this.name, Buffer.from(hexSecret, "hex"))
        return new DigestBuilder(hmac);
    }

    withBase64Secret(base64Secret, urlSafe) {
        var hmac;
        if (urlSafe) {
            hmac = nodeCrypto.createHmac(this.name, Buffer.from(base64Secret, "base64url"));
        } else {
            hmac = nodeCrypto.createHmac(this.name, Buffer.from(base64Secret, "base64"));
        }
        return new DigestBuilder(hmac);
    }
}

class Digest {
    constructor(hash) {
        this.hash = hash;
    }

    toHex() {
        return this.hash.digest("hex");
    }

    toBase64(urlSafe) {
        if (urlSafe) {
            return this.hash.digest("base64url");
        } else {
            return this.hash.digest("base64");
        }
    };
}

const request = new HttpClientRequest();

const crypto = {
    sha1: function () {
        return new DigestBuilder(nodeCrypto.createHash("sha1"));
    },
    sha256: function () {
        return new DigestBuilder(nodeCrypto.createHash("sha256"));
    },
    sha512: function () {
        return new DigestBuilder(nodeCrypto.createHash("sha512"));
    },
    md5: function () {
        return new DigestBuilder(nodeCrypto.createHash("md5"));
    },

    hmac: {
        sha1: function () {
            return new HmacInitializer("sha1");
        },

        sha256: function () {
            return new HmacInitializer("sha256");
        },

        sha512: function () {
            return new HmacInitializer("sha512");
        },

        md5: function () {
            return new HmacInitializer("md5");
        }
    }

}

//let output = crypto.hmac.md5().withTextSecret("secret").updateWithText("text1").digest().toHex();
//console.log(output);