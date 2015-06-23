package darack.com.player;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class Mp3TagReader {

	public static Music ReadMp3Info(String path) {
		Music music = null;

		try {

			File file = new File(path);
			AudioFile audioFile = AudioFileIO.read(file);
			Tag tag = audioFile.getTag();
			
			if(tag == null){
				return new Music("", file.getName(), "", null, 0, path);
			}

			String title = tag.getFirst(FieldKey.TITLE);
			String artist = tag.getFirst(FieldKey.ARTIST);
			String album = tag.getFirst(FieldKey.ALBUM);

			Drawable albumArt = null;
			Artwork artwork = tag.getFirstArtwork();
			if (artwork != null) {
				byte[] data = artwork.getBinaryData();
				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
						data.length);// 디코드
				albumArt = new BitmapDrawable(bitmap);
			}

			music = new Music(artist, title, album, albumArt, 0, path);

		} catch (CannotReadException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TagException e) {
			e.printStackTrace();
		} catch (ReadOnlyFileException e) {
			e.printStackTrace();
		} catch (InvalidAudioFrameException e) {
			e.printStackTrace();
		}

		return music;
	}

}
