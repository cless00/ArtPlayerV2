package darack.com.player;

public class MyFiles implements Comparable<MyFiles> {
	private String path;
	private String fileName;
	private boolean dir;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean isDir() {
		return dir;
	}

	public void setDir(boolean dir) {
		this.dir = dir;
	}

	@Override
	public int compareTo(MyFiles another) {
		// TODO Auto-generated method stub
		return path.compareTo(another.path);
	}
}
