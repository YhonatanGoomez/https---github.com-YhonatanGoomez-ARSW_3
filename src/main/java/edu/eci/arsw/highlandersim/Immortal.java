package edu.eci.arsw.highlandersim;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Immortal extends Thread {

    private ImmortalUpdateReportCallback updateCallback = null;

    private final ReentrantLock battlesSync = new ReentrantLock();

    private int health;

    private int defaultDamageValue;

    private final List<Immortal> immortalsPopulation;

    private final String name;

    private final Random r = new Random(System.currentTimeMillis());

    private boolean isPaused = false;

    private boolean alive;

     private final Lock lock = new ReentrantLock();

    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue,
            ImmortalUpdateReportCallback ucb) {
        super(name);
        this.updateCallback = ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue = defaultDamageValue;
        this.alive = true;
    }

    public void run() {

        while (this.alive) {

            if (isPaused) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Immortal im;
                // asigna el valor retornado por inmortalsPopulation
                // tendremos el indice "mio" en la lista de inmortales
                int myIndex = immortalsPopulation.indexOf(this);

                // genera un número aleatorio entre 0 hasta la longitud de la lista
                // inmortalsPopulation
                // estos son los indices de donde están cada inmortal
                int nextFighterIndex = r.nextInt(immortalsPopulation.size());
                System.out.println(immortalsPopulation);
                // avoid self-fight
                if (nextFighterIndex == myIndex) {
                    nextFighterIndex = ((nextFighterIndex + 1) % immortalsPopulation.size());
                    
                }
                
                im = immortalsPopulation.get(nextFighterIndex);


                if (this.getHealth() > 0 && im.alive) {
                    this.fight(im);
                }

                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                try {
                    Thread.sleep(4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    /*
     * public void stopInmortal() {
     * interrupt();
     * }
     */

    public synchronized void resumeImmortal() {
        isPaused = false;
        notify();
    }

    public synchronized void pauseImmortal() {
        isPaused = true;
    }

    public void fight(Immortal i2) {

        synchronized (i2.updateCallback) {
            if (i2.getHealth() > 0) {
                i2.changeHealth(i2.getHealth() - defaultDamageValue);
                this.health += defaultDamageValue;
                updateCallback.processReport("Fight: " + this + " vs " + i2 + "\n");
            } else {
                i2.alive = false;
                updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
      
            }
        }
    }

    public void fight2(Immortal i2) {
        boolean myLock = false;
        boolean otherLock = false;

        try {
            myLock = lock.tryLock();
            otherLock = i2.lock.tryLock();

            if (myLock && otherLock) {
                if (i2.getHealth() > 0) {
                    i2.changeHealth(i2.getHealth() - defaultDamageValue);
                    this.health += defaultDamageValue;
                    updateCallback.processReport("Fight: " + this + " vs " + i2 + "\n");
                } else {
                    i2.alive = false;
                    updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
                }
            }
        } finally {
            if (myLock) {
                lock.unlock();
            }
            if (otherLock) {
                i2.lock.unlock();
            }
        }
    }

    public void changeHealth(int v) {

        health = v;

    }

    public int getHealth() {
        return health;
    }

    @Override
    public String toString() {

        return name + "[" + health + "]";
    }

}
