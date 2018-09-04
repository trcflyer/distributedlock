package com.distributed.lock.nolock;

public class Test {
	static int n = 500;
	public static void main(String[] args) {
		for (int i = 0; i < 100; i++) {
            new Thread(new Runnable() {
				
				public void run() {
					System.out.println(--n);
				}
			}).start();
        }
	}
}
