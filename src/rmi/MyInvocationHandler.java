package rmi;

import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

public class MyInvocationHandler implements InvocationHandler, Serializable {

    String host;
    int port;
    public MyInvocationHandler(int port, String host) {
        this.port = port;
        this.host = host;
       
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  
        if (method.getName().equals("equals")) {
            if (proxy == null || args[0] == null) {
                return false;
            }
            return checkEquals((Proxy) proxy, (Proxy) args[0]);
        }

        
        if (method.getName().equals("hashCode")) {
            return checkHashCode(proxy);
        }

      
        if (method.getName().equals("toString")) {
            return checktoString(proxy);
        }
         
        if(isValidMethod((Proxy)proxy, method)) {
          Socket socket = new Socket(host,port);
     try{

       

        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        oos.writeObject(method.getName()); 
         oos.writeObject(args);
         oos.writeObject(method.getParameterTypes());
         
         ObjectInputStream ois  = new ObjectInputStream(socket.getInputStream());
      
        Object o=ois.readObject(); 
        
        if(o instanceof Exception){ 
            throw (Exception)((Exception) o).getCause();
        }
         socket.close(); 
         return o; 
       }catch(Exception ex){
          if(ex instanceof FileNotFoundException){ //check what type of exception is returned by the server
              throw ex; 
          }
          if(ex instanceof NullPointerException){
           throw ex;
          }
          if(ex instanceof IndexOutOfBoundsException){
              throw ex;
          }
          
          if(ex instanceof IllegalStateException){
              throw ex;
          }
          
          if(ex instanceof IllegalArgumentException){
              throw ex;
          }
         //ex.printStackTrace();
          throw new RMIException("error");
       }finally{
           try{
                
            if(!socket.isClosed()){
                 socket.close();
            }
           }catch(Exception ex){
               //throw ex;
               ex.printStackTrace();
          }
    }
        }else{
            throw new RMIException("Not a valid method");
        }
    }

    private String checktoString(Object proxy) {
        String s = "";
        MyInvocationHandler i1 = (MyInvocationHandler) Proxy.getInvocationHandler(proxy);

        Class[] proxyInterfaces = proxy.getClass().getInterfaces();
        for (Class c : proxyInterfaces) {
            s += "\n" + c.getName();
        }

        s += "\nhost = " + i1.host.toString() + "\nport = " + String.valueOf(i1.port);

        return s;
    }

    private int checkHashCode(Object proxy) {
        String s = "";
        MyInvocationHandler i1 = (MyInvocationHandler) Proxy.getInvocationHandler(proxy);

        Class[] proxyInterfaces = proxy.getClass().getInterfaces();
        for (Class c : proxyInterfaces) {
            s += "\n" + c.getName();
        }

        s += "\nhost = " + i1.host.toString() + "\nport = " + String.valueOf(i1.port);

        return s.hashCode();
    }

    private boolean checkEquals(Proxy proxy, Proxy arg) {
        Class[] proxyInterfaces = proxy.getClass().getInterfaces();
        Class[] argsInterfaces = arg.getClass().getInterfaces();
        for (Class c : proxyInterfaces) {
            if (!(Arrays.asList(argsInterfaces).contains(c))) {
                return false;
            }
        }
        MyInvocationHandler i1 = (MyInvocationHandler) Proxy.getInvocationHandler(proxy);
        MyInvocationHandler i2 = (MyInvocationHandler) Proxy.getInvocationHandler(arg);
        if ((i1.host.equals(i2.host)) && (i1.port == i2.port)) {
            return true;
        }

        return false;
    }

    /**
     * Function to check if interface being used is Remote.
     */
    private boolean isValidMethod(Proxy proxy, Method m) {
        Class[] interfaces = proxy.getClass().getInterfaces();
        for (Class iface : interfaces) {
            Method[] methods = iface.getMethods();
            for (Method method : methods) {
                for (Class<?> exception : method.getExceptionTypes()) {
                    if (exception.getName().contains("rmi.RMIException")) {
                        if (m.getName().toString().equals(method.getName().toString())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
