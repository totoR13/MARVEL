package com.lody.virtual.helper.ipcbus;

import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;

import com.lody.virtual.custom.models.CustomException;


/**
 * @author Lody
 */
public class TransformBinder extends Binder {

    private ServerInterface serverInterface;
    private Object server;

    public TransformBinder(ServerInterface serverInterface, Object server) {
        this.serverInterface = serverInterface;
        this.server = server;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            reply.writeString(serverInterface.getInterfaceName());
            return true;
        }
        IPCMethod method = serverInterface.getIPCMethod(code);
        if (method != null) {
            try {
                method.handleTransact(server, data, reply);
            } catch (Throwable e) {
                Object cause = e.getCause().getCause();
                if (cause instanceof CustomException.SignatureException) {
                    throw (CustomException.SignatureException) cause;
                }

                e.printStackTrace();
            }
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }
}
