package storage;

import java.io.*;
import java.net.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

 <p>
 Storage servers respond to client file access requests. The files accessible
 through a storage server are those accessible under a given directory of the
 local filesystem.
 */
public class StorageServer implements Storage, Command
{

    Skeleton<Storage> storageSkeleton;
    Skeleton<Command> commandSkeleton;

    Storage storageStub;
    Command commandStub;

    Path[] filePaths;
    public File root;

    /** Creates a storage server, given a directory on the local filesystem, and
     ports to use for the client and command interfaces.

     <p>
     The ports may have to be specified if the storage server is running
     behind a firewall, and specific ports are open.

     @param root Directory on the local filesystem. The contents of this
     directory will be accessible through the storage server.
     @param client_port Port to use for the client interface, or zero if the
     system should decide the port.
     @param command_port Port to use for the command interface, or zero if
     the system should decide the port.
     @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root, int client_port, int command_port)
    {
        if(root == null) {
            throw new NullPointerException();
        }
        this.root=root;
        storageSkeleton= new Skeleton<>(Storage.class, this, new InetSocketAddress(client_port));
        commandSkeleton= new Skeleton<>(Command.class, this, new InetSocketAddress(command_port));

        //throw new UnsupportedOperationException("not implemented");
    }

    /** Creats a storage server, given a directory on the local filesystem.

     <p>
     This constructor is equivalent to
     <code>StorageServer(root, 0, 0)</code>. The system picks the ports on
     which the interfaces are made available.

     @param root Directory on the local filesystem. The contents of this
     directory will be accessible through the storage server.
     @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
    {
        if(root == null) {
            throw new NullPointerException();
        }

        try {
            this.root=root;
            this.root=root;
            storageSkeleton = new Skeleton(Storage.class,this);
            commandSkeleton = new Skeleton(Command.class,this);

            this.filePaths = Path.list(root);

            System.out.println("Skeleton started$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /** Starts the storage server and registers it with the given naming
     server.

     @param hostname The externally-routable hostname of the local host on
     which the storage server is running. This is used to
     ensure that the stub which is provided to the naming
     server by the <code>start</code> method carries the
     externally visible hostname or address of this storage
     server.
     @param naming_server Remote interface for the naming server with which
     the storage server is to register.
     @throws UnknownHostException If a stub cannot be created for the storage
     server because a valid address has not been
     assigned.
     @throws FileNotFoundException If the directory with which the server was
     created does not exist or is in fact a
     file.
     @throws RMIException If the storage server cannot be started, or if it
     cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
            throws RMIException, UnknownHostException, FileNotFoundException
    {
        startSkeleton(hostname);
        System.out.println("Start called with hostname - " + hostname);

        System.out.println("Stub created");
        System.out.println(storageStub);
        System.out.println(commandStub);

        for(int i = 0;i < filePaths.length; i++) {
            System.out.println(this.filePaths[i].toString());
        }

        System.out.println("naming servre " + naming_server.getClass().getName());
        Path[] delete_files = naming_server.register(storageStub, commandStub, filePaths);

        for( Path path : delete_files)
        {
            System.out.println("Deleting the files");
            delete(path);
        }


        //throw new UnsupportedOperationException("not implemented");
    }

    private void startSkeleton(String hostname)
    {
        try {
            storageSkeleton.start();

            this.storageStub = Stub.create(Storage.class, this.storageSkeleton, hostname);

            commandSkeleton.start();

            this.commandStub = Stub.create(Command.class, this.commandSkeleton, hostname);

        } catch (RMIException e) {
            e.printStackTrace();
        }


    }

    /** Stops the storage server.

     <p>
     The server should not be restarted.
     */
    public void stop()
    {
        this.storageSkeleton.stop();
        this.commandSkeleton.stop();

        stopped(null);
        //throw new UnsupportedOperationException("not implemented");
    }

    /** Called when the storage server has shut down.

     @param cause The cause for the shutdown, if any, or <code>null</code> if
     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause) {}

    // The following methods are documented in Storage.java.
    @Override
    /** Returns the length of a file, in bytes.

     @param file Path to the file.
     @return The length of the file.
     @throws FileNotFoundException If the file cannot be found or the path
     refers to a directory.
     @throws RMIException If the call cannot be completed due to a network
     error.
     */
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File f = new File(this.root.getAbsolutePath() + "/" +file.toString());
        // throw new UnsupportedOperationException("not implemented");

        if( !f.exists() ||  f.isDirectory()) {
            throw new FileNotFoundException();
        }

