package net.i2p.router;

import java.io.*;

public class RouterLaunch {
    public static void main(String args[]) {
        try {
            System.setOut(new PrintStream(new FileOutputStream("wrapper.log")));
        } catch (IOException ioe) {
            ioe.printStackTrace();
	}
	Router.main(args);
    }
}