/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

/**
 *
 * @author rajatpandey
 */
 class invocationHandler1 implements InvocationHandler, Serializable{
   
    
    private InetSocketAddress address;
     
     invocationHandler1(InetSocketAddress address) {
        this.address=address;
    }
    
    

    @Override
    public  Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ObjectOutputStream output = null;
        ObjectInputStream input = null;
        if(method.getName().equals("equals")){ //if equal method is called on the created stub
            return compareStub(proxy, args[0]); //compares the stub
        }
        
        if(method.getName().equals("hashCode")){//if chashcode computation is called on the stub
            int hashValue= Integer.parseInt(computeRemoteString(proxy, true));
            return hashValue;
        }
        
        if(method.getName().equals("toString")){//if to string conversion is called on the stub
            String hashValue= computeRemoteString(proxy, false);
            return hashValue;
        }
        Socket soc = null;
        
       try{    
       
           Object stubRes = null;  
        soc= new Socket(this.address.getAddress(), this.address.getPort());// create client side socket
        output=new ObjectOutputStream(soc.getOutputStream());//create output stream
        output.flush();
        output.writeObject(method.getName()); //send the method name on the output stream
         output.writeObject(args);//send the arguments on the output stream
         output.writeObject(method.getParameterTypes());
         
        
        input= new ObjectInputStream(soc.getInputStream()); //create input stream
        stubRes=input.readObject(); //read the response from the server passed by skeleton
        if(stubRes instanceof Exception){ //check the type of exception returned by the server
            throw (Exception)((Exception) stubRes).getCause();
        }
         soc.close(); //close the socket
         return stubRes; 
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
         //ex.printStackTrace();
          throw new RMIException("error");
       }finally{
           try{
            if(!soc.isClosed()){
                 soc.close();
            }
           }catch(Exception ex){
               ex.printStackTrace();
           }
       }
        
      
    }

    private boolean compareStub(Object proxy, Object arg) {
         if(arg == null){
             return false;
         }
        Class[] interfac=proxy.getClass().getInterfaces(); //all the interfaces of the source stubs are fetched
        Class[] argInt= arg.getClass().getInterfaces();//all the interfaces of the target stubs(to which comparision has to be made) are fetched
        invocationHandler1 source=(invocationHandler1)Proxy.getInvocationHandler(proxy); 
        invocationHandler1 target= (invocationHandler1)Proxy.getInvocationHandler(arg);
       
        for(Class compInt : interfac){
            for(Class targetInt: argInt){
                if(compInt.getName().equals(targetInt.getName())){// first criteria is name must be same of both source and target
                    if(source.address.getPort() == target.address.getPort()){//source and target port must be same
                        if(source.address.getHostName().equals(target.address.getHostName())){//source and destination host name must be same
                            return true;
                        }
                    }
                    
                }
            }
        }
         return false;
    }
   
    // this is a common method for computing hashcode and toString
    //if hashcode is required then isHashCodeRequired parameter would be true, else it would be false and only returns string
    private String computeRemoteString(Object proxy, boolean isHashCodeRequired) {
         String str="";
        for(Class source: proxy.getClass().getInterfaces()){
            str=str + source.getName();
            str=" ";
        } 
        invocationHandler1 source=(invocationHandler1)Proxy.getInvocationHandler(proxy); 
        str=str + source.address.getHostName() + " " +source.address.getPort();
        if(!isHashCodeRequired){
        return str; //
        }else{
           return String.valueOf(str.hashCode());
        }
    }
    
}
