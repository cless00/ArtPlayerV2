package darack.com.player;

import android.graphics.drawable.Drawable;

/**
 * 뮤직에 대한 정보를 저장하는 클래스 가수, 제목, 앨범명, 앨범아트
 * 
 * @author Scott
 * 
 */
public class Music {
	private String gasu;
	private String jemok;
	private String albumName;
	private long artIndex;
	private Drawable albumArt;
	private String uri;
	private boolean selected;

	public Music(String _gasu, String _jemok, String _albumName, Drawable _albumArt, int _artIndex, String _uri) {
		this.gasu = _gasu;
		this.jemok = _jemok;
		this.albumName = _albumName;
		this.albumArt = _albumArt;
		this.artIndex = _artIndex;
		this.uri = _uri;
		this.selected = false;
	}

	public Drawable getAlbumArt() {
		return albumArt;
	}

	public String getGasu() {
		return gasu;
	}

	public String getJemok() {
		return jemok;
	}

	public String getAlbumName() {
		return albumName;
	}

	public long getArtIndex() {
		return artIndex;
	}
	
	public String getUri(){
		return uri;
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
//		return super.toString();
		return this.gasu + " | " + this.jemok + " | " + this.albumName + " | " + this.uri;
	}
}
