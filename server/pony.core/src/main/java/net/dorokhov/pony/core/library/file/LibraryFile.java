package net.dorokhov.pony.core.library.file;

public interface LibraryFile extends LibraryNode {

	public String getMimeType();

	public String getChecksum() throws Exception;

}
