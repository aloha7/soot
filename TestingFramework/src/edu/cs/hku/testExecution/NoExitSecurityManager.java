/*
 * NoExitSecurityManager.java
 * 
 * Copyright 2004 Christoph Csallner and Yannis Smaragdakis.
 */
package edu.cs.hku.testExecution;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.acl.Permission;


/**
 * Allows everything except System.exit.
 * 
 * @author csallner@gatech.edu (Christoph Csallner)
 */
public class NoExitSecurityManager extends SecurityManager {
	
	/* Disallow System.exit(int). */
	public void checkExit(int status) {
		throw new ExitSecurityException();
	}
  
	/* Allow everything else. */
	public void checkAccept(String host, int port) { /* empty */ }
	public void checkAccess(Thread t) { /* empty */ }
	public void checkAccess(ThreadGroup g) { /* empty */ }
	public void checkAwtEventQueueAccess() { /* empty */ }
	public void checkConnect(String host, int port, Object context) { /* empty */ }
	public void checkConnect(String host, int port) { /* empty */ }
	public void checkCreateClassLoader() { /* empty */ }
	public void checkDelete(String file) { /* empty */ }
	public void checkExec(String cmd) { /* empty */ }
	public void checkLink(String lib) { /* empty */ }
	public void checkListen(int port) { /* empty */ }
	public void checkMemberAccess(Class clazz, int which) { /* empty */ }
	public void checkMulticast(InetAddress maddr, byte ttl) { /* empty */ }
	public void checkMulticast(InetAddress maddr) { /* empty */ }
	public void checkPackageAccess(String pkg) { /* empty */ }
	public void checkPackageDefinition(String pkg) { /* empty */ }
	public void checkPermission(Permission perm, Object context) { /* empty */ }
	public void checkPermission(Permission perm) { /* empty */ }
	public void checkPrintJobAccess() { /* empty */ }
	public void checkPropertiesAccess() { /* empty */ }
	public void checkPropertyAccess(String key) { /* empty */ }
	public void checkRead(FileDescriptor fd) { /* empty */ }
	public void checkRead(String file, Object context) { /* empty */ }
	public void checkRead(String file) { /* empty */ }
	public void checkSecurityAccess(String target) { /* empty */ }
	public void checkSetFactory() { /* empty */ }
	public void checkSystemClipboardAccess() { /* empty */ }
	public boolean checkTopLevelWindow(Object window) {return true;}
	public void checkWrite(FileDescriptor fd) { /* empty */ }
	public void checkWrite(String file) { /* empty */ }
}