        return f.length();
    }

    /** Reads a sequence of bytes from a file.

     @param file Path to the file.
     @param offset Offset into the file to the beginning of the sequence.
     @param length The number of bytes to be read.
     @return An array containing the bytes read. If the call succeeds, the
     number of bytes read is equal to the number of bytes requested.
     @throws IndexOutOfBoundsException If the sequence specified by
     <code>offset</code> and
     <code>length</code> is outside the
     bounds of the file, or if
     <code>length</code> is negative.
     @throws FileNotFoundException If the file cannot be found or the path
     refers to a directory.
     @throws IOException If the file read cannot be completed on the server.
     @throws RMIException If the call cannot be completed due to a network
     error.
     */
    @Override
    public synchronized byte[] read(Path file, long offset, int length)
            throws FileNotFoundException, IOException
    {
        byte[] datum = null;
        File f = new File(this.root.getAbsolutePath() + "/" +file.toString());
        // throw new UnsupportedOperationException("not implemented");

        if( !f.exists() ||  f.isDirectory()) {
            throw new FileNotFoundException();
        }

        if(length < 0 || length > (f.length()-offset) || offset < 0) {
            throw new IndexOutOfBoundsException();
        }


        RandomAccessFile data = new RandomAccessFile(f, "r");
        datum = new byte[length];
        data.seek(offset);
        data.readFully(datum,0, length);
        data.close();
        return datum;
    }

    /** Writes bytes to a file.
     @param file Path to the file.
     @param offset Offset into the file where data is to be written.
     @param data Array of bytes to be written.
     @throws IndexOutOfBoundsException If <code>offset</code> is negative.
     @throws FileNotFoundException If the file cannot be found or the path
     refers to a directory.
     @throws IOException If the file write cannot be completed on the server.
     @throws RMIException If the call cannot be completed due to a network
     error.
     */
    @Override
    public synchronized void write(Path file, long offset, byte[] data)
            throws FileNotFoundException, IOException {

        if(data == null) {
            System.out.println(" Data  = null");
            throw new NullPointerException();
        }

        System.out.println("Write request for offset - " + offset + " and data size = " + data.length);

        File f = new File(this.root.getAbsolutePath() + "/" +file.toString());

        if( !f.exists() ||  f.isDirectory()) {
            System.out.println(" File not found. ");
            throw new FileNotFoundException();
        }

        if(offset < 0) {
            System.out.println(" Wrong offset.");
            throw new IndexOutOfBoundsException();
        }

        RandomAccessFile dataFile = new RandomAccessFile(f, "rw");
        System.out.println("Writing to offset");
        dataFile.seek(offset);
        dataFile.write(data);
        dataFile.close();
        System.out.println("Write completed");
    }

    // The following methods are documented in Command.java.
    @Override
    /** Creates a file on the storage server.

     @param file Path to the file to be created. The parent directory will be
     created if it does not exist. This path may not be the root
     directory.
     @return <code>true</code> if the file is created; <code>false</code>
     if it cannot be created.
     @throws RMIException If the call cannot be completed due to a network
     error.
     */
    public synchronized boolean create(Path file)
    {
        System.out.println(" Create requested for - " + file.toString());
        if(file.pathList.size() != 1 ) {

            String dirPath  = this.root.getAbsolutePath() + file.parent().toString();
            String fileName = file.last();
            File parentDir  = new File(dirPath);

            if(!parentDir.exists()) {
                System.out.println("Creating parent directory = " + parentDir.getAbsolutePath());
                boolean pdc = parentDir.mkdirs();
                if(pdc) {
                    System.out.println("Parent created");
                }
                else {
                    System.out.println("Unable to create parent");
                }
            }
            else{
                System.out.println("Parent dir exists already");
            }

            File newFile = new File(parentDir.getAbsolutePath() + "/" + fileName);

            if(newFile.exists()) {
                return false;
            }

            try{
//                FileWriter fw = new FileWriter(newFile);
//                BufferedWriter bw = new BufferedWriter(fw);
//                bw.write("File created");
//                bw.close();
                System.out.println("Creating file " + newFile.getAbsolutePath());
                boolean newFile1 = newFile.createNewFile();
                if(newFile1) {
                    System.out.println("Created - " +  newFile.getAbsolutePath());
                }

                else {
                    System.out.println(" Not created");
                }
                return true;
            }
            //TODO: check network error
            catch (IOException e){
                e.printStackTrace();
                return false;
            }
        }

        else {
//            String fileName = file.last();
//            File f = new File(this.root.getAbsolutePath()+ "/" + fileName);
            if(file.pathList.get(0).equals("/")) {
                return false;
            }
        }

        return false;
    }


    /**
     * Delete a file or a directory and its children.
     * @param file The directory to delete.
     * @throws IOException Exception when problem occurs during deleting the directory.
     */
    private static void deleteChild(File file) throws IOException {

        for (File childFile : file.listFiles()) {

            if (childFile.isDirectory()) {
                deleteChild(childFile);
            } else {
                if (!childFile.delete()) {
                    throw new IOException();
                }
            }
        }

        if (!file.delete()) {
            throw new IOException();
        }
    }


    @Override
    public synchronized boolean delete(Path path)
    {
        System.out.println("Delete requestd for " + path.toString());
        boolean ans = false;
        System.out.println("Purging the path " + path.toString());
        if(path.toString().equals("/")) {
            return false;
        }

        System.out.println("Delete all - " + path.pathList);
        try {

            File file = path.toFile(this.root);
            System.out.println("!!!!!!!File " + file.getAbsolutePath() + " !!!!!" +file.exists() + "\n");


            if(!file.exists()) {
                return false;
            }

            if(file.delete()) {
                ans = true;
                System.out.println(file.getName() + " - deleted" + "\n");
            }
            else{
                deleteChild(file);
                ans = true;
            }


            File directory = new File(this.root.getAbsolutePath() +"/"+ path.parent().toString());

            System.out.println("Directory - " + directory.getAbsolutePath());

            while(!directory.getAbsolutePath().equals(this.root.getAbsolutePath()) && directory.isDirectory() && directory.list().length == 0) {

                System.out.println("Directory empty");
                boolean isDeleted = directory.delete();
                path.pathList.remove(path.pathList.size()-1);
                System.out.println("Removed, new path - " + path.toString());
                directory = new File(this.root.getAbsolutePath() +"/"+ path.parent().toString());
                ans = true;
            }

            System.out.println("Directory is not empty - " + directory.getAbsolutePath() + " number of files " + directory.length() );
            return ans;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }


    @Override
    public synchronized boolean copy(Path file, Storage server)
            throws RMIException, FileNotFoundException, IOException
    {
        //throw new UnsupportedOperationException("not implemented");
        return true;
    }
}
