package de.unikassel.util.callables;

import com.googlecode.mobilityrpc.session.MobilityContext;

public class DestroyRunnable implements Runnable {
    @Override
    public void run() {
        MobilityContext.getCurrentSession().release();
    }
}
