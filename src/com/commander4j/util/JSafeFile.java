package com.commander4j.util;

/**
 * The safe files mark a folder as one sftpTransfer is allowed to work in.
 * They are created by hand or with the buttons beside the folder fields, one
 * in the local folder and one in the remote folder, and nothing is
 * transferred or removed unless both are present -
 * a share which has not mounted, or a mistyped path, has no safe file.
 * The files themselves are never sent, fetched, backed up or deleted.
 */
public class JSafeFile
{

	public static final String local = "local.safe";
	public static final String remote = "remote.safe";

	// The Create buttons on the Put and Get tabs copy the safe files from here.
	public static final String templateFolder = "./safety";

	public static boolean isSafeFile(String filename)
	{
		return filename.equalsIgnoreCase(local) || filename.equalsIgnoreCase(remote);
	}

}
