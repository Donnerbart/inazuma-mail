package main;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import queue.MailStorageQueue;

import com.couchbase.client.CouchbaseClient;


import database.ConnectionManager;

public class Main
{
	public static void main(String[] args)
	{
		final long runtime = Config.RUNTIME;
		final CouchbaseClient client = ConnectionManager.getConnection();
		
		// Startup mail storage threads
		MailStorageQueue mailStorageQueue = new MailStorageQueue(client, Config.STORAGE_THREADS, Config.MAX_RETRIES);
		
		// Configure thread pool
		ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(10);
		for (int i = 0; i < Config.CREATION_JOBS; ++i)
		{
			threadPool.submit(new MailCreationJob(threadPool, mailStorageQueue));
		}
		threadPool.submit(new MailQueueSizeJob(threadPool, mailStorageQueue));
		
		// Create mails
		System.out.println("Create mails for " + runtime + " ms...");
		try
		{
			Thread.sleep(runtime);
		}
		catch (InterruptedException e)
		{
		}
		
		// Shutdown of thread pool
		System.out.println("Shutting down thread pool...");
		threadPool.shutdown();
		try
		{
			threadPool.awaitTermination(60, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		System.out.println("Done!\n");
		
		// Shutdown storage threads
		System.out.println("Shutting down storage threads...");
		mailStorageQueue.shutdown();
		mailStorageQueue.awaitShutdown();
		System.out.println("Done!\n");

		// Statistics
		(new MailCheckJob()).run();
		System.out.println();
		
		// Shutdown of connection manager
		System.out.println("Shutting down ConnectionManager...");
		ConnectionManager.shutdown();
		System.out.println("Done!\n");
	}
}