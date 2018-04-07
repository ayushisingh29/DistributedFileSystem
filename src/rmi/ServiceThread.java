package rmi;

import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;

class ServiceThread<T> implements Runnable {

    private Socket clientSocket;
    private T server;
    ServiceThread(Socket clientSocket, T server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        String methodName = null;
        Object[] args = null;

        try {
            if(!this.clientSocket.isClosed()) {

                if( ! this.clientSocket.isOutputShutdown()) {
                    OutputStream clientOutputStream = this.clientSocket.getOutputStream();
                    oos = new ObjectOutputStream(clientOutputStream);
                }

                if(! this.clientSocket.isInputShutdown()) {
                    InputStream clientInputStream = this.clientSocket.getInputStream();
                    ois = new ObjectInputStream(clientInputStream);
                }

                if(oos != null && ois != null) {
                    Method m = null;

                    methodName = (String) ois.readObject();
                    if( methodName == null) {
                        throw new RMIException("Method null received");
                    }

                    args = (Object[]) ois.readObject();
                    Class<?>[] paramTypes = null;
                    for(Method meth: server.getClass().getMethods()){
                        if(meth.getName().equals(methodName)) {
                            paramTypes = meth.getParameterTypes();
                        }
                    }

                    m = server.getClass().getMethod(methodName, paramTypes);

                    if(args != null && args.length > 0) {
                        Object o = m.invoke(server,args);
                        oos.writeObject(o);
                        oos.flush();

                    }
                    else{
                        Object o = m.invoke(server,(Object[]) null);
                        oos.writeObject(o);
                        oos.flush();
                    }
                    clientSocket.getOutputStream().flush();
                }
            }
        }
        catch(EOFException ex) {
        }
        catch (Exception ex) {
            try {
                if(oos != null) {
                    ex.toString();
                    oos.writeObject(ex);
                }
            } catch (IOException e) {}
        }
    }
}
