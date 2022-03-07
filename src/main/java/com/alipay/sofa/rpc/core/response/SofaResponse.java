package com.alipay.sofa.rpc.core.response;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Sofa RPC Response class
 *
 * @author <a href=mailto:hongwei.yhw@antfin.com>HongWei Yi</a>
 */
public final class SofaResponse implements Serializable {

    private static final long serialVersionUID = -4364536436151723421L;

    /**
     * 框架异常
     */
    private boolean isError = false;

    /**
     * 框架异常的消息
     */
    private String errorMsg;

    /**
     * 业务返回或者业务异常
     */
    private Object appResponse;

    /**
     * extensional properties
     */
    private Map<String, String> responseProps;

    //====================== 下面是非传递属性 ===============

    /**
     * 序列化类型
     */
    private transient byte serializeType;

    /**
     * Gets app response.
     *
     * @return the app response
     */
    public Object getAppResponse() {
        return appResponse;
    }

    /**
     * Sets app response.
     *
     * @param response the response
     */
    public void setAppResponse(Object response) {
        appResponse = response;
    }

    /**
     * Is error boolean.
     *
     * @return the boolean
     */
    public boolean isError() {
        return isError;
    }

    /**
     * Gets error msg.
     *
     * @return the error msg
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Sets error msg.
     *
     * @param error the error
     */
    public void setErrorMsg(String error) {
        if (error == null) {
            return;
        }
        errorMsg = error;
        isError = true;
    }

    /**
     * Gets response prop.
     *
     * @param key the key
     * @return the response prop
     */
    public Object getResponseProp(String key) {
        return responseProps == null ? null : responseProps.get(key);
    }

    /**
     * Add response prop.
     *
     * @param key   the key
     * @param value the value
     */
    public void addResponseProp(String key, String value) {
        if (responseProps == null) {
            responseProps = new HashMap<String, String>(16);
        }
        if (key != null && value != null) {
            responseProps.put(key, value);
        }
    }

    /**
     * Remove response props.
     *
     * @param key the key
     */
    public void removeResponseProp(String key) {
        if (responseProps != null && key != null) {
            responseProps.remove(key);
        }
    }

    /**
     * Gets response props.
     *
     * @return the response props
     */
    public Map<String, String> getResponseProps() {
        return responseProps;
    }

    /**
     * Sets response props.
     *
     * @param responseProps the response props
     */
    public void setResponseProps(Map<String, String> responseProps) {
        this.responseProps = responseProps;
    }

    /**
     * Gets serialize type.
     *
     * @return the serialize type
     */
    public byte getSerializeType() {
        return serializeType;
    }

    /**
     * Sets serialize type.
     *
     * @param serializeType the serialize type
     * @return the serialize type
     */
    public SofaResponse setSerializeType(byte serializeType) {
        this.serializeType = serializeType;
        return this;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("SofaResponse[");
        sb.append("sofa-rpc exception=").append(isError).append(", ");
        sb.append("sofa-rpc errorMsg=").append(errorMsg).append(", ");
        sb.append("appResponse=").append(appResponse).append("]");
        return sb.toString();
    }
}
