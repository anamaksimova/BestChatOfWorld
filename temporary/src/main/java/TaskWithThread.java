public class TaskWithThread {
    static Object mon = new Object();
    static volatile char currentLetter = 'A';
    public static void main(String[] args) {

        Thread t1 = new Thread( new Runnable(){

            @Override
            public void run() {
                for (int i = 0; i <5 ; i++) {
                    synchronized (mon){
                    while (currentLetter !='A'){
                        try {
                            mon.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.print("A");
                    currentLetter = 'B';
                    mon.notifyAll();
                }
                }
            }
        });

        Thread t2 = new Thread( new Runnable(){

            @Override
            public void run() {
                for (int i = 0; i < 5; i++) {
                    synchronized (mon){
                    while (currentLetter !='B'){
                        try {
                            mon.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.print("B");
                    currentLetter = 'C';
                    mon.notifyAll();
                }
                }


            }
        });

        Thread t3 = new Thread( new Runnable(){

            @Override
            public void run() {
                for (int i = 0; i <5 ; i++) {
                    synchronized (mon){
                    while (currentLetter !='C'){
                        try {
                            mon.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.print("C ");
                    currentLetter = 'A';
                    mon.notifyAll();
                }
                }

            }
        });

        t1.start();
        t2.start();
        t3.start();
        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("end");
    }
}
