package naming;

public class ReadWriteLock{

    public int readers       = 0;
    public int writers       = 0;
    public int writeRequests = 0;

    public synchronized void lockRead() throws InterruptedException{
        while(writers > 0 || writeRequests > 0){
            wait();
        }
        readers++;
    }

    public synchronized void unlockRead(){
        //System.out.println("r439039403940349305dvjzsd,vz d,vmz d v,mcv,xdm,/....................................... ");
        readers--;
        notifyAll();
    }

    public synchronized void lockWrite() throws InterruptedException{
        writeRequests++;

        while(readers > 0 || writers > 0){
            wait();
        }
        writeRequests--;
        writers++;
    }

    public synchronized void unlockWrite() throws InterruptedException{
        writers--;
        notifyAll();
    }
}
